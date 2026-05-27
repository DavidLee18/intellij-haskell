/*
 * Copyright 2014-2020 Rik van der Kleij
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package intellij.haskell.navigation

import com.intellij.openapi.util.TextRange
import com.intellij.psi.{PsiPolyVariantReferenceBase, ResolveResult}
import intellij.haskell.psi.HaskellNamedElement

class HaskellReference(element: HaskellNamedElement, textRange: TextRange) extends PsiPolyVariantReferenceBase[HaskellNamedElement](element, textRange) {

  // LSP4IJ owns goto-declaration, find-usages, and rename via its
  // gotoDeclarationHandler, findUsagesHandlerFactory, and renameHandler
  // extensions. PSI-level reference resolution is intentionally a no-op.
  override def multiResolve(incompleteCode: Boolean): Array[ResolveResult] = Array.empty

  override def getVariants: Array[AnyRef] = Array.empty
}
