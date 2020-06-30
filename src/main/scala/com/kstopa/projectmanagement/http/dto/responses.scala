package com.kstopa.projectmanagement.http.dto

import java.time.{Duration, LocalDateTime}

import cats.effect.Sync
import com.kstopa.projectmanagement.model.{MaybeDeletedProject, MaybeDeletedProjectWithTasks, MaybeDeletedTask, Project, ProjectWithTasks, Statistics, StatisticsList, Task}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.jsonEncoderOf
import org.http4s.circe.jsonOf
import io.circe.generic.auto._


case class ProjectWithTasksDTO(project: ProjectDTO, tasks: List[MaybeDeletedTaskDTO], totalTime: Duration)
object ProjectWithTasksDTO {
  implicit val projectWithTasksDTOEncoder: Encoder[ProjectWithTasksDTO]                            = deriveEncoder[ProjectWithTasksDTO]
  implicit def projectWithTasksDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, ProjectWithTasksDTO] = jsonEncoderOf

  def fromService(projectWithTasks: ProjectWithTasks): ProjectWithTasksDTO =
    ProjectWithTasksDTO(
      ProjectDTO.fromService(projectWithTasks.project),
      projectWithTasks.tasks.map(MaybeDeletedTaskDTO.fromService),
      projectWithTasks.totalTime,
    )
}

case class MaybeDeletedProjectWithTasksDTO(project: MaybeDeletedProjectDTO, tasks: List[MaybeDeletedTaskDTO], totalTime: Duration)
object MaybeDeletedProjectWithTasksDTO {
  implicit val projectWithTasksDTOEncoder: Encoder[MaybeDeletedProjectWithTasksDTO]                            = deriveEncoder[MaybeDeletedProjectWithTasksDTO]
  implicit def projectWithTasksDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, MaybeDeletedProjectWithTasksDTO] = jsonEncoderOf

  def fromService(projectWithTasks: MaybeDeletedProjectWithTasks): MaybeDeletedProjectWithTasksDTO =
    MaybeDeletedProjectWithTasksDTO(
      MaybeDeletedProjectDTO.fromService(projectWithTasks.project),
      projectWithTasks.tasks.map(MaybeDeletedTaskDTO.fromService),
      projectWithTasks.totalTime,
    )
}

case class TaskDTO(
  id: Int,
  projectId: Int,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  author: String,
  comment: Option[String],
  volume: Option[Int],
)
object TaskDTO {
  implicit val taskDTOEncoder: Encoder[TaskDTO]                            = deriveEncoder[TaskDTO]
  implicit def taskDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, TaskDTO] = jsonEncoderOf
  implicit def taskDTOEntityDecoder[F[_]: Sync]: EntityDecoder[F, TaskDTO] = jsonOf

  def fromService(task: Task): TaskDTO =
    TaskDTO(
      task.id.value,
      task.projectId.value,
      task.startTime,
      task.endTime,
      task.author,
      task.comment,
      task.volume
    )
}

case class MaybeDeletedTaskDTO(
  id: Int,
  projectId: Int,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  author: String,
  comment: Option[String],
  volume: Option[Int],
  deletedOn: Option[LocalDateTime],
)
object MaybeDeletedTaskDTO {
  implicit val taskDTOEncoder: Encoder[MaybeDeletedTaskDTO]                            = deriveEncoder[MaybeDeletedTaskDTO]
  implicit def taskDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, MaybeDeletedTaskDTO] = jsonEncoderOf

  def fromService(task: MaybeDeletedTask): MaybeDeletedTaskDTO =
    MaybeDeletedTaskDTO(
      task.id.value,
      task.projectId.value,
      task.startTime,
      task.endTime,
      task.author,
      task.comment,
      task.volume,
      task.deletedOn,
    )
}

case class ProjectDTO(
  id: Int,
  name: String,
  author: String,
  created_on: LocalDateTime
)
object ProjectDTO {
  implicit val projectDTOEncode: Encoder[ProjectDTO]                             = deriveEncoder[ProjectDTO]
  implicit def projectDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, ProjectDTO] = jsonEncoderOf
  implicit def projectDTOEntityDecoder[F[_]: Sync]: EntityDecoder[F, ProjectDTO] = jsonOf

  def fromService(project: Project): ProjectDTO =
    ProjectDTO(
      project.id.value,
      project.name,
      project.author,
      project.createdOn
    )
}

case class MaybeDeletedProjectDTO(
                                   id: Int,
                                   name: String,
                                   author: String,
                                   createdOn: LocalDateTime,
                                   deletedOn: Option[LocalDateTime]
)
object MaybeDeletedProjectDTO {
  implicit val maybeDeletedProjectDTOEncode: Encoder[MaybeDeletedProjectDTO]                             = deriveEncoder[MaybeDeletedProjectDTO]
  implicit def maybeDeletedProjectDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, MaybeDeletedProjectDTO] = jsonEncoderOf
  implicit def maybeDeletedProjectDTOEntityDecoder[F[_]: Sync]: EntityDecoder[F, MaybeDeletedProjectDTO] = jsonOf

  def fromService(project: MaybeDeletedProject): MaybeDeletedProjectDTO =
    MaybeDeletedProjectDTO(
      project.id.value,
      project.name,
      project.author,
      project.createdOn,
      project.deletedOn,
    )
}

case class ListOfProjectsWithTasksDTO(projects: List[MaybeDeletedProjectWithTasksDTO])
object ListOfProjectsWithTasksDTO {
  implicit val listOfProjectsWithTasksDTOEncoder: Encoder[ListOfProjectsWithTasksDTO] =
    deriveEncoder[ListOfProjectsWithTasksDTO]
  implicit def listOfProjectsWithTasksDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, ListOfProjectsWithTasksDTO] =
    jsonEncoderOf
}

case class StatisticsDTO(
  userId: String,
  numberOfTasks: Int,
  numberOfTasksWithVolume: Int,
  averageDuration: Duration,
  averageVolume: Option[Float],
  averageDurationPerVolume: Option[Duration],
)
object StatisticsDTO {
  implicit val statisticsDTOEncode: Encoder[StatisticsDTO]                             = deriveEncoder[StatisticsDTO]
  implicit def statisticsDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, StatisticsDTO] = jsonEncoderOf

  def fromService(statistics: Statistics): StatisticsDTO =
    StatisticsDTO(
      statistics.userId,
      statistics.numberOfTasks,
      statistics.numberOfTasksWithVolume,
      Duration.ofSeconds(statistics.averageDuration),
      statistics.averageVolume,
      statistics.averageDurationPerVolume.map(seconds => Duration.ofSeconds(seconds)),
    )
}

case class StatisticsListDTO(
  statistics: List[StatisticsDTO]
)
object StatisticsListDTO {
  implicit val statisticsListDTOEncode: Encoder[StatisticsListDTO]                             = deriveEncoder[StatisticsListDTO]
  implicit def statisticsListDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, StatisticsListDTO] = jsonEncoderOf

  def fromService(statisticsList: StatisticsList): StatisticsListDTO =
    StatisticsListDTO(statisticsList.statistics.map(StatisticsDTO.fromService))
}
