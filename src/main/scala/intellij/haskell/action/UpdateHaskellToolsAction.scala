package intellij.haskell.action

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.intellij.openapi.project.Project
import intellij.haskell.HaskellNotificationGroup
import intellij.haskell.external.component.StackProjectManager
import intellij.haskell.util.HaskellEditorUtil

class UpdateHaskellToolsAction extends AnAction {

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableExternalAction(actionEvent, (project: Project) => !StackProjectManager.isInstallingHaskellTools(project) && !StackProjectManager.isInitializing(project) && !StackProjectManager.isPreloadingAllLibraryIdentifiers(project))
  }

  def actionPerformed(actionEvent: AnActionEvent): Unit = {
    Option(actionEvent.getProject).foreach(project => {
      HaskellNotificationGroup.logInfoEvent(project, "Updating Haskell Tools")
      StackProjectManager.installHaskellTools(project, update = true)
    })
  }
}
