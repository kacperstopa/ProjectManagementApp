package com.kstopa.projectmanagement.http.routes

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.effect.Sync
import cats.syntax.all._
import com.kstopa.projectmanagement.http.dto.{CreateTaskDTO, ErrorResponse, TaskDTO, UpdateTaskDTO}
import com.kstopa.projectmanagement.model._
import com.kstopa.projectmanagement.service.TaskService
import io.circe.{Decoder, Encoder}
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl

object TaskRoutes {

  implicit val dateDecoder = Decoder.decodeString.emap[LocalDateTime](str => {
    Either.catchNonFatal(LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)).leftMap(_.getMessage)
  })
  implicit val dateEncoder = Encoder.encodeString.contramap[LocalDateTime](_.format(DateTimeFormatter.ISO_DATE_TIME))

  def create[F[_]: Sync](
    taskService: TaskService[F],
  ): AuthedRoutes[AuthUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    AuthedRoutes.of[AuthUser, F] {
      case req @ POST -> Root as user =>
        for {
          createProjectDTO <- req.req.as[CreateTaskDTO]
          insertionResult <- taskService.insert(user)(
            ProjectId(createProjectDTO.projectId),
            createProjectDTO.startTime,
            createProjectDTO.endTime,
            createProjectDTO.volume,
            createProjectDTO.comment,
          )
          response <- insertionResult match {
            case TaskInsertionResult.TaskInserted(task) => Created(TaskDTO.fromService(task))
            case TaskInsertionResult.TaskOverlapsOtherTask =>
              BadRequest(ErrorResponse("Task time overlaps with other user's task"))
            case TaskInsertionResult.StartAfterEndTimeError =>
              BadRequest(ErrorResponse("Start time must be after end time"))
            case TaskInsertionResult.ProjectNotExistsError =>
              BadRequest(ErrorResponse("Given project not exists"))
          }
        } yield response
      case req @ PUT -> Root / IntVar(taskId) as user =>
        for {
          updateTaskDTO <- req.req.as[UpdateTaskDTO]
          update <- taskService.update(user)(TaskId(taskId))(
            updateTaskDTO.startTime,
            updateTaskDTO.endTime,
            updateTaskDTO.volume,
            updateTaskDTO.comment,
          )
          response <- update match {
            case TaskUpdateResult.TaskUpdated(oldTask, newTask) => Ok(TaskDTO.fromService(newTask))
            case TaskUpdateResult.TaskNotUpdated                => BadRequest(ErrorResponse("Task couldn't be updated"))
            case TaskUpdateResult.TaskOverlapsOtherTask         => BadRequest(ErrorResponse("Task overlaps other user's task"))
            case TaskUpdateResult.StartAfterEndTimeError =>
              BadRequest(ErrorResponse("Start time must be after end time"))
          }
        } yield response
      case DELETE -> Root / IntVar(taskId) as user =>
        for {
          deleteResult <- taskService.delete(user)(TaskId(taskId))
          response <- deleteResult match {
            case TaskDeletionResult.TaskDeleted(task) => Ok()
            case TaskDeletionResult.TaskNotDeleted    => BadRequest(ErrorResponse("Task couldn't be deleted"))
          }
        } yield response
    }
  }
}
