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

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.RefFixture;
import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.events.ProjectDeletedListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDeletedSharedDbCleanupTest implements RefFixture {
  @Rule public TestName nameRule = new TestName();

  @Mock ValidationMetrics mockValidationMetrics;
  @Mock SharedRefDatabaseWrapper sharedRefDatabase;

  @Test
  public void aDeleteProjectEventShouldCleanupProjectFromZk() throws Exception {
    String projectName = A_TEST_PROJECT_NAME;
    ProjectDeletedSharedDbCleanup projectDeletedSharedDbCleanup =
        new ProjectDeletedSharedDbCleanup(sharedRefDatabase, mockValidationMetrics);

    ProjectDeletedListener.Event event =
        new ProjectDeletedListener.Event() {
          @Override
          public String getProjectName() {
            return projectName;
          }

          @Override
          public NotifyHandling getNotify() {
            return NotifyHandling.NONE;
          }
        };

    projectDeletedSharedDbCleanup.onProjectDeleted(event);

    Mockito.verify(sharedRefDatabase, Mockito.times(1)).remove(A_TEST_PROJECT_NAME_KEY);
  }
}
