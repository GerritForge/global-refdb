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

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;

// import com.google.gerrit.entities.AccessSection;

@Singleton
public class ProjectsFilter {
  public enum PatternType {
    REGEX,
    WILDCARD,
    EXACT_MATCH;

    public static PatternType getPatternType(String pattern) {
      // TODO: Use AccessSection
      if (pattern.startsWith("^")) {
        //      if (pattern.startsWith(AccessSection.REGEX_PREFIX)) {
        return REGEX;
      } else if (pattern.endsWith("*")) {
        return WILDCARD;
      } else {
        return EXACT_MATCH;
      }
    }
  }

  private Set<NameKey> globalProjects = Sets.newConcurrentHashSet();
  private Set<NameKey> localProjects = Sets.newConcurrentHashSet();
  private final List<String> projectPatterns;

  @Inject
  public ProjectsFilter(SharedRefDbConfiguration cfg) {
    projectPatterns = cfg.projects().getPatterns();
  }

  public boolean matches(Event event) {
    if (event == null) {
      throw new IllegalArgumentException("Event object cannot be null");
    }
    if (event instanceof ProjectEvent) {
      return matches(((ProjectEvent) event).getProjectNameKey());
    }
    return false;
  }

  public boolean matches(String projectName) {
    return matches(NameKey.parse(projectName));
  }

  public boolean matches(Project.NameKey name) {
    if (name == null || Strings.isNullOrEmpty(name.get())) {
      throw new IllegalArgumentException(
          String.format("Project name cannot be null or empty, but was %s", name));
    }
    if (projectPatterns.isEmpty() || globalProjects.contains(name)) {
      return true;
    }

    if (localProjects.contains(name)) {
      return false;
    }

    String projectName = name.get();

    for (String pattern : projectPatterns) {
      if (matchesPattern(projectName, pattern)) {
        globalProjects.add(name);
        return true;
      }
    }
    localProjects.add(name);
    return false;
  }

  private boolean matchesPattern(String projectName, String pattern) {
    boolean match = false;
    switch (PatternType.getPatternType(pattern)) {
      case REGEX:
        match = projectName.matches(pattern);
        break;
      case WILDCARD:
        match = projectName.startsWith(pattern.substring(0, pattern.length() - 1));
        break;
      case EXACT_MATCH:
        match = projectName.equals(pattern);
    }
    return match;
  }
}
