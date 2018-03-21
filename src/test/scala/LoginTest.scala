import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.specs2.mutable.Specification

class LoginTest extends Specification {
  sequential

  //TODO add init data for each test case
  // specs2 context?

  "login as root -> logout -> exit" >> {
    val loginData = "login -u root -p root\nlogout\nexit"
    System.setIn(new ByteArrayInputStream(loginData.getBytes()))
    val outContent = new ByteArrayOutputStream()
    Console.withOut(outContent) {
      org.todo.gnat.Main.main(Array())
    }
    outContent.toString must contain("user root successfully logged in")
    outContent.toString must contain("user root successfully logged out")
    outContent.toString must contain("Bye-bye!")
  }

  "logout before login" >> {
    val logoutData = "logout\nexit"
    System.setIn(new ByteArrayInputStream(logoutData.getBytes()))
    val outContent = new ByteArrayOutputStream()
    Console.withOut(outContent) {
      org.todo.gnat.Main.main(Array())
    }
    outContent.toString must contain("you need to login first")
  }

}
