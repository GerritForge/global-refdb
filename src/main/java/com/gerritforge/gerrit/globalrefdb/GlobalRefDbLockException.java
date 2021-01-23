// Copyright (C) 2019 GerritForge Ltd
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

package com.gerritforge.gerrit.globalrefdb;

/**
 * {@code GlobalRefDbLockException} is an exception that can be thrown when interacting with the
 * global-refdb to represent the inability to lock or acquire a resource.
 */
public class GlobalRefDbLockException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new {@code GlobalRefDbLockException} with the specified project, refName and
   * cause.
   *
   * @param project the project containing refName
   * @param refName the specific ref for which the locking failed
   * @param cause the cause of the locking failure
   */
  public GlobalRefDbLockException(String project, String refName, Exception cause) {
    super(String.format("Unable to lock ref %s on project %s", refName, project), cause);
  }
}
