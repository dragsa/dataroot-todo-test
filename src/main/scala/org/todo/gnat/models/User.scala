package org.todo.gnat.models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import scala.concurrent.Future

case class User(userName: String, userPass: String, isAdmin: Boolean = false, userId: Option[Int] = None)

final class UserTable(tag: Tag)
  extends Table[User](tag, "users") {

  def userName = column[String]("user_name", O.Unique)

  // TODO salt this?
  def userPass = column[String]("user_pass")

  def isAdmin = column[Boolean]("is_admin")

  def userId = column[Int]("user_ID", O.PrimaryKey, O.AutoInc)

  def * =
    (userName, userPass, isAdmin, userId.?) <> (User.apply _ tupled, User.unapply)

}

object UserTable {
  val table = TableQuery[UserTable]
}

class UserRepository(implicit db: Database) {
  val userTableQuery = UserTable.table

  def createOne(user: User): Future[User] = {
    db.run(userTableQuery returning userTableQuery += user)
  }

  def createMany(users: List[User]): Future[Seq[User]] = {
    db.run(userTableQuery returning userTableQuery ++= users)
  }

  def updateOne(user: User): Future[Int] = {
    db.run(
      userTableQuery
        .filter(_.userId === user.userId)
        .update(user))
  }

  def deleteOne(userId: Int): Future[Int] = {
    db.run(userTableQuery.filter(_.userId === userId).delete)
  }

  def getById(userId: Int): Future[Option[User]] = {
    db.run(
      userTableQuery.filter(_.userId === userId).result.headOption)
  }

  def getByName(userName: String): Future[Option[User]] = {
    db.run(
      userTableQuery.filter(_.userName === userName).result.headOption)
  }
}
