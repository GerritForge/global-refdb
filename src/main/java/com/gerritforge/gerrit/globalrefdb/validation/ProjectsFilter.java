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

/**
 * Filter to match against project names to indicate whether a project should be validated against a
 * shared ref-db.
 *
 * <p>Filters are computed by reading the configuration of the plugin consuming this library.
 */
@Singleton
public class ProjectsFilter {

  /** The type of the pattern defining this filter */
  public enum PatternType {
    /**
     * Values starting with a caret `^` are treated as regular expressions. For the regular
     * expressions details please @see <a
     * href="https://docs.oracle.com/javase/tutorial/essential/regex/">official java
     * documentation</a>
     */
    REGEX,
    /**
     * Values that are not regular expressions and end in `*` are treated as wildcard matches.
     * Wildcards match projects whose name agrees from the beginning until the trailing `*`. So
     * `foo/b*` would match the projects `foo/b`, `foo/bar`, and `foo/baz`, but neither `foobar`,
     * nor `bar/foo/baz`.
     */
    WILDCARD,
    /**
     * Values that are neither regular expressions nor wildcards are treated as single project
     * matches. So `foo/bar` matches only the project `foo/bar`, but no other project.
     */
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

  /**
   * Constructs a {@code ProjectsFilter} by providing the plugin configuration
   *
   * @param cfg the plugin configuration
   */
  @Inject
  public ProjectsFilter(SharedRefDbConfiguration cfg) {
    projectPatterns = cfg.projects().getPatterns();
  }

  /**
   * Given an event matches checks whether this project filter matches the project, the event is
   * for. It only applies to {@link ProjectEvent}s.
   *
   * @see #matches(NameKey)
   * @param event the event to check against
   * @return true when it matches, false otherwise.
   */
  public boolean matches(Event event) {
    if (event == null) {
      throw new IllegalArgumentException("Event object cannot be null");
    }
    if (event instanceof ProjectEvent) {
      return matches(((ProjectEvent) event).getProjectNameKey());
    }
    return false;
  }

  /**
   * checks whether this project filter matches the {@param projectName}
   *
   * @see #matches(NameKey)
   * @param projectName the project name to check against
   * @return true when it matches, false otherwise.
   */
  public boolean matches(String projectName) {
    return matches(NameKey.parse(projectName));
  }

  /**
   * checks whether this project filter matches the project {@param name}.
   *
   * @param name the name of the project to check
   * @return true, when the project matches, false otherwise.
   */
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
