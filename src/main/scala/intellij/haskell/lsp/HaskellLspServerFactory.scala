package intellij.haskell.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.features.{LSPClientFeatures, LSPCodeLensFeature, LSPInlayHintFeature}
import com.redhat.devtools.lsp4ij.server.{CannotStartProcessException, OSProcessStreamConnectionProvider, StreamConnectionProvider}
import intellij.haskell.external.component.StackProjectManager
import intellij.haskell.settings.HaskellSettingsState

import java.io.File

class HaskellLspServerFactory extends LanguageServerFactory {

  override def createConnectionProvider(project: Project): StreamConnectionProvider = {
    new HaskellLspConnectionProvider(project)
  }

  override def createClientFeatures(): LSPClientFeatures = {
    val features = new HaskellLspClientFeatures
    features.setInlayHintFeature(new DisabledInlayHintFeature)
    features.setCodeLensFeature(new DisabledCodeLensFeature)
    features
  }
}

private class HaskellLspClientFeatures extends LSPClientFeatures

// HLS's importLens (textDocument/codeLens) and explicitly-resolved inlay hints
// repeatedly fail with "Rule Failed: ImportActions" / "Rule Failed: GhcSession"
// while the cradle is still settling, surfacing as error popups in LSP4IJ.
// Both features are informational decorations; disabling them silences the
// noise without losing any core diagnostic/nav/hover behavior.
private class DisabledInlayHintFeature extends LSPInlayHintFeature {
  override def isInlayHintSupported(file: PsiFile): Boolean = false
}

private class DisabledCodeLensFeature extends LSPCodeLensFeature {
  override def isCodeLensSupported(file: PsiFile): Boolean = false
}

private class HaskellLspConnectionProvider(project: Project) extends OSProcessStreamConnectionProvider {

  override def start(): Unit = {
    // Resolution order: user-set settings path → project's auto-resolved/installed path
    // (StackProjectManager runs `ghcup install hls` during init if needed) → bare wrapper
    // name on PATH as last resort.
    val wrapper = HaskellSettingsState.hlsPath
      .orElse(StackProjectManager.isHlsAvailable(project))
      .getOrElse("haskell-language-server-wrapper")

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

  // HLS initializationOptions. Schema:
  //   https://haskell-language-server.readthedocs.io/en/latest/configuration.html
  // We enable semantic tokens (HLS defaults to off and would respond
  // "semanticTokens is disabled globally in your config" otherwise) and turn
  // off the import-lens code-lens plugin server-side - we already disable the
  // client-side feature in HaskellLspClientFeatures, but disabling the plugin
  // here too avoids it computing import-lens rules that just get discarded.
  override def getInitializationOptions(rootUri: VirtualFile): Object = {
    def m(entries: (String, Object)*): java.util.Map[String, Object] = {
      val r = new java.util.HashMap[String, Object]()
      entries.foreach { case (k, v) => r.put(k, v) }
      r
    }
    val on  = m("globalOn" -> java.lang.Boolean.TRUE)
    val off = m("globalOn" -> java.lang.Boolean.FALSE)
    m("haskell" -> m(
      "plugin" -> m(
        "semanticTokens" -> on,
        "importLens"     -> off
      )
    ))
  }
}
