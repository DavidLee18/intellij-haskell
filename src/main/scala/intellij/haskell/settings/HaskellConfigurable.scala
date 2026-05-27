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

package intellij.haskell.settings

import com.intellij.openapi.options.{Configurable, ConfigurationException}
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.DocumentAdapter

import java.awt.{GridBagConstraints, GridBagLayout, Insets}
import javax.swing._
import javax.swing.event.DocumentEvent

class HaskellConfigurable extends Configurable {
  private var isModifiedByUser = false
  private val useSystemGhcToggle = new JCheckBox
  private val afterRestartLabel = new JLabel("*) Changes will take effect after restarting IntelliJ")
  private val newProjectTemplateNameField = new JTextField
  private val cachePathField = new JTextField
  private val hooglePathField = new JTextField
  private val ormoluPathField = new JTextField
  private val stylishHaskellPathField = new JTextField
  private val useCustomToolsToggle = new JCheckBox
  private val extraStackArgumentsField = new JTextField
  private val defaultGhcOptionsField = new JTextField
  private val hlsPathField = new JTextField

  override def getDisplayName: String = {
    "Haskell"
  }

  override def isModified: Boolean = this.isModifiedByUser

  import HaskellConfigurable._

  override def createComponent: JComponent = {

    def toggleToolPathsVisibility(): Unit = {
      val visible = useCustomToolsToggle.isSelected
      hooglePathField.setVisible(visible)
      stylishHaskellPathField.setVisible(visible)
      ormoluPathField.setVisible(visible)
    }

    toggleToolPathsVisibility()

    val settingsPanel = new JPanel(new GridBagLayout())

    settingsPanel.getInsets()
    val docListener: DocumentAdapter = (_: DocumentEvent) => {
      isModifiedByUser = true
    }

    newProjectTemplateNameField.getDocument.addDocumentListener(docListener)
    cachePathField.getDocument.addDocumentListener(docListener)
    hooglePathField.getDocument.addDocumentListener(docListener)
    ormoluPathField.getDocument.addDocumentListener(docListener)
    stylishHaskellPathField.getDocument.addDocumentListener(docListener)
    useSystemGhcToggle.addChangeListener { _ =>
      isModifiedByUser = true
    }
    useCustomToolsToggle.addChangeListener { _ =>
      isModifiedByUser = true
      toggleToolPathsVisibility()
    }
    extraStackArgumentsField.getDocument.addDocumentListener(docListener)
    defaultGhcOptionsField.getDocument.addDocumentListener(docListener)
    hlsPathField.getDocument.addDocumentListener(docListener)

    class SettingsGridBagConstraints extends GridBagConstraints {

      def setConstraints(anchor: Int = GridBagConstraints.CENTER, gridx: Int, gridy: Int, weightx: Double = 0, weighty: Double = 0, fill: Int = GridBagConstraints.NONE): SettingsGridBagConstraints = {
        this.anchor = anchor
        this.gridx = gridx
        this.gridy = gridy
        this.weightx = weightx
        this.weighty = weighty
        this.fill = fill
        this.insets = new Insets(2, 0, 2, 3)
        this
      }
    }

    val baseGridBagConstraints = new SettingsGridBagConstraints

    def addLabeledControl(row: Int, label: JLabel, component: JComponent): Unit = {
      settingsPanel.add(label, baseGridBagConstraints.setConstraints(
        anchor = GridBagConstraints.LINE_START,
        gridx = 0,
        gridy = row
      ))

      settingsPanel.add(component, baseGridBagConstraints.setConstraints(
        gridx = 1,
        gridy = row,
        fill = GridBagConstraints.HORIZONTAL,
        weightx = 1.0
      ))

      settingsPanel.add(Box.createHorizontalStrut(1), baseGridBagConstraints.setConstraints(
        gridx = 2,
        gridy = row,
        weightx = 0.1
      ))
    }

    val labeledControls = List(
      (new JLabel(DefaultGhcOptions), defaultGhcOptionsField),
      (new JLabel(ExtraStackArguments), extraStackArgumentsField),
      (new JLabel(""), afterRestartLabel),
      (new JLabel(NewProjectTemplateName), newProjectTemplateNameField),
      (new JLabel(CachePath), cachePathField),
      (new JLabel(BuildToolsUsingSystemGhc), useSystemGhcToggle),
      (new JLabel(UseCustomTool), useCustomToolsToggle),
      (new JLabel(HooglePath), hooglePathField),
      (new JLabel(OrmoluPath), ormoluPathField),
      (new JLabel(StylishHaskellPath), stylishHaskellPathField),
      (new JLabel(HlsPath), hlsPathField),
      (new JLabel(""), afterRestartLabel)
    )

    labeledControls.zipWithIndex.foreach {
      case ((label, control), row) => addLabeledControl(row, label, control)
    }

    settingsPanel.add(new JPanel(), baseGridBagConstraints.setConstraints(
      gridx = 0,
      gridy = labeledControls.length,
      weighty = 10.0
    ))

    val customToolPathWarning = new JTextArea(CustomToolPathWarning)
    customToolPathWarning.setLineWrap(true)
    customToolPathWarning.setWrapStyleWord(true)
    settingsPanel.add(customToolPathWarning, new GridBagConstraints(0, labeledControls.length, 3, 1, 0, 0,
      GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 2, 3), 0, 0))

    settingsPanel
  }

  override def apply(): Unit = {
    val state = HaskellSettingsPersistentStateComponent.getInstance().getState

    validateCustomTools()

    state.defaultGhcOptions = defaultGhcOptionsField.getText
    state.useSystemGhc = useSystemGhcToggle.isSelected
    state.newProjectTemplateName = newProjectTemplateNameField.getText
    state.cachePath = cachePathField.getText
    state.hooglePath = hooglePathField.getText
    state.ormoluPath = ormoluPathField.getText
    state.stylishHaskellPath = stylishHaskellPathField.getText
    state.customTools = useCustomToolsToggle.isSelected
    state.extraStackArguments = extraStackArgumentsField.getText
    state.hlsPath = hlsPathField.getText
  }

  private def checkFileExists(path: String): Unit = {
    if (FileUtil.isAbsolute(path) && !FileUtil.exists(path)) {
      throw new ConfigurationException(s"$path does not exists")
    }
  }

  private def validateCustomTools(): Unit = {
    if (useCustomToolsToggle.isSelected) {
      if (
        ormoluPathField.getText.trim.isEmpty ||
          hooglePathField.getText.trim.isEmpty) {
        throw new ConfigurationException(s"All Haskell tools paths have to be set")
      }

      checkFileExists(hooglePathField.getText)
      checkFileExists(ormoluPathField.getText)
      checkFileExists(stylishHaskellPathField.getText)
    }
  }


  override def disposeUIResources(): Unit = {}

  override def getHelpTopic: String = ""

  override def reset(): Unit = {
    val state = HaskellSettingsPersistentStateComponent.getInstance().getState
    defaultGhcOptionsField.setText(state.defaultGhcOptions)
    useSystemGhcToggle.setSelected(state.useSystemGhc)
    newProjectTemplateNameField.setText(state.newProjectTemplateName)
    cachePathField.setText(state.cachePath)
    hooglePathField.setText(state.hooglePath)
    ormoluPathField.setText(state.ormoluPath)
    stylishHaskellPathField.setText(state.stylishHaskellPath)
    useCustomToolsToggle.setSelected(state.customTools)
    extraStackArgumentsField.setText(state.extraStackArguments)
    hlsPathField.setText(state.hlsPath)
  }
}

object HaskellConfigurable {
  final val DefaultGhcOptions = "Default GHC options"
  final val NewProjectTemplateName = "Template name for new project"
  final val CachePath = "Cache path *"
  final val BuildToolsUsingSystemGhc = "Build tools using system GHC *"
  final val HooglePath = "Hoogle path"
  final val StylishHaskellPath = "Stylish Haskell path"
  final val OrmoluPath = "Ormolu path"
  final val UseCustomTool = "Use custom Haskell tools *"
  final val HlsPath = "HLS wrapper path (leave blank to use 'haskell-language-server-wrapper' on PATH)"
  final val CustomToolPathWarning =
    """WARNING! Specifying a path for a Haskell tool will override the default
      |behavior of building that tool from the Stackage LTS. This plugin was
      |tested only with the Haskell tools from the Stackage LTS. Providing a
      |path with your own Haskell tool (and thus overriding automatic rebuild
      |or download of that tool) could cause some features of this plugin to
      |break, because the API that your tool provide may differ from what the
      |plugin expects.""".stripMargin.replace('\n', ' ')
  final val ExtraStackArguments = "Extra stack arguments"
}
