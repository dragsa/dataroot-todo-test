package org.todo.gnat

import com.typesafe.scalalogging.LazyLogging
import org.todo.gnat.models.{TaskRepository, User, UserRepository}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Main extends LazyLogging {

  implicit val db = Database.forConfig("todo")

  val usersRepository = new UserRepository
  val tasksRepository = new TaskRepository
  val tables = Map("users" -> usersRepository.userTableQuery, "tasks" -> tasksRepository.taskTableQuery)
  val defaultUsers = List(User("data", "data", isAdmin = true), User("root", "root", isAdmin = true))

  def initTables: Unit = {
    tables.keys.foreach(tableCreator)
  }

  def tableCreator(tableName: String) = {
    Await.result(db.run(MTable.getTables(tableName)).flatMap(matchedTables => if (matchedTables.isEmpty) {
      logger.info(tableName + " table doesn't exist, creating...")
      db.run(tables(tableName).schema.create)
    } else Future {}).andThen { case _ => logger.info(tableName + " table check finished") }, Duration.Inf)
  }

  def fillTablesWithDefaultData: Unit = {
    defaultUsers.foreach(userToCreate => usersRepository.getByName(userToCreate.userName).flatMap {
      case None =>
        logger.info("creating user " + userToCreate.userName)
        usersRepository.createOne(userToCreate)
    })
  }

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 10.seconds)

  def main(args: Array[String]): Unit = {
    initTables
    fillTablesWithDefaultData
    // TODO this will be removed as soon as user interaction part will be in place
    Thread.sleep(5000)
  }
}
