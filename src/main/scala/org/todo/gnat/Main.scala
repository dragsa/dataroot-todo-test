package org.todo.gnat

import java.util.Scanner

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import org.todo.gnat.fsm.{LoggedOut, Session}
import org.todo.gnat.models.{TaskRepository, User, UserRepository}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.Breaks._

object Main extends StrictLogging {

  implicit val db = Database.forConfig("todo")

  implicit val usersRepository = new UserRepository
  implicit val tasksRepository = new TaskRepository
  val tables = Map("users" -> usersRepository.userTableQuery, "tasks" -> tasksRepository.taskTableQuery)
  val defaultUsers = List(User("data", "data", isAdmin = true), User("root", "root", isAdmin = true))

  def initTables: Unit = {
    tables.keys.foreach(tableCreator)
  }

  // TODO switch to future composition here
  def tableCreator(tableName: String) = {
    Await.result(db.run(MTable.getTables(tableName)).flatMap(matchedTables => if (matchedTables.isEmpty) {
      logger.info(tableName + " table doesn't exist, creating...")
      db.run(tables(tableName).schema.create)
    } else Future {}).andThen { case _ => logger.info(tableName + " table check finished") }, Duration.Inf)
  }

  // TODO switch to future composition here
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
    val scanner = new Scanner(System.in)
    val system = ActorSystem("todoSystem")
    val todoActor = system.actorOf(MainRELPActor.props(Session("session one", LoggedOut)), name = "todoActor")
    breakable(
      while (true) {
        println("type some command:")
        val currentCommand = scanner.nextLine
        implicit val timeout = Timeout(3 seconds)
        if (currentCommand.equals("exit")) {
          println("Bye-bye!")
          system.terminate
          break
        }
        else {
          val actorReply = Await.result(todoActor ? currentCommand, Duration.Inf)
          println(actorReply)
        }
      }
    )
  }
}
