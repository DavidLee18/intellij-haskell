package intellij.haskell.psi.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiNameHelper

object HaskellPsiNameHelper {
  def getInstance: PsiNameHelper = new HaskellPsiNameHelper {
    override protected def getLanguageLevel: LanguageLevel = {
      LanguageLevel.HIGHEST
    }
  }

  // Haskell 2010 reservedid (Report §2.4). A Haskell identifier may never equal one of these.
  // Previously this delegated to JavaLexer.isKeyword, which is both deprecated and wrong for Haskell.
  private final val ReservedIds: Set[String] = Set(
    "case", "class", "data", "default", "deriving", "do", "else", "foreign",
    "if", "import", "in", "infix", "infixl", "infixr", "instance", "let",
    "module", "newtype", "of", "then", "type", "where", "_"
  )
}

class HaskellPsiNameHelper private() extends PsiNameHelper {
  final private var myLanguageLevelExtension: LanguageLevelProjectExtension = _

  def this(project: Project) = {
    this()
    myLanguageLevelExtension = LanguageLevelProjectExtension.getInstance(project)
  }

  override def isIdentifier(text: String): Boolean = isIdentifier(text, getLanguageLevel)

  protected def getLanguageLevel: LanguageLevel = myLanguageLevelExtension.getLanguageLevel

  override def isIdentifier(text: String, languageLevel: LanguageLevel): Boolean = text != null

  override def isKeyword(text: String): Boolean = text != null && HaskellPsiNameHelper.ReservedIds.contains(text)

  override def isQualifiedName(text: String): Boolean = {
    if (text == null) return false

    if (text.contains(".") && text.length > 2) {
      true
    } else {
      false
    }
  }
}
