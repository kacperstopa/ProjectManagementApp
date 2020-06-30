package com.kstopa.projectmanagement.repository

import java.time.{Duration, LocalDate, LocalDateTime}

import com.kstopa.projectmanagement.model.{AuthUser, Project, ProjectId, ProjectInsertionResult, ProjectRenameResult, SortBy, Statistics, Task}
import cats.data.{NonEmptyList, OptionT}
import cats.effect._
import cats.free.Free
import cats.free.Free.Pure
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres._
import doobie.implicits.javatime._
import cats.implicits._
import com.kstopa.projectmanagement.model.ProjectInsertionResult.ProjectInserted
import com.kstopa.projectmanagement.model.ProjectRenameResult.ProjectRenamed
import doobie.util.fragment.Fragment
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._

import scala.util.Try

case class SingleMonthStatistics(
  userId: String,
  month: LocalDate,
  numberOfTasks: Int,
  numberOfTasksWithVolume: Int,
  averageDuration: Int,
  averageVolume: Option[Float],
  averageDurationPerVolume: Option[Float]
)

class StatisticsRepository {
  def addTask(task: Task): ConnectionIO[Unit] =
    for {
      singleMonthStatistics <- selectSingleMonthStatisticsForTask(task)
      _ <- singleMonthStatistics match {
        case Some(value) =>
          val duration = Duration
            .between(task.startTime, task.endTime)
            .getSeconds
          (fr"""UPDATE statistics
             |SET
             |number_of_tasks = ${value.numberOfTasks + 1},""".stripMargin ++
          task.volume
            .map(
              volume =>
                fr"""
                  |number_of_tasks_with_volume = ${value.numberOfTasksWithVolume + 1},
                  |average_volume = ${(value.averageVolume
                      .getOrElse(0f) * value.numberOfTasksWithVolume + volume) / (value.numberOfTasksWithVolume + 1)},
                  |average_duration_per_volume = ${(value.averageDurationPerVolume
                      .getOrElse(0f) * value.numberOfTasksWithVolume + duration / volume) / (value.numberOfTasksWithVolume + 1)},
                  |""".stripMargin
            )
            .getOrElse(Fragment.empty) ++
          fr"""|average_duration = ${(value.averageDuration * value.numberOfTasks + duration) / (value.numberOfTasks + 1)}
             |""".stripMargin ++ whereCondition(task)).update.run
        case None =>
          sql"""INSERT INTO statistics(user_id, month, number_of_tasks, number_of_tasks_with_volume, average_duration, average_volume, average_duration_per_volume)
             |VALUES(
             |${task.author},
             | ${task.startTime.toLocalDate.withDayOfMonth(1)},
             |  1,
             |  ${task.volume.fold(0)(_ => 1)},
             |   ${Duration.between(task.startTime, task.endTime).getSeconds},
             |    ${task.volume},
             |     ${task.volume.map(Duration.between(task.startTime, task.endTime).getSeconds / _)}
             |     )
             |""".stripMargin.update.run
      }
    } yield ()

  def removeTask(task: Task): ConnectionIO[Unit] =
    for {
      singleMonthStatistics <- selectSingleMonthStatisticsForTask(task)
      _ <- singleMonthStatistics match {
        case Some(value) =>
          val duration = Duration
            .between(task.startTime, task.endTime)
            .getSeconds
          (if (value.numberOfTasks == 1) {
             sql"""DELETE FROM statistics
                 |""".stripMargin ++ whereCondition(task)
           } else {
             fr"""UPDATE statistics
               |SET
               |number_of_tasks = ${value.numberOfTasks - 1},""".stripMargin ++
             task.volume
               .map(
                 volume =>
                   fr"""
                      |number_of_tasks_with_volume = ${value.numberOfTasksWithVolume - 1},
                      |average_volume = ${Try(
                         (value.averageVolume
                           .getOrElse(0f) * value.numberOfTasksWithVolume - volume) / (value.numberOfTasksWithVolume - 1)
                       ).toOption},
                      |average_duration_per_volume = ${Try(
                         (value.averageDurationPerVolume
                           .getOrElse(0f) * value.numberOfTasksWithVolume - duration / volume) / (value.numberOfTasksWithVolume - 1)
                       ).toOption},
                      |""".stripMargin
               )
               .getOrElse(Fragment.empty) ++
             fr"""|average_duration = ${(value.averageDuration * value.numberOfTasks - duration) / (value.numberOfTasks - 1)}
                  |""".stripMargin ++ whereCondition(task)
           }).update.run
        case _ => AsyncConnectionIO.pure(())
      }
    } yield ()


  def getStatistics(users: List[String], from: LocalDate, to: LocalDate): ConnectionIO[List[Statistics]] =
    sql"""
         |SELECT
         | user_id,
         | SUM(number_of_tasks),
         | SUM(number_of_tasks_with_volume),
         | SUM(number_of_tasks * average_duration) / SUM(number_of_tasks),
         | SUM(number_of_tasks_with_volume * average_volume) / SUM(number_of_tasks_with_volume),
         | SUM(number_of_tasks_with_volume * average_duration_per_volume) / SUM(number_of_tasks_with_volume)
         |FROM statistics
         |WHERE month >= $from AND month <= $to
         |GROUP BY user_id
         |""".stripMargin.query[Statistics].to[List]


  private def selectSingleMonthStatisticsForTask(task: Task): ConnectionIO[Option[SingleMonthStatistics]] =
    (sql"""SELECT user_id, month, number_of_tasks, number_of_tasks_with_volume, average_duration, average_volume, average_duration_per_volume
          FROM statistics
         """.stripMargin ++ whereCondition(task)).query[SingleMonthStatistics].option

  private def whereCondition(task: Task): Fragment =
    fr"""|WHERE
       | user_id = ${task.author} AND
       | extract(month from month) = ${task.startTime.getMonthValue} AND
       | extract(year from month) = ${task.startTime.getYear}
       |""".stripMargin
}
