package org.todo.gnat.fsm

import org.todo.gnat.models.User


sealed abstract class SessionState

case object LoggedOut extends SessionState

case object LoggedInAdmin extends SessionState

case object LoggedInUser extends SessionState

case class Session(name: String, sessionState: SessionState)


sealed trait Command

case class UserCommand(user: User) extends Command

case class AdminCommand(user: User) extends Command

case class LogIn(user: User) extends Command

case class LogOut() extends Command

//case class AdminUserList(override val user: User) extends AdminCommand(user)
//
//case class AdminUserAdd(override val user: User) extends AdminCommand(user)
//
//case class AdminUserDelete(override val user: User) extends AdminCommand(user)
//
//case class UserTaskList(override val user: User) extends UserCommand(user)
//
//case class UserTaskAdd(override val user: User) extends UserCommand(user)
//
//case class UserTaskDelete(override val user: User) extends UserCommand(user)
//
//case class UserTaskMarkDone(override val user: User) extends UserCommand(user)
//
//case class UserTaskMarkNew(override val user: User) extends UserCommand(user)

object Session {

  private def newState(session: Session, _sessionState: SessionState) =
    session.copy(sessionState = _sessionState)

  def transition(session: Session, command: Command): Either[Session, String] = {
    session.sessionState match {
      case LoggedOut =>
        command match {
          case LogIn(user) => Left(newState(session, if (user.isAdmin) LoggedInAdmin else LoggedInUser))
          case LogOut() => Left(session)
          case _ => Right("you need to login first")
        }
      case LoggedInUser =>
        command match {
          case LogIn(user) => Right(user + " is already logged in")
          case AdminCommand(user) => if (user.isAdmin) Left(session) else Right("only admins case use this command")
          case UserCommand(_) => Left(session)
          case LogOut() => Left(newState(session, LoggedOut))
        }
      case LoggedInAdmin =>
        command match {
          case LogIn(user) => Right(user + " is already logged in")
          case AdminCommand(_) | UserCommand(_) => Left(session)
          case LogOut() => Left(newState(session, LoggedOut))
        }
    }
  }
}