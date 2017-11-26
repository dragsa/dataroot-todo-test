package org.todo.gnat.models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import scala.concurrent.Future

case class Task(taskName: String, taskBody: Option[String] = None, taskState: String = "New", taskOwner: Int, taskId: Option[Int] = None)

final class TaskTable(tag: Tag)
  extends Table[Task](tag, "tasks") {

  def taskName = column[String]("task_name")

  def taskBody = column[String]("task_body")

  // TODO switch to enums here?
  def taskState = column[String]("task_state")

  def taskOwner = column[Int]("task_owner")

  def taskId = column[Int]("task_ID", O.PrimaryKey, O.AutoInc)

  def userFk =
    foreignKey("user_fk_ID", taskOwner, TableQuery[UserTable])(_.userId)

  def uniqueTaskPerUser = index("task_user_unique", (taskName, taskOwner), unique = true)

  def * =
    (taskName, taskBody.?, taskState, taskOwner, taskId.?) <> (Task.apply _ tupled, Task.unapply)
}

object TaskTable {
  val table = TableQuery[TaskTable]
}

class TaskRepository(implicit db: Database) {
  val taskTableQuery = TaskTable.table

  def createOne(task: Task): Future[Task] = {
    db.run(taskTableQuery returning taskTableQuery += task)
  }

  def createMany(tasks: List[Task]): Future[Seq[Task]] = {
    db.run(taskTableQuery returning taskTableQuery ++= tasks)
  }

  def updateOne(task: Task): Future[Int] = {
    db.run(
      taskTableQuery
        .filter(_.taskId === task.taskId)
        .update(task))
  }

  def deleteOne(taskId: Int): Future[Int] = {
    db.run(taskTableQuery.filter(_.taskId === taskId).delete)
  }

  def getById(taskId: Int): Future[Option[Task]] = {
    db.run(
      taskTableQuery.filter(_.taskId === taskId).result.headOption)
  }
}

