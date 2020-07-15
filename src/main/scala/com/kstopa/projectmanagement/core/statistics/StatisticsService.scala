package com.kstopa.projectmanagement.core.statistics

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.Bracket
import com.kstopa.projectmanagement.entities._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import cats.implicits._
import doobie.free.connection.ConnectionIO

trait StatisticsService[F[_]] {
  def getStatistics(users: List[String], from: YearWithMonth, to: YearWithMonth): F[StatisticsList]
}

class StatisticsServiceImpl[F[_]: Applicative](
  statisticsRepository: StatisticsRepository[ConnectionIO],
  transactor: HikariTransactor[F]
)(implicit bracket: Bracket[F, Throwable])
    extends StatisticsService[F] {
  def getStatistics(users: List[String], from: YearWithMonth, to: YearWithMonth): F[StatisticsList] =
    users match {
      case head :: tail =>
        statisticsRepository
          .getStatistics(NonEmptyList(head, tail), from.toLocalDate, to.toLocalDate)
          .map(StatisticsList)
          .transact(transactor)
      case Nil => StatisticsList(List.empty).pure[F]
    }
}
