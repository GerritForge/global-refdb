// Copyright (C) 2023 The Android Open Source Project
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.Project;
import com.google.gerrit.metrics.Timer0.Context;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SharedRefDatabaseWrapperTest {

  @Mock private SharedRefDBMetrics metrics;
  @Mock SharedRefLogger sharedRefLogger;
  @Mock private Context context;
  @Mock private Ref ref;

  private SharedRefDatabaseWrapper objectUnderTest;
  private String refName = "refs/heads/master";
  private Project.NameKey projectName = Project.nameKey("test_project");

  @Before
  public void setup() {
    when(metrics.startCompareAndPutExecutionTime()).thenReturn(context);
    when(metrics.startLockRefExecutionTime()).thenReturn(context);
    when(metrics.startGetExecutionTime()).thenReturn(context);
    when(metrics.startExistsExecutionTime()).thenReturn(context);
    when(metrics.startIsUpToDateExecutionTime()).thenReturn(context);
    when(metrics.startRemoveExecutionTime()).thenReturn(context);
    objectUnderTest = new SharedRefDatabaseWrapper(sharedRefLogger, metrics);
  }

  @Test
  public void shouldUpdateCompareAndPutExecutionTimeMetricWhenCompareAndPut() {
    objectUnderTest.compareAndPut(projectName, refName, ObjectId.zeroId(), ObjectId.zeroId());
    verify(metrics).startCompareAndPutExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateLockRefExecutionTimeMetricWhenLockRefIsCalled() {
    objectUnderTest.lockRef(projectName, refName);
    verify(metrics).startLockRefExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateIsUpToDateExecutionTimeMetricWhenIsUpToDate() {
    objectUnderTest.isUpToDate(projectName, ref);
    verify(metrics).startIsUpToDateExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateExistsExecutionTimeMetricWhenExistsIsCalled() {
    objectUnderTest.exists(projectName, refName);
    verify(metrics).startExistsExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateGetExecutionTimeMetricWhenGetIsCalled() {
    objectUnderTest.get(projectName, refName, String.class);
    verify(metrics).startGetExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateRemoveExecutionTimeMetricWhenRemoveCalled() {
    objectUnderTest.remove(projectName);
    verify(metrics).startRemoveExecutionTime();
    verify(context).close();
  }
}
