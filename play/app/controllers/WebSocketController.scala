/*
 * Based on example at https://github.com/playframework/play-scala-websocket-example/blob/2.6.x/app/controllers/HomeController.scala
 */
package controllers

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import filters.LoggingFilter
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, ControllerComponents, RequestHeader, WebSocket}
import services.SameOriginCheck

import scala.concurrent.{ExecutionContext, Future}

class WebSocketController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) with SameOriginCheck {
  val logger = play.api.Logger(getClass)

  def ws: WebSocket = {
    WebSocket.acceptOrResult[JsValue, JsValue] {
      case rh if sameOriginCheck(rh) =>
        wsFutureFlow(rh).map { flow =>
          logger.info(s"Accepting WebSocket request for ${rh.path}")
          LoggingFilter.logRequestHeader(None, rh)
          Right(flow)
        }.recover {
          case e: Exception =>
            logger.error("Cannot create websocket", e)
            val jsError = Json.obj("error" -> "Cannot create websocket")
            val result = InternalServerError(jsError)
            Left(result)
        }

      case rejected =>
        logger.error(s"Request ${rejected} failed same origin check")
        Future.successful {
          Left(Forbidden("forbidden"))
        }
    }
  }

  private def wsFutureFlow(header: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    Future(Flow.fromSinkAndSource(Sink.ignore, Source.single[JsValue](Json.obj("success" -> "Successfully connected to websocket"))))
  }

}

