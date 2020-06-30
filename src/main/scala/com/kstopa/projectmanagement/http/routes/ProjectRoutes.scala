package com.kstopa.projectmanagement.http.routes

import cats.effect.Sync
import cats.syntax.all._
import com.kstopa.projectmanagement.http.dto._
import com.kstopa.projectmanagement.model._
import com.kstopa.projectmanagement.service.ProjectService
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl

object ProjectRoutes {
  def create[F[_]: Sync](
    projectService: ProjectService[F],
  ): AuthedRoutes[AuthUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    AuthedRoutes.of[AuthUser, F] {
      case req @ POST -> Root as user =>
        for {
          createProjectDTO <- req.req.as[CreateProjectDTO]
          insertionResult  <- projectService.insert(user)(createProjectDTO.name)
          response <- insertionResult match {
            case ProjectInsertionResult.ProjectInserted(project) => Created(ProjectDTO.fromService(project))
            case ProjectInsertionResult.ProjectNotInserted       => BadRequest(ErrorResponse("Project not inserted"))
            case ProjectInsertionResult.ProjectAlreadyExistsError =>
              BadRequest(ErrorResponse("Project with that name already exists"))
          }
        } yield response
      case req @ PUT -> Root / IntVar(id) as user =>
        for {
          createProjectDTO <- req.req.as[RenameProjectDTO]
          renameResult     <- projectService.rename(user)(ProjectId(id), createProjectDTO.name)
          response <- renameResult match {
            case ProjectRenameResult.ProjectRenamed(_) => Ok()
            case ProjectRenameResult.ProjectAlreadyExistsError =>
              BadRequest(ErrorResponse("Project with that name already exists"))
          }
        } yield response
      case DELETE -> Root / IntVar(id) as user =>
        for {
          deleteResult <- projectService.delete(user)(ProjectId(id))
          response <- deleteResult match {
            case ProjectDeletionResult.ProjectDeleted(_, _) => Ok()
            case ProjectDeletionResult.ProjectNotDeleted    => BadRequest(ErrorResponse("Project not deleted"))
          }
        } yield response
      case GET -> Root / IntVar(id) as _ =>
        for {
          projectOption <- projectService.getProjectWithTasks(ProjectId(id))
          response <- projectOption match {
            case Some(projectWithTasks) => Ok(ProjectWithTasksDTO.fromService(projectWithTasks))
            case None                   => NotFound()
          }
        } yield response
      case req @ POST -> Root / "query" as _ =>
        for {
          projectQueryDTO <- req.req.as[ProjectQueryDTO]
          projects <- projectService.query(
            projectQueryDTO.ids.map(_.map(ProjectId)),
            projectQueryDTO.from,
            projectQueryDTO.to,
            projectQueryDTO.deleted,
            projectQueryDTO.sortBy.map(_.toService),
            projectQueryDTO.order.map(_.toService),
          )(projectQueryDTO.page, projectQueryDTO.size)
          result <- Ok(ListOfProjectsWithTasksDTO(projects.map(MaybeDeletedProjectWithTasksDTO.fromService)))
        } yield result
    }
  }
}
