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

import static java.util.Collections.EMPTY_SET;

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.DefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.LocalDiskRepositoryManager;
import com.google.inject.*;
import com.google.inject.name.Names;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

@TestPlugin(name = "test-plugin")
public class ValidationModuleTest extends LightweightPluginDaemonTest {

  @Override
  public void setUpTestPlugin() {}

  @Test
  public void shouldSetupValidationWithEmptyIgnoredRefs() throws Exception {
    installPlugin("test-plugin", ValidationModuleWithEmptyIgnoredRefs.class);
  }

  @Test
  public void shouldSetupValidationWithNonEmptyIgnoredRefs() throws Exception {
    installPlugin("test-plugin", ValidationModuleWithNonEmptyIgnoredRefs.class);
  }

  @Test
  public void shouldSetupValidationWithoutIgnoredRefs() throws Exception {
    installPlugin("test-plugin", ValidationModuleWithoutIgnoredRefs.class);
  }

  public static class ValidationModuleWithNonEmptyIgnoredRefs extends ValidationModule {
    @Inject
    public ValidationModuleWithNonEmptyIgnoredRefs(@GerritServerConfig Config config) {
      super(config, Optional.of(ImmutableSet.of("foo", "bar")));
    }
  }

  public static class ValidationModuleWithEmptyIgnoredRefs extends ValidationModule {
    @Inject
    public ValidationModuleWithEmptyIgnoredRefs(@GerritServerConfig Config config) {
      super(config, Optional.of(EMPTY_SET));
    }
  }

  public static class ValidationModuleWithoutIgnoredRefs extends ValidationModule {
    @Inject
    public ValidationModuleWithoutIgnoredRefs(@GerritServerConfig Config config) {
      super(config, Optional.empty());
    }
  }

  abstract static class ValidationModule extends FactoryModule {
    private final Config config;
    private final Optional<Set<String>> ignoredRefs;

    public ValidationModule(@GerritServerConfig Config config, Optional<Set<String>> ignoredRefs) {
      this.config = config;
      this.ignoredRefs = ignoredRefs;
    }

    @Override
    protected void configure() {
      ignoredRefs.ifPresent(
          ir ->
              bind(new TypeLiteral<Set<String>>() {})
                  .annotatedWith(Names.named(SharedRefDbGitRepositoryManager.IGNORED_REFS))
                  .toInstance(ir));

      bind(SharedRefDatabaseWrapper.class).in(Scopes.SINGLETON);
      bind(SharedRefLogger.class).to(Log4jSharedRefLogger.class);
      factory(LockWrapper.Factory.class);

      factory(SharedRefDbRepository.Factory.class);
      factory(SharedRefDbRefDatabase.Factory.class);
      factory(SharedRefDbRefUpdate.Factory.class);
      factory(SharedRefDbBatchRefUpdate.Factory.class);
      factory(RefUpdateValidator.Factory.class);
      factory(BatchRefUpdateValidator.Factory.class);

      SharedRefDbConfiguration cfg = new SharedRefDbConfiguration(config, "test");

      bind(SharedRefDbConfiguration.class).toInstance(cfg);
      bind(ValidationMetrics.class);

      bind(GitRepositoryManager.class)
          .annotatedWith(Names.named(SharedRefDbGitRepositoryManager.LOCAL_DISK_REPOSITORY_MANAGER))
          .to(LocalDiskRepositoryManager.class);
      bind(SharedRefDbGitRepositoryManager.class);
      bind(SharedRefEnforcement.class).to(DefaultSharedRefEnforcement.class).in(Scopes.SINGLETON);
    }
  }
}
