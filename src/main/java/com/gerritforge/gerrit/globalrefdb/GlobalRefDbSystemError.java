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
 * {@code GlobalRefDbSystemError} is an exception that can be thrown when interacting with the
 * global-refdb to represent any error in performing operations such as creating or deleting a ref.
 */
public class GlobalRefDbSystemError extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new {@code GlobalRefDbSystemError} with the specified detail message and cause.
   *
   * @param msg the detail message
   * @param cause the cause
   */
  public GlobalRefDbSystemError(String msg, Exception cause) {
    super(msg, cause);
  }
}
