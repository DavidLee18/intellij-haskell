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

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi._
import com.intellij.psi.util.PsiTreeUtil
import intellij.haskell.external.component.NameInfoComponentResult.{LibraryNameInfo, NameInfo, ProjectNameInfo}
import intellij.haskell.external.component._
import intellij.haskell.psi._
import intellij.haskell.util._
import intellij.haskell.util.index.HaskellModuleNameIndex

class HaskellReference(element: HaskellNamedElement, textRange: TextRange) extends PsiPolyVariantReferenceBase[HaskellNamedElement](element, textRange) {

  // LSP4IJ owns goto-declaration, find-usages, and rename via its
  // gotoDeclarationHandler, findUsagesHandlerFactory, and renameHandler
  // extensions. PSI-level reference resolution is intentionally a no-op.
  override def multiResolve(incompleteCode: Boolean): Array[ResolveResult] = Array.empty

  override def getVariants: Array[AnyRef] = Array.empty

}

object HaskellReference {

  import scala.jdk.CollectionConverters._

  def findIdentifiersByLibraryNameInfo(project: Project, libraryNameInfo: LibraryNameInfo, name: String): Either[NoInfo, (String, HaskellNamedElement)] = {
    findIdentifiersByModulesAndName(project, Seq(libraryNameInfo.moduleName), name)
  }

  def findIdentifiersByModulesAndName(project: Project, moduleNames: Seq[String], name: String, prioIdInExpression: Boolean = true): Either[NoInfo, (String, HaskellNamedElement)] = {
    ProgressManager.checkCanceled()

    findIdentifiersByModuleAndName2(project, moduleNames, name, prioIdInExpression).flatMap { case (mn, nes) => nes.map((mn, _)) }.sortWith(sortByD).
      headOption.map { case (mn, ne) => Right((mn, ne)) }.getOrElse(Left(NoInfoAvailable(name, moduleNames.mkString(" | "))))
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  private def findIdentifiersByModuleAndName2(project: Project, moduleNames: Seq[String], name: String, prioIdInExpression: Boolean, result: Seq[(String, Seq[HaskellNamedElement])] = Seq()): Seq[(String, Seq[HaskellNamedElement])] = {

    ProgressManager.checkCanceled()

    val distinctModuleNames = moduleNames.distinct
    distinctModuleNames.flatMap(mn => HaskellModuleNameIndex.findFilesByModuleName(project, mn) match {
      case Left(_) => result
      case Right(files) =>
        ProgressManager.checkCanceled()

        val identifiers = files.flatMap(f => findIdentifierInFileByName(f, name, prioIdInExpression))
        val importedModuleNames = files.flatMap { f => {
          if (HaskellProjectUtil.isSourceFile(f)) {
            FileModuleIdentifiers.findAvailableModuleIdentifiers(f)
          } else {
            HaskellPsiUtil.findModuleName(f).flatMap(mn => ScalaFutureUtil.waitForValue(project,
              HaskellComponentsManager.findModuleIdentifiers(project, mn), s"finding library module identifiers in HaskellReference for $mn")).flatten.getOrElse(Iterable())
          }
        }
        }.filter(mid => mid.name == name || mid.name == "_" + name || mid.name == mid.moduleName + "." + name).map(_.moduleName)

        if (importedModuleNames.isEmpty || distinctModuleNames == Seq(mn)) {
          result ++ Seq((mn, identifiers))
        } else {
          ProgressManager.checkCanceled()
          findIdentifiersByModuleAndName2(project, importedModuleNames, name, prioIdInExpression, result ++ Seq((mn, identifiers)))
        }
    })
  }

  def findIdentifierInFileByName(psiFile: PsiFile, name: String, prioIdInExpression: Boolean): Option[HaskellNamedElement] = {

    def findIdInExpressions = {
      ProgressManager.checkCanceled()

      val topLevelExpressions = HaskellPsiUtil.findTopLevelExpressions(psiFile)

      ProgressManager.checkCanceled()

      topLevelExpressions.flatMap(_.getQNameList.asScala.headOption.map(_.getIdentifierElement)).find(_.getName == name)
    }

    def findIdInDeclarations = {
      ProgressManager.checkCanceled()

      val declarationElements = HaskellPsiUtil.findHaskellDeclarationElements(psiFile)

      ProgressManager.checkCanceled()

      val declarationIdentifiers = declarationElements.flatMap(_.getIdentifierElements).filter(d => d.getName == name || d.getName == "_" + name)

      ProgressManager.checkCanceled()

      declarationIdentifiers.toSeq.sortWith(sortByDefiningDeclarationFirst).headOption
    }

    if (prioIdInExpression) {
      val expressionIdentifiers = findIdInExpressions
      if (expressionIdentifiers.isEmpty) {
        findIdInDeclarations
      } else {
        expressionIdentifiers
      }
    } else {
      val declarationIdentifiers = findIdInDeclarations
      if (declarationIdentifiers.isEmpty) {
        findIdInExpressions
      } else {
        declarationIdentifiers
      }
    }
  }

  def findIdentifierByLocation(project: Project, virtualFile: VirtualFile, psiFile: PsiFile, lineNr: Integer, columnNr: Integer, name: String): Option[HaskellNamedElement] = {
    ProgressManager.checkCanceled()
    val namedElement = for {
      offset <- LineColumnPosition.getOffset(virtualFile, LineColumnPosition(lineNr, columnNr))
      () = ProgressManager.checkCanceled()
      element <- Option(psiFile.findElementAt(offset))
      () = ProgressManager.checkCanceled()
      namedElement <- HaskellPsiUtil.findNamedElement(element).find(_.getName == name).
        orElse {
          ProgressManager.checkCanceled()
          None
        }.orElse(HaskellPsiUtil.findHighestDeclarationElement(element).flatMap(_.getIdentifierElements.find(_.getName == name)).
        orElse {
          ProgressManager.checkCanceled()
          None
        }.orElse(HaskellPsiUtil.findQualifiedName(element).map(_.getIdentifierElement)).find(_.getName == name)).orElse {
        HaskellPsiUtil.findTtype(element).flatMap(_.getQNameList.asScala.map(_.getIdentifierElement).find(_.getName == name))
      }
    } yield namedElement

    ProgressManager.checkCanceled()

    namedElement
  }

  private def sortByD(t1: (String, HaskellNamedElement), t2: (String, HaskellNamedElement)): Boolean = {
    (t1, t2) match {
      case ((mn1, ne1), (mn2, ne2)) => sortByDefiningDeclarationFirst(ne1, ne2)
    }
  }

  private def sortByDefiningDeclarationFirst(namedElement1: HaskellNamedElement, namedElement2: HaskellNamedElement): Boolean = {
    (HaskellPsiUtil.findDeclarationElement(namedElement1), HaskellPsiUtil.findDeclarationElement(namedElement2)) match {
      case (Some(_: HaskellClassDeclaration), _) => true
      case (Some(_: HaskellDataDeclaration), _) => true
      case (_, _) => false
    }
  }

  def findIdentifiersByNameInfo(nameInfo: NameInfo, namedElement: HaskellNamedElement, project: Project): Either[NoInfo, (Option[String], HaskellNamedElement, Option[String])] = {
    findIdentifiersByNameInfo(nameInfo, namedElement.getName, project)
  }

  def findIdentifiersByNameInfo(nameInfo: NameInfo, name: String, project: Project): Either[NoInfo, (Option[String], HaskellNamedElement, Option[String])] = {
    ProgressManager.checkCanceled()

    nameInfo match {
      case pni: ProjectNameInfo =>
        val (virtualFile, psiFile) = HaskellFileUtil.findFileInRead(project, pni.filePath)
        ProgressManager.checkCanceled()
        (virtualFile, psiFile) match {
          case (Some(vf), Right(pf)) => findIdentifierByLocation(project, vf, pf, pni.lineNr, pni.columnNr, name).map(r => Right(HaskellPsiUtil.findModuleName(pf), r, None)).getOrElse(Left(NoInfoAvailable(name, "-")))
          case (_, Right(_)) => Left(NoInfoAvailable(name, "-"))
          case (_, Left(noInfo)) => Left(noInfo)
        }
      case lni: LibraryNameInfo => findIdentifiersByLibraryNameInfo(project, lni, name).map({ case (mn, nes) => (Some(mn), nes, lni.packageName) })
      case _ => Left(NoInfoAvailable(name, "-"))
    }
  }
}
