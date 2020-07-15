import java.time.LocalDateTime

import cats.effect.{IO, _}
import com.kstopa.projectmanagement.config.Config
import com.kstopa.projectmanagement.core.project.ProjectId
import com.kstopa.projectmanagement.core.task.{TaskComment, TaskRepositoryImpl, TaskVolume}
import com.kstopa.projectmanagement.db.Database
import com.kstopa.projectmanagement.entities.AuthUser
import doobie.scalatest.IOChecker
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.scalatest.PrivateMethodTester
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext

class DoobieCheckers extends AnyFunSuite with IOChecker {
  val taskRepository = new TaskRepositoryImpl()

  implicit val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val config = Config.load().use(IO(_)).unsafeRunSync().database

  override def transactor: doobie.Transactor[IO] =
    Transactor.fromDriverManager(
      config.driver,
      "jdbc:postgresql://0.0.0.0:5432/postgres",
      config.user,
      config.password,
      Blocker.liftExecutionContext(ExecutionContext.global),
    )

  test("taskInsert") {
    check(
      taskRepository.insertQuery(
        AuthUser("user"),
        ProjectId(1),
        LocalDateTime.now().minusDays(1),
        LocalDateTime.now(),
        Option(TaskVolume(5)),
        Option(TaskComment("comment"))
      )
    )
  }
  test("taskInsertIntoDeleted") {
    check(taskRepository.insertIntoDeletedQuery(ProjectId(1)))
  }
  test("getTasksForProjectQuery") {
    check(taskRepository.getTasksForProjectQuery(ProjectId(1)))
  }
}
