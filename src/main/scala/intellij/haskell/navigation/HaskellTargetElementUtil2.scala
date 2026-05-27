package intellij.haskell.navigation

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import intellij.haskell.psi.HaskellNamedElement

class HaskellTargetElementUtil2 extends TargetElementEvaluatorEx2 {

  // The leaf token under the caret (e.g. an HS_VARID identifier) is usually
  // wrapped by a HaskellNamedElement ancestor. Walk up until we find one so
  // that TargetElementUtil.findTargetElement yields a usable PSI element
  // even though HaskellReference.multiResolve is intentionally a no-op
  // (LSP4IJ owns navigation). Without this walk, Find Usages fails with
  // "Cannot search for usages from this location".
  override def getNamedElement(element: PsiElement): PsiElement = {
    var current: PsiElement = element
    while (current != null && !current.isInstanceOf[HaskellNamedElement]) {
      current = current.getParent
    }
    current
  }

  override def isAcceptableNamedParent(parent: PsiElement): Boolean = false
}