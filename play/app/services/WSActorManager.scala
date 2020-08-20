package services

import java.util.UUID
import java.util.Set
import java.util.concurrent.ConcurrentHashMap

import akka.actor.{ActorRef, Terminated, _}
import akka.stream.{Materializer, OverflowStrategy, Supervision}
import akka.stream.scaladsl.{Sink, Source}
import akka.actor.SupervisorStrategy.Resume
import akka.pattern.ask

import scala.collection.mutable.HashSet
import scala.concurrent.duration.Duration
import scala.collection.JavaConversions._

class WSActorManager(name: String) extends Actor {

  private val connections: Set[ActorRef] = ConcurrentHashMap.newKeySet[ActorRef]()

  override def supervisorStrategy =
    OneForOneStrategy(5, Duration(60, "seconds")) {
      case error: Exception =>
        println(s"exception:${error.getMessage}")
        println(s"Resuming...")
        Resume
    }

  override def receive = {

    case WSActorCreate(props, id) =>
      try {
        val moduloActor = context.actorOf(props, id)
        connections.add(moduloActor)
        context.watch(moduloActor)
        println(s"WSManager:WSActorCreate: ${moduloActor.path}")
        sender ! moduloActor

      }catch {
        case error:Exception =>
          println(s"WSManager:WSActorCreate: ${error.getMessage}")
      }

    case WSBroadcast(message) =>
      for(conn <- connections){
        conn ! WSBroadcastMsg(message)
      }

    case Terminated(actorRef: ActorRef) =>
      connections.remove(actorRef)
      context.unwatch(actorRef)
      println(s"Actor Terminated:${actorRef.path}")
  }

  override def postStop(): Unit = {
    println(s"Actor Manager post stop")
    for(actorRef <- connections){
      println(s"unwatching actor:${actorRef.path}")
      connections.remove(actorRef)
      context.unwatch(actorRef)
    }
  }
}


object WSActorManager {
  def props(name: String) = Props(new WSActorManager(name: String))
}

case class WSActorCreated(actorRef: ActorRef)

case class WSActorCreate(props:Props, actorId: String)

case class WSBroadcast(message: String)
