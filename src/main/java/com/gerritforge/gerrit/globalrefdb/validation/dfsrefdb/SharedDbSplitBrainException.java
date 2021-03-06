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

import java.io.IOException;

/** Split-brain detected when trying to update a ref */
public class SharedDbSplitBrainException extends IOException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a {@code SharedDbSplitBrainException} with a message
   *
   * @param message details about the detected split brain
   */
  public SharedDbSplitBrainException(String message) {
    super(message);
  }

  /**
   * Constructs a {@code SharedDbSplitBrainException} with a 'message' and a 'cause'
   *
   * @param message details about the detected split brain
   * @param cause the cause of the split brain detection
   */
  public SharedDbSplitBrainException(String message, Throwable cause) {
    super(message, cause);
  }
}
