import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class LoginTest extends Specification {
  sequential

  //TODO factor out data for each test case
  // specs2 context?

  "login as root -> logout -> exit" in new CommandsStack {
    cmds.append("login -u root -p root\nlogout\nexit")
    System.setIn(new ByteArrayInputStream(cmds.toString.getBytes))
    val outContent = new ByteArrayOutputStream()
    Console.withOut(outContent) {
      org.todo.gnat.Main.main(Array())
    }
    outContent.toString must contain("user root successfully logged in")
    outContent.toString must contain("user root successfully logged out")
    outContent.toString must contain("Bye-bye!")
  }

  "logout before login" in new CommandsStack {
    cmds.append("logout\nexit")
    System.setIn(new ByteArrayInputStream(cmds.toString.getBytes))
    val outContent = new ByteArrayOutputStream()
    Console.withOut(outContent) {
      org.todo.gnat.Main.main(Array())
    }
    outContent.toString must contain("you need to login first")
    outContent.toString must contain("Bye-bye!")
  }

  trait CommandsStack extends Scope {
    lazy val cmds = new java.lang.StringBuilder()
  }

}
