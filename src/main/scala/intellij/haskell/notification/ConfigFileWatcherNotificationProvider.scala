package intellij.haskell.notification

import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Function => JFunction}
import javax.swing.JComponent

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.{VFileContentChangeEvent, VFileEvent}
import com.intellij.ui.{EditorNotificationPanel, EditorNotificationProvider, EditorNotifications}
import intellij.haskell.external.component.StackProjectManager
import intellij.haskell.util.{HaskellFileUtil, HaskellProjectUtil}

import scala.collection.concurrent
import scala.jdk.CollectionConverters._

object ConfigFileWatcherNotificationProvider {
  val showNotificationsByProject: concurrent.Map[Project, Boolean] = new ConcurrentHashMap[Project, Boolean]().asScala
}

class ConfigFileWatcherNotificationProvider extends EditorNotificationProvider {

  override def collectNotificationData(project: Project, file: VirtualFile): JFunction[_ >: FileEditor, _ <: JComponent] = {
    if (HaskellProjectUtil.isHaskellProject(project) && ConfigFileWatcherNotificationProvider.showNotificationsByProject.get(project).contains(true)) {
      (_: FileEditor) => createPanel(project)
    } else {
      null
    }
  }

  private def createPanel(project: Project): EditorNotificationPanel = {
    val notifications = EditorNotifications.getInstance(project)

    val panel = new EditorNotificationPanel
    panel.setText("Haskell project configuration file is updated")
    panel.createActionLabel("Update Settings and Restart", () => {
      ConfigFileWatcherNotificationProvider.showNotificationsByProject.put(project, false)
      notifications.updateAllNotifications()
      StackProjectManager.restart(project)
    })
    panel.createActionLabel("Ignore", () => {
      ConfigFileWatcherNotificationProvider.showNotificationsByProject.put(project, false)
      notifications.updateAllNotifications()
    })
    panel
  }
}

class ConfigFileWatcher(project: Project, notifications: EditorNotifications) extends BulkFileListener {

  private val watchFiles = HaskellProjectUtil.findStackFile(project).iterator.to(Iterable) ++ HaskellProjectUtil.findCabalFiles(project) ++ HaskellProjectUtil.findPackageFiles(project)

  override def before(events: util.List[_ <: VFileEvent]): Unit = {}

  override def after(events: util.List[_ <: VFileEvent]): Unit = {
    if (!StackProjectManager.isInitializing(project)) {
      if (events.asScala.exists(e => e.isInstanceOf[VFileContentChangeEvent] && !e.isFromRefresh && watchFiles.exists(_.getAbsolutePath == HaskellFileUtil.getAbsolutePath(e.getFile)))) {
        ConfigFileWatcherNotificationProvider.showNotificationsByProject.put(project, true)
        notifications.updateAllNotifications()
      }
    }
  }
}
