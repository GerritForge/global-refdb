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

package com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb;

import com.gerritforge.gerrit.globalrefdb.GlobalRefDatabase;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbLockException;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.google.gerrit.entities.Project;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

/**
 * Default implementation of the {@link GlobalRefDatabase} interface. accepts any refs without
 * checking for consistency.
 *
 * <p>This is useful for setting up a test environment and allows multi-site library to be installed
 * independently from any additional libModules or the existence of a specific Ref-DB installation.
 */
public class NoopSharedRefDatabase implements GlobalRefDatabase {

  /**
   * Project/ref is always considered up-to-date
   *
   * @param project project name of the ref
   * @param ref to be checked against global ref-db
   * @return true
   * @throws GlobalRefDbLockException Never thrown by this implementation
   */
  @Override
  public boolean isUpToDate(Project.NameKey project, Ref ref) throws GlobalRefDbLockException {
    return true;
  }

  /**
   * Put is always considered successful
   *
   * @param project project name of the ref
   * @param currRef old value to compare to.
   * @param newRefValue new reference to store.
   * @return true
   * @throws GlobalRefDbSystemError Never thrown by this implementation
   */
  @Override
  public boolean compareAndPut(Project.NameKey project, Ref currRef, ObjectId newRefValue)
      throws GlobalRefDbSystemError {
    return true;
  }

  /**
   * Put is always considered successful
   *
   * @param project project name of the ref.
   * @param refName to store the value for.
   * @param currValue current expected value in the DB.
   * @param newValue new value to store.
   * @param <T> Type of the current and new value
   * @return true
   * @throws GlobalRefDbSystemError Never thrown by this implementation
   */
  @Override
  public <T> boolean compareAndPut(Project.NameKey project, String refName, T currValue, T newValue)
      throws GlobalRefDbSystemError {
    return true;
  }

  /**
   * Locking the ref does nothing, but return an dummy {@link java.io.Closeable}.
   *
   * @param project project name
   * @param refName ref to lock
   * @return a dummy {@link java.io.Closeable}.
   * @throws GlobalRefDbLockException Never thrown by this implementation
   */
  @Override
  public AutoCloseable lockRef(Project.NameKey project, String refName)
      throws GlobalRefDbLockException {
    return () -> {};
  }

  /**
   * project/refs are always assumed to be new as to never be considered out-of-sync
   *
   * @param project project containing the ref
   * @param refName the name of the ref
   * @return false
   */
  @Override
  public boolean exists(Project.NameKey project, String refName) {
    return false;
  }

  /**
   * Does nothing, the project is always considered to have been removed correctly from the shared
   * ref-db.
   *
   * @param project project name
   * @throws GlobalRefDbSystemError Never thrown by this implementation
   */
  @Override
  public void remove(Project.NameKey project) throws GlobalRefDbSystemError {}

  /**
   * Always return an empty object as to never be considered existing in the global refdb.
   *
   * @param project project name
   * @param refName reference name
   * @param clazz wanted Class of the returned value
   * @return {@link Optional#empty()}
   * @throws GlobalRefDbSystemError Never thrown by this implementation
   */
  @Override
  public <T> Optional<T> get(Project.NameKey project, String refName, Class<T> clazz)
      throws GlobalRefDbSystemError {
    return Optional.empty();
  }
}
