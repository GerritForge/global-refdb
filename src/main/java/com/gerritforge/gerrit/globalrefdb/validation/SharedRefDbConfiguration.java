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
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
 * configuration file of the libModule is consuming the library.
 */
public class SharedRefDbConfiguration {
  private static final Logger log = LoggerFactory.getLogger(SharedRefDbConfiguration.class);

  private final Supplier<Projects> projects;
  private final Supplier<SharedRefDatabase> sharedRefDb;
  private final String pluginName;

  /**
   * Constructs a {@code SharedRefDbConfiguration} by providing the libModule name and a 'config'
   * object
   *
   * @param config the libModule configuration
   * @param pluginName the name of the libModule consuming this library
   */
  public SharedRefDbConfiguration(Config config, String pluginName) {
    Supplier<Config> lazyCfg = lazyLoad(config);
    projects = memoize(() -> new Projects(lazyCfg));
    sharedRefDb = memoize(() -> new SharedRefDatabase(lazyCfg));
    this.pluginName = pluginName;
  }

  /**
   * @return the {@link SharedRefDatabase} computed from the configuration libModule configuration
   *     file
   */
  public SharedRefDatabase getSharedRefDb() {
    return sharedRefDb.get();
  }

  /** @return Getter of projects checked against the global refdb */
  public Projects projects() {
    return projects.get();
  }

  /** @return name of the libModule consuming this library */
  public String pluginName() {
    return pluginName;
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
   * Represents the global refdb configuration, which is computed by reading the 'ref-database'
   * section from the configuration file of this library's consumers. It allows to specify whether
   * it is enabled, specific {@link SharedRefEnforcement}s and to tune other parameters that define
   * specific behaviours of the global refdb.
   */
  public static class SharedRefDatabase {
    public static final String SECTION = "ref-database";
    public static final String ENABLE_KEY = "enabled";
    public static final String SUBSECTION_ENFORCEMENT_RULES = "enforcementRules";
    public static final String IGNORED_REFS_PREFIXES = "ignoredRefsPrefixes";

    private final boolean enabled;
    private final Multimap<EnforcePolicy, String> enforcementRules;
    private final ImmutableSet<String> ignoredRefsPrefixes;

    private SharedRefDatabase(Supplier<Config> cfg) {
      enabled = getBoolean(cfg, SECTION, null, ENABLE_KEY, false);
      enforcementRules = MultimapBuilder.hashKeys().arrayListValues().build();
      for (EnforcePolicy policy : EnforcePolicy.values()) {
        enforcementRules.putAll(
            policy, getList(cfg, SECTION, SUBSECTION_ENFORCEMENT_RULES, policy.name()));
      }

      ignoredRefsPrefixes = ImmutableSet.copyOf(getList(cfg, SECTION, null, IGNORED_REFS_PREFIXES));
    }

    /**
     * Whether the use of the global refdb is enabled. Defaults 'false'
     *
     * @return true when enabled, false otherwise
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Getter for the map of {@link EnforcePolicy} to a specific "project:refs". Each entry can be
     * either be {@link SharedRefEnforcement.EnforcePolicy#IGNORED} or {@link
     * SharedRefEnforcement.EnforcePolicy#REQUIRED} and it represents the level of consistency
     * enforcements for that specific "project:refs". If the project or ref is omitted, apply the
     * policy to all projects or all refs.
     *
     * <p>The projec/ref will not be validated against the global refdb if it one to be ignored by
     * default ({@link SharedRefEnforcement#isRefToBeIgnoredBySharedRefDb(String)} or if it has been
     * configured so, for example:
     *
     * <pre>
     *     [ref-database "enforcementRules"]
     *    IGNORED = AProject:/refs/heads/feature
     * </pre>
     *
     * @return Map of "project:refs" policies
     */
    public Multimap<EnforcePolicy, String> getEnforcementRules() {
      return enforcementRules;
    }

    /**
     * Returns the set of refs prefixes that are ignored during the validation and enforcement of
     * the global refdb.
     *
     * @return Set of ignored prefixes of ignored refs
     */
    public ImmutableSet<String> getIgnoredRefsPrefixes() {
      return ignoredRefsPrefixes;
    }

    private List<String> getList(
        Supplier<Config> cfg, String section, String subsection, String name) {
      return ImmutableList.copyOf(cfg.get().getStringList(section, subsection, name));
    }
  }

  /**
   * Represents a set of projects for which ref updates operations should be validated against the
   * global refdb. The list is computed from the consuming libModule's configuration file by looking
   * at the "project.pattern" section. By defaults all projects are matched.
   */
  public static class Projects {
    public static final String SECTION = "projects";
    public static final String PATTERN_KEY = "pattern";
    public List<String> patterns;

    /**
     * Constructs a {@code Projects} object by reading the list of "projects.pattern" possibly
     * specified in the consuming libModule's configuration file.
     *
     * @param cfg the libModule's configuration supplier
     */
    public Projects(Supplier<Config> cfg) {
      patterns = ImmutableList.copyOf(cfg.get().getStringList(SECTION, null, PATTERN_KEY));
    }

    /**
     * The list of project patterns read from the consuming libModule's configuration file.
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
