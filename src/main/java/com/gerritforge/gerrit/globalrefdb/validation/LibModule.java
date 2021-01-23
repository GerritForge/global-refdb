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

import com.gerritforge.gerrit.globalrefdb.GlobalRefDatabase;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.NoopSharedRefDatabase;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.inject.Scopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Binds {@link GlobalRefDatabase} to the {@link NoopSharedRefDatabase} */
public class LibModule extends LifecycleModule {
  private static final Logger log = LoggerFactory.getLogger(LibModule.class);

  @Override
  protected void configure() {
    DynamicItem.itemOf(binder(), GlobalRefDatabase.class);
    DynamicItem.bind(binder(), GlobalRefDatabase.class)
        .to(NoopSharedRefDatabase.class)
        .in(Scopes.SINGLETON);

    log.info("Shared ref-db engine: none");
  }
}
