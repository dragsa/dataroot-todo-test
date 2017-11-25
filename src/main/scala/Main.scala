import models.{TaskRepository, TaskTable, UserRepository, UserTable}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  implicit val db = Database.forConfig("todo")

  val usersRepository = new UserRepository
  val tasksRepository = new TaskRepository
  val tables = Map("users" -> UserTable.table, "tasks" -> TaskTable.table)

  def initTables(): Unit = {
    tables.keys.foreach(tableCreator)
  }

  def tableCreator(tableName: String) = {
    Await.result(db.run(MTable.getTables(tableName)).flatMap(tasks => if (tasks.isEmpty) {
      println(tableName + " table doesn't exist, creating...")
      db.run(tables(tableName).schema.create)
    } else Future {}).andThen { case _ => println(tableName + " table check finished") }, Duration.Inf)
  }

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 10.seconds)

  def main(args: Array[String]): Unit = {
    initTables()
  }
}
