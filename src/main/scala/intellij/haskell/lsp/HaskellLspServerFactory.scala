package intellij.haskell.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.server.{CannotStartProcessException, OSProcessStreamConnectionProvider, StreamConnectionProvider}
import intellij.haskell.settings.HaskellSettingsState

import java.io.File

class HaskellLspServerFactory extends LanguageServerFactory {

  override def createConnectionProvider(project: Project): StreamConnectionProvider = {
    new HaskellLspConnectionProvider(project)
  }
}

private class HaskellLspConnectionProvider(project: Project) extends OSProcessStreamConnectionProvider {

  override def start(): Unit = {
    if (!HaskellSettingsState.useHlsLsp) {
      throw new CannotStartProcessException(
        "Haskell LSP is disabled. Enable 'Use HLS via LSP' in Preferences → Languages & Frameworks → Haskell."
      )
    }

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
