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
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.LocalDiskRepositoryManager;
import com.google.gerrit.server.git.RepositoryCaseMismatchException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.SortedSet;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements a {@link GitRepositoryManager} interface with the intent of managing repository access
 * and repository creation through instances of {@link SharedRefDbRepository}, which can therefore
 * validate operations on the repository itself against the shared ref-db
 */
@Singleton
public class SharedRefDbGitRepositoryManager implements GitRepositoryManager {
  public static final String IGNORED_REFS = "ignored_refs";
  private final GitRepositoryManager gitRepositoryManager;
  private final SharedRefDbRepository.Factory sharedRefDbRepoFactory;

  @Inject(optional = true)
  @Named(IGNORED_REFS)
  private ImmutableSet<String> ignoredRefs = ImmutableSet.of();

  /**
   * Constructs a {@code SharedRefDbGitRepositoryManager} that can create and open Git repositories
   * by wrapping them in a {@code SharedRefDbRepository} object, so that operations on them can be
   * validated against a shared ref-db
   *
   * @param sharedRefDbRepoFactory a factory providing a {@link SharedRefDbRepository} instance
   * @param localDiskRepositoryManager an instance to manage repositories stored on the local file
   *     system
   * @param ignoredRefs an optional set of refs that should not be validated against the shared
   *     ref-db, this list could be {@link Inject}ed via Guice named binding.
   */
  @Inject
  public SharedRefDbGitRepositoryManager(
      SharedRefDbRepository.Factory sharedRefDbRepoFactory,
      LocalDiskRepositoryManager localDiskRepositoryManager) {
    this.sharedRefDbRepoFactory = sharedRefDbRepoFactory;
    this.gitRepositoryManager = localDiskRepositoryManager;
  }

  /**
   * Get (or open) a {@link SharedRefDbRepository} by name.
   *
   * @param name the repository name, relative to the base directory.
   * @return
   * @throws RepositoryNotFoundException
   * @throws IOException
   */
  @Override
  public Repository openRepository(Project.NameKey name)
      throws RepositoryNotFoundException, IOException {
    return wrap(name, gitRepositoryManager.openRepository(name));
  }

  /**
   * Create (and open) a {@link SharedRefDbRepository} by name.
   *
   * @param name the repository name, relative to the base directory.
   * @return
   * @throws RepositoryCaseMismatchException
   * @throws RepositoryNotFoundException
   * @throws IOException
   */
  @Override
  public Repository createRepository(Project.NameKey name)
      throws RepositoryCaseMismatchException, RepositoryNotFoundException, IOException {
    return wrap(name, gitRepositoryManager.createRepository(name));
  }

  @Override
  public SortedSet<Project.NameKey> list() {
    return gitRepositoryManager.list();
  }

  private Repository wrap(Project.NameKey projectName, Repository projectRepo) {
    return sharedRefDbRepoFactory.create(projectName.get(), projectRepo, ignoredRefs);
  }
}
