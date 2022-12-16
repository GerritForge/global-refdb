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
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.LocalDiskRepositoryManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import java.util.Collections;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SharedRefDbGitRepositoryManagerTest implements RefFixture {
  private static final ImmutableSet<String> IGNORED_REFS =
      ImmutableSet.of("refs/heads/foo", "refs/heads/bar");

  @Mock LocalDiskRepositoryManager localDiskRepositoryManagerMock;

  @Mock SharedRefDbRepository.Factory sharedRefDbRepositoryFactoryMock;

  @Mock Repository repositoryMock;

  @Mock SharedRefDbRepository sharedRefDbRepositoryMock;

  SharedRefDbGitRepositoryManager msRepoMgr;

  @Override
  public String testBranch() {
    return "foo";
  }

  @Before
  public void setUp() throws Exception {
    doReturn(sharedRefDbRepositoryMock)
        .when(sharedRefDbRepositoryFactoryMock)
        .create(A_TEST_PROJECT_NAME, repositoryMock, ImmutableSet.of());
    msRepoMgr = getInjector(ImmutableSet.of()).getInstance(SharedRefDbGitRepositoryManager.class);
  }

  @Test
  public void openRepositoryShouldCreateSharedRefDbRepositoryWrapper() throws Exception {
    doReturn(repositoryMock)
        .when(localDiskRepositoryManagerMock)
        .openRepository(A_TEST_PROJECT_NAME_KEY);

    msRepoMgr.openRepository(A_TEST_PROJECT_NAME_KEY);

    verifyThatSharedRefDbRepositoryWrapperHasBeenCreated(ImmutableSet.of());
  }

  @Test
  public void openRepositoryShouldCreateSharedRefDbRepositoryWrapperWithIgnoredRefs()
      throws Exception {
    doReturn(repositoryMock)
        .when(localDiskRepositoryManagerMock)
        .openRepository(A_TEST_PROJECT_NAME_KEY);

    getInjector(IGNORED_REFS)
        .getInstance(SharedRefDbGitRepositoryManager.class)
        .openRepository(A_TEST_PROJECT_NAME_KEY);

    verifyThatSharedRefDbRepositoryWrapperHasBeenCreated(IGNORED_REFS);
  }

  @Test
  public void createRepositoryShouldCreateSharedRefDbRepositoryWrapper() throws Exception {
    doReturn(repositoryMock)
        .when(localDiskRepositoryManagerMock)
        .createRepository(A_TEST_PROJECT_NAME_KEY);

    msRepoMgr.createRepository(A_TEST_PROJECT_NAME_KEY);

    verifyThatSharedRefDbRepositoryWrapperHasBeenCreated(ImmutableSet.of());
  }

  @Test
  public void createRepositoryShouldCreateSharedRefDbRepositoryWrapperWithIgnoredRefs()
      throws Exception {
    doReturn(repositoryMock)
        .when(localDiskRepositoryManagerMock)
        .createRepository(A_TEST_PROJECT_NAME_KEY);

    getInjector(IGNORED_REFS)
        .getInstance(SharedRefDbGitRepositoryManager.class)
        .createRepository(A_TEST_PROJECT_NAME_KEY);

    verifyThatSharedRefDbRepositoryWrapperHasBeenCreated(IGNORED_REFS);
  }

  @Test
  public void canPerformGCShouldDelegateToLocalDiskRepositoryManager() {
    doReturn(true).when(localDiskRepositoryManagerMock).canPerformGC();

    assertThat(msRepoMgr.canPerformGC()).isTrue();
    verify(localDiskRepositoryManagerMock).canPerformGC();
  }

  @Test
  public void getRepositoryStatusShouldDelegateToLocalDiskRepositoryManager() {
    doReturn(GitRepositoryManager.Status.ACTIVE)
        .when(localDiskRepositoryManagerMock)
        .getRepositoryStatus(A_TEST_PROJECT_NAME_KEY);

    assertThat(msRepoMgr.getRepositoryStatus(A_TEST_PROJECT_NAME_KEY))
        .isEqualTo(GitRepositoryManager.Status.ACTIVE);
    verify(localDiskRepositoryManagerMock).getRepositoryStatus(A_TEST_PROJECT_NAME_KEY);
  }

  @Test
  public void listShouldDelegateToLocalDiskRepositoryManager() {
    doReturn(Collections.emptySortedSet()).when(localDiskRepositoryManagerMock).list();

    assertThat(msRepoMgr.list()).isEqualTo(Collections.emptySortedSet());
    verify(localDiskRepositoryManagerMock).list();
  }

  private Injector getInjector(ImmutableSet<String> ignoredRefs) {
    return Guice.createInjector(
        new AbstractModule() {

          @Override
          protected void configure() {
            bind(new TypeLiteral<ImmutableSet<String>>() {})
                .annotatedWith(Names.named(SharedRefDbGitRepositoryManager.IGNORED_REFS))
                .toInstance(ignoredRefs);
            bind(SharedRefDbRepository.Factory.class).toInstance(sharedRefDbRepositoryFactoryMock);
            bind(LocalDiskRepositoryManager.class).toInstance(localDiskRepositoryManagerMock);
          }
        });
  }

  private void verifyThatSharedRefDbRepositoryWrapperHasBeenCreated(
      ImmutableSet<String> ignoredRefs) {
    verify(sharedRefDbRepositoryFactoryMock)
        .create(A_TEST_PROJECT_NAME, repositoryMock, ignoredRefs);
  }
}
