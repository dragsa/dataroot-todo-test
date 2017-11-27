package org.todo.gnat

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory
import org.todo.gnat.fsm.{Command, Session}
import org.todo.gnat.models.{Task, TaskRepository, User, UserRepository}
import org.todo.gnat.parser.CommandConfigHolder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class MainRELPActor(_currentSession: Session, _user: User = User("fake_service_user", "fake_service_pass"))(implicit usersRepo: UserRepository, tasksRepo: TaskRepository)
  extends Actor with ActorLogging with StrictLogging {

  val actorLogger = LoggerFactory.getLogger("RELP actor logger")
  // using absolutely idiomatic vars for actor state
  var currentSession = _currentSession
  var currentUser = _user
  // capturing "stable" sender ref to avoid dead letters
  var senderLocal: ActorRef = _

  override def receive: Receive = {
    case (command, args) => {
      senderLocal = sender
      val commandArgs = args.asInstanceOf[CommandConfigHolder]
      command match {
        case a@"login" =>
          val userTryingToLogIn = User(commandArgs.userName, commandArgs.userPass)
          usersRepo.getByName(userTryingToLogIn.userName).onComplete {
            case Success(users) => users.filter(_.userPass == userTryingToLogIn.userPass) match {
              case Some(userFromDb) => Session.transition(currentSession, Command(a, userFromDb)) match {
                case Left(newSession) =>
                  actorLogger.info("current session state is changing: " + currentSession.sessionState + " -> " + newSession.sessionState)
                  senderLocal ! "user " + userFromDb.userName + " successfully logged in"
                  currentSession = newSession
                  currentUser = userFromDb
                case Right(message) =>
                  actorLogger.info("current session state is: " + currentSession.sessionState)
                  senderLocal ! message
              }
              case None => processReject("user or pass is wrong")
            }
            case Failure(message) => senderLocal ! message
          }
        case a@"logout" =>
          Session.transition(currentSession, Command(a, currentUser)) match {
            case Left(newSession) =>
              actorLogger.info("current session state is changing: " + currentSession.sessionState + " -> " + newSession.sessionState)
              senderLocal ! "user " + currentUser.userName + " successfully logged out"
              currentSession = newSession
              currentUser = User("fake_service_user", "fake_service_pass")
            case Right(message) => processReject(message)
          }

        case a@"taskList" =>
          Session.transition(currentSession, Command(a, currentUser)) match {
            case Left(newSession) =>
              actorLogger.info("current session state is changing: " + currentSession.sessionState + " -> " + newSession.sessionState)
              currentSession = newSession
              commandArgs.taskState match {
                  // TODO add sealed trait for these, not really required due to parser behavior
                case "all" => tasksRepo.getAllByUserId(currentUser.userId.get) onComplete processTaskList
                case "open" => tasksRepo.getAllByUserIdAndStatus(currentUser.userId.get, "open") onComplete processTaskList
                case "done" => tasksRepo.getAllByUserIdAndStatus(currentUser.userId.get, "done") onComplete processTaskList
              }
            case Right(message) => processReject(message)
          }

        case a@"taskAdd" =>
          Session.transition(currentSession, Command(a, currentUser)) match {
            case Left(newSession) =>
              actorLogger.info("current session state is changing: " + currentSession.sessionState + " -> " + newSession.sessionState)
              tasksRepo.createOne(Task(commandArgs.taskName, Option(commandArgs.taskDescription), currentUser.userId.get)).onComplete {
                processTaskChange[Task]("created")
              }
              currentSession = newSession
            case Right(message) => processReject(message)
          }

        case a@"taskDelete" =>
          Session.transition(currentSession, Command(a, currentUser)) match {
            case Left(newSession) =>
              actorLogger.info("current session state is changing: " + currentSession.sessionState + " -> " + newSession.sessionState)
              tasksRepo.deleteOneByUserIdAndName(currentUser.userId.get, commandArgs.taskName).onComplete {
                processTaskChange[Int]("deleted")
              }
              currentSession = newSession
            case Right(message) => processReject(message)
          }

        case a@("taskMarkDone" | "taskMarkOpen") =>
          Session.transition(currentSession, Command(a.asInstanceOf[String], currentUser)) match {
            case Left(newSession) =>
              actorLogger.info("current session state is changing: " + currentSession.sessionState + " -> " + newSession.sessionState)
              tasksRepo.updateOneStateByUserIdAndName(currentUser.userId.get, commandArgs.taskName, a.asInstanceOf[String].takeRight(4).toLowerCase).onComplete {
                processTaskChange[Int]("updated")
              }
              currentSession = newSession
            case Right(message) => processReject(message)
          }

        case _ => senderLocal ! "I've heard your message and it makes sense for me, yet it is not supported; my session state is " + currentSession.sessionState
      }
    }
  }

  private def processReject(message: String): Unit = {
    actorLogger.info("current session state is: " + currentSession.sessionState)
    senderLocal ! message
  }

  private def processTaskChange[T](operation: String): PartialFunction[Try[T], Unit] = {
    case Success(tasks) => senderLocal ! tasks + " task " + operation
    case Failure(message) => senderLocal ! message
  }

  private def processTaskList: PartialFunction[Try[Seq[Task]], Unit] = {
    case Success(tasks) => senderLocal ! (if (tasks.isEmpty) "no tasks were fetched" else "next tasks\n" + tasks.mkString("\n") + "\nwere fetched")
    case Failure(message) => senderLocal ! message
  }
}

object MainRELPActor {
  def props(currentSession: Session)(implicit usersRepo: UserRepository, tasksRepo: TaskRepository): Props =
    Props(new MainRELPActor(currentSession))
}
