package org.todo.gnat.models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import scala.concurrent.Future

case class Task(taskName: String, taskDescription: Option[String] = None, taskOwner: Int, taskState: String = "open", taskId: Option[Int] = None)

final class TaskTable(tag: Tag)
  extends Table[Task](tag, "tasks") {

  def taskName = column[String]("task_name")

  def taskDescription = column[String]("task_description")

  // TODO switch to enums here?
  def taskState = column[String]("task_state")

  def taskOwner = column[Int]("task_owner")

  def taskId = column[Int]("task_ID", O.PrimaryKey, O.AutoInc)

  def userFk =
    foreignKey("user_fk_ID", taskOwner, TableQuery[UserTable])(_.userId)

  def uniqueTaskPerUser = index("task_user_unique", (taskName, taskOwner), unique = true)

  def * =
    (taskName, taskDescription.?, taskOwner, taskState, taskId.?) <> (Task.apply _ tupled, Task.unapply)
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

  def updateOneStateByUserIdAndName(userId: Int, name: String, newState: String): Future[Int] = {
    db.run(
      taskTableQuery
        .filter(task => task.taskOwner === userId && task.taskName === task.taskName && task.taskState =!= newState).
        map(_.taskState).update(newState))
  }

  def deleteOne(taskId: Int): Future[Int] = {
    db.run(taskTableQuery.filter(_.taskId === taskId).delete)
  }

  def deleteOneByUserIdAndName(userId: Int, name: String): Future[Int] = {
    db.run(taskTableQuery.filter(task => task.taskOwner === userId && task.taskName === name).delete)
  }

  def getById(taskId: Int): Future[Option[Task]] = {
    db.run(
      taskTableQuery.filter(_.taskId === taskId).result.headOption)
  }

  def getByName(taskName: String): Future[Option[Task]] = {
    db.run(
      taskTableQuery.filter(_.taskName === taskName).result.headOption)
  }

  def getAllByUserId(userId: Int): Future[Seq[Task]] = {
    db.run(
      taskTableQuery.filter(_.taskOwner === userId).result)
  }

  def getAllByUserIdAndStatus(userId: Int, state: String): Future[Seq[Task]] = {
    db.run(
      taskTableQuery.filter(task => task.taskOwner === userId && task.taskState === state).result)
  }

  def getAll: Future[Seq[Task]] = {
    db.run(
      taskTableQuery.result)
  }
}

