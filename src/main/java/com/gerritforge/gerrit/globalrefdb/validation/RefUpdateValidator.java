// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.globalrefdb.validation;

import com.gerritforge.gerrit.globalrefdb.GlobalRefDbLockException;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.CustomSharedRefEnforcementByProject;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.DefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.OutOfSyncException;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedDbSplitBrainException;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedLockException;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement.EnforcePolicy;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.HashMap;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;

/** Enables the detection of out-of-sync by validating ref updates against the global refdb. */
public class RefUpdateValidator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected final SharedRefDatabaseWrapper sharedRefDb;
  protected final ValidationMetrics validationMetrics;

  protected final String projectName;
  private final LockWrapper.Factory lockWrapperFactory;
  protected final RefDatabase refDb;
  protected final SharedRefEnforcement refEnforcement;
  protected final ProjectsFilter projectsFilter;
  private final ImmutableSet<String> ignoredRefs;

  /** {@code RefUpdateValidator} Factory for Guice assisted injection. */
  public interface Factory {
    RefUpdateValidator create(
        String projectName, RefDatabase refDb, ImmutableSet<String> ignoredRefs);
  }

  public interface ExceptionThrowingSupplier<T, E extends Exception> {
    T create() throws E;
  }

  public interface RefValidationWrapper {
    RefUpdate.Result apply(NoParameterFunction<RefUpdate.Result> arg, RefUpdate refUpdate)
        throws IOException;
  }

  public interface NoParameterFunction<T> {
    T invoke() throws IOException;
  }

  public interface NoParameterVoidFunction {
    void invoke() throws IOException;
  }

  /**
   * Constructs a {@code RefUpdateValidator} able to check the validity of ref-updates against a
   * global refdb before execution.
   *
   * @param sharedRefDb an instance of the global refdb to check for out-of-sync refs.
   * @param validationMetrics to update validation results, such as split-brains.
   * @param refEnforcement Specific ref enforcements for this project. Either a {@link
   *     CustomSharedRefEnforcementByProject} when custom policies are provided via configuration
   *     file or a {@link DefaultSharedRefEnforcement} for defaults.
   * @param lockWrapperFactory factory providing a {@link LockWrapper}
   * @param projectsFilter filter to match whether the project being updated should be validated
   *     against global refdb
   * @param projectName the name of the project being updated.
   * @param refDb for ref operations
   * @param ignoredRefs A set of refs for which updates should not be checked against the shared
   *     ref-db
   */
  @Inject
  public RefUpdateValidator(
      SharedRefDatabaseWrapper sharedRefDb,
      ValidationMetrics validationMetrics,
      SharedRefEnforcement refEnforcement,
      LockWrapper.Factory lockWrapperFactory,
      ProjectsFilter projectsFilter,
      @Assisted String projectName,
      @Assisted RefDatabase refDb,
      @Assisted ImmutableSet<String> ignoredRefs) {
    this.sharedRefDb = sharedRefDb;
    this.validationMetrics = validationMetrics;
    this.lockWrapperFactory = lockWrapperFactory;
    this.refDb = refDb;
    this.ignoredRefs = ignoredRefs;
    this.projectName = projectName;
    this.refEnforcement = refEnforcement;
    this.projectsFilter = projectsFilter;
  }

  /**
   * Checks whether the provided refUpdate should be validated first against the shared ref-db. If
   * not it just execute the provided refUpdateFunction. If it should be validated against the
   * global refdb then it does so by executing the {@link
   * RefUpdateValidator#doExecuteRefUpdate(RefUpdate, NoParameterFunction)} first. Upon success the
   * refUpdate is returned, upon failure split brain metrics are incremented and a {@link
   * SharedDbSplitBrainException} is thrown.
   *
   * <p>Validation is performed when either of these condition is true
   *
   * <ul>
   *   <li>The ref being updated is not to be ignored ({@link
   *       RefUpdateValidator#isRefToBeIgnored(String)})
   *   <li>The project being updated is a global project ({@link
   *       RefUpdateValidator#isGlobalProject(String)}
   *   <li>The enforcement policy for the project being updated is {@link EnforcePolicy#IGNORED}
   * </ul>
   *
   * @param refUpdate the refUpdate command
   * @param refUpdateFunction the refUpdate function to execute after validation
   * @return the result of the update, or "null" in case a split brain was detected but the policy
   *     enforcement was not REQUIRED
   * @throws IOException Execution of ref update failed
   */
  public RefUpdate.Result executeRefUpdate(
      RefUpdate refUpdate,
      NoParameterFunction<RefUpdate.Result> refUpdateFunction,
      NoParameterFunction<RefUpdate.Result> rollbackFunction)
      throws IOException {
    if (isRefToBeIgnored(refUpdate.getName())
        || !isGlobalProject(projectName)
        || refEnforcement.getPolicy(projectName) == EnforcePolicy.IGNORED) {
      return refUpdateFunction.invoke();
    }

    try {
      return doExecuteRefUpdate(refUpdate, refUpdateFunction, rollbackFunction);
    } catch (SharedDbSplitBrainException e) {
      validationMetrics.incrementSplitBrain();

      logger.atWarning().withCause(e).log(
          "Unable to execute ref-update on project=%s ref=%s",
          projectName, refUpdate.getRef().getName());
      if (refEnforcement.getPolicy(projectName) == EnforcePolicy.REQUIRED) {
        throw e;
      }
    }
    return null;
  }

  private Boolean isRefToBeIgnored(String refName) {
    Boolean isRefToBeIgnored = ignoredRefs.contains(refName);
    logger.atFine().log("Is project version update? " + isRefToBeIgnored);
    return isRefToBeIgnored;
  }

  private <T extends Throwable> void softFailBasedOnEnforcement(T e, EnforcePolicy policy)
      throws T {
    logger.atWarning().withCause(e).log(
        String.format(
            "Failure while running with policy enforcement %s. Error message: %s",
            policy, e.getMessage()));
    if (policy == EnforcePolicy.REQUIRED) {
      throw e;
    }
  }

  protected Boolean isGlobalProject(String projectName) {
    Boolean isGlobalProject = projectsFilter.matches(projectName);
    logger.atFine().log("Is global project? " + isGlobalProject);
    return isGlobalProject;
  }

  protected RefUpdate.Result doExecuteRefUpdate(
      RefUpdate refUpdate,
      NoParameterFunction<Result> refUpdateFunction,
      NoParameterFunction<Result> rollbackFunction)
      throws IOException {
    try (CloseableSet<AutoCloseable> locks = new CloseableSet<>()) {
      RefPair refPairForUpdate = newRefPairFrom(refUpdate);
      compareAndGetLatestLocalRef(refPairForUpdate, locks);
      RefUpdate.Result result = refUpdateFunction.invoke();
      try {
        if (isSuccessful(result)) {
          updateSharedDbOrThrowExceptionFor(refPairForUpdate);
        }
      } catch (GlobalRefDbSystemError | GlobalRefDbLockException e) {
        logger.atSevere().withCause(e).log(
            String.format("Local node is out of sync with ref-db: %s", e.getMessage()));
        result = rollbackFunction.invoke();
        if (isSuccessful(result)) {
          result = RefUpdate.Result.LOCK_FAILURE;
        }
      }
      return result;
    } catch (OutOfSyncException e) {
      logger.atWarning().withCause(e).log(
          String.format("Local node is out of sync with ref-db: %s", e.getMessage()));

      return RefUpdate.Result.LOCK_FAILURE;
    }
  }

  protected void updateSharedDbOrThrowExceptionFor(RefPair refPair) throws IOException {
    // We are not checking refs that should be ignored
    final EnforcePolicy refEnforcementPolicy =
        refEnforcement.getPolicy(projectName, refPair.getName());
    if (refEnforcementPolicy == EnforcePolicy.IGNORED) return;

    String errorMessage =
        String.format(
            "Not able to persist the data in Zookeeper for project '%s' and ref '%s',"
                + "the cluster is now in Split Brain since the commit has been "
                + "persisted locally but not in SharedRef the value %s",
            projectName, refPair.getName(), refPair.putValue);
    boolean succeeded;
    try {
      succeeded =
          sharedRefDb.compareAndPut(
              Project.nameKey(projectName), refPair.compareRef, refPair.putValue);
    } catch (GlobalRefDbSystemError e) {
      logger.atWarning().withCause(e).log(
          "Not able to persist the data in Zookeeper for project '{}' and ref '{}', message: {}",
          projectName,
          refPair.getName(),
          e.getMessage());
      throw e;
    }

    if (!succeeded) {
      throw new SharedDbSplitBrainException(errorMessage);
    }
  }

  protected RefPair compareAndGetLatestLocalRef(RefPair refPair, CloseableSet<AutoCloseable> locks)
      throws SharedLockException, OutOfSyncException, IOException {
    String refName = refPair.getName();
    EnforcePolicy refEnforcementPolicy = refEnforcement.getPolicy(projectName, refName);
    if (refEnforcementPolicy == EnforcePolicy.IGNORED) {
      return refPair;
    }

    locks.addResourceIfNotExist(
        String.format("%s-%s", projectName, refName),
        () ->
            lockWrapperFactory.create(
                projectName, refName, sharedRefDb.lockRef(Project.nameKey(projectName), refName)));

    RefPair latestRefPair = getLatestLocalRef(refPair);
    if (sharedRefDb.isUpToDate(Project.nameKey(projectName), latestRefPair.compareRef)) {
      return latestRefPair;
    }

    if (isNullRef(latestRefPair.compareRef)
        || sharedRefDb.exists(Project.nameKey(projectName), refName)) {
      validationMetrics.incrementSplitBrainPrevention();

      softFailBasedOnEnforcement(
          new OutOfSyncException(projectName, latestRefPair.compareRef), refEnforcementPolicy);
    }

    return latestRefPair;
  }

  private boolean isNullRef(Ref ref) {
    return ref.getObjectId().equals(ObjectId.zeroId());
  }

  private RefPair getLatestLocalRef(RefPair refPair) throws IOException {
    Ref latestRef = refDb.exactRef(refPair.getName());
    return new RefPair(
        latestRef == null ? nullRef(refPair.getName()) : latestRef, refPair.putValue);
  }

  private Ref nullRef(String name) {
    return new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, name, ObjectId.zeroId());
  }

  protected boolean isSuccessful(RefUpdate.Result result) {
    switch (result) {
      case NEW:
      case FORCED:
      case FAST_FORWARD:
      case NO_CHANGE:
      case RENAMED:
        return true;

      case REJECTED_OTHER_REASON:
      case REJECTED_MISSING_OBJECT:
      case REJECTED_CURRENT_BRANCH:
      case NOT_ATTEMPTED:
      case LOCK_FAILURE:
      case IO_FAILURE:
      case REJECTED:
      default:
        return false;
    }
  }

  protected RefPair newRefPairFrom(RefUpdate refUpdate) throws IOException {
    return new RefPair(getCurrentRef(refUpdate.getName()), refUpdate.getNewObjectId());
  }

  protected Ref getCurrentRef(String refName) throws IOException {
    return MoreObjects.firstNonNull(refDb.findRef(refName), nullRef(refName));
  }

  public static class CloseableSet<T extends AutoCloseable> implements AutoCloseable {
    private final HashMap<String, AutoCloseable> elements;

    public CloseableSet() {
      this(new HashMap<>());
    }

    public CloseableSet(HashMap<String, AutoCloseable> elements) {
      this.elements = elements;
    }

    public void addResourceIfNotExist(
        String key, ExceptionThrowingSupplier<T, SharedLockException> resourceFactory)
        throws SharedLockException {
      if (!elements.containsKey(key)) {
        elements.put(key, resourceFactory.create());
      }
    }

    @Override
    public void close() {
      elements.values().stream()
          .forEach(
              closeable -> {
                try {
                  closeable.close();
                } catch (Exception closingException) {
                  logger.atSevere().withCause(closingException).log(
                      "Exception trying to release resource %s, "
                          + "the locked resources won't be accessible in all cluster unless"
                          + " the lock is removed from ZK manually",
                      closeable);
                }
              });
    }
  }
}
