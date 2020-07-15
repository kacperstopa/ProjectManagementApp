package com.kstopa.projectmanagement.http.dto

import java.time.LocalDateTime

import cats.data.NonEmptyList
import cats.effect.Sync
import com.kstopa.projectmanagement.entities.{Order, SortBy, YearWithMonth}
import io.circe.Decoder
import io.circe.generic.auto._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.jsonOf
import org.http4s.circe.jsonEncoderOf

case class CreateProjectDTO(name: String)
object CreateProjectDTO {
  implicit def createProjectDTODecoder[F[_]: Sync]: EntityDecoder[F, CreateProjectDTO]       = jsonOf
  implicit def createProjectDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, CreateProjectDTO] = jsonEncoderOf
}

case class RenameProjectDTO(name: String)
object RenameProjectDTO {
  implicit def renameProjectDTODecoder[F[_]: Sync]: EntityDecoder[F, RenameProjectDTO] = jsonOf
}

case class CreateTaskDTO(
  projectId: Int,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  comment: Option[String],
  volume: Option[Int],
)
object CreateTaskDTO {
  implicit def createTaskDTODecoder[F[_]: Sync]: EntityDecoder[F, CreateTaskDTO] = jsonOf
  implicit def createTaskDTOEncoder[F[_]: Sync]: EntityEncoder[F, CreateTaskDTO] = jsonEncoderOf
}

case class UpdateTaskDTO(
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  comment: Option[String],
  volume: Option[Int],
)
object UpdateTaskDTO {
  implicit def updateTaskDTODecoder[F[_]: Sync]: EntityDecoder[F, UpdateTaskDTO] = jsonOf
  implicit def updateTaskDTOEntityEncoder[F[_]: Sync]: EntityEncoder[F, UpdateTaskDTO] = jsonEncoderOf
}

case class ProjectQueryDTO(
  ids: Option[NonEmptyList[Int]],
  from: Option[LocalDateTime],
  to: Option[LocalDateTime],
  deleted: Option[Boolean],
  sortBy: Option[SortByDTO],
  order: Option[OrderDTO],
  page: Int,
  size: Int,
)
object ProjectQueryDTO {
  implicit def projectQueryDTODecoder[F[_]: Sync]: EntityDecoder[F, ProjectQueryDTO] = jsonOf
}

sealed trait SortByDTO {
  def toService: SortBy
}
object SortByDTO {
  case object CreationTimeDTO extends SortByDTO {
    override def toService: SortBy = SortBy.CreationTime
  }
  case object UpdateTimeDTO extends SortByDTO {
    override def toService: SortBy = SortBy.UpdateTime
  }

  implicit val sortByDTODecoder: Decoder[SortByDTO] = Decoder[String].emap {
    case "creationTime" => Right(CreationTimeDTO)
    case "updateTime"   => Right(UpdateTimeDTO)
    case other          => Left(s"Invalid sortBy: $other")
  }
}

sealed trait OrderDTO {
  def toService: Order
}
object OrderDTO {
  case object DescDTO extends OrderDTO {
    override def toService: Order = Order.Desc
  }
  case object AscDTO extends OrderDTO {
    override def toService: Order = Order.Asc
  }

  implicit val orderDTODecoder: Decoder[OrderDTO] = Decoder[String].emap {
    case "desc" => Right(DescDTO)
    case "asc"  => Right(AscDTO)
    case other  => Left(s"Invalid order: $other")
  }
}

case class GetStatisticsDTO(
  users: List[String],
  from: YearWithMonthDTO,
  to: YearWithMonthDTO,
)
object GetStatisticsDTO {
  implicit def getStatisticsDTODecoder[F[_]: Sync]: EntityDecoder[F, GetStatisticsDTO] = jsonOf
}

case class YearWithMonthDTO(year: Int, month: Int) {
  def toService: YearWithMonth =
    YearWithMonth(year, month)
}
object YearWithMonthDTO {
  val Regex = "([12]\\d{3})-(0[1-9]|1[0-2])".r

  implicit val yearWithMonthDTODecoder: Decoder[YearWithMonthDTO] = Decoder[String].emap {
    case Regex(year, month) => Right(YearWithMonthDTO(year.toInt, month.toInt))
    case other              => Left(s"Invalid year-month: $other")
  }
}
