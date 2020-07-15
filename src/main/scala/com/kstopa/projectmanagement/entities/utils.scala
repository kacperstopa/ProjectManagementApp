package com.kstopa.projectmanagement.entities

import java.time.LocalDate

import io.circe.Decoder

sealed trait SortBy
object SortBy {
  case object CreationTime extends SortBy
  case object UpdateTime   extends SortBy
}

sealed trait Order
object Order {
  case object Desc extends Order
  case object Asc extends Order
}

case class YearWithMonth(year: Int, month: Int) {
  def toLocalDate: LocalDate =
    LocalDate.of(year, month, 1)
}
