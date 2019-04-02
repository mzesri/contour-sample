package filters

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoggingFilter @Inject()(implicit override val mat: Materializer,
                              exec: ExecutionContext) extends Filter{
  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {

    nextFilter(requestHeader).map { result =>
      LoggingFilter.logRequestHeader(Option(result), requestHeader)
      result
    }
  }
}

object LoggingFilter {
  private val LOGGER = play.api.Logger(getClass)

  def logRequestHeader(resultOption: Option[Result], requestHeader: RequestHeader): Unit = {
    val buffer = new StringBuilder
    resultOption match {
      case Some(result) =>
        buffer.append(s"${result.header.status} ${requestHeader.method} ${requestHeader.uri}\n")
      case _ =>
        buffer.append(s"${requestHeader.method} ${requestHeader.uri}\n")
    }
    buffer.append("Request Headers:\n")
    requestHeader.headers.toSimpleMap.foreach( tuple => buffer.append(s"\t${tuple._1}: ${tuple._2}\n"))
    resultOption match {
      case Some(result) =>
        buffer.append("Response Headers:\n")
        result.header.headers.foreach( tuple => buffer.append(s"\t${tuple._1}: ${tuple._2}\n"))
        LOGGER.info(buffer.toString())
      case _ =>
        // Do Nothing
    }
    LOGGER.info(buffer.toString())
  }
}
