package org.todo.gnat

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory
import org.todo.gnat.fsm.{LogIn, Session}
import org.todo.gnat.models.{TaskRepository, User, UserRepository}

class MainRELPActor(_currentSession: Session, _user: Option[User])(implicit usersRepo: UserRepository, tasksRepo: TaskRepository) extends Actor with ActorLogging with StrictLogging {

  val actorLogger = LoggerFactory.getLogger("dnsProxyLogger")
  var currentSession = _currentSession
  var currentUser = _user

  override def receive: Receive = {
    case a => {
      if (a.equals("log in")) currentSession = Session.transition(currentSession, LogIn(User("root", "root"))).left.get
      actorLogger.info("current session state is: " + currentSession.sessionState)
      sender ! "I've heard your message " + a + " and my session is " + currentSession.sessionState
    }
  }
}

object MainRELPActor {
    def props(currentSession: Session, user: Option[User] = None)(implicit usersRepo: UserRepository, tasksRepo: TaskRepository): Props =
      Props(new MainRELPActor(currentSession, user))
}
