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

import com.google.gerrit.server.git.DelegateRepository;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

public class SharedRefDbRepository extends DelegateRepository {

  private final SharedRefDbRefDatabase sharedRefDatabase;

  public interface Factory {
    public SharedRefDbRepository create(String projectName, Repository repository);
  }

  @Inject
  public SharedRefDbRepository(
      SharedRefDbRefDatabase.Factory refDbFactory,
      @Assisted String projectName,
      @Assisted Repository repository) {
    super(repository);
    this.sharedRefDatabase = refDbFactory.create(projectName, repository.getRefDatabase());
  }

  @Override
  public RefDatabase getRefDatabase() {
    return sharedRefDatabase;
  }

  @Override
  public RefUpdate updateRef(String ref) throws IOException {
    return sharedRefDatabase.wrapRefUpdate(delegate.updateRef(ref));
  }

  @Override
  public RefUpdate updateRef(String ref, boolean detach) throws IOException {
    return sharedRefDatabase.wrapRefUpdate(delegate.updateRef(ref, detach));
  }
}
