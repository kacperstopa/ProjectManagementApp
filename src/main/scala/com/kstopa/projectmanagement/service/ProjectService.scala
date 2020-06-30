package com.kstopa.projectmanagement.service

import java.time.{Duration, LocalDateTime}

import cats.data.{NonEmptyList, OptionT}
import cats.effect.Bracket
import com.kstopa.projectmanagement.model.ProjectDeletionResult.{ProjectDeleted, ProjectNotDeleted}
import com.kstopa.projectmanagement.model._
import com.kstopa.projectmanagement.repository.{ProjectRepository, StatisticsRepository, TaskRepository}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.implicits._
import cats.implicits._

class ProjectService[F[_]](
  projectRepository: ProjectRepository,
  taskRepository: TaskRepository,
  statisticsRepository: StatisticsRepository,
  transactor: HikariTransactor[F]
)(implicit bracket: Bracket[F, Throwable]) {

  def insert(authUser: AuthUser)(name: String): F[ProjectInsertionResult] =
    projectRepository.insert(authUser)(name).transact(transactor)

  def delete(authUser: AuthUser)(id: ProjectId): F[ProjectDeletionResult] =
    (for {
      project        <- OptionT(projectRepository.getForIdAndAuthor(id, authUser.userId))
      tasks          <- OptionT.liftF(taskRepository.softDeleteForProjectId(project.id))
      deletedProject <- OptionT(projectRepository.softDelete(authUser)(id))
      _              <- OptionT.liftF(tasks.map(statisticsRepository.removeTask).sequence)
    } yield (ProjectDeleted(deletedProject, tasks))).getOrElse(ProjectNotDeleted).transact(transactor)

  def rename(
    authUser: AuthUser
  )(projectId: ProjectId, newName: String): F[ProjectRenameResult] =
    projectRepository.rename(authUser)(projectId, newName).transact(transactor)

  def getProjectWithTasks(projectId: ProjectId): F[Option[ProjectWithTasks]] =
    (for {
      project   <- OptionT(projectRepository.get(projectId))
      tasks     <- OptionT.liftF(taskRepository.getTasksForProject(project.id))
      totalTime <- OptionT.liftF(AsyncConnectionIO.pure(tasksToDuration(tasks)))
    } yield ProjectWithTasks(project, tasks, totalTime)).value.transact(transactor)

  def query(
    ids: Option[NonEmptyList[ProjectId]],
    from: Option[LocalDateTime],
    to: Option[LocalDateTime],
    deleted: Option[Boolean],
    sortBy: Option[SortBy],
    order: Option[Order],
  )(page: Int, size: Int): F[List[MaybeDeletedProjectWithTasks]] =
    (for {
      project   <- projectRepository.query(ids, from, to, deleted, sortBy, order)(page, size)
      tasks     <- fs2.Stream.eval(taskRepository.getTasksForProject(project.id))
      totalTime <- fs2.Stream.eval(AsyncConnectionIO.pure(tasksToDuration(tasks)))
    } yield MaybeDeletedProjectWithTasks(project, tasks, totalTime)).compile.toList.transact(transactor)

  def getSumOftime(projectId: ProjectId): F[Long] =
    taskRepository.getSumOfTimeForProject(projectId).transact(transactor)

  private def tasksToDuration(tasks: List[MaybeDeletedTask]): Duration =
    Duration.ofNanos(tasks.map(task => Duration.between(task.startTime, task.endTime).toNanos).sum)
}
