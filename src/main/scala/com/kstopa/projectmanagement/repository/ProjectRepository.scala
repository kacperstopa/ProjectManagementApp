package com.kstopa.projectmanagement.repository

import java.time.LocalDateTime

import cats.data.{NonEmptyList, OptionT}
import cats.effect._
import cats.free.Free
import cats.free.Free.Pure
import com.kstopa.projectmanagement.model.{AuthUser, MaybeDeletedProject, Project, ProjectId, ProjectInsertionResult, ProjectRenameResult, SortBy}
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres._
import doobie.implicits.javatime._
import cats.implicits._
import com.kstopa.projectmanagement.model.ProjectInsertionResult.ProjectInserted
import com.kstopa.projectmanagement.model.ProjectRenameResult.ProjectRenamed
import doobie.util.fragment.Fragment
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._

trait ProjectRepositoryException          extends Throwable
case object ProjectAlreadyExistsException extends ProjectRepositoryException

class ProjectRepository() {

  def insert(authUser: AuthUser)(name: String): ConnectionIO[ProjectInsertionResult] =
    sql"INSERT INTO projects(name, author, created_on) VALUES ($name, ${authUser.userId}, now())".update
      .withUniqueGeneratedKeys[Project]("id", "name", "author", "created_on")
      .map(ProjectInserted(_): ProjectInsertionResult)
      .exceptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => Free.pure(ProjectInsertionResult.ProjectAlreadyExistsError)
      }

  def rename(
    authUser: AuthUser
  )(projectId: ProjectId, newName: String): ConnectionIO[ProjectRenameResult] =
    sql"UPDATE projects SET name = $newName WHERE id = $projectId AND author = ${authUser.userId}".update
      .withUniqueGeneratedKeys[Project]("id", "name", "author", "created_on")
      .map(ProjectRenamed(_): ProjectRenameResult)
      .exceptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => Free.pure(ProjectRenameResult.ProjectAlreadyExistsError)
      }

  def softDelete(authUser: AuthUser)(id: ProjectId): ConnectionIO[Option[Project]] =
    for {
      _ <- sql"""
              INSERT INTO deleted_projects(id, name, author, created_on, deleted_on)
              SELECT id, name, author, created_on, now()
              FROM projects
              WHErE id = $id AND author = ${authUser.userId}""".update.run
      deleted <- sql"""
             DELETE FROM projects
             WHERe id = $id AND author = ${authUser.userId}""".update
        .withGeneratedKeys[Project]("id", "name", "author", "created_on")
        .head
        .compile
        .toList
        .map(_.headOption)
    } yield deleted

  def getAll: ConnectionIO[List[Project]] =
    sql"SELECT id, name, author, created_on from projects"
      .query[Project]
      .to[List]

  def getForIdAndAuthor(id: ProjectId, author: String): ConnectionIO[Option[Project]] =
    sql"SELECT id, name, author, created_on FROM projects WHERE id = $id AND author = $author"
      .query[Project]
      .option

  def get(id: ProjectId): ConnectionIO[Option[Project]] =
    sql"SELECT id, name, author, created_on FROM projects WHERE id = $id"
      .query[Project]
      .option

  def query(
    ids: Option[NonEmptyList[ProjectId]],
    from: Option[LocalDateTime],
    to: Option[LocalDateTime],
    deleted: Option[Boolean]
  )(page: Int, size: Int): fs2.Stream[ConnectionIO, MaybeDeletedProject] = {
    val sortBy: Option[SortBy] = SortBy.UpdateTime.some

    val select =
      if (deleted.isEmpty) {
        fr"SELECT id, name, author, created_on, CAST(NULL as TIMESTAMP) as deleted_on FROM projects UNION SELECT id, name, author, created_on, deleted_on FROM deleted_projects"
      } else if (deleted.contains(false)) {
        fr"SELECT id, name, author, created_on, CAST(NULL as TIMESTAMP) as deleted_on FROM projects"
      } else {
        fr"SELECT id, name, author, created_on, deleted_on FROM deleted_projects"
      }

    val conditions =
      (List(fr" WHERE ") ++
      ids.toList.flatMap(i => List(Fragments.in(fr"id", i), fr" AND ")) ++
      from.toList.flatMap(f => List(fr"created_on > $f", fr" AND ")) ++
      to.toList.flatMap(f => List(fr"created_on < $f", fr" AND "))).init.fold(Fragment.empty)(_ ++ _)

    val orderBy =
      if(sortBy.isEmpty) {
        Fragment.empty
      } else if (sortBy.contains(SortBy.CreationTime)) {
        fr"ORDER BY created_on"
      } else {
        fr"ORDER BY coalesce(max, created_on)"
      }

    val finalSelect = if(sortBy.contains(SortBy.UpdateTime)) {
      fr"""SELECT id, name, author, created_on, deleted_on
           FROM (
             SELECT max(start_time), project_id FROM tasks GROUP BY project_id
             UNION SELECT max(start_time), project_id FROM deleted_tasks GROUP BY project_id
            ) t right join (""".stripMargin ++ select ++ fr") p on t.project_id = p.id "
    } else select

    val paging =
      fr"offset ${page*size} rows fetch next $size rows only"
    (finalSelect ++ conditions ++ orderBy ++ paging).query[MaybeDeletedProject].stream
  }
}
