package com.kstopa.projectmanagement.core.project

import java.time.{Duration, LocalDateTime}

import com.kstopa.projectmanagement.core.task.MaybeDeletedTask

case class ProjectId(value: Int) extends AnyVal
case class ProjectName(value: String) extends AnyVal
case class ProjectAuthor(value: String) extends AnyVal

case class Project(id: ProjectId, name: ProjectName, author: ProjectAuthor, createdOn: LocalDateTime)

case class MaybeDeletedProject(
  id: ProjectId,
  name: ProjectName,
  author: ProjectAuthor,
  createdOn: LocalDateTime,
  deletedOn: Option[LocalDateTime],
)

case class ProjectWithTasks(project: Project, tasks: List[MaybeDeletedTask], totalTime: Duration)

case class MaybeDeletedProjectWithTasks(project: MaybeDeletedProject, tasks: List[MaybeDeletedTask], totalTime: Duration)
