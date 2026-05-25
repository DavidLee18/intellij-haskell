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
import intellij.haskell.util.HaskellFileUtil

/**
  * Reformats Haskell files (via Ormolu) when the platform's "Reformat code" before-commit
  * option is enabled. Haskell has no internal IntelliJ formatter, so the platform's own
  * reformat pass is a no-op for `.hs` files; this handler makes that single checkbox also
  * cover Haskell instead of adding a separate "Haskell reformat code" checkbox.
  */
class HaskellReformatBeforeCheckinHandler(project: Project, checkinProjectPanel: CheckinProjectPanel) extends CheckinHandler {

  override def beforeCheckin(): CheckinHandler.ReturnResult = {
    import scala.jdk.CollectionConverters._
    val virtualFiles = checkinProjectPanel.getVirtualFiles

    if (VcsConfiguration.getInstance(project).REFORMAT_BEFORE_PROJECT_COMMIT && !DumbService.isDumb(project)) {
      val reformatResult = virtualFiles.asScala.filter(vf => HaskellFileUtil.isHaskellFile(vf)).forall(vf => HaskellFileUtil.convertToHaskellFileDispatchThread(project, vf).exists(OrmoluReformatAction.reformat))
      if (reformatResult) {
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
