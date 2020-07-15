package com.kstopa.projectmanagement.core.task

import java.time.LocalDateTime

import cats.free.Free
import com.kstopa.projectmanagement.core.project.ProjectId
import com.kstopa.projectmanagement.entities.TaskDeletionResult.{TaskDeleted, TaskNotDeleted}
import com.kstopa.projectmanagement.entities.TaskInsertionResult.{ProjectNotExistsError, StartAfterEndTimeError, TaskInserted}
import com.kstopa.projectmanagement.entities._
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.implicits.javatime._

trait TaskRepository[F[_]] {
  def insert(
    authUser: AuthUser
  )(
    projectId: ProjectId,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    volume: Option[TaskVolume],
    comment: Option[TaskComment],
  ): F[TaskInsertionResult]
  def softDeleteForProjectId(projectId: ProjectId): F[List[Task]]
  def softDelete(authUser: AuthUser)(taskId: TaskId): F[TaskDeletionResult]
  def getOverlappingTasksCountForUser(
    authUser: AuthUser
  )(startTime: LocalDateTime, endTime: LocalDateTime): F[Int]
  def getTasksForProject(projectId: ProjectId): F[List[MaybeDeletedTask]]
}

class TaskRepositoryImpl() extends TaskRepository[ConnectionIO] {
  def insert(
    authUser: AuthUser
  )(
    projectId: ProjectId,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    volume: Option[TaskVolume],
    comment: Option[TaskComment],
  ): ConnectionIO[TaskInsertionResult] =
    insertQuery(authUser, projectId, startTime, endTime, volume, comment)
      .withUniqueGeneratedKeys[Task]("id", "project_id", "start_time", "end_time", "author", "comment", "volume")
      .map(TaskInserted(_): TaskInsertionResult)
      .exceptSomeSqlState {
        case sqlstate.class23.FOREIGN_KEY_VIOLATION => Free.pure(ProjectNotExistsError)
        case sqlstate.class23.CHECK_VIOLATION       => Free.pure(StartAfterEndTimeError)
      }

  def insertQuery(
    authUser: AuthUser,
    projectId: ProjectId,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    volume: Option[TaskVolume],
    comment: Option[TaskComment],
  ) =
    sql"INSERT INTO tasks(project_id, start_time, end_time, author, volume, comment) VALUES ($projectId, $startTime, $endTime, ${authUser.userId}, $volume, $comment)".update

  def softDeleteForProjectId(projectId: ProjectId): ConnectionIO[List[Task]] =
    for {
      _ <- insertIntoDeletedQuery(projectId).run
      deleted <- fr"DELETE FROM tasks WHERE project_id = $projectId".update
        .withGeneratedKeysWithChunkSize[Task](
          "id",
          "project_id",
          "start_time",
          "end_time",
          "author",
          "comment",
          "volume"
        )(1)
        .compile
        .toList
    } yield deleted

  def insertIntoDeletedQuery(projectId: ProjectId) =
    fr"""INSERT INTO deleted_tasks(id, project_id, start_time, end_time, author, volume, comment, deleted_on)
        |             SELECT id, project_id, start_time, end_time, author, volume, comment, now()
        |             FROM tasks
        |             WHERE project_id = $projectId""".stripMargin.update

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
    getTasksForProjectQuery(projectId)
      .to[List]

  def getTasksForProjectQuery(projectId: ProjectId): Query0[MaybeDeletedTask] =
    sql"""SELECT id, project_id, start_time, end_time, author, comment, volume, null FROM tasks
          UNION SELECT id, project_id, start_time, end_time, author, comment, volume, deleted_on FROM deleted_tasks
          WHERE project_id = ${projectId.value}"""
      .query[MaybeDeletedTask]
}
