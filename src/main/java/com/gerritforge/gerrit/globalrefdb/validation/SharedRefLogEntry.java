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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.common.GitPerson;

/**
 * Rather than allowing free form logging of operations, this class provides the types of possible
 * entries being logged.
 */
public class SharedRefLogEntry {

  /**
   * Types of log entries
   *
   * <p>{@link #UPDATE_REF} {@link #DELETE_REF} {@link #DELETE_PROJECT} {@link #LOCK_ACQUIRE} {@link
   * #LOCK_RELEASE}
   */
  public enum Type {
    UPDATE_REF,
    DELETE_REF,
    DELETE_PROJECT,
    LOCK_ACQUIRE,
    LOCK_RELEASE
  }

  public String projectName;
  public Type type;

  /** A ref update log entry */
  public static class UpdateRef extends SharedRefLogEntry {

    public String refName;
    public String oldId;
    public String newId;
    public GitPerson committer;
    public String comment;

    /**
     * Constructs a new {@code SharedRefLogEntry.UpdateRef} to represent the logging of an update
     * ref operation.
     *
     * @param projectName the name of the project being updated
     * @param refName the name of the ref being updated
     * @param oldId the old value of the ref
     * @param newId the new value of the ref
     * @param committer the committer of the ref update. Nullable to allow logging of blob updates.
     * @param comment the comment associated to this commit. Nullabloe to allow logging of blob
     *     updates.
     */
    UpdateRef(
        String projectName,
        String refName,
        String oldId,
        String newId,
        @Nullable GitPerson committer,
        @Nullable String comment) {
      this.type = Type.UPDATE_REF;
      this.projectName = projectName;
      this.refName = refName;
      this.oldId = oldId;
      this.newId = newId;
      this.committer = committer;
      this.comment = comment;
    }
  }

  /** A delete project log entry */
  public static class DeleteProject extends SharedRefLogEntry {

    /**
     * Constructs a new {@code SharedRefLogEntry.DeleteProject} to represent the logging of a
     * project deletion operation.
     *
     * @param projectName the name of the project being deleted from the shared ref-db.
     */
    DeleteProject(String projectName) {
      this.type = Type.DELETE_PROJECT;
      this.projectName = projectName;
    }
  }

  /** A delete ref log entry */
  public static class DeleteRef extends SharedRefLogEntry {

    public String refName;
    public String oldId;

    /**
     * Constructs a new {@code SharedRefLogEntry.DeleteRef} to represent the logging of a ref
     * deletion operation.
     *
     * @param projectName the name of the project containing the ref being deleted.
     * @param refName the ref being deleted.
     * @param oldId the id value of the ref before being deleted.
     */
    DeleteRef(String projectName, String refName, String oldId) {
      this.type = Type.DELETE_REF;
      this.projectName = projectName;
      this.refName = refName;
      this.oldId = oldId;
    }
  }

  /** A lock acquisition log entry */
  public static class LockAcquire extends SharedRefLogEntry {

    public String refName;

    /**
     * Constructs a new {@code SharedRefLogEntry.LockAcquire} to represent the logging of a lock
     * acquisition
     *
     * @param projectName the name of the project containing the ref being locked.
     * @param refName the ref being locked.
     */
    LockAcquire(String projectName, String refName) {
      this.type = Type.LOCK_ACQUIRE;
      this.projectName = projectName;
      this.refName = refName;
    }
  }

  /** A lock release log entry */
  public static class LockRelease extends SharedRefLogEntry {

    public String refName;

    /**
     * Constructs a new {@code SharedRefLogEntry.LockAcquire} to represent the release of a lock
     * previously acquired.
     *
     * @param projectName the name of the project containing the ref being released.
     * @param refName the ref being released.
     */
    LockRelease(String projectName, String refName) {
      this.type = Type.LOCK_RELEASE;
      this.projectName = projectName;
      this.refName = refName;
    }
  }
}
