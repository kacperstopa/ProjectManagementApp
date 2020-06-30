package com.kstopa.projectmanagement.model

import java.time.{Duration, LocalDateTime}

case class ProjectId(value: Int)

case class Project(id: ProjectId, name: String, author: String, created_on: LocalDateTime)

case class ProjectWithTasks(project: Project, tasks: List[TaskWithDeleteTimeOption], totalTime: Duration)