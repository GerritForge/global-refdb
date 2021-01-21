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

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.DefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import java.util.Set;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

@TestPlugin(name = "test-plugin")
public class ValidationModuleTest extends LightweightPluginDaemonTest {

  static Set<String> ignoreRefs;

  @Override
  public void setUpTestPlugin() {
    ignoreRefs = ImmutableSet.of();
  }

  @Test
  public void shouldSetupValidationWhenRefToIgnoreAreEmpty() throws Exception {
    ignoreRefs = ImmutableSet.of();
    installPlugin("test-plugin", ValidationModule.class);
  }

  @Test
  public void shouldSetupValidationWhenRefsToIgnore() throws Exception {
    ignoreRefs = ImmutableSet.of("refs/plugin-name/version", "refs/plugin-name/version/value");
    installPlugin("test-plugin", ValidationModule.class);
  }

  @Test
  public void shouldSetupValidationWhenRefsToIgnoreAreNull() throws Exception {
    ignoreRefs = null;
    installPlugin("test-plugin", ValidationModule.class);
  }

  public static class ValidationModule extends FactoryModule {
    @Inject Config config;

    @Override
    protected void configure() {
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
      if (ignoreRefs != null) {
        bind(new TypeLiteral<Set<String>>() {})
            .annotatedWith(Names.named(SharedRefDbGitRepositoryManager.IGNORED_REFS))
            .toInstance(ignoreRefs);
      }
      bind(SharedRefDbGitRepositoryManager.class);
      bind(SharedRefEnforcement.class).to(DefaultSharedRefEnforcement.class).in(Scopes.SINGLETON);
    }
  }
}
