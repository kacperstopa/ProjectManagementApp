package com.kstopa.projectmanagement.model

import java.time.LocalDateTime

case class TaskId(value: Int)

case class Task(
  id: TaskId,
  projectId: ProjectId,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  author: String,
  comment: Option[String],
  volume: Option[Int],
)

case class TaskWithDeleteTimeOption(
  id: TaskId,
  projectId: ProjectId,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  author: String,
  comment: Option[String],
  volume: Option[Int],
  deletedOn: Option[LocalDateTime],
)
