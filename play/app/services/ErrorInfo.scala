package services

import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

case class ErrorInfo(error: SSTErrorInfo) extends Serializable {
  def toJson: String = {
    implicit val formats = DefaultFormats
    write(this)
  }
}

case class SSTErrorInfo(code: Int, message: String, details: Seq[String] = Seq.empty)

case class SSTExtractParametersError(code: Int, message: String, details: Seq[String] = Seq.empty)
