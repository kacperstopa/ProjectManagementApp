package com.kstopa.projectmanagement.http.dto

import cats.effect.Sync
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.http4s.EntityEncoder
import org.http4s.circe._

case class ErrorResponse(message: String)
object ErrorResponse {
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  implicit def errorResponseEntityEncoder[F[_] : Sync]: EntityEncoder[F, ErrorResponse] = jsonEncoderOf
}