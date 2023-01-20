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

import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.metrics.Timer0;
import com.google.gerrit.metrics.Timer0.Context;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SharedRefDBMetrics {

  private final Timer0 lockRefExecutionTime;
  private final Timer0 getOperationExecutionTime;
  private final Timer0 existsExecutionTime;
  private Timer0 compareAndPutExecutionTime;
  private Timer0 removeExecutionTime;
  private Timer0 isUpToDateExecutionTime;

  @Inject
  public SharedRefDBMetrics(MetricMaker metricMaker) {
    compareAndPutExecutionTime =
        metricMaker.newTimer(
            "global_refdb/compare_and_put_latency",
            new Description("Time spent on compareAndPut.")
                .setCumulative()
                .setUnit(Description.Units.MILLISECONDS));
    getOperationExecutionTime =
        metricMaker.newTimer(
            "global_refdb/get_latency",
            new Description("Time spent on get operation.")
                .setCumulative()
                .setUnit(Description.Units.MILLISECONDS));

    lockRefExecutionTime =
        metricMaker.newTimer(
            "global_refdb/lock_ref_latency",
            new Description("Time spent on locking ref.")
                .setCumulative()
                .setUnit(Description.Units.MILLISECONDS));
    existsExecutionTime =
        metricMaker.newTimer(
            "global_refdb/exists_latency",
            new Description("Time spent on verifying if the global-refdb contains a value.")
                .setCumulative()
                .setUnit(Description.Units.MILLISECONDS));
    removeExecutionTime =
        metricMaker.newTimer(
            "global_refdb/remove_latency",
            new Description("Time spent on cleaning up the path from global-ref db.")
                .setCumulative()
                .setUnit(Description.Units.MILLISECONDS));
    isUpToDateExecutionTime =
        metricMaker.newTimer(
            "global_refdb/is_up_to_date_latency",
            new Description("Time spent on checking in global ref-db if ref is up-to-date.")
                .setCumulative()
                .setUnit(Description.Units.MILLISECONDS));
  }

  public Context startCompareAndPutExecutionTime() {
    return compareAndPutExecutionTime.start();
  }

  public Context startGetExecutionTime() {
    return getOperationExecutionTime.start();
  }

  public Context startLockRefExecutionTime() {
    return lockRefExecutionTime.start();
  }

  public Context startExistsExecutionTime() {
    return existsExecutionTime.start();
  }

  public Context startRemoveExecutionTime() {
    return removeExecutionTime.start();
  }

  public Context startIsUpToDateExecutionTime() {
    return isUpToDateExecutionTime.start();
  }
}
