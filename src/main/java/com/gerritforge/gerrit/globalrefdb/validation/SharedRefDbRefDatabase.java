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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.BatchRefUpdate;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;

/**
 * Wraps an instance of {@link RefDatabase} with the intent of wrapping {@link RefUpdate} operations
 * to instances of {@link SharedRefDbRefUpdate} in order to allow validation of those operation
 * against a shared ref-database before actually executing them.
 */
public class SharedRefDbRefDatabase extends RefDatabase {
  private final SharedRefDbRefUpdate.Factory refUpdateFactory;
  private final SharedRefDbBatchRefUpdate.Factory batchRefUpdateFactory;
  private final String projectName;
  private final RefDatabase refDatabase;
  private final ImmutableSet<String> ignoredRefs;

  /** {@code SharedRefDbRefDatabase} Factory for Guice assisted injection. */
  public interface Factory {
    SharedRefDbRefDatabase create(
        String projectName, RefDatabase refDatabase, ImmutableSet<String> ignoredRefs);
  }

  /**
   * Constructs a {@code SharedRefDbRefDatabase} by wrapping an underlying {@param refDatabase}, so
   * that update refs operations can be validated against a shared ref-database.
   *
   * @param refUpdateFactory a factory to provide a {@link SharedRefDbRefUpdate}
   * @param batchRefUpdateFactory a factory to provide a {@link SharedRefDbBatchRefUpdate}
   * @param projectName the name of the project to perform Git operations on
   * @param refDatabase the wrapped {@link RefDatabase}
   * @param ignoredRefs a set of reference for which ref-db validation should not be executed.
   */
  @Inject
  public SharedRefDbRefDatabase(
      SharedRefDbRefUpdate.Factory refUpdateFactory,
      SharedRefDbBatchRefUpdate.Factory batchRefUpdateFactory,
      @Assisted String projectName,
      @Assisted RefDatabase refDatabase,
      @Assisted ImmutableSet<String> ignoredRefs) {
    this.refUpdateFactory = refUpdateFactory;
    this.batchRefUpdateFactory = batchRefUpdateFactory;
    this.projectName = projectName;
    this.refDatabase = refDatabase;
    this.ignoredRefs = ignoredRefs;
  }

  @Override
  public int hashCode() {
    return refDatabase.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return refDatabase.equals(obj);
  }

  @Override
  public void create() throws IOException {
    refDatabase.create();
  }

  @Override
  public void close() {
    refDatabase.close();
  }

  @Override
  public boolean isNameConflicting(String name) throws IOException {
    return refDatabase.isNameConflicting(name);
  }

  @Override
  public Collection<String> getConflictingNames(String name) throws IOException {
    return refDatabase.getConflictingNames(name);
  }

  /**
   * Wrap a {@link RefUpdate} obtained by calling the underlying {@link RefDatabase} in a {@link
   * SharedRefDbRefUpdate}
   *
   * @see RefDatabase#newUpdate(String, boolean)
   * @throws IOException
   */
  @Override
  public RefUpdate newUpdate(String name, boolean detach) throws IOException {
    return wrapRefUpdate(refDatabase.newUpdate(name, detach));
  }

  @Override
  public RefRename newRename(String fromName, String toName) throws IOException {
    return refDatabase.newRename(fromName, toName);
  }

  /**
   * Obtains a {@link SharedRefDbBatchRefUpdate} via the {@code BatchRefUpdate.Factory} invoked on
   * the underlying {@link RefDatabase}, so that batch updates can be validated against the shared
   * ref-db.
   *
   * @see RefDatabase#newUpdate(String, boolean)
   * @throws IOException
   */
  @Override
  public BatchRefUpdate newBatchUpdate() {
    return batchRefUpdateFactory.create(projectName, refDatabase, ignoredRefs);
  }

  @Override
  public boolean performsAtomicTransactions() {
    return refDatabase.performsAtomicTransactions();
  }

  @Override
  public String toString() {
    return refDatabase.toString();
  }

  @Override
  public Ref exactRef(String name) throws IOException {
    return refDatabase.exactRef(name);
  }

  @Override
  public Map<String, Ref> exactRef(String... refs) throws IOException {
    return refDatabase.exactRef(refs);
  }

  @Override
  public Ref firstExactRef(String... refs) throws IOException {
    return refDatabase.firstExactRef(refs);
  }

  @Override
  public List<Ref> getRefs() throws IOException {
    return refDatabase.getRefs();
  }

  @SuppressWarnings("deprecation")
  @Override
  public Map<String, Ref> getRefs(String prefix) throws IOException {
    return refDatabase.getRefs(prefix);
  }

  @Override
  public List<Ref> getRefsByPrefix(String prefix) throws IOException {
    return refDatabase.getRefsByPrefix(prefix);
  }

  @Override
  public boolean hasRefs() throws IOException {
    return refDatabase.hasRefs();
  }

  @Override
  public List<Ref> getAdditionalRefs() throws IOException {
    return refDatabase.getAdditionalRefs();
  }

  @Override
  public Ref peel(Ref ref) throws IOException {
    return refDatabase.peel(ref);
  }

  @Override
  public void refresh() {
    refDatabase.refresh();
  }

  RefUpdate wrapRefUpdate(RefUpdate refUpdate) {
    return refUpdateFactory.create(projectName, refUpdate, refDatabase, ignoredRefs);
  }
}
