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

import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.events.ProjectDeletedListener;
import com.google.inject.Inject;

/** Removes a project from the global refdb upon deletion */
public class ProjectDeletedSharedDbCleanup implements ProjectDeletedListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final SharedRefDatabaseWrapper sharedDb;

  private final ValidationMetrics validationMetrics;

  /**
   * Constructs a {@code ProjectDeletedSharedDbCleanup} with the provided validation metrics and
   * shared ref-database
   *
   * @param sharedDb global refdb used to validate project deletion
   * @param validationMetrics to increase split-brain upon project failed validation
   */
  @Inject
  public ProjectDeletedSharedDbCleanup(
      SharedRefDatabaseWrapper sharedDb, ValidationMetrics validationMetrics) {
    this.sharedDb = sharedDb;
    this.validationMetrics = validationMetrics;
  }

  /**
   * Attempts to delete a project from the global refdb. Upon failure, it swallows {@link
   * GlobalRefDbSystemError} exceptions and increments split brain metrics. Executed upon project
   * deletion.
   *
   * @param event the project deletion event
   */
  @Override
  public void onProjectDeleted(Event event) {
    String projectName = event.getProjectName();
    logger.atInfo().log(
        "Deleting project '%s'. Will perform a cleanup in Shared-Ref database.", projectName);

    try {
      sharedDb.remove(Project.nameKey(projectName));
    } catch (GlobalRefDbSystemError e) {
      validationMetrics.incrementSplitBrain();
      logger.atSevere().withCause(e).log(
          "Project '%s' deleted from GIT but it was not able to cleanup"
              + " from Shared-Ref database",
          projectName);
    }
  }
}
