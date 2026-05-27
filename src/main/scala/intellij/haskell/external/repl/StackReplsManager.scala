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

package intellij.haskell.external.repl

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import intellij.haskell.HaskellNotificationGroup
import intellij.haskell.cabal._
import intellij.haskell.external.component.HaskellComponentsManager.ComponentTarget
import intellij.haskell.external.component._
import intellij.haskell.util._

// Project structure holder, populated by reading .cabal files. The class
// is named StackReplsManager only for historical continuity; with HLS owning
// language intelligence via LSP, no stack-ghci REPLs are started anymore.
// Survives because run/test/console configurations and the library-build
// orchestration still need the parsed cabal layout (component targets,
// stanza types, source dirs, build-depends).
object StackReplsManager {

  case class ProjectReplTargets(stanzaType: StanzaType, targets: Seq[ComponentTarget]) {
    def targetsName: String = targets.map(_.target).mkString(" ")
  }

  def getReplsManager(project: Project): Option[StackReplsManager] = {
    StackProjectManager.getStackProjectManager(project).flatMap(_.getStackReplsManager)
  }

  private def createPackageInfos(project: Project): Iterable[(Module, PackageInfo)] = {
    val modules = HaskellProjectUtil.findProjectHaskellModules(project)
    val moduleDirs = modules.map(HaskellProjectUtil.getModuleDir)
    if (moduleDirs.isEmpty) {
      HaskellNotificationGroup.logWarningBalloonEvent(project, s"No Haskell modules found for project `${project.getName}`. Check your project configuration.")
      Iterable()
    } else {
      val cabalFiles = for {
        m <- modules
        dir = HaskellProjectUtil.getModuleDir(m)
        cf <- HaskellProjectUtil.findCabalFile(dir)
        ci <- PackageInfo.create(project, cf)
      } yield (m, ci)
      if (cabalFiles.isEmpty) {
        HaskellNotificationGroup.logWarningBalloonEvent(project, s"No Cabal files found for project `${project.getName}`. Check your project configuration.")
      }
      cabalFiles
    }
  }

  private def createComponentTargets(moduleCabalInfos: Iterable[(Module, PackageInfo)]): Iterable[ComponentTarget] = {
    moduleCabalInfos.flatMap {
      case (m: Module, cabalInfo: PackageInfo) => cabalInfo.cabalStanzas.map {
        case cs: LibraryCabalStanza => ComponentTarget(m, cs.modulePath, cs.packageName, cs.targetName, LibType, cs.sourceDirs, None, cs.isNoImplicitPreludeActive, cs.buildDepends, cs.exposedModuleNames)
        case cs: ExecutableCabalStanza => ComponentTarget(m, cs.modulePath, cs.packageName, cs.targetName, ExeType, cs.sourceDirs, cs.mainIs, cs.isNoImplicitPreludeActive, cs.buildDepends)
        case cs: TestSuiteCabalStanza => ComponentTarget(m, cs.modulePath, cs.packageName, cs.targetName, TestSuiteType, cs.sourceDirs, cs.mainIs, cs.isNoImplicitPreludeActive, cs.buildDepends)
        case cs: BenchmarkCabalStanza => ComponentTarget(m, cs.modulePath, cs.packageName, cs.targetName, BenchmarkType, cs.sourceDirs, cs.mainIs, cs.isNoImplicitPreludeActive, cs.buildDepends)
      }
    }
  }
}

class StackReplsManager(val project: Project) {

  val modulePackageInfos: Iterable[(Module, PackageInfo)] = StackReplsManager.createPackageInfos(project)

  val componentTargets: Iterable[ComponentTarget] = StackReplsManager.createComponentTargets(modulePackageInfos)

  val projectReplTargets: Iterable[StackReplsManager.ProjectReplTargets] = componentTargets.groupBy(_.stanzaType).flatMap { case (stanzaType, targets) =>
    if (stanzaType == LibType) {
      Seq(StackReplsManager.ProjectReplTargets(stanzaType, targets.toSeq))
    } else {
      targets.map(target => StackReplsManager.ProjectReplTargets(stanzaType, Seq(target)))
    }
  }

  def libTargetsName: Option[String] = {
    projectReplTargets.find(_.stanzaType == LibType).map(_.targetsName)
  }

  def findProjectReplTargets(componentTarget: ComponentTarget): Option[StackReplsManager.ProjectReplTargets] = {
    projectReplTargets.find(_.targets.contains(componentTarget))
  }
}
