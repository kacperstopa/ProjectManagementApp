package com.kstopa.projectmanagement.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class AuthUser(userId: String)
object AuthUser {
  implicit val authUserDecoder: Decoder[AuthUser] = deriveDecoder[AuthUser]
}
