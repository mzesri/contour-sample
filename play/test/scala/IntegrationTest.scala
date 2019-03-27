package scala


import java.util.function.Consumer

import org.scalatest.FunSuite
import play.test.{Helpers, TestBrowser}

class IntegrationTest extends FunSuite {

  test("Launch PlayApp") {
    val port = 3333
    val tServer = Helpers.testServer(port, Helpers.fakeApplication(Helpers.inMemoryDatabase))
    Helpers.running(tServer, Helpers.HTMLUNIT, new Consumer[TestBrowser]() {
      override def accept(testBrowser: TestBrowser): Unit = {
        testBrowser.goTo(s"http://localhost:$port/echo")
        assert(testBrowser.pageSource.length > 0)

        println(s"Reply:\n${testBrowser.pageSource}")
      }
    })
  }
}
