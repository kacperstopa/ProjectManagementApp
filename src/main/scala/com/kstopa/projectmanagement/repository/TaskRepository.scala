package com.kstopa.projectmanagement.repository

import java.time.{Duration, LocalDateTime}

import com.kstopa.projectmanagement.model.{AuthUser, Project, ProjectId, Task, TaskDeletionResult, TaskId, TaskInsertionResult, MaybeDeletedTask}
import doobie.implicits._
import doobie.postgres._
import doobie._
import cats._
import cats.data._
import cats.free.Free
import com.kstopa.projectmanagement.model.TaskDeletionResult.{TaskDeleted, TaskNotDeleted}
import com.kstopa.projectmanagement.model.TaskInsertionResult.{ProjectNotExistsError, StartAfterEndTimeError, TaskInserted, TaskOverlapsOtherTask}
import doobie.implicits._
import cats.implicits._
import doobie.implicits.javatime._


class TaskRepository() {

  def insert(
    authUser: AuthUser
  )(
    projectId: ProjectId,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    volume: Option[Int],
    comment: Option[String],
  ): ConnectionIO[TaskInsertionResult] =
    sql"INSERT INTO tasks(project_id, start_time, end_time, author, volume, comment) VALUES ($projectId, $startTime, $endTime, ${authUser.userId}, $volume, $comment)".update
      .withUniqueGeneratedKeys[Task]("id", "project_id", "start_time", "end_time", "author", "comment", "volume")
      .map(TaskInserted(_): TaskInsertionResult)
      .exceptSomeSqlState {
        case sqlstate.class23.FOREIGN_KEY_VIOLATION => Free.pure(ProjectNotExistsError)
        case sqlstate.class23.CHECK_VIOLATION => Free.pure(StartAfterEndTimeError)
      }

  def softDeleteForProjectId(projectId: ProjectId): ConnectionIO[List[Task]] =
    for {
      _ <- fr"""
             INSERT INTO deleted_tasks(id, project_id, start_time, end_time, author, volume, comment, deleted_on)
             SELECT id, project_id, start_time, end_time, author, volume, comment, now()
             FROM tasks
             WHERE project_id = $projectId""".update.run
      deleted <- fr"DELETE FROM tasks WHERE project_id = $projectId".update
        .withGeneratedKeysWithChunkSize[Task]("id", "project_id", "start_time", "end_time", "author", "comment", "volume")(1)
        .compile
        .toList
    } yield deleted

  def softDelete(authUser: AuthUser)(taskId: TaskId): ConnectionIO[TaskDeletionResult] =
    for {
      _ <- sql"""
             INSERT INTO deleted_tasks(id, project_id, start_time, end_time, author, volume, comment, deleted_on)
             SELECT id, project_id, start_time, end_time, author, volume, comment, now()
             FROM tasks
             WHERE id = $taskId AND author = ${authUser.userId}""".update.run
      deletionResult <- sql"DELETE FROM tasks WHERE id = $taskId AND author = ${authUser.userId}".update
        .withGeneratedKeys[Task]("id", "project_id", "start_time", "end_time", "author", "comment", "volume")
        .head
        .compile
        .toList
        .map(_.headOption.fold[TaskDeletionResult](TaskNotDeleted)(TaskDeleted))
    } yield deletionResult

  def getOverlappingTasksCountForUser(
    authUser: AuthUser
  )(startTime: LocalDateTime, endTime: LocalDateTime): ConnectionIO[Int] =
    sql"SELECT COUNT(*) FROM tasks where author = ${authUser.userId} AND start_time < $endTime AND end_time > $startTime"
      .query[Int]
      .unique

  def getTasksForProject(projectId: ProjectId): ConnectionIO[List[MaybeDeletedTask]] =
    sql"""SELECT id, project_id, start_time, end_time, author, comment, volume, null FROM tasks
          UNION SELECT id, project_id, start_time, end_time, author, comment, volume, deleted_on FROM deleted_tasks
          WHERE project_id = ${projectId.value}"""
      .query[MaybeDeletedTask]
      .to[List]

  def getSumOfTimeForProject(projectId: ProjectId): ConnectionIO[Long] =
    sql"SELECT sum(age(end_time, start_time)) FROM tasks WHERE project_id = ${projectId.value}"
      .query[Long]
      .unique
}
