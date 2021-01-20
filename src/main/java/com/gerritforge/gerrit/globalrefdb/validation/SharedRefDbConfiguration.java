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

import static com.google.common.base.Suppliers.memoize;
import static com.google.common.base.Suppliers.ofInstance;

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement.EnforcePolicy;
import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedRefDbConfiguration {
  private static final Logger log = LoggerFactory.getLogger(SharedRefDbConfiguration.class);

  private final Supplier<Projects> projects;
  private final Supplier<SharedRefDatabase> sharedRefDb;

  public SharedRefDbConfiguration(Config config, String pluginName) {
    Supplier<Config> lazyCfg = lazyLoad(config);
    projects = memoize(() -> new Projects(lazyCfg));
    sharedRefDb = memoize(() -> new SharedRefDatabase(lazyCfg, pluginName));
  }

  public SharedRefDatabase getSharedRefDb() {
    return sharedRefDb.get();
  }

  public Projects projects() {
    return projects.get();
  }

  private Supplier<Config> lazyLoad(Config config) {
    if (config instanceof FileBasedConfig) {
      return memoize(
          () -> {
            FileBasedConfig fileConfig = (FileBasedConfig) config;
            String fileConfigFileName = fileConfig.getFile().getPath();
            try {
              log.info("Loading configuration from {}", fileConfigFileName);
              fileConfig.load();
            } catch (IOException | ConfigInvalidException e) {
              log.error("Unable to load configuration from " + fileConfigFileName, e);
            }
            return fileConfig;
          });
    }
    return ofInstance(config);
  }

  public static class SharedRefDatabase {
    public static final String SECTION = "ref-database";
    public static final String ENABLE_KEY = "enabled";
    public static final String SUBSECTION_ENFORCEMENT_RULES = "enforcementRules";
    public static final String METRICS_ROOT = "metricsRoot";

    private final boolean enabled;
    private final Multimap<EnforcePolicy, String> enforcementRules;
    private final String metricsRoot;

    private SharedRefDatabase(Supplier<Config> cfg, String pluginName) {
      enabled = getBoolean(cfg, SECTION, null, ENABLE_KEY, false);
      metricsRoot =
          MoreObjects.firstNonNull(cfg.get().getString(SECTION, null, METRICS_ROOT), pluginName);
      enforcementRules = MultimapBuilder.hashKeys().arrayListValues().build();
      for (EnforcePolicy policy : EnforcePolicy.values()) {
        enforcementRules.putAll(
            policy, getList(cfg, SECTION, SUBSECTION_ENFORCEMENT_RULES, policy.name()));
      }
    }

    public boolean isEnabled() {
      return enabled;
    }

    public Multimap<EnforcePolicy, String> getEnforcementRules() {
      return enforcementRules;
    }

    public String getMetricsRoot() {
      return metricsRoot;
    }

    private List<String> getList(
        Supplier<Config> cfg, String section, String subsection, String name) {
      return ImmutableList.copyOf(cfg.get().getStringList(section, subsection, name));
    }
  }

  public static class Projects {
    public static final String SECTION = "projects";
    public static final String PATTERN_KEY = "pattern";
    public List<String> patterns;

    public Projects(Supplier<Config> cfg) {
      patterns = ImmutableList.copyOf(cfg.get().getStringList(SECTION, null, PATTERN_KEY));
    }

    public List<String> getPatterns() {
      return patterns;
    }
  }

  static boolean getBoolean(
      Supplier<Config> cfg, String section, String subsection, String name, boolean defaultValue) {
    try {
      return cfg.get().getBoolean(section, subsection, name, defaultValue);
    } catch (IllegalArgumentException e) {
      log.error("invalid value for {}; using default value {}", name, defaultValue);
      log.debug("Failed to retrieve boolean value: {}", e.getMessage(), e);
      return defaultValue;
    }
  }
}
