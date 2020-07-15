package com.kstopa.projectmanagement.core.task

import java.time.LocalDateTime

import com.kstopa.projectmanagement.core.project.ProjectId

case class TaskId(value: Int) extends AnyVal
case class TaskAuthor(value: String) extends AnyVal
case class TaskComment(value: String) extends AnyVal
case class TaskVolume(value: Int) extends AnyVal


case class Task(
  id: TaskId,
  projectId: ProjectId,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  author: TaskAuthor,
  comment: Option[TaskComment],
  volume: Option[TaskVolume],
)

case class MaybeDeletedTask(
  id: TaskId,
  projectId: ProjectId,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  author: TaskAuthor,
  comment: Option[TaskComment],
  volume: Option[TaskVolume],
  deletedOn: Option[LocalDateTime],
)

case class InsertTaskEntity(
  projectId: ProjectId,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  comment: Option[TaskComment],
  volume: Option[TaskVolume],
)

case class UpdateTaskEntity(
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  comment: Option[TaskComment],
  volume: Option[TaskVolume],
)
