package com.kstopa.projectmanagement.http.routes

import cats.effect.Sync
import cats.syntax.all._
import com.kstopa.projectmanagement.http.dto.{GetStatisticsDTO, StatisticsListDTO}
import com.kstopa.projectmanagement.model.AuthUser
import com.kstopa.projectmanagement.service.StatisticsService
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl

object StatisticsRoutes {
  def create[F[_]: Sync](
    statisticsService: StatisticsService[F],
  ): AuthedRoutes[AuthUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    AuthedRoutes.of[AuthUser, F] {
      case req @ POST -> Root as user =>
        for {
          getStatisticsDTO <- req.req.as[GetStatisticsDTO]
          statistics <- statisticsService
            .getStatistics(getStatisticsDTO.users, getStatisticsDTO.from.toService, getStatisticsDTO.to.toService)
          response <- Ok(StatisticsListDTO.fromService(statistics))
        } yield response
    }
  }
}
