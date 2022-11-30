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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.RepositoryCaseMismatchException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.NavigableSet;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements a {@link GitRepositoryManager} interface with the intent of managing repository access
 * and repository creation through instances of {@link SharedRefDbRepository}, which can therefore
 * validate operations on the repository itself against the global refdb
 */
@Singleton
public class SharedRefDbGitRepositoryManager implements GitRepositoryManager {
  /**
   * This value must be used for the named injection binding provided by libModules that want to
   * define refs that should not be validated against the global refdb. For example:
   *
   * <pre>
   *         bind(new TypeLiteral&lt;ImmutableSet&lt;String&gt;&gt;() {})
   *         .annotatedWith(Names.named(SharedRefDbGitRepositoryManager.IGNORED_REFS))
   *         .toInstance(
   *             ImmutableSet.of(
   *                 "refs/foo/bar",
   *                 "refs/foo/baz"));
   * </pre>
   */
  public static final String IGNORED_REFS = "ignored_refs";

  public static final String LOCAL_DISK_REPOSITORY_MANAGER = "local_disk_repository_manager";

  private final GitRepositoryManager gitRepositoryManager;
  private final SharedRefDbRepository.Factory sharedRefDbRepoFactory;

  @Inject(optional = true)
  @Named(IGNORED_REFS)
  private ImmutableSet<String> ignoredRefs = ImmutableSet.of();

  /**
   * Constructs a {@code SharedRefDbGitRepositoryManager} that can create and open Git repositories
   * by wrapping them in a {@code SharedRefDbRepository} object, so that operations on them can be
   * validated against a global refdb
   *
   * @param sharedRefDbRepoFactory a factory providing a {@link SharedRefDbRepository} instance
   * @param localDiskRepositoryManager an instance to manage repositories stored on the local file
   *     system
   */
  @Inject
  public SharedRefDbGitRepositoryManager(
      SharedRefDbRepository.Factory sharedRefDbRepoFactory,
      @Named(LOCAL_DISK_REPOSITORY_MANAGER) GitRepositoryManager localDiskRepositoryManager) {
    this.sharedRefDbRepoFactory = sharedRefDbRepoFactory;
    this.gitRepositoryManager = localDiskRepositoryManager;
  }

  /**
   * Get (or open) a {@link Repository} by name.
   *
   * @param name the repository name, relative to the base directory.
   * @return the repository instance
   * @throws RepositoryNotFoundException the name does not denote an existing repository.
   * @throws IOException the name cannot be read as a repository.
   */
  @Override
  public Repository openRepository(Project.NameKey name)
      throws RepositoryNotFoundException, IOException {
    return wrap(name, gitRepositoryManager.openRepository(name));
  }

  /**
   * Create (and open) a {@link Repository} by name.
   *
   * @param name the repository name, relative to the base directory.
   * @return the repository instance
   * @throws RepositoryCaseMismatchException the name collides with an existing repository name, but
   *     only in case of a character within the name.
   * @throws RepositoryNotFoundException the name is invalid.
   * @throws IOException the repository cannot be created.
   */
  @Override
  public Repository createRepository(Project.NameKey name)
      throws RepositoryCaseMismatchException, RepositoryNotFoundException, IOException {
    return wrap(name, gitRepositoryManager.createRepository(name));
  }

  @Override
  public NavigableSet<NameKey> list() {
    return gitRepositoryManager.list();
  }

  @Override
  public Boolean canPerformGC() {
    return gitRepositoryManager.canPerformGC();
  }

  @Override
  public Status getRepositoryStatus(NameKey name) {
    return gitRepositoryManager.getRepositoryStatus(name);
  }

  private Repository wrap(Project.NameKey projectName, Repository projectRepo) {
    return sharedRefDbRepoFactory.create(projectName.get(), projectRepo, ignoredRefs);
  }
}
