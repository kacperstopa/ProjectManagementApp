package com.kstopa.projectmanagement.service

import cats.effect.Bracket
import com.kstopa.projectmanagement.model._
import com.kstopa.projectmanagement.repository.StatisticsRepository
import doobie.hikari.HikariTransactor
import doobie.implicits._

class StatisticsService[F[_]](
  statisticsRepository: StatisticsRepository,
  transactor: HikariTransactor[F]
)(implicit bracket: Bracket[F, Throwable]) {

  def getStatistics(users: List[String], from: YearWithMonth, to: YearWithMonth): F[StatisticsList] =
    statisticsRepository.getStatistics(users, from.toLocalDate, to.toLocalDate).map(StatisticsList).transact(transactor)
}
