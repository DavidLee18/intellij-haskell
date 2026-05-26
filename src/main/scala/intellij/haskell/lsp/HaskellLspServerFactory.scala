package intellij.haskell.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.server.{CannotStartProcessException, OSProcessStreamConnectionProvider, StreamConnectionProvider}
import intellij.haskell.settings.HaskellSettingsState

import java.io.File

class HaskellLspServerFactory extends LanguageServerFactory {

  override def createConnectionProvider(project: Project): StreamConnectionProvider = {
    new HaskellLspConnectionProvider(project)
  }

  override def createClientFeatures(): LSPClientFeatures = new HaskellLspClientFeatures
}

private class HaskellLspClientFeatures extends LSPClientFeatures {
  override def isEnabled(file: VirtualFile): Boolean = HaskellSettingsState.useHlsLsp
}

private class HaskellLspConnectionProvider(project: Project) extends OSProcessStreamConnectionProvider {

  override def start(): Unit = {
    val wrapper = HaskellSettingsState.hlsPath.getOrElse("haskell-language-server-wrapper")
    if (wrapper.contains(File.separator) && !new File(wrapper).canExecute) {
      throw new CannotStartProcessException(
        s"Haskell Language Server wrapper not found or not executable at '$wrapper'. " +
          "Set the path in Preferences → Languages & Frameworks → Haskell, or install via `ghcup install hls`."
      )
    }

    val cmd = new GeneralCommandLine(wrapper, "--lsp")
    Option(project.getBasePath).foreach(cmd.setWorkDirectory)
    setCommandLine(cmd)
    super.start()
  }
}
