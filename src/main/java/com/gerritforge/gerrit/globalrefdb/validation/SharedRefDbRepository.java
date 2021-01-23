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
import com.google.gerrit.server.git.DelegateRepository;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

/**
 * Wrapper around a delegated Git repository {@link DelegateRepository}, which allows to intercept
 * and proxy git operations through the shared ref-db to perform validation.
 */
public class SharedRefDbRepository extends DelegateRepository {

  private final SharedRefDbRefDatabase sharedRefDatabase;

  /** {@code SharedRefDbRepository} Factory for Guice assisted injection. */
  public interface Factory {
    SharedRefDbRepository create(
        String projectName, Repository repository, ImmutableSet<String> ignoredRefs);
  }

  /**
   * Constructs a {@code SharedRefDbRepository} to validate git operation for {@param projectName}
   * in {@code repository} against a shared refdb constructed via {@param RefDBFactory}
   *
   * @param refDbFactory a factory generating SharedRefDbRefDatabase ({@link Inject}ed by Guice)
   * @param projectName the name of the project receiving the update
   * @param repository the git repository
   * @param ignoredRefs a set of references that do not need to be checked against the sared ref-db
   */
  @Inject
  public SharedRefDbRepository(
      SharedRefDbRefDatabase.Factory refDbFactory,
      @Assisted String projectName,
      @Assisted Repository repository,
      @Assisted ImmutableSet<String> ignoredRefs) {
    super(repository);
    this.sharedRefDatabase =
        refDbFactory.create(projectName, repository.getRefDatabase(), ignoredRefs);
  }

  /**
   * Getter for the shared ref database
   *
   * @return {@link RefDatabase} the shared ref database
   */
  @Override
  public RefDatabase getRefDatabase() {
    return sharedRefDatabase;
  }

  /**
   * Create a command to update, create or delete {@param ref} in this repository.
   *
   * @param ref name of the ref the caller wants to modify.
   * @return an update command. The caller must finish populating this command and then invoke one
   *     of the update methods to actually make a change.
   * @throws java.io.IOException symbolic ref was passed in and could not be resolved back to the
   *     base ref, as the symbolic ref could not be read.
   */
  @Override
  public RefUpdate updateRef(String ref) throws IOException {
    return sharedRefDatabase.wrapRefUpdate(delegate.updateRef(ref));
  }

  /**
   * Create a command to update, create or delete {@param ref} in this repository.
   *
   * @param ref name of the ref the caller wants to modify.
   * @param detach true to create a detached head
   * @return an update command. The caller must finish populating this command and then invoke one
   *     of the update methods to actually make a change.
   * @throws java.io.IOException a symbolic ref was passed in and could not be resolved back to the
   *     base ref, as the symbolic ref could not be read.
   */
  @Override
  public RefUpdate updateRef(String ref, boolean detach) throws IOException {
    return sharedRefDatabase.wrapRefUpdate(delegate.updateRef(ref, detach));
  }
}
