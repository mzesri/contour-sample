package services

object ConfigUtils {

  val WHERE_CLAUSE_PARAM = "where"
  val GEOMETRY_PARAM = "geometry"
  val SPATIAL_RELATIONSHIP_PARAM = "spatialRel"
  val OUT_FIELDS_PARAM = "outFields"
  val OUT_SPATIAL_REFERENCE_PARAM = "outSR"
  val ERROR = "error"

  val ZOOKEEPER_SESSION_TIMEOUT_MS_VAL = "400"
  val ZOOKEEPER_SYNC_TIME_MS_VAL = "200"
  val ZOOKEEPER_SESSION_TIMEOUT_MS_CONFIG_VAL = "30000"

  val KAFKA_POLL_TIMEOUT_VAL: Long = 200
  val KAFKA_AUTO_COMMIT_INTERVAL_MS_CONFIG_VAL = "1000"
  val AUTO_OFFSET_RESET_CONFIG_VAL = "latest"


  val ZOOKEEPER_SESSION_TIMEOUT_MS = "zookeeper.session.timeout.ms"
  val ZOOKEEPER_SYNC_TIME_MS = "zookeeper.sync.time.ms"
  val KAFKA_KEY_DESERIALIZER_CLASS_CONFIG = "org.apache.kafka.common.serialization.StringDeserializer"
  val KAFKA_VALUE_DESERIALIZER_CLASS_CONFIG = "org.apache.kafka.common.serialization.StringDeserializer"

  val queryParams = Seq(WHERE_CLAUSE_PARAM, GEOMETRY_PARAM, SPATIAL_RELATIONSHIP_PARAM, OUT_FIELDS_PARAM, OUT_SPATIAL_REFERENCE_PARAM)

  /**
    * Extracts the query parameters
    *
    * @param params the query params
    * @return
    */
  def extractParams(params: Map[String, Seq[String]]): Either[SSTExtractParametersError, Map[String, String]] = {

    try {
      var _paramVal: String = null
      var _paramMap: Map[String, String] = Map.empty

      for (paramName <- queryParams) {
        _paramVal = extractAsString(params, paramName)
        if (_paramVal != null) {
          _paramMap += paramName -> _paramVal
        }
      }
      Right(_paramMap)
    } catch {
      case error: Exception =>
        Left(SSTExtractParametersError(500, error.getMessage, error.getStackTrace.map(e => e.toString)))
    }
  }

  private def extractAsString(params: Map[String, Seq[String]], param: String): String = {
    val option = params.get(param).map(_.head)
    option match {
      case Some(valueStr)
        if valueStr != null && !valueStr.isEmpty => valueStr
      case _ => null
    }
  }
}
