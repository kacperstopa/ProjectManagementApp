package com.kstopa.projectmanagement.model

sealed trait ProjectDeletionResult
object ProjectDeletionResult {
  case class ProjectDeleted(deletedProject: Project, deletedTasks: List[Task]) extends ProjectDeletionResult
  object ProjectNotDeleted                                                     extends ProjectDeletionResult
}

sealed trait ProjectInsertionResult
object ProjectInsertionResult {
  case class ProjectInserted(project: Project) extends ProjectInsertionResult
  case object ProjectNotInserted               extends ProjectInsertionResult
  case object ProjectAlreadyExistsError        extends ProjectInsertionResult
}

sealed trait ProjectRenameResult
object ProjectRenameResult {
  case class ProjectRenamed(newProject: Project) extends ProjectRenameResult
  case object ProjectAlreadyExistsError          extends ProjectRenameResult
}

sealed trait TaskInsertionResult
object TaskInsertionResult {
  case class TaskInserted(task: Task) extends TaskInsertionResult
  case object TaskOverlapsOtherTask   extends TaskInsertionResult
  case object StartAfterEndTimeError  extends TaskInsertionResult
  case object ProjectNotExistsError   extends TaskInsertionResult
}

sealed trait TaskDeletionResult
object TaskDeletionResult {
  case class TaskDeleted(task: Task) extends TaskDeletionResult
  case object TaskNotDeleted         extends TaskDeletionResult
}

sealed trait TaskUpdateResult
object TaskUpdateResult {
  case class TaskUpdated(oldTask: Task, newTask: Task) extends TaskUpdateResult
  case object TaskNotUpdated                           extends TaskUpdateResult
  case object TaskOverlapsOtherTask                    extends TaskUpdateResult
  case object StartAfterEndTimeError                   extends TaskUpdateResult
}
