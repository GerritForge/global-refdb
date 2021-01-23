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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.eclipse.jgit.lib.BatchRefUpdate;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.PushCertificate;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.util.time.ProposedTimestamp;

/**
 * Batch of reference updates to be applied to a repository prior having checked the validity of
 * those operations against the shared ref-db.
 */
public class SharedRefDbBatchRefUpdate extends BatchRefUpdate {

  private final BatchRefUpdate batchRefUpdate;
  private final String project;
  private final BatchRefUpdateValidator.Factory batchRefValidatorFactory;
  private final RefDatabase refDb;
  private final ImmutableSet<String> ignoredRefs;

  /** {@code SharedRefDbBatchRefUpdate} Factory for Guice assisted injection. */
  public interface Factory {
    SharedRefDbBatchRefUpdate create(
        String project, RefDatabase refDb, ImmutableSet<String> ignoredRefs);
  }

  /**
   * Constructs an instance of {@code SharedRefDbBatchRefUpdate} able to validate ref updates via a
   * {@link BatchRefUpdateValidator}. It is basically a thin wrapper around {@link BatchRefUpdate},
   * where the validity of the update operation is checked first against the shared ref-db instance.
   *
   * @param batchRefValidatorFactory Factory to provide {@link BatchRefUpdateValidator}
   * @param project the project this batch update is for
   * @param refDb a ref-database instance to create the underlying {@link BatchRefUpdate}
   * @param ignoredRefs a set of refs that should not be validated against the shared ref-db
   */
  @Inject
  public SharedRefDbBatchRefUpdate(
      BatchRefUpdateValidator.Factory batchRefValidatorFactory,
      @Assisted String project,
      @Assisted RefDatabase refDb,
      @Assisted ImmutableSet<String> ignoredRefs) {
    super(refDb);
    this.refDb = refDb;
    this.project = project;
    this.batchRefUpdate = refDb.newBatchUpdate();
    this.batchRefValidatorFactory = batchRefValidatorFactory;
    this.ignoredRefs = ignoredRefs;
  }

  @Override
  public int hashCode() {
    return batchRefUpdate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return batchRefUpdate.equals(obj);
  }

  @Override
  public boolean isAllowNonFastForwards() {
    return batchRefUpdate.isAllowNonFastForwards();
  }

  @Override
  public BatchRefUpdate setAllowNonFastForwards(boolean allow) {
    return batchRefUpdate.setAllowNonFastForwards(allow);
  }

  @Override
  public PersonIdent getRefLogIdent() {
    return batchRefUpdate.getRefLogIdent();
  }

  @Override
  public BatchRefUpdate setRefLogIdent(PersonIdent pi) {
    return batchRefUpdate.setRefLogIdent(pi);
  }

  @Override
  public String getRefLogMessage() {
    return batchRefUpdate.getRefLogMessage();
  }

  @Override
  public boolean isRefLogIncludingResult() {
    return batchRefUpdate.isRefLogIncludingResult();
  }

  @Override
  public BatchRefUpdate setRefLogMessage(String msg, boolean appendStatus) {
    return batchRefUpdate.setRefLogMessage(msg, appendStatus);
  }

  @Override
  public BatchRefUpdate disableRefLog() {
    return batchRefUpdate.disableRefLog();
  }

  @Override
  public BatchRefUpdate setForceRefLog(boolean force) {
    return batchRefUpdate.setForceRefLog(force);
  }

  @Override
  public boolean isRefLogDisabled() {
    return batchRefUpdate.isRefLogDisabled();
  }

  @Override
  public BatchRefUpdate setAtomic(boolean atomic) {
    return batchRefUpdate.setAtomic(atomic);
  }

  @Override
  public boolean isAtomic() {
    return batchRefUpdate.isAtomic();
  }

  @Override
  public void setPushCertificate(PushCertificate cert) {
    batchRefUpdate.setPushCertificate(cert);
  }

  @Override
  public List<ReceiveCommand> getCommands() {
    return batchRefUpdate.getCommands();
  }

  @Override
  public BatchRefUpdate addCommand(ReceiveCommand cmd) {
    return batchRefUpdate.addCommand(cmd);
  }

  @Override
  public BatchRefUpdate addCommand(ReceiveCommand... cmd) {
    return batchRefUpdate.addCommand(cmd);
  }

  @Override
  public BatchRefUpdate addCommand(Collection<ReceiveCommand> cmd) {
    return batchRefUpdate.addCommand(cmd);
  }

  @Override
  public List<String> getPushOptions() {
    return batchRefUpdate.getPushOptions();
  }

  @Override
  public List<ProposedTimestamp> getProposedTimestamps() {
    return batchRefUpdate.getProposedTimestamps();
  }

  @Override
  public BatchRefUpdate addProposedTimestamp(ProposedTimestamp ts) {
    return batchRefUpdate.addProposedTimestamp(ts);
  }

  /**
   * Execute this batch update by delegating to the underlying {@link BatchRefUpdate} after checking
   * first the validity of the operation via a {@link BatchRefUpdateValidator}.
   *
   * @param walk a RevWalk to parse tags in case the storage system wants to store them pre-peeled,
   *     a common performance optimization.
   * @param monitor progress monitor to receive update status on.
   * @param options a list of option strings; set null to execute without
   * @throws IOException the database is unable to accept the update. This could be either caused by
   *     one of the underlying update command or by the batch ref validator proving that the current
   *     ref-db is not up-to-date with the shared ref-db
   * @see BatchRefUpdate
   * @see BatchRefUpdateValidator#executeBatchUpdateWithValidation(BatchRefUpdate,
   *     RefUpdateValidator.NoParameterVoidFunction)
   */
  @Override
  public void execute(RevWalk walk, ProgressMonitor monitor, List<String> options)
      throws IOException {
    batchRefValidatorFactory
        .create(project, refDb, ignoredRefs)
        .executeBatchUpdateWithValidation(
            batchRefUpdate, () -> batchRefUpdate.execute(walk, monitor, options));
  }

  /**
   * Execute this batch update by delegating to the underlying {@link BatchRefUpdate} after checking
   * first the validity of the operation via a {@link BatchRefUpdateValidator}.
   *
   * @param walk a RevWalk to parse tags in case the storage system wants to store them pre-peeled,
   *     a common performance optimization.
   * @param monitor progress monitor to receive update status on.
   * @throws IOException the database is unable to accept the update. This could be either caused by
   *     one of the underlying update command or by the batch ref validator proving that the current
   *     ref-db is not up-to-date with the shared ref-db
   * @see BatchRefUpdate
   * @see BatchRefUpdateValidator#executeBatchUpdateWithValidation(BatchRefUpdate,
   *     RefUpdateValidator.NoParameterVoidFunction)
   */
  @Override
  public void execute(RevWalk walk, ProgressMonitor monitor) throws IOException {
    batchRefValidatorFactory
        .create(project, refDb, ignoredRefs)
        .executeBatchUpdateWithValidation(
            batchRefUpdate, () -> batchRefUpdate.execute(walk, monitor));
  }

  @Override
  public String toString() {
    return batchRefUpdate.toString();
  }
}
