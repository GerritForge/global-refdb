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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.RefFixture;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SharedRefDbRefDatabaseTest implements RefFixture {

  @Rule public TestName nameRule = new TestName();

  @Mock SharedRefDbRefUpdate.Factory refUpdateFactoryMock;
  @Mock SharedRefDbBatchRefUpdate.Factory refBatchUpdateFactoryMock;

  @Mock RefDatabase refDatabaseMock;

  @Mock RefUpdate refUpdateMock;

  @Override
  public String testBranch() {
    return "branch_" + nameRule.getMethodName();
  }

  @Test
  public void newUpdateShouldCreateSharedRefDbRefUpdate() throws Exception {
    String refName = aBranchRef();
    SharedRefDbRefDatabase sharedRefDbRefDb =
        new SharedRefDbRefDatabase(
            refUpdateFactoryMock, refBatchUpdateFactoryMock, A_TEST_PROJECT_NAME, refDatabaseMock);
    doReturn(refUpdateMock).when(refDatabaseMock).newUpdate(refName, false);

    sharedRefDbRefDb.newUpdate(refName, false);

    verify(refUpdateFactoryMock).create(A_TEST_PROJECT_NAME, refUpdateMock, refDatabaseMock);
  }
}
