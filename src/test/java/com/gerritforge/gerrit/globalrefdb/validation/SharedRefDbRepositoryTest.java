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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.RefFixture;
import java.io.IOException;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SharedRefDbRepositoryTest implements RefFixture {

  @Mock SharedRefDbRefDatabase.Factory sharedRefDbRefDbFactory;
  @Mock SharedRefDbRefDatabase sharedRefDb;
  @Mock RefDatabase genericRefDb;
  @Mock ObjectDatabase objectDatabase;

  @Mock SharedRefDbRefUpdate sharedRefDbRefUpdate;

  @Mock Repository repository;

  private final String PROJECT_NAME = "ProjectName";
  private final String REFS_HEADS_MASTER = "refs/heads/master";

  @Override
  public String testBranch() {
    return null;
  }

  private void setMockitoCommon() {
    doReturn(true).when(repository).isBare();
    doReturn(genericRefDb).when(repository).getRefDatabase();
    doReturn(sharedRefDb).when(sharedRefDbRefDbFactory).create(PROJECT_NAME, genericRefDb);
  }

  @Test
  public void shouldInvokeSharedRefDbRefDbFactoryCreate() {
    setMockitoCommon();
    try (SharedRefDbRepository sharedRefDbRepository =
        new SharedRefDbRepository(sharedRefDbRefDbFactory, PROJECT_NAME, repository)) {

      sharedRefDbRepository.getRefDatabase();
      verify(sharedRefDbRefDbFactory).create(PROJECT_NAME, genericRefDb);
    }
  }

  @Test
  public void shouldInvokeNewUpdateInSharedRefDbRefDatabase() throws IOException {
    setMockitoCommon();
    try (SharedRefDbRepository sharedRefDbRepository =
        new SharedRefDbRepository(sharedRefDbRefDbFactory, PROJECT_NAME, repository)) {
      sharedRefDbRepository.getRefDatabase().newUpdate(REFS_HEADS_MASTER, false);

      verify(sharedRefDb).newUpdate(REFS_HEADS_MASTER, false);
    }
  }

  @Test
  public void shouldInvokeUpdateInSharedRefDbRefUpdate() throws IOException {
    setMockitoCommon();
    doReturn(Result.NEW).when(sharedRefDbRefUpdate).update();
    doReturn(sharedRefDbRefUpdate).when(sharedRefDb).newUpdate(REFS_HEADS_MASTER, false);

    try (SharedRefDbRepository sharedRefDbRepository =
        new SharedRefDbRepository(sharedRefDbRefDbFactory, PROJECT_NAME, repository)) {

      Result updateResult =
          sharedRefDbRepository.getRefDatabase().newUpdate(REFS_HEADS_MASTER, false).update();

      verify(sharedRefDbRefUpdate).update();
      assertThat(updateResult).isEqualTo(Result.NEW);
    }
  }
}
