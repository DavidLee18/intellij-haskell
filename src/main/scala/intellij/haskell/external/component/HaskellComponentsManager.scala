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

package intellij.haskell.external.component

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import intellij.haskell.HaskellNotificationGroup
import intellij.haskell.cabal.{PackageInfo, StanzaType}
import intellij.haskell.external.repl.StackReplsManager
import intellij.haskell.util.GhcVersion

object HaskellComponentsManager {

  case class ComponentTarget(module: Module, modulePath: String, packageName: String, target: String, stanzaType: StanzaType, sourceDirs: Seq[String],
                             mainIs: Option[String], isImplicitPreludeActive: Boolean, buildDepends: Seq[String], exposedModuleNames: Seq[String] = Seq.empty)

  def findAvailableModuleLibraryModuleNamesWithIndex(module: Module): Iterable[String] = {
    AvailableModuleNamesComponent.findAvailableModuleLibraryModuleNamesWithIndex(module)
  }

  def findStackComponentGlobalInfo(stackComponentInfo: ComponentTarget): Option[StackComponentGlobalInfo] = {
    StackComponentGlobalInfoComponent.findStackComponentGlobalInfo(stackComponentInfo)
  }

  def findStackComponentInfo(psiFile: PsiFile): Option[ComponentTarget] = {
    HaskellModuleInfoComponent.findComponentTarget(psiFile)
  }

  def findComponentTarget(project: Project, filePath: String): Option[ComponentTarget] = {
    HaskellModuleInfoComponent.findComponentTarget(project, filePath)
  }

  def getGlobalProjectInfo(project: Project): Option[GlobalProjectInfo] = {
    GlobalProjectInfoComponent.findGlobalProjectInfo(project)
  }

  def getSupportedLanguageExtension(project: Project): Iterable[String] = {
    GlobalProjectInfoComponent.findGlobalProjectInfo(project).map(_.supportedLanguageExtensions).getOrElse(Iterable())
  }

  def getGhcVersion(project: Project): Option[GhcVersion] = {
    GlobalProjectInfoComponent.findGlobalProjectInfo(project).map(_.ghcVersion)
  }

  def getAvailableStackagePackages(project: Project): Iterable[String] = {
    GlobalProjectInfoComponent.findGlobalProjectInfo(project).map(_.availableStackagePackageNames).getOrElse(Iterable())
  }

  def findProjectPackageNames(project: Project): Option[Iterable[String]] = {
    StackReplsManager.getReplsManager(project).map(_.modulePackageInfos.map { case (_, ci) => ci.packageName })
  }

  def findCabalInfos(project: Project): Iterable[PackageInfo] = {
    StackReplsManager.getReplsManager(project).map(_.modulePackageInfos.map { case (_, ci) => ci }).getOrElse(Iterable())
  }

  def invalidateFileInfos(psiFile: PsiFile): Unit = {
    HaskellModuleInfoComponent.invalidate(psiFile)
  }

  def findProjectModulePackageNames(project: Project): Seq[(Module, String)] = {
    findStackComponentInfos(project).map(info => (info.module, info.packageName)).distinct
  }

  def findLibraryPackageInfos(project: Project): Seq[LibraryPackageInfo] = {
    LibraryPackageInfoComponent.libraryPackageInfos(project).toSeq
  }

  def findStackComponentInfos(project: Project): Seq[ComponentTarget] = {
    StackReplsManager.getReplsManager(project).map(_.componentTargets.toSeq).getOrElse(Seq())
  }

  def invalidateGlobalCaches(project: Project): Unit = {
    HaskellNotificationGroup.logInfoEvent(project, "Start to invalidate cache")
    GlobalProjectInfoComponent.invalidate(project)
    LibraryPackageInfoComponent.invalidate(project)
    HaskellModuleInfoComponent.invalidate(project)
    StackComponentGlobalInfoComponent.invalidate(project)
    HaskellNotificationGroup.logInfoEvent(project, "Finished with invalidating cache")
  }
}
