
package services

import scala.concurrent.Future
import akka.Done
import akka.NotUsed
import akka.pattern.ask
import akka.actor._
import akka.actor.SupervisorStrategy.Resume
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.KillSwitches
import akka.stream._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class WSModuloActor(id: String, filter: Option[Int])(implicit actorSystem: ActorSystem, mat: Materializer) extends Actor {

  private var flowActor: ActorRef = null
  private var killSwitch: UniqueKillSwitch = null
  private var theFilter: String = null
  implicit val askTimeout = Timeout(3 seconds)

  override def preStart() = {
    println("Actor started:" + id)
  }

  override def postStop(): Unit = {
    if (killSwitch != null)
      killSwitch.shutdown()
    println("Actor stopped:" + id)
  }

  override def receive = {

    case filter: String =>
      println("new filter :" + filter)
      theFilter = filter

      if (filter.toInt > 1)
        throw new Exception(s"filter $filter  not allowed")

      self ! UpdateModStream(filter.toInt)

    case StreamData(record) =>
      if (record.toInt == 7)
        throw new Exception("7 found")

      if(theFilter!=null && theFilter.toInt == 999){
        println(s"Filter :$theFilter ")
      }else{
        if (flowActor != null)
          flowActor ! record
        else
          println("flowActor null: " + record)
      }


    case WSBroadcastMsg(message) =>
      flowActor ! message

    case StartModStream(actor) =>
      flowActor = actor
      killSwitch = createSource(filter.getOrElse(0))
      println("Started stream")

    case UpdateModStream(newfilter) =>
      //kill the existing stream
      if (killSwitch != null) {
        killSwitch.shutdown()
        killSwitch = null
      }

      println("Updating stream filter:" + newfilter)
      killSwitch = createSource(newfilter)
      println("new stream created")
  }

  private def createSource(filter: Int): UniqueKillSwitch = {
    val (killSwitchStream, sourceFuture) =
      Source.fromGraph(new NumberSource(filter))
        //.async
        .viaMat(KillSwitches.single)(Keep.right)
        .throttle(1, 1 seconds, 1, ThrottleMode.Shaping)
        .toMat(Sink.foreach { el: String => self ! StreamData(el) })(Keep.both)
        .run()

    sourceFuture.onComplete(_ => println("Source completed " + id))
    killSwitchStream
  }
}

object WSModuloActor {
  def props(id: String, filter: Option[Int])(implicit actorSystem: ActorSystem, mat: Materializer) = Props(new WSModuloActor(id, filter))
}

case class StartModStream(actorRef: ActorRef)

case class UpdateModStream(newFilter: Int)

case class StreamData(data: String)

case class WSBroadcastMsg(message: String)

object EndModStream
