package services

import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream._

import scala.util.Random

class NumberSource(filter: Int) extends GraphStage[SourceShape[String]] {

  val out: Outlet[String] = Outlet("NumberSource.out")
  override val shape: SourceShape[String] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      private val random = new Random()

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          //println("filter: " + filter)
          //println("onPull")
          push(out, getNumber(filter)) // Emits an element through the given output port.
          //          complete(out) // Signals that there will be no more elements emitted on the given port.
          //          fail(out, new RuntimeException) // Signals failure through the given port.
        }

        @scala.throws[Exception](classOf[Exception])
        override def onDownstreamFinish(): Unit = {
          //          println("===> Upstream cancelled the stream")
          // re-using super
          super.onDownstreamFinish()
        }
      })

      private def getNumber(filter: Int) = {
        var returnVal = -1
        var found = false
        while (!found) {
          returnVal = random.nextInt(100)
          //println(s"rnd val: $returnVal")
          if (filter == returnVal % 2)
            found = true
        }
        Integer.toString(returnVal)
      }
    }
}

object NumberSource {
  def create(filter: Int) = new NumberSource(filter)
}
