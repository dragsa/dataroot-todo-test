package org.todo.gnat.parser

case class CommandConfigHolder(user: String = "",
                               pass: String = "",
                               taskType: String = "",
                               taskName: String = "",
                               taskDescription: String = "")

object CommandConfig {
  val parser = new scopt.OptionParser[CommandConfigHolder]("") {
    override def showUsageOnError = false

    override def terminate(exitState: Either[String, Unit]): Unit = ()

    // TODO there seems to be no way to add help specialized for each command
    help("help").text("prints this usage text\n")

    cmd("login") children(
      opt[String]('u', "user") required() action { (x, c) =>
        c.copy(user = x)
      } text "user name for login",
      opt[String]('p', "pass") required() action { (x, c) =>
        c.copy(pass = x)
      } text "user pass for login\n"
    ) text "login user into TODO with given credentials"

    cmd("logout").
      text("logout current user from TODO\n")

    cmd("taskList") children (
      arg[String]("<type>") required() validate { x =>
        if (x.equals("*") || x.equals("open") || x.equals("done")) success
        else failure("\"type\" must be '*', 'open' or 'done'")
      }
        action { (x, c) =>
        c.copy(taskType = x)
      }
        text "task type to display\n"
      ) text "display tasks meeting given criteria"

    cmd("taskAdd") children(
      opt[String]('n', "name") required() action { (x, c) =>
        c.copy(taskName = x)
      } text "name of task to create",
      opt[String]('d', "description") optional() action { (x, c) =>
        c.copy(taskDescription = x)
      } text "description of task to create (optional, empty by default)",
      opt[String]('s', "state") optional() action { (x, c) =>
        c.copy(taskType = x)
      } text "state of task to create (optional, 'open' by default)\n",
    ) text "create task with given parameters"

    cmd("taskDelete") children (
      arg[String]("<name>") required() action { (x, c) =>
        c.copy(taskName = x)
      } text "name of task to delete\n"
      ) text "delete task by given name"

    cmd("taskMarkDone") children (
      arg[String]("<name>") required() action { (x, c) =>
        c.copy(taskName = x)
      } text "name of task to mark as 'done'\n"
      ) text "mark task as 'done' by given name"

    cmd("taskMarkOpen") children (
      arg[String]("<name>") required() action { (x, c) =>
        c.copy(taskName = x)
      } text "name of task to mark as 'open'\n"
      ) text "mark task as 'open' by given name"
  }
}
