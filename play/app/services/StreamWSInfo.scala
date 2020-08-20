package services

import java.util.UUID

import akka.pattern.{Backoff, BackoffSupervisor, ask}
import akka.actor.{ActorRef, ActorSystem, OneForOneStrategy, Props, PoisonPill, SupervisorStrategy}
import akka.stream.{ActorAttributes, Materializer, OverflowStrategy, Supervision}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.apache.commons.logging.LogFactory
import play.api.mvc.{AbstractController, ControllerComponents}
import akka.util.Timeout

import scala.util.{Success, Failure}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.collection.JavaConversions._

class StreamWSInfo(val cc: ControllerComponents)
                  (implicit val actorSystem: ActorSystem, val mat: Materializer, val ec: ExecutionContext) extends AbstractController(cc) {

  private val LOGGER = LogFactory.getLog(getClass)
  implicit val askTimeout = Timeout(3 seconds)
  private val wsConnectionManagerId = UUID.randomUUID().toString
  println(s"Creating WSActor Manager id:$wsConnectionManagerId")
  private val wsConnectionManager = actorSystem.actorOf(WSActorManager.props("WS Connection manager"), wsConnectionManagerId)

  /*
     kafka Flow uses Source.actorRef[String](1, OverflowStrategy.dropBuffer)
   */
  def randomFlowSourceActorRef(params: Map[String, Seq[String]])(implicit actorSystem: ActorSystem, mat: Materializer): Future[Flow[String, String, _]] = {

    ConfigUtils.extractParams(params) match {
      case Right(parameters) =>
        val urlFilter: String = parameters.getOrElse("where", "0")
        //randomNumberFlowWithBackOff(urlFilter.toInt)
        randomNumberFlowManager(urlFilter.toInt)
      case Left(error) =>
        LOGGER.error(error.message)
        Future(errorFlow(error.message))
    }
  }

  private def randomNumberFlowManager(filter: Int)(implicit actorSystem: ActorSystem, mat: Materializer): Future[Flow[String, String, _]] = {
    println(">>randomNumberFlowManager")

    try {

      val actorId = UUID.randomUUID().toString
      val future = wsConnectionManager ? WSActorCreate(WSModuloActor.props(actorId, Option(filter)), actorId)

      val responseFuture = future.mapTo[ActorRef].map { moduloActor =>

        val out = Source.actorRef[String](1, OverflowStrategy.dropTail)
          .mapMaterializedValue(moduloActor ! StartModStream(_))

        val in = Sink.actorRef[String](moduloActor, PoisonPill)

        Flow.fromSinkAndSourceCoupled(in, out)
      }
      responseFuture.onFailure {
        case error =>
          println(s"error: ${error}")
          Future(errorFlow(s"error creating the flow: ${error}"))
      }
      responseFuture

    } catch {
      case error: Exception =>
        println(s"error: ${error.getMessage}")
        Future(errorFlow(s"error creating the flow: ${error.getMessage}"))
    }
  }

  def broadcastFlow(params: Map[String, Seq[String]])(implicit actorSystem: ActorSystem, mat: Materializer): Future[Flow[String, String, _]] = {

    try {
      ConfigUtils.extractParams(params) match {

        case Right(parameters) =>
          val message: String = parameters.getOrElse("message", "Hello")
          wsConnectionManager ! WSBroadcast(message)
          //send OK message
          Future(Flow.fromSinkAndSource(Sink.ignore, Source.single[String]("OK")))

        case Left(error) =>
          LOGGER.error(error.message)
          Future(errorFlow(error.message))
      }
    } catch {
      case err: Exception =>
        println(s"error: ${err.getMessage}")
        Future(errorFlow(s"error creating the flow: ${err.getMessage}"))
    }
  }


  private def randomNumberFlowWithBackOff(filter: Int)(implicit actorSystem: ActorSystem, mat: Materializer): Future[Flow[String, String, _]] = {
    println(">>randomNumberFlowWithBackOff")

    try {
      val actorId = UUID.randomUUID().toString
      val supervisorProps = BackoffSupervisor.props(
        Backoff.onFailure(
          WSModuloActor.props(actorId, Option(filter)),
          childName = "MyModuleActor",
          minBackoff = 3.seconds,
          maxBackoff = 30.seconds,
          randomFactor = 0.2)
          .withSupervisorStrategy(
            OneForOneStrategy() {
              case error: Exception =>
                println("MyModuleActor error:" + error.printStackTrace())
                println("MyModuleActor resuming...")
                SupervisorStrategy.Resume

              case _ =>
                println("MyModuleActor unknown error")
                println("MyModuleActor stopping...")
                SupervisorStrategy.Stop
            })
      )

      val supervisor = actorSystem.actorOf(supervisorProps, name = s"moduloActorSupervisor-${actorId}")

      val out = Source.actorRef[String](1, OverflowStrategy.dropTail)
        .mapMaterializedValue(supervisor ! StartModStream(_))

      val in = Sink.actorRef[String](supervisor, PoisonPill)

      Future(Flow.fromSinkAndSourceCoupled(in, out))
    }catch {
      case error:Exception =>
        Future(errorFlow(error.getMessage))
    }
  }

  def errorFlow(message: String) = {
    Flow.fromSinkAndSource(Sink.ignore, Source.single[String](message))
  }
}
