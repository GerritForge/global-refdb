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

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

/**
 * Base interface providing the contract to log shared ref-db operations, such as project update,
 * project deletion and lock acquisitions.
 */
public interface SharedRefLogger {

  /**
   * Log the update of {@param currRef} in project {@param project} to ref {@param newRefValue}
   *
   * @param project the project the update is for
   * @param currRef the ref being updated
   * @param newRefValue the new value of the ref being updated
   */
  void logRefUpdate(String project, Ref currRef, ObjectId newRefValue);

  /**
   * Log the update of {@param currRef}, pointed to by {@param refName}, in project {@param project}
   * to ref {@param newRefValue}
   *
   * @param project the project the update is for
   * @param refName the name of the ref being updatex
   * @param currRef the current value of the ref being updated
   * @param newRefValue the new value of the ref being updated
   * @param <T> Type of the {@param currRef} and the {@param newRefValue}
   */
  <T> void logRefUpdate(String project, String refName, T currRef, T newRefValue);

  /**
   * Log the deletion of {@param project} from the shared ref-db
   *
   * @param project the project being deleted
   */
  void logProjectDelete(String project);

  /**
   * Log the acquisition of a lock for the {@param refName} of {@param project}
   *
   * @param project the project containing the ref
   * @param refName the name of the ref the lock is acquired for
   */
  void logLockAcquisition(String project, String refName);

  /**
   * Log the releasing of a previously acquired lock for the {@param refName} of {@param project}
   *
   * @param project the project containing the ref
   * @param refName the name of the ref the lock is being releaed for
   */
  void logLockRelease(String project, String refName);
}
