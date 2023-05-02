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

import com.google.inject.ImplementedBy;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

@ImplementedBy(Log4jSharedRefLogger.class)
public interface SharedRefLogger {

  /**
   * Log the update of currRef in project project to ref newRefValue
   *
   * @param project the project the update is for
   * @param currRef the ref being updated
   * @param newRefValue the new value of the ref being updated
   */
  void logRefUpdate(String project, Ref currRef, ObjectId newRefValue);

  /**
   * Log the update of currRef, pointed to by refName, in project 'project' to ref 'newRefValue'
   *
   * @param project the project the update is for
   * @param refName the name of the ref being updatex
   * @param currRef the current value of the ref being updated
   * @param newRefValue the new value of the ref being updated
   * @param <T> Type of the 'currRef' and the 'newRefValue'
   */
  <T> void logRefUpdate(String project, String refName, T currRef, T newRefValue);

  /**
   * Log the deletion of 'project' from the global refdb
   *
   * @param project the project being deleted
   */
  void logProjectDelete(String project);

  /**
   * Log the acquisition of a lock for the 'refName' of 'project'
   *
   * @param project the project containing the ref
   * @param refName the name of the ref the lock is acquired for
   */
  void logLockAcquisition(String project, String refName);

  /**
   * Log the releasing of a previously acquired lock for the 'refName' of 'project'
   *
   * @param project the project containing the ref
   * @param refName the name of the ref the lock is being releaed for
   */
  void logLockRelease(String project, String refName);
}
