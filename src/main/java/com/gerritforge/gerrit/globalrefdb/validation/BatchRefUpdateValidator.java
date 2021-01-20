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

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.OutOfSyncException;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement.EnforcePolicy;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.BatchRefUpdate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.transport.ReceiveCommand;

public class BatchRefUpdateValidator extends RefUpdateValidator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static interface Factory {
    BatchRefUpdateValidator create(String projectName, RefDatabase refDb);
  }

  public interface BatchValidationWrapper {
    void apply(BatchRefUpdate batchRefUpdate, NoParameterVoidFunction arg) throws IOException;
  }

  @Inject
  public BatchRefUpdateValidator(
      SharedRefDatabaseWrapper sharedRefDb,
      ValidationMetrics validationMetrics,
      SharedRefEnforcement refEnforcement,
      LockWrapper.Factory lockWrapperFactory,
      ProjectsFilter projectsFilter,
      @Assisted String projectName,
      @Assisted RefDatabase refDb,
      @Assisted Set<String> ignoredRefs) {
    super(
        sharedRefDb,
        validationMetrics,
        refEnforcement,
        lockWrapperFactory,
        projectsFilter,
        projectName,
        refDb,
        ignoredRefs);
  }

  public void executeBatchUpdateWithValidation(
      BatchRefUpdate batchRefUpdate, NoParameterVoidFunction batchRefUpdateFunction)
      throws IOException {
    if (refEnforcement.getPolicy(projectName) == EnforcePolicy.IGNORED
        || !isGlobalProject(projectName)) {
      batchRefUpdateFunction.invoke();
      return;
    }

    try {
      doExecuteBatchUpdate(batchRefUpdate, batchRefUpdateFunction);
    } catch (IOException e) {
      logger.atWarning().withCause(e).log(
          "Failed to execute Batch Update on project %s", projectName);
      if (refEnforcement.getPolicy(projectName) == EnforcePolicy.REQUIRED) {
        throw e;
      }
    }
  }

  private void doExecuteBatchUpdate(
      BatchRefUpdate batchRefUpdate, NoParameterVoidFunction delegateUpdate) throws IOException {

    List<ReceiveCommand> commands = batchRefUpdate.getCommands();
    if (commands.isEmpty()) {
      return;
    }

    List<RefPair> refsToUpdate = getRefsPairs(commands).collect(Collectors.toList());
    List<RefPair> refsFailures =
        refsToUpdate.stream().filter(RefPair::hasFailed).collect(Collectors.toList());
    if (!refsFailures.isEmpty()) {
      String allFailuresMessage =
          refsFailures.stream()
              .map(refPair -> String.format("Failed to fetch ref %s", refPair.compareRef.getName()))
              .collect(Collectors.joining(", "));
      Exception firstFailureException = refsFailures.get(0).exception;

      logger.atSevere().withCause(firstFailureException).log(allFailuresMessage);
      throw new IOException(allFailuresMessage, firstFailureException);
    }

    try (CloseableSet<AutoCloseable> locks = new CloseableSet<>()) {
      refsToUpdate = compareAndGetLatestLocalRefs(refsToUpdate, locks);
      delegateUpdate.invoke();
      updateSharedRefDb(batchRefUpdate.getCommands().stream(), refsToUpdate);
    } catch (OutOfSyncException e) {
      List<ReceiveCommand> receiveCommands = batchRefUpdate.getCommands();
      logger.atWarning().withCause(e).log(
          String.format(
              "Batch ref-update failing because node is out of sync with the shared ref-db. Set all commands Result to LOCK_FAILURE [%d]",
              receiveCommands.size()));
      receiveCommands.forEach((command) -> command.setResult(ReceiveCommand.Result.LOCK_FAILURE));
    }
  }

  private void updateSharedRefDb(Stream<ReceiveCommand> commandStream, List<RefPair> refsToUpdate)
      throws IOException {
    if (commandStream
        .filter(cmd -> cmd.getResult() != ReceiveCommand.Result.OK)
        .findFirst()
        .isPresent()) {
      return;
    }

    for (RefPair refPair : refsToUpdate) {
      updateSharedDbOrThrowExceptionFor(refPair);
    }
  }

  private Stream<RefPair> getRefsPairs(List<ReceiveCommand> receivedCommands) {
    return receivedCommands.stream().map(this::getRefPairForCommand);
  }

  private RefPair getRefPairForCommand(ReceiveCommand command) {
    try {
      switch (command.getType()) {
        case CREATE:
          return new RefPair(nullRef(command.getRefName()), getNewRef(command));

        case UPDATE:
        case UPDATE_NONFASTFORWARD:
          return new RefPair(getCurrentRef(command.getRefName()), getNewRef(command));

        case DELETE:
          return new RefPair(getCurrentRef(command.getRefName()), ObjectId.zeroId());

        default:
          return new RefPair(
              command.getRef(),
              new IllegalArgumentException("Unsupported command type " + command.getType()));
      }
    } catch (IOException e) {
      return new RefPair(command.getRef(), e);
    }
  }

  private ObjectId getNewRef(ReceiveCommand command) {
    return command.getNewId();
  }

  private List<RefPair> compareAndGetLatestLocalRefs(
      List<RefPair> refsToUpdate, CloseableSet<AutoCloseable> locks) throws IOException {
    List<RefPair> latestRefsToUpdate = new ArrayList<>();
    for (RefPair refPair : refsToUpdate) {
      latestRefsToUpdate.add(compareAndGetLatestLocalRef(refPair, locks));
    }
    return latestRefsToUpdate;
  }

  private static final Ref nullRef(String refName) {
    return new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, refName, ObjectId.zeroId());
  }
}
