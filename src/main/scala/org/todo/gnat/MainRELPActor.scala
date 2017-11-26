package org.todo.gnat

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory
import org.todo.gnat.fsm.{Command, LogIn, Session}
import org.todo.gnat.models.{TaskRepository, User, UserRepository}
import org.todo.gnat.parser.CommandConfigHolder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class MainRELPActor(_currentSession: Session, _user: Option[User])(implicit usersRepo: UserRepository, tasksRepo: TaskRepository) extends Actor with ActorLogging with StrictLogging {

  val actorLogger = LoggerFactory.getLogger("RELP actor logger")
  var currentSession = _currentSession
  var currentUser = _user
  // capturing "stable" sender ref
  var senderLocal: ActorRef = _

  override def receive: Receive = {
    case (command, args) => {
      senderLocal = sender
      val commandArgs = args.asInstanceOf[CommandConfigHolder]
      command match {
        case "login" =>
          val userTryingToLogIn = User(commandArgs.user, commandArgs.pass)
          usersRepo.getByName(userTryingToLogIn.userName).onComplete {
            case Success(users) => users.filter(_.userPass == userTryingToLogIn.userPass) match {
              case Some(userFromDb) => Session.transition(currentSession, LogIn(userFromDb)) match {
                case Left(newSession) =>
                  actorLogger.info("current session state is changing: " + currentSession.sessionState + " -> " + newSession)
                  senderLocal ! "user " + userTryingToLogIn.userName + " successfully logged in"
                  currentSession = newSession
                  currentUser = Option(userFromDb)
                case Right(message) =>
                  actorLogger.info("current session state is: " + currentSession.sessionState)
                  senderLocal ! message
              }
              case None =>
                actorLogger.info("current session state is: " + currentSession.sessionState)
                senderLocal ! "user or pass is wrong"
            }
            case Failure(message) => senderLocal ! message
          }
        case "logout" =>
          Session.transition(currentSession, Command("logout", currentUser.get)) match {
            case Left(newSession) =>
              actorLogger.info("current session state is changing: " + currentSession.sessionState + " -> " + newSession)
              senderLocal ! "user " + currentUser.get.userName + " successfully logged out"
              currentSession = newSession
              currentUser = None
            case Right(message) =>
              actorLogger.info("current session state is: " + currentSession.sessionState)
              senderLocal ! message
          }
        case _ => senderLocal ! "I've heard your message and it makes sense for me, yet it is not supported; my session state is " + currentSession.sessionState
      }
    }
  }
}

object MainRELPActor {
  def props(currentSession: Session, user: Option[User] = None)(implicit usersRepo: UserRepository, tasksRepo: TaskRepository): Props =
    Props(new MainRELPActor(currentSession, user))
}
