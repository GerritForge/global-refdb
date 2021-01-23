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

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
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

/**
 * Represents a configuration for the shared ref-database. This configuration is retrieved from the
 * configuration file of the plugin is consuming the library.
 */
public class SharedRefDbConfiguration {
  private static final Logger log = LoggerFactory.getLogger(SharedRefDbConfiguration.class);

  private final Supplier<Projects> projects;
  private final Supplier<SharedRefDatabase> sharedRefDb;

  /**
   * Constructs a {@code SharedRefDbConfiguration} by providing the plugin name and a {@param
   * Config} object
   *
   * @param config the plugin configuration
   * @param pluginName the name of the plugin consuming this library
   */
  public SharedRefDbConfiguration(Config config, String pluginName) {
    Supplier<Config> lazyCfg = lazyLoad(config);
    projects = memoize(() -> new Projects(lazyCfg));
    sharedRefDb = memoize(() -> new SharedRefDatabase(lazyCfg, pluginName));
  }

  /**
   * @return the {@link SharedRefDatabase} computed from the configuration plugin configuration file
   */
  public SharedRefDatabase getSharedRefDb() {
    return sharedRefDb.get();
  }

  /** @return Getter of projects checked against the shared ref-db */
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

  /**
   * Represents the shared ref-db configuration, which is computed by reading the 'ref-database'
   * section from the configuration file of this library's consumers. It allows to specify whether
   * it is enabled, specific {@link SharedRefEnforcement}s and to tune other parameters that define
   * specific behaviours of the shared ref-db.
   */
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

    /**
     * Whether the use of the shared ref-db is enabled. Defaults 'false'
     *
     * @return true when enabled, false otherwise
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Getter for the map of {@link EnforcePolicy} to a specific "project:refs". Each entry can be
     * either be {@link SharedRefEnforcement.EnforcePolicy#IGNORED} or {@link *
     * SharedRefEnforcement.EnforcePolicy#REQUIRED} and it represents the level of consistency
     * enforcements for that specific "project:refs". If the project or ref is omitted, apply the
     * policy to all projects or all refs.
     *
     * @return Map of "project:refs" policies
     */
    public Multimap<EnforcePolicy, String> getEnforcementRules() {
      return enforcementRules;
    }

    /**
     * String to prefix all metrics created and exposed by this library. Defaults to the name of the
     * plugin consuming this library.
     *
     * @return prefix for all metrics
     */
    public String getMetricsRoot() {
      return metricsRoot;
    }

    private List<String> getList(
        Supplier<Config> cfg, String section, String subsection, String name) {
      return ImmutableList.copyOf(cfg.get().getStringList(section, subsection, name));
    }
  }

  /**
   * Represents a set of projects for which ref updates operations should be validated against the
   * shared ref-db. The list is computed from the consuming plugin's configuration file by looking
   * at the "project.pattern" section. By defaults all projects are matched.
   */
  public static class Projects {
    public static final String SECTION = "projects";
    public static final String PATTERN_KEY = "pattern";
    public List<String> patterns;

    /**
     * Constructs a {@code Projects} object by reading the list of "projects.pattern" possibly
     * specified in the consuming plugin's configuration file.
     *
     * @param cfg the plugin's configuration supplier
     */
    public Projects(Supplier<Config> cfg) {
      patterns = ImmutableList.copyOf(cfg.get().getStringList(SECTION, null, PATTERN_KEY));
    }

    /**
     * The list of project patterns read from the consuming plugin's configuration file.
     *
     * @return list of project patterns.
     */
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
