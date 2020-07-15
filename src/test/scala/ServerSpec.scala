import java.time.LocalDateTime

import cats.effect.IO
import com.kstopa.projectmanagement.config.Config
import com.kstopa.projectmanagement.core.task.UpdateTaskEntity
import com.kstopa.projectmanagement.http.dto.{CreateProjectDTO, CreateTaskDTO, ProjectDTO, ProjectWithTasksDTO, TaskDTO, UpdateTaskDTO}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import scala.concurrent.ExecutionContext.global
class ServerSpec extends AnyFunSuite with Matchers {
  implicit private val cs = IO.contextShift(global)

  private lazy val configIO = Config.load("application.conf")
  private lazy val config   = configIO.use(IO(_)).unsafeRunSync()

  private lazy val client = BlazeClientBuilder.apply[IO](global).resource

  private lazy val rootUrl = s"http://${config.server.host}:${config.server.port}"

  test("Server should return 403 if authentication not valid") {
    val status = client
      .use { client =>
        client.status(Request[IO](uri = Uri.unsafeFromString(s"$rootUrl/projects")))
      }
      .unsafeRunSync()
    assert(status == Status.Forbidden)
  }

  test("Server should create new project") {
    val projectDTO = createProject("test1")

    assert(projectDTO.name == "test1")
  }

  test("Server should create new project and add new task") {
    val projectDTO = createProject("test2")
    val taskDTO = createTask(
      projectDTO.id,
      LocalDateTime.now().minusHours(1),
      LocalDateTime.now(),
      Option.empty,
      Option(5),
    )

    assert(taskDTO.projectId == projectDTO.id)
  }

  test("Server should return 400 after trying to add overlapping task") {
    val projectDTO = createProject("test3")
    val taskDTO = createTask(
      projectDTO.id,
      LocalDateTime.now().minusDays(1),
      LocalDateTime.now().minusDays(1).plusHours(1),
      Option.empty,
      Option(5),
    )

    val status = client
      .use { client =>
        val request = requestWithAuthorization
          .withMethod(Method.POST)
          .withEntity(
            CreateTaskDTO(
              projectDTO.id,
              LocalDateTime.now().minusDays(1),
              LocalDateTime.now().minusDays(1).plusHours(1),
              Option.empty,
              Option(5),
            )
          )
          .withUri(Uri.unsafeFromString(s"$rootUrl/tasks"))
        client.status(request)
      }
      .unsafeRunSync()

    assert(status == Status.BadRequest)
  }

  test("Server should delete project") {
    val projectDTO = createProject("test4")

    val deleteStatus = client
      .use { client =>
        val request = requestWithAuthorization
          .withMethod(Method.DELETE)
          .withUri(Uri.unsafeFromString(s"$rootUrl/projects/${projectDTO.id}"))
        client.status(request)
      }
      .unsafeRunSync()

    assert(deleteStatus == Status.Ok)

    val getStatus = client
      .use { client =>
        val request = requestWithAuthorization
          .withMethod(Method.GET)
          .withUri(Uri.unsafeFromString(s"$rootUrl/projects/${projectDTO.id}"))
        client.status(request)
      }
      .unsafeRunSync()

    assert(getStatus == Status.NotFound)
  }

  test("Server should update task") {
    val projectDTO = createProject("test5")
    val taskDTO = createTask(
      projectDTO.id,
      LocalDateTime.now().minusDays(2),
      LocalDateTime.now().minusDays(2).plusHours(1),
      Option.empty,
      Option(5),
    )

    val updatedTask = client
      .use { client =>
        val request = requestWithAuthorization
          .withMethod(Method.PUT)
          .withEntity(
            UpdateTaskDTO(
              LocalDateTime.now().minusDays(2).plusMinutes(10),
              LocalDateTime.now().minusDays(2).plusMinutes(40),
              Option("qwe"),
              Option.empty
            )
          )
          .withUri(Uri.unsafeFromString(s"$rootUrl/tasks/${taskDTO.id}"))
        client.expect[TaskDTO](request)
      }
      .unsafeRunSync()

    val projectWithTasks = client
      .use { client =>
        val request = requestWithAuthorization
          .withMethod(Method.GET)
          .withUri(Uri.unsafeFromString(s"$rootUrl/projects/${projectDTO.id}"))
        client.expect[ProjectWithTasksDTO](request)
      }
      .unsafeRunSync()

    assert(projectWithTasks.tasks.find(_.id == taskDTO.id).exists(_.deletedOn.isDefined))
    assert(projectWithTasks.tasks.find(_.id == updatedTask.id).exists(_.deletedOn.isEmpty))
  }

  private def createProject(name: String): ProjectDTO =
    client
      .use { client =>
        val request = requestWithAuthorization
          .withMethod(Method.POST)
          .withEntity(CreateProjectDTO(name))
          .withUri(Uri.unsafeFromString(s"$rootUrl/projects"))
        client.expect[ProjectDTO](request)
      }
      .unsafeRunSync()

  private def createTask(
    projectId: Int,
    from: LocalDateTime,
    to: LocalDateTime,
    comment: Option[String],
    volume: Option[Int]
  ): TaskDTO =
    client
      .use { client =>
        val request = requestWithAuthorization
          .withMethod(Method.POST)
          .withEntity(
            CreateTaskDTO(
              projectId,
              from,
              to,
              comment,
              volume,
            )
          )
          .withUri(Uri.unsafeFromString(s"$rootUrl/tasks"))
        client.expect[TaskDTO](request)
      }
      .unsafeRunSync()

  private val requestWithAuthorization = Request[IO]()
    .withHeaders(
      Header(
        "Authorization",
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwidXNlcklkIjoiZjdhNjdjNDAtYjRjMi0xMWVhLWIzZGUtMDI0MmFjMTMwMDA2IiwiaWF0IjoxNTE2MjM5MDIyfQ.R67oWhPCZbI_vqYym7f7QgNbmfQpJFRMfCG9peZoHho"
      )
    )
}
