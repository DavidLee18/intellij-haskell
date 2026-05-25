/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package intellij.haskell.action

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.{DumbService, Project}
import com.intellij.openapi.vcs.checkin._
import com.intellij.openapi.vcs.{CheckinProjectPanel, VcsConfiguration}
import intellij.haskell.editor.HaskellImportOptimizer
import intellij.haskell.util.HaskellFileUtil

/**
  * Removes redundant imports from Haskell files when the platform's "Optimize imports"
  * before-commit option is enabled, so that single checkbox also covers Haskell instead of
  * adding a separate "Haskell optimize imports" checkbox.
  */
class HaskellOptimizeImportsBeforeCheckinHandler(project: Project, checkinProjectPanel: CheckinProjectPanel) extends CheckinHandler {

  override def beforeCheckin(): CheckinHandler.ReturnResult = {
    import scala.jdk.CollectionConverters._
    val virtualFiles = checkinProjectPanel.getVirtualFiles

    if (VcsConfiguration.getInstance(project).OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT && !DumbService.isDumb(project)) {
      val optimizeResult = virtualFiles.asScala.filter(vf => HaskellFileUtil.isHaskellFile(vf)).forall(vf => HaskellFileUtil.convertToHaskellFileDispatchThread(project, vf).exists(HaskellImportOptimizer.removeRedundantImports))
      if (optimizeResult) {
        FileDocumentManager.getInstance.saveAllDocuments()
        CheckinHandler.ReturnResult.COMMIT
      } else {
        CheckinHandler.ReturnResult.CANCEL
      }
    } else {
      CheckinHandler.ReturnResult.COMMIT
    }
  }
}
