package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import org.apache.commons.logging.LogFactory
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import services._

@Singleton
class HomeController @Inject()(cc: ControllerComponents)(implicit actorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext)
  extends StreamWSInfo(cc) with SameOriginCheck {

  private val LOGGER = LogFactory.getLog(getClass)

  def index = Action { httpRequest =>
    Ok("OK")
  }

  /*
     numbers Flow uses Source.actorRef[String](1, OverflowStrategy.dropBuffer)
   */
  def wsNumberSourceActorRefFlow = WebSocket.acceptOrResult[String, String] {

    case rh if sameOriginCheck(rh) =>

      randomFlowSourceActorRef(rh.queryString).map { flow =>
        Right(flow)
      }
        .recover {
          case e: Exception =>
            val msg = "Cannot create websocket"
            LOGGER.error(msg, e)
            val result = InternalServerError(msg)
            Left(result)
        }
    case rejected =>
      LOGGER.error(s"Request $rejected failed same origin check")
      Future.successful {
        Left(Forbidden("forbidden"))
      }
  }

  def wsBroadcastFlow = WebSocket.acceptOrResult[String, String] {
    case rh if sameOriginCheck(rh) =>

      broadcastFlow(rh.queryString).map { flow =>
        Right(flow)
      }
        .recover {
          case e: Exception =>
            val msg = "Cannot create websocket"
            LOGGER.error(msg, e)
            val result = InternalServerError(msg)
            Left(result)
        }
    case rejected =>
      LOGGER.error(s"Request $rejected failed same origin check")
      Future.successful {
        Left(Forbidden("forbidden"))
      }
  }
}
