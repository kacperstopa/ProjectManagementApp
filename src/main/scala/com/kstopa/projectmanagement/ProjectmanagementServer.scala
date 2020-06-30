package com.kstopa.projectmanagement

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, Resource, Timer}
import com.kstopa.projectmanagement.config.Config
import com.kstopa.projectmanagement.db.Database
import com.kstopa.projectmanagement.http.authentication.JwtAuthentication
import com.kstopa.projectmanagement.http.routes.{ProjectRoutes, StatisticsRoutes, TaskRoutes}
import com.kstopa.projectmanagement.repository.{ProjectRepository, StatisticsRepository, TaskRepository}
import com.kstopa.projectmanagement.service.{ProjectService, StatisticsService, TaskService}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object ProjectmanagementServer {
  case class Resources(transactor: HikariTransactor[IO], config: Config)

  def create(configFile: String = "application.conf")(
    implicit contextShift: ContextShift[IO],
    concurrentEffect: ConcurrentEffect[IO],
    timer: Timer[IO]
  ): IO[ExitCode] =
    resources(configFile).use(create)

  private def resources(
    configFile: String
  )(implicit contextShift: ContextShift[IO]): Resource[IO, Resources] =
    for {
      config     <- Config.load(configFile)
      ec         <- ExecutionContexts.fixedThreadPool[IO](config.database.threadPoolSize)
      blocker    <- Blocker[IO]
      transactor <- Database.transactor(config.database, ec, blocker)
    } yield Resources(transactor, config)

  private def create(resources: Resources)(
    implicit concurrentEffect: ConcurrentEffect[IO],
    timer: Timer[IO]
  ): IO[ExitCode] = {
    val projectRepository    = new ProjectRepository()
    val taskRepository       = new TaskRepository()
    val statisticsRepository = new StatisticsRepository()
    val projectService =
      new ProjectService(projectRepository, taskRepository, statisticsRepository, resources.transactor)
    val taskService       = new TaskService(taskRepository, statisticsRepository, resources.transactor)
    val statisticsService = new StatisticsService(statisticsRepository, resources.transactor)
    val authMiddleware    = JwtAuthentication.getMiddleware(resources.config.authentication.secret)

    val projectRoutes    = authMiddleware(ProjectRoutes.create(projectService))
    val taskRoutes       = authMiddleware(TaskRoutes.create(taskService))
    val statisticsRoutes = authMiddleware(StatisticsRoutes.create(statisticsService))

    val httpApp =
      Router(
        "/projects" -> projectRoutes,
        "/tasks"    -> taskRoutes,
        "/statistics" -> statisticsRoutes,
      ).orNotFound

    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    for {
      exitCode <- BlazeServerBuilder[IO](global)
        .bindHttp(resources.config.server.port, resources.config.server.host)
        .withHttpApp(finalHttpApp)
        .serve
        .compile
        .lastOrError
    } yield exitCode
  }
}
