package intellij.haskell.cabal

sealed trait StanzaType

case object LibType extends StanzaType

case object ExeType extends StanzaType

case object TestSuiteType extends StanzaType

case object BenchmarkType extends StanzaType
