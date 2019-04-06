package controllers

import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.mvc.{BaseController, ControllerComponents, Request, Result}
import play.twirl.api.utils.StringEscapeUtils

class GetController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  private val LOGGER = play.api.Logger(getClass)

  private def echoRequest(request: Request[_]): String = {
    val buffer = new StringBuilder()
    buffer.append("<tr><th>Header Key</th><th>Header Value</th></tr>\n")
    request.headers.toSimpleMap.foreach( entry =>
        buffer.append(s"   <tr><td>${StringEscapeUtils.escapeXml11(entry._1)}</td><td>${StringEscapeUtils.escapeXml11(entry._2)}</td></tr>\n")
    )
    buffer.toString()
  }

  private def echo(request: Request[_]): Result = {
    Ok(
      s"""
         |<!DOCTYPE html>
         |<html lang="en">
         | <head><title>Hello World!</title></head>
         | <body>
         | <h1>Request:</h1>
         | <table border="1">
         | ${echoRequest(request)}
         | </table>
         | </body>
         |</html>
      """.stripMargin).as(ContentTypes.HTML)
  }

  def index = Action{ request =>
    echo(request)
  }

  def delay = Action { request =>
    val defDelay = 30
    val delay: Int = request.queryString.get("delay").flatMap(_.headOption) match {
      case Some(delayParam) =>
        Int(delayParam).getOrElse(defDelay)
      case _ =>
        defDelay

    }
    LOGGER.info(s"Sleeping for $delay seconds before echoing request....")
    Thread.sleep(delay*1000)
    echo(request)
  }

}

object Int {
  def apply(s: String): Option[Int] = util.Try(s.toInt).toOption
}
