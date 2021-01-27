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

import static com.google.common.truth.Truth.assertThat;

import org.eclipse.jgit.lib.Ref;
import org.junit.Test;

public class NoopSharedRefDatabaseTest implements RefFixture {

  private Ref sampleRef = newRef(A_TEST_REF_NAME, AN_OBJECT_ID_1);
  private NoopSharedRefDatabase objectUnderTest = new NoopSharedRefDatabase();

  @Test
  public void isUpToDateShouldAlwaysReturnTrue() {
    assertThat(objectUnderTest.isUpToDate(A_TEST_PROJECT_NAME_KEY, sampleRef)).isTrue();
  }

  @Test
  public void compareAndPutShouldAlwaysReturnTrue() {
    assertThat(objectUnderTest.compareAndPut(A_TEST_PROJECT_NAME_KEY, sampleRef, AN_OBJECT_ID_2))
        .isTrue();
  }

  @Test
  public void compareAndPutWithRefNameShouldAlwaysReturnTrue() {
    assertThat(
            objectUnderTest.compareAndPut(
                A_TEST_PROJECT_NAME_KEY, A_TEST_REF_NAME, AN_OBJECT_ID_1, AN_OBJECT_ID_2))
        .isTrue();
  }

  @Test
  public void existsShouldAlwaysReturnFalse() {
    assertThat(objectUnderTest.exists(A_TEST_PROJECT_NAME_KEY, A_TEST_REF_NAME)).isFalse();
  }
}
