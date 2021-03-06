package org.todo.gnat.fsm

import org.todo.gnat.models.User


sealed abstract class Command(user: User)

case class UserCommand(user: User) extends Command(user)

case class AdminCommand(user: User) extends Command(user)

case class LogIn(user: User) extends Command(user)

case class LogOut(user: User) extends Command(user)

object Command {
  def apply(kind: String, user: User): Command = kind match {
      // TODO ugly factory, move to enum all of this
    case "login" => LogIn(user)
    case "logout" => LogOut(user)
    case "taskList" | "taskAdd" | "taskDelete" | "taskMarkDone" | "taskMarkOpen" => UserCommand(user)
      // TODO add these later, admin-level commands
    case "userList" | "userAdd" | "userDelete" => AdminCommand(user)
  }
}

sealed trait SessionState

case object LoggedOut extends SessionState

case object LoggedInAdmin extends SessionState

case object LoggedInUser extends SessionState

case class Session(name: String, sessionState: SessionState)

object Session {

  private def newState(session: Session, _sessionState: SessionState) =
    session.copy(sessionState = _sessionState)

  def transition(session: Session, command: Command): Either[Session, String] = {
    session.sessionState match {
      case LoggedOut =>
        command match {
          case LogIn(user) => Left(newState(session, if (user.isAdmin) LoggedInAdmin else LoggedInUser))
          case LogOut(_) => Right("you need to login first")
          case _ => Right("you need to login first")
        }
      case LoggedInUser =>
        command match {
          case LogIn(_) => Right("you need to logout first")
          case AdminCommand(user) => if (user.isAdmin) Left(session) else Right("only admins can use this command")
          case UserCommand(_) => Left(session)
          case LogOut(_) => Left(newState(session, LoggedOut))
        }
      case LoggedInAdmin =>
        command match {
          case LogIn(_) => Right("you need to logout first")
          case AdminCommand(_) | UserCommand(_) => Left(session)
          case LogOut(_) => Left(newState(session, LoggedOut))
        }
    }
  }
}