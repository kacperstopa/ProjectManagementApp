package com.kstopa.projectmanagement.model

import java.time.{Duration, LocalDateTime}

case class ProjectId(value: Int)

case class Project(id: ProjectId, name: String, author: String, createdOn: LocalDateTime)

case class MaybeDeletedProject(
  id: ProjectId,
  name: String,
  author: String,
  createdOn: LocalDateTime,
  deletedOn: Option[LocalDateTime],
)

case class ProjectWithTasks(project: Project, tasks: List[MaybeDeletedTask], totalTime: Duration)

case class MaybeDeletedProjectWithTasks(project: MaybeDeletedProject, tasks: List[MaybeDeletedTask], totalTime: Duration)
