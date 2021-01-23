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

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;
import static org.eclipse.jgit.lib.Constants.OBJ_COMMIT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.common.GitPerson;
import com.google.gerrit.json.OutputFormat;
import com.google.gerrit.server.CommonConverters;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.util.SystemLog;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import org.apache.log4j.PatternLayout;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SharedRefLogger for Log4j. Logs to 'sharedref_log' file
 *
 * @see <a href="https://logging.apache.org/log4j/2.x/javadoc.html">log4j</a>
 */
@Singleton
public class Log4jSharedRefLogger extends LibModuleLogFile implements SharedRefLogger {
  private static final String LOG_NAME = "sharedref_log";
  private Logger sharedRefDBLog;
  private final GitRepositoryManager gitRepositoryManager;
  private static final Gson gson = OutputFormat.JSON_COMPACT.newGson();

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /**
   * Constructs a {@code Log4jSharedRefLogger} instance with the provided systemLog used to create
   * the Log4J apppender and the repository manager to hydrate ref information with extra
   * information, such as committer, before logging.
   *
   * @param systemLog systemLog to create log appender
   * @param gitRepositoryManager to hydrate log entries with commit data
   */
  @Inject
  public Log4jSharedRefLogger(SystemLog systemLog, GitRepositoryManager gitRepositoryManager) {
    super(systemLog, LOG_NAME, new PatternLayout("[%d{ISO8601}] [%t] %-5p : %m%n"));
    this.gitRepositoryManager = gitRepositoryManager;
    sharedRefDBLog = LoggerFactory.getLogger(LOG_NAME);
  }

  /**
   * {@inheritDoc}.
   *
   * <p>Only logs commits and blob refs (throws {@link IncorrectObjectTypeException} for any other
   * unexpected type). Additionally, it hydrates commits with information about the committer and
   * commit message.
   *
   * <p>Logs Json serialization of {@link SharedRefLogEntry.UpdateRef}
   */
  @Override
  public void logRefUpdate(String project, Ref currRef, ObjectId newRefValue) {
    if (!ObjectId.zeroId().equals(newRefValue)) {
      try (Repository repository = gitRepositoryManager.openRepository(Project.nameKey(project));
          RevWalk walk = new RevWalk(repository)) {
        GitPerson committer = null;
        String commitMessage = null;
        if (newRefValue != null) {
          int objectType = walk.parseAny(newRefValue).getType();
          switch (objectType) {
            case OBJ_COMMIT:
              RevCommit commit = walk.parseCommit(newRefValue);
              committer = CommonConverters.toGitPerson(commit.getCommitterIdent());
              commitMessage = commit.getShortMessage();
              break;
            case OBJ_BLOB:
              break;
            default:
              throw new IncorrectObjectTypeException(newRefValue, Constants.typeString(objectType));
          }
        }
        sharedRefDBLog.info(
            gson.toJson(
                new SharedRefLogEntry.UpdateRef(
                    project,
                    currRef.getName(),
                    currRef.getObjectId().getName(),
                    newRefValue == null ? ObjectId.zeroId().name() : newRefValue.getName(),
                    committer,
                    commitMessage)));
      } catch (IOException e) {
        logger.atSevere().withCause(e).log(
            "Cannot log sharedRefDB interaction for ref %s on project %s",
            currRef.getName(), project);
      }
    } else {
      sharedRefDBLog.info(
          gson.toJson(
              new SharedRefLogEntry.DeleteRef(
                  project, currRef.getName(), currRef.getObjectId().getName())));
    }
  }

  /**
   * {@inheritDoc}.
   *
   * <p>Logs Json serialization of {@link SharedRefLogEntry.UpdateRef} or a {@link
   * SharedRefLogEntry.DeleteRef}, when 'newRefValue' is null
   */
  @Override
  public <T> void logRefUpdate(String project, String refName, T currRef, T newRefValue) {
    if (newRefValue != null) {
      sharedRefDBLog.info(
          gson.toJson(
              new SharedRefLogEntry.UpdateRef(
                  project, refName, safeToString(currRef), safeToString(newRefValue), null, null)));
    } else {
      sharedRefDBLog.info(
          gson.toJson(new SharedRefLogEntry.DeleteRef(project, refName, safeToString(currRef))));
    }
  }

  /**
   * {@inheritDoc}.
   *
   * <p>Logs Json serialization of {@link SharedRefLogEntry.DeleteProject}
   */
  @Override
  public void logProjectDelete(String project) {
    sharedRefDBLog.info(gson.toJson(new SharedRefLogEntry.DeleteProject(project)));
  }

  /**
   * {@inheritDoc}.
   *
   * <p>Logs Json serialization of {@link SharedRefLogEntry.LockAcquire}
   */
  @Override
  public void logLockAcquisition(String project, String refName) {
    sharedRefDBLog.info(gson.toJson(new SharedRefLogEntry.LockAcquire(project, refName)));
  }

  /**
   * {@inheritDoc}.
   *
   * <p>Logs Json serialization of {@link SharedRefLogEntry.LockRelease}
   */
  @Override
  public void logLockRelease(String project, String refName) {
    sharedRefDBLog.info(gson.toJson(new SharedRefLogEntry.LockRelease(project, refName)));
  }

  @VisibleForTesting
  public void setLogger(Logger logger) {
    this.sharedRefDBLog = logger;
  }

  private <T> String safeToString(T currRef) {
    if (currRef == null) {
      return "<null>";
    }
    return currRef.toString();
  }
}
