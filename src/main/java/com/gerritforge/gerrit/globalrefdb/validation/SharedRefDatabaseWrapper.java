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

import com.gerritforge.gerrit.globalrefdb.GlobalRefDatabase;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbLockException;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.NoopSharedRefDatabase;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.metrics.Timer0.Context;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

/**
 * Wraps an instance of {@link GlobalRefDatabase} provided as {@link DynamicItem} via a Guice
 * binding. Such instance is bound optionally and, in case no explicit binding is registered a
 * {@link NoopSharedRefDatabase} instance is wrapped instead.
 */
@Singleton
public class SharedRefDatabaseWrapper implements GlobalRefDatabase {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final GlobalRefDatabase NOOP_REFDB = new NoopSharedRefDatabase();

  @Inject(optional = true)
  private DynamicItem<GlobalRefDatabase> sharedRefDbDynamicItem;

  private final SharedRefLogger sharedRefLogger;
  private final SharedRefDBMetrics metrics;

  /**
   * Constructs a {@code SharedRefDatabaseWrapper} wrapping an optional {@link GlobalRefDatabase},
   * which might have been bound by consumers of this library.
   *
   * @param sharedRefLogger logger of shared ref-db operations.
   */
  @Inject
  public SharedRefDatabaseWrapper(
      DynamicItem<GlobalRefDatabase> sharedRefDbDynamicItem,
      SharedRefLogger sharedRefLogger,
      SharedRefDBMetrics metrics) {
    this.sharedRefLogger = sharedRefLogger;
    this.metrics = metrics;
    this.sharedRefDbDynamicItem = sharedRefDbDynamicItem;
  }

  @Override
  public boolean isUpToDate(Project.NameKey project, Ref ref) throws GlobalRefDbLockException {
    try (Context context = metrics.startIsUpToDateExecutionTime()) {
      return sharedRefDb().isUpToDate(project, ref);
    }
  }

  /** {@inheritDoc}. The operation is logged upon success. */
  @Override
  public boolean compareAndPut(Project.NameKey project, Ref currRef, ObjectId newRefValue)
      throws GlobalRefDbSystemError {
    try (Context context = metrics.startCompareAndPutExecutionTime()) {
      boolean succeeded = sharedRefDb().compareAndPut(project, currRef, newRefValue);
      if (succeeded) {
        sharedRefLogger.logRefUpdate(project.get(), currRef, newRefValue);
      }
      return succeeded;
    }
  }

  /** {@inheritDoc} the operation is logged upon success. */
  @Override
  public <T> boolean compareAndPut(Project.NameKey project, String refName, T currValue, T newValue)
      throws GlobalRefDbSystemError {
    try (Context context = metrics.startCompareAndPutExecutionTime()) {
      boolean succeeded = sharedRefDb().compareAndPut(project, refName, currValue, newValue);
      if (succeeded) {
        sharedRefLogger.logRefUpdate(project.get(), refName, currValue, newValue);
      }
      return succeeded;
    }
  }

  /** {@inheritDoc}. The operation is logged. */
  @Override
  public AutoCloseable lockRef(Project.NameKey project, String refName)
      throws GlobalRefDbLockException {
    try (Context context = metrics.startLockRefExecutionTime()) {
      AutoCloseable locker = sharedRefDb().lockRef(project, refName);
      sharedRefLogger.logLockAcquisition(project.get(), refName);
      return locker;
    }
  }

  @Override
  public boolean exists(Project.NameKey project, String refName) {
    try (Context context = metrics.startExistsExecutionTime()) {
      return sharedRefDb().exists(project, refName);
    }
  }

  /** {@inheritDoc}. The operation is logged. */
  @Override
  public void remove(Project.NameKey project) throws GlobalRefDbSystemError {
    try (Context context = metrics.startRemoveExecutionTime()) {
      sharedRefDb().remove(project);
      sharedRefLogger.logProjectDelete(project.get());
    }
  }

  @Override
  public <T> Optional<T> get(Project.NameKey nameKey, String s, Class<T> clazz)
      throws GlobalRefDbSystemError {
    try (Context context = metrics.startGetExecutionTime()) {
      return sharedRefDb().get(nameKey, s, clazz);
    }
  }

  private GlobalRefDatabase sharedRefDb() {
    if (sharedRefDbDynamicItem == null) {
      log.atWarning().log("DynamicItem<GlobalRefDatabase> has not been injected");
    }

    return Optional.ofNullable(sharedRefDbDynamicItem)
        .map(di -> di.get())
        .orElseGet(
            () -> {
              log.atWarning().log("Using NOOP_REFDB");
              return NOOP_REFDB;
            });
  }
}
