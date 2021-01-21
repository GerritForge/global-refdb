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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.DefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.RefFixture;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedDbSplitBrainException;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Project;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RefUpdateValidatorTest implements RefFixture {
  private static final DefaultSharedRefEnforcement defaultRefEnforcement =
      new DefaultSharedRefEnforcement();

  @Mock SharedRefDatabaseWrapper sharedRefDb;

  @Mock SharedRefLogger sharedRefLogger;

  @Mock RefDatabase localRefDb;

  @Mock ValidationMetrics validationMetrics;

  @Mock RefUpdate refUpdate;

  @Mock ProjectsFilter projectsFilter;

  String refName;
  Ref oldUpdateRef;
  Ref newUpdateRef;
  Ref localRef;

  RefUpdateValidator refUpdateValidator;

  @Before
  public void setupMocks() throws Exception {
    refName = aBranchRef();
    oldUpdateRef = newRef(refName, AN_OBJECT_ID_1);
    newUpdateRef = newRef(refName, AN_OBJECT_ID_2);
    localRef = newRef(refName, AN_OBJECT_ID_3);

    doReturn(localRef).when(localRefDb).findRef(refName);
    doReturn(localRef).when(localRefDb).exactRef(refName);
    doReturn(oldUpdateRef).when(refUpdate).getRef();
    doReturn(newUpdateRef.getObjectId()).when(refUpdate).getNewObjectId();
    doReturn(refName).when(refUpdate).getName();
    lenient().doReturn(oldUpdateRef.getObjectId()).when(refUpdate).getOldObjectId();

    doReturn(true).when(projectsFilter).matches(anyString());

    refUpdateValidator = newRefUpdateValidator(sharedRefDb);
  }

  @Test
  public void validationShouldSucceedWhenSharedRefDbIsNoop() throws Exception {
    SharedRefDatabaseWrapper noopSharedRefDbWrapper = new SharedRefDatabaseWrapper(sharedRefLogger);

    Result result =
        newRefUpdateValidator(noopSharedRefDbWrapper).executeRefUpdate(refUpdate, () -> Result.NEW);
    assertThat(result).isEqualTo(Result.NEW);
  }

  @Test
  public void validationShouldSucceedWhenLocalRefDbIsUpToDate() throws Exception {
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, localRef, newUpdateRef.getObjectId());

    Result result = refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW);

    assertThat(result).isEqualTo(Result.NEW);
  }

  @Test
  public void sharedRefDbShouldBeUpdatedWithRefDeleted() throws Exception {
    doReturn(ObjectId.zeroId()).when(refUpdate).getNewObjectId();
    doReturn(true).when(sharedRefDb).isUpToDate(any(Project.NameKey.class), any(Ref.class));
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, localRef, ObjectId.zeroId());
    doReturn(localRef).doReturn(null).when(localRefDb).findRef(refName);

    Result result = refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.FORCED);

    assertThat(result).isEqualTo(Result.FORCED);
  }

  @Test
  public void sharedRefDbShouldBeUpdatedWithNewRefCreated() throws Exception {
    Ref localNullRef = nullRef(refName);

    doReturn(true).when(sharedRefDb).isUpToDate(any(Project.NameKey.class), any(Ref.class));
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, localNullRef, newUpdateRef.getObjectId());
    doReturn(localNullRef).doReturn(newUpdateRef).when(localRefDb).findRef(refName);

    Result result = refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW);

    assertThat(result).isEqualTo(Result.NEW);
  }

  @Test
  public void validationShouldFailWhenLocalRefDbIsOutOfSync() throws Exception {
    lenient()
        .doReturn(true)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).exists(A_TEST_PROJECT_NAME_KEY, refName);
    doReturn(false).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);

    Result result = refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW);

    assertThat(result).isEqualTo(Result.LOCK_FAILURE);
  }

  @Test(expected = SharedDbSplitBrainException.class)
  public void shouldTrowSplitBrainWhenLocalRefDbIsUpToDateButFinalCompareAndPutIsFailing()
      throws Exception {
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);
    lenient()
        .doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    doReturn(false)
        .when(sharedRefDb)
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, localRef, newUpdateRef.getObjectId());

    refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW);
  }

  @Test
  public void shouldNotUpdateSharedRefDbWhenFinalCompareAndPutIsFailing() throws Exception {
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);

    Result result = refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.LOCK_FAILURE);

    verify(sharedRefDb, never())
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    assertThat(result).isEqualTo(Result.LOCK_FAILURE);
  }

  @Test
  public void shouldNotUpdateSharedRefDbWhenProjectIsLocal() throws Exception {
    when(projectsFilter.matches(anyString())).thenReturn(false);

    refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW);

    verify(sharedRefDb, never())
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
  }

  private RefUpdateValidator newRefUpdateValidator(SharedRefDatabaseWrapper refDbWrapper) {
    return new RefUpdateValidator(
        refDbWrapper,
        validationMetrics,
        defaultRefEnforcement,
        new DummyLockWrapper(),
        projectsFilter,
        A_TEST_PROJECT_NAME,
        localRefDb,
        ImmutableSet.of());
  }
}
