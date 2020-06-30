package com.kstopa.projectmanagement.service

import java.time.LocalDateTime

import cats.data.OptionT
import cats.effect.Bracket
import cats.free.Free.Pure
import com.kstopa.projectmanagement.model.ProjectDeletionResult.{ProjectDeleted, ProjectNotDeleted}
import com.kstopa.projectmanagement.model.{AuthUser, Project, ProjectDeletionResult, ProjectId, ProjectInsertionResult, ProjectRenameResult, TaskDeletionResult, TaskId, TaskInsertionResult, TaskUpdateResult}
import com.kstopa.projectmanagement.repository.{ProjectRepository, StatisticsRepository, TaskRepository}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import cats.effect._
import cats.free.Free.Pure
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres._
import doobie.implicits.javatime._
import cats.implicits._
import com.kstopa.projectmanagement.model
import com.kstopa.projectmanagement.model.TaskUpdateResult.{TaskNotUpdated, TaskUpdated}

class TaskService[F[_]](
  taskRepository: TaskRepository,
  statisticsRepository: StatisticsRepository,
  transactor: HikariTransactor[F]
)(implicit bracket: Bracket[F, Throwable]) {

  def insert(authUser: AuthUser)(
    projectId: ProjectId,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    volume: Option[Int],
    comment: Option[String],
  ): F[TaskInsertionResult] =
    (for {
      overlappingTasksCount <- taskRepository.getOverlappingTasksCountForUser(authUser)(startTime, endTime)
      result <- if (overlappingTasksCount > 0)
        (TaskInsertionResult.TaskOverlapsOtherTask: TaskInsertionResult).pure[ConnectionIO]
      else
        taskRepository.insert(authUser)(projectId, startTime, endTime, volume, comment)
      _ <- result match {
        case TaskInsertionResult.TaskInserted(task) => statisticsRepository.addTask(task)
        case _                                      => AsyncConnectionIO.pure(())
      }
    } yield result).transact(transactor)

  def delete(authUser: AuthUser)(taskId: TaskId): F[TaskDeletionResult] =
    (for {
      result <- taskRepository.softDelete(authUser)(taskId)
      _ <- result match {
        case TaskDeletionResult.TaskDeleted(task) => statisticsRepository.removeTask(task)
        case _                                    => AsyncConnectionIO.pure(())
      }
    } yield result).transact(transactor)

  def update(
    authUser: AuthUser
  )(id: TaskId)(
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    volume: Option[Int],
    comment: Option[String],
  ): F[TaskUpdateResult] =
    (for {
      deletionResult <- taskRepository.softDelete(authUser)(id)
      result <- deletionResult match {
        case TaskDeletionResult.TaskDeleted(deletedTask) =>
          for {
            overlappingTasksCount <- taskRepository.getOverlappingTasksCountForUser(authUser)(startTime, endTime)
            result <- if (overlappingTasksCount > 0)
              (TaskInsertionResult.TaskOverlapsOtherTask: TaskInsertionResult).pure[ConnectionIO]
            else
              taskRepository.insert(authUser)(deletedTask.projectId, startTime, endTime, volume, comment)
          } yield
            result match {
              case TaskInsertionResult.TaskInserted(insertedTask) =>
                TaskUpdated(deletedTask, insertedTask): TaskUpdateResult
              case TaskInsertionResult.StartAfterEndTimeError =>
                TaskUpdateResult.StartAfterEndTimeError: TaskUpdateResult
              case TaskInsertionResult.TaskOverlapsOtherTask => TaskUpdateResult.TaskOverlapsOtherTask: TaskUpdateResult
              case _                                         => TaskUpdateResult.TaskNotUpdated: TaskUpdateResult
            }
        case TaskDeletionResult.TaskNotDeleted => (TaskNotUpdated: TaskUpdateResult).pure[ConnectionIO]
      }
      _ <- result match {
        case TaskUpdated(oldTask, newTask) =>
          for {
            _ <- statisticsRepository.removeTask(oldTask)
            _ <- statisticsRepository.addTask(newTask)
          } yield ()
        case _ => AsyncConnectionIO.pure(())
      }
    } yield result).transact(transactor)
}
