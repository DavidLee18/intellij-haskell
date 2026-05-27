package intellij.haskell.external

package object component {

  sealed trait NoInfo {
    def message: String
  }

  case class NoInfoAvailable(name: String, locationName: String, errorMessage: Option[String] = None) extends NoInfo {
    override def message: String = s"No info available for $name in $locationName" + errorMessage.map(m => s" | Error message: $m").getOrElse("")
  }

  case object IndexNotReady extends NoInfo {
    override def message: String = "No info because index isn't ready"
  }

  case class ReadActionTimeout(readActionDescription: String) extends NoInfo {
    def message = s"No info because read action timed out while $readActionDescription"
  }
}
