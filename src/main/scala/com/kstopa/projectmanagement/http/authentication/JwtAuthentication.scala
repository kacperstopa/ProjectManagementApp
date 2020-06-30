package com.kstopa.projectmanagement.http.authentication

import java.util.UUID

import cats.effect.IO
import cats.implicits._
import com.kstopa.projectmanagement.model.AuthUser
import dev.profunktor.auth._
import dev.profunktor.auth.jwt.{JwtToken, _}
import io.circe.parser.decode
import org.http4s.server.AuthMiddleware
import pdi.jwt.{JwtClaim, _}

import scala.util.Try

object JwtAuthentication {
  def getMiddleware(secretKey: String): AuthMiddleware[IO, AuthUser] = {
    val authenticate: JwtToken => JwtClaim => IO[Option[AuthUser]] =
      token =>
        claim =>
          IO(decode[AuthUser](claim.content).toOption.filter(user => Try(UUID.fromString(user.userId)).isSuccess))

    val jwtAuth = JwtAuth.hmac(secretKey, JwtAlgorithm.HS256)

    JwtAuthMiddleware[IO, AuthUser](jwtAuth, authenticate)
  }
}
