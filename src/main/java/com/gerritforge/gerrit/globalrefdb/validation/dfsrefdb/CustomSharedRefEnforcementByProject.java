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

package com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb;

import static com.google.common.base.Suppliers.memoize;

import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implementation of the {@link SharedRefEnforcement} interface which derives project and
 * project/ref enforcement policy from the configuration of the plugin consuming this library
 */
public class CustomSharedRefEnforcementByProject implements SharedRefEnforcement {
  private static final String ALL = ".*";

  private final Supplier<Map<String, Map<String, EnforcePolicy>>> predefEnforcements;

  /**
   * Constructs a {@code CustomSharedRefEnforcementByProject} with the values specified in the
   * configuration of the plugin consuming this library
   *
   * @param config the plugin configuration
   */
  @Inject
  public CustomSharedRefEnforcementByProject(SharedRefDbConfiguration config) {
    this.predefEnforcements = memoize(() -> parseDryRunEnforcementsToMap(config));
  }

  private static Map<String, Map<String, EnforcePolicy>> parseDryRunEnforcementsToMap(
      SharedRefDbConfiguration config) {
    Map<String, Map<String, EnforcePolicy>> enforcementMap = new HashMap<>();

    for (Map.Entry<EnforcePolicy, String> enforcementEntry :
        config.getSharedRefDb().getEnforcementRules().entries()) {
      parseEnforcementEntry(enforcementMap, enforcementEntry);
    }

    return enforcementMap;
  }

  private static void parseEnforcementEntry(
      Map<String, Map<String, EnforcePolicy>> enforcementMap,
      Map.Entry<EnforcePolicy, String> enforcementEntry) {
    Iterator<String> projectAndRef = Splitter.on(':').split(enforcementEntry.getValue()).iterator();
    EnforcePolicy enforcementPolicy = enforcementEntry.getKey();

    if (projectAndRef.hasNext()) {
      String projectName = emptyToAll(projectAndRef.next());
      String refName = emptyToAll(projectAndRef.hasNext() ? projectAndRef.next() : ALL);

      Map<String, EnforcePolicy> existingOrDefaultRef =
          enforcementMap.getOrDefault(projectName, new HashMap<>());

      existingOrDefaultRef.put(refName, enforcementPolicy);

      enforcementMap.put(projectName, existingOrDefaultRef);
    }
  }

  private static String emptyToAll(String value) {
    return value.trim().isEmpty() ? ALL : value;
  }

  /**
   * The enforcement policy for {@param refName} in {@param projectName} as computed from the
   * plugin's configuration file.
   *
   * <p>The projec/ref will not be validate against the shared ref-db if it one to be ignored by
   * default ({@link SharedRefEnforcement#isRefToBeIgnoredBySharedRefDb(String)} or if it has been
   * configured so, for example:
   *
   * <pre>
   *     [ref-database "enforcementRules"]
   *    IGNORED = AProject:/refs/heads/feature
   * </pre>
   *
   * By default all projects are REQUIRED to be consistent on all refs.
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return
   */
  @Override
  public EnforcePolicy getPolicy(String projectName, String refName) {
    if (isRefToBeIgnoredBySharedRefDb(refName)) {
      return EnforcePolicy.IGNORED;
    }

    return getRefEnforcePolicy(projectName, refName);
  }

  private EnforcePolicy getRefEnforcePolicy(String projectName, String refName) {
    Map<String, EnforcePolicy> orDefault =
        predefEnforcements
            .get()
            .getOrDefault(
                projectName, predefEnforcements.get().getOrDefault(ALL, ImmutableMap.of()));

    return MoreObjects.firstNonNull(
        orDefault.getOrDefault(refName, orDefault.get(ALL)), EnforcePolicy.REQUIRED);
  }

  /**
   * The enforcement policy for {@param projectName} as computed from the plugin's configuration
   * file.
   *
   * <p>By default all projects are REQUIRED to be consistent on all refs.
   *
   * @param projectName
   * @return
   */
  @Override
  public EnforcePolicy getPolicy(String projectName) {
    Map<String, EnforcePolicy> policiesForProject =
        predefEnforcements
            .get()
            .getOrDefault(
                projectName, predefEnforcements.get().getOrDefault(ALL, ImmutableMap.of()));
    return policiesForProject.getOrDefault(ALL, EnforcePolicy.REQUIRED);
  }
}
