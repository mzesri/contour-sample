package controllers

import javax.inject.Inject
import play.api.mvc.{BaseController, ControllerComponents}

class GetController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  private val LOGGER = play.api.Logger(getClass)

  def index = Action{ request =>
    Ok(views.html.echo(request))
  }

  def delay = Action { request =>
    val defDelay = 30
    val delay: Int = request.queryString.get("delay").flatMap(_.headOption) match {
      case Some(delayParam) =>
        val delayParamAsInt = Int(delayParam).getOrElse(defDelay)
        if (delayParamAsInt >= 0) {
          delayParamAsInt
        } else {
          defDelay
        }
      case _ =>
        defDelay

    }
    LOGGER.info(s"Sleeping for $delay seconds before echoing request....")
    Thread.sleep(delay*1000)
    Ok(views.html.echo(request))
  }

}

object Int {
  def apply(s: String): Option[Int] = util.Try(s.toInt).toOption
}
