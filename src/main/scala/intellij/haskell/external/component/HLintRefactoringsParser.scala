package intellij.haskell.external.component

import fastparse.Parsed.{Failure, Success}
import fastparse.SingleLineWhitespace._
import fastparse._

object HLintRefactoringsParser {

  case class SrcSpan(startLine: Int, startCol: Int, endLine: Int, endCol: Int)

  type Subts = Seq[(String, SrcSpan)]

  sealed trait Refactoring
  case class Delete(rType: RType, pos: SrcSpan) extends Refactoring
  case class Replace(rType: RType, pos: SrcSpan, subts: Subts, orig: String, deletes: Seq[Delete]) extends Refactoring
  case class ModifyComment(pos: SrcSpan, newComment: String) extends Refactoring
  case class InsertComment(pos: SrcSpan, insertComment: String) extends Refactoring
  case class RemoveAsKeyword(pos: SrcSpan) extends Refactoring

  sealed trait RType
  case object Expr extends RType
  case object Decl extends RType
  case object Pattern extends RType
  case object Stmt extends RType
  case object Type extends RType
  case object ModuleName extends RType
  case object Bind extends RType
  case object Match extends RType
  case object Import extends RType

  def parseRefactoring(hlintOutput: String): Either[String, Refactoring] = parse(hlintOutput, refactoringParser(_), verboseFailures = true) match {
    case Success(value, _) => Right(value)
    case Failure(label, i, _) => Left(s"Could not parse HLint output | HLintOutput: $hlintOutput | Label: $label | Index: $i")
  }

  @annotation.nowarn
  private def refactoringParser[$: P]: P[Refactoring] = P("[" ~ (deleteParser | replaceParser | modifyCommentParser | insertCommentParser | removeAsKeywordParser) ~ "]")

  private[component] def parseSubts(hlintOutput: String): Parsed[Subts] = parse(hlintOutput, subtsParser(_), verboseFailures = true)

  private[component] def parsePos(hlintOutput: String): Parsed[SrcSpan] = parse(hlintOutput, posParser(_), verboseFailures = true)

  @annotation.nowarn
  private def deleteParser[$: P]: P[Delete] = P("Delete" ~ keyRtypePosParser(Pass)).map({ case (x, y, _) => Delete(x, y) })

  private def replaceParser[$: P]: P[Replace] = P("Replace" ~ keyRtypePosParser(commaParser ~ "subts =" ~ subtsParser ~ commaParser ~ keyValueParser("orig", string)) ~ (commaParser ~ deleteParser).rep)
    .map({ case (x, y, (w, z), q) => Replace(x, y, w, z, q) })

  private def modifyCommentParser[$: P]: P[ModifyComment] = P("ModifyComment" ~ "{" ~ posParser ~ commaParser ~ keyValueParser("newComment", string) ~ "}").
    map({ case (x, y) => ModifyComment(x, y) })

  private def insertCommentParser[$: P]: P[InsertComment] = P("InsertComment" ~ "{" ~ posParser ~ commaParser ~ keyValueParser("newComment", string) ~ "}").
    map({ case (x, y) => InsertComment(x, y) })

  private def removeAsKeywordParser[$: P]: P[RemoveAsKeyword] = P("RemoveAsKeyword" ~ "{" ~ posParser ~ "}").
    map({ case (x) => RemoveAsKeyword(x) })

  private def keyRtypePosParser[$: P, A](rest: => P[A]) = "{" ~ keyRtypeParser ~ commaParser ~ posParser ~ rest ~ "}"

  private def subtsParser[$: P] = P("[" ~ (subtParser ~ commaParser.?).rep ~ "]")

  private def subtParser[$: P] = P("(" ~ string ~ commaParser ~ srcSpanParser ~ ")")

  private def rtypeParser[$: P]: P[RType] = {
    P(IgnoreCase("Expr")).map(_ => Expr) |
      P(IgnoreCase("Decl")).map(_ => Decl) |
      P(IgnoreCase("Type")).map(_ => Type) |
      P(IgnoreCase("Pattern")).map(_ => Pattern) |
      P(IgnoreCase("Stmt")).map(_ => Stmt) |
      P(IgnoreCase("ModuleName")).map(_ => ModuleName) |
      P(IgnoreCase("Bind")).map(_ => Bind) |
      P(IgnoreCase("Match")).map(_ => Match) |
      P(IgnoreCase("Import")).map(_ => Import)
  }

  private def stringChars(c: Char) = c != '\"' && c != '\\'

  private def strChars[$: P] = P(CharsWhile(stringChars))

  private def hexDigit[$: P] = P(CharIn("0-9a-fA-F"))

  private def unicodeEscape[$: P] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)

  private def escape[$: P] = P("\\" ~ (CharIn("\"/\\\\bfnrt") | unicodeEscape))

  private def string[$: P] = P("\"" ~/ (strChars | escape).rep.! ~ "\"")

  @annotation.nowarn
  private def digits[$: P] = P(CharsWhileIn("0-9"))

  @annotation.nowarn
  private def keyValueParser[_: P, A](keyName: String, valueParser: => P[A]) = s"$keyName" ~ "=" ~ valueParser

  private def keyDigitsParser[p: P](keyName: String) = keyValueParser[p, String](keyName, digits.!).map(_.toInt)

  private def keyRtypeParser[$: P] = keyValueParser("rtype", rtypeParser)

  private def commaParser[$: P] = ","

  private def posParser[$: P] = "pos" ~ "=" ~ srcSpanParser

  private def srcSpanParser[$: P] = "SrcSpan" ~
    ("{" ~
      keyDigitsParser("startLine") ~ commaParser ~
      keyDigitsParser("startCol") ~ commaParser ~
      keyDigitsParser("endLine") ~ commaParser ~
      keyDigitsParser("endCol") ~
      "}").map { case (sl, sc, el, ec) => SrcSpan(sl, sc, el, ec) }
}
