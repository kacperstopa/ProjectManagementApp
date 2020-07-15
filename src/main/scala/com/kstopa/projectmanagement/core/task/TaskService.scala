package com.kstopa.projectmanagement.core.task

import java.time.LocalDateTime

import cats.effect.Bracket
import cats.implicits._
import com.kstopa.projectmanagement.core.statistics.StatisticsRepository
import com.kstopa.projectmanagement.entities.TaskUpdateResult.{TaskNotUpdated, TaskUpdated}
import com.kstopa.projectmanagement.entities._
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._

trait TaskService[F[_]] {
  def insert(authUser: AuthUser)(insertTaskEntity: InsertTaskEntity): F[TaskInsertionResult]
  def delete(authUser: AuthUser)(taskId: TaskId): F[TaskDeletionResult]
  def update(authUser: AuthUser)(id: TaskId)(updateTaskEntity: UpdateTaskEntity): F[TaskUpdateResult]
}

class TaskServiceImpl[F[_]](
  taskRepository: TaskRepository[ConnectionIO],
  statisticsRepository: StatisticsRepository[ConnectionIO],
  transactor: HikariTransactor[F]
)(implicit bracket: Bracket[F, Throwable]) {

  def insert(authUser: AuthUser)(
    insertTaskEntity: InsertTaskEntity
  ): F[TaskInsertionResult] = {
    val InsertTaskEntity(projectId, startTime, endTime, comment, volume) = insertTaskEntity
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
  }

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
    updateTaskEntity: UpdateTaskEntity
  ): F[TaskUpdateResult] = {
    def insertNewTask(
      deletedTask: Task
    )(startTime: LocalDateTime, endTime: LocalDateTime, comment: Option[TaskComment], volume: Option[TaskVolume]) =
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

    val UpdateTaskEntity(startTime, endTime, comment, volume) = updateTaskEntity
    (for {
      deletionResult <- taskRepository.softDelete(authUser)(id)
      result <- deletionResult match {
        case TaskDeletionResult.TaskDeleted(deletedTask) => insertNewTask(deletedTask)(startTime, endTime, comment, volume)
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
}
