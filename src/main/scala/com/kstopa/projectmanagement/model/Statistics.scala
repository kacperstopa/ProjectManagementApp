package com.kstopa.projectmanagement.model

case class StatisticsList(statistics: List[Statistics])

case class Statistics(
  userId: String,
  numberOfTasks: Int,
  numberOfTasksWithVolume: Int,
  averageDuration: Int,
  averageVolume: Option[Float],
  averageDurationPerVolume: Option[Int]
)
