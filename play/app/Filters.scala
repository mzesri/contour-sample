import javax.inject.{Inject, Singleton}
import play.api.Environment
import filters.LoggingFilter
import play.api.http.HttpFilters

@Singleton
class Filters @Inject()(env: Environment,
                        loggingFilter: LoggingFilter) extends HttpFilters {
  override val filters = Seq(loggingFilter)
}
