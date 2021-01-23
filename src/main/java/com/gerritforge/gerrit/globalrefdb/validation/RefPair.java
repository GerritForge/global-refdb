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
 * A convenience object encompassing the old (current) and the new (candidate) value of a {@link
 * Ref}. This is used to snapshot the current status of a ref update so that validations against the
 * shared ref-db are unaffected by changes on the {@link org.eclipse.jgit.lib.RefDatabase}.
 */
public class RefPair {
  public final Ref compareRef;
  public final ObjectId putValue;
  public final Exception exception;

  /**
   * Constructs a {@code RefPair} with the provided old and new ref values. The oldRef value is
   * required not to be null, in which case an {@link IllegalArgumentException} is thrown.
   *
   * @param oldRef the old ref
   * @param newRefValue the new (candidate) value for this ref.
   */
  RefPair(Ref oldRef, ObjectId newRefValue) {
    if (oldRef == null) {
      throw new IllegalArgumentException("Required not-null ref in RefPair");
    }
    this.compareRef = oldRef;
    this.putValue = newRefValue;
    this.exception = null;
  }

  /**
   * Constructs a {@code RefPair} with the current ref and an Exception indicating why the new ref
   * value failed being retrieved.
   *
   * @param newRef
   * @param e
   */
  RefPair(Ref newRef, Exception e) {
    this.compareRef = newRef;
    this.exception = e;
    this.putValue = ObjectId.zeroId();
  }

  /**
   * Getter for the current ref
   *
   * @return the current ref value
   */
  public String getName() {
    return compareRef.getName();
  }

  /**
   * Whether the new value failed being retrieved
   *
   * @return true when this refPair has failed, false otherwise.
   */
  public boolean hasFailed() {
    return exception != null;
  }
}
