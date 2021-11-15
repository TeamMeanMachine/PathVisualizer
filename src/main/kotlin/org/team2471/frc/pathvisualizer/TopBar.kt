package org.team2471.frc.pathvisualizer

import edu.wpi.first.math.trajectory.TrajectoryUtil
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCombination
import javafx.stage.FileChooser
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Autonomous
import java.io.File
import java.io.PrintWriter
import java.util.prefs.Preferences
import java.util.Stack

object TopBar : MenuBar() {
    private const val userFilenameKey = "org-frc2471-PathVisualizer-FileName"
    private val userPref = Preferences.userRoot()
    private var fileName = userPref.get(userFilenameKey, "")

    val undoStack = SizedStack<Action>(20)
    val redoStack = SizedStack<Action>(20)
    val toggleVisualizeActiveRobotMenuItem = MenuItem("☐ Display Active Robot")
    val toggleVisualizeParralaxMenuItem = MenuItem("☑ Display Parallax")
    val toggleVisualizeTargetMenuItem = MenuItem("☐ Display Odometry Target")
    val toggleVisualizeRecordMenuItem = MenuItem("Start Recording")
    init {
        if (!fileName.isEmpty()) {
            openFile(File(fileName))
        }

        // add the test auto and paths
        val testAuto = Autonomous("Tests")
        ControlPanel.autonomi.put(testAuto)
        testAuto.putPath(EightFootStraight)
        testAuto.putPath(EightFootCircle)
        testAuto.putPath(FourFootCircle)
        testAuto.putPath(TwoFootCircle)
        testAuto.putPath(AngleAndMagnitudeBug)
        testAuto.putPath(HookPath)

        val menuFile = Menu("File")
        val openMenuItem = MenuItem("Open...")
        openMenuItem.accelerator = KeyCombination.keyCombination("Ctrl + O")
        openMenuItem.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.title = "Open Autonomi File..."
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Autonomi files (*.json)", "*.json"))
            fileChooser.initialDirectory = File(System.getProperty("user.dir"))
            fileChooser.initialFileName = "Test.json"  // this is supposed to be saved in the registry, but it didn't work
            val file = fileChooser.showOpenDialog(PathVisualizer.stage)
            if (file != null) {
                fileName = file.absolutePath
                openFile(file)
            }
        }
        val saveAsMenuItem = MenuItem("Save As...")
        saveAsMenuItem.accelerator = KeyCombination.keyCombination("Ctrl + Shift + S")
        saveAsMenuItem.setOnAction {
            saveAs()
        }
        val saveMenuItem = MenuItem("Save")
        saveMenuItem.accelerator = KeyCombination.keyCombination("Ctrl + S")
        saveMenuItem.setOnAction {
            if (fileName.isEmpty()) {
                saveAs()
            } else {
                performSave()
            }
        }
        val sendToRobotItem = MenuItem("Send to Robot")
        sendToRobotItem.accelerator = KeyCombination.keyCombination("Ctrl + R")
        sendToRobotItem.setOnAction {
            ControlPanel.autonomi.publishToNetworkTables(ControlPanel.networkTableInstance)
        }

        val getFromRobotItem = MenuItem("Get from Robot")
        getFromRobotItem.setOnAction {
            readFromRobot()
        }

        menuFile.items.addAll(openMenuItem, saveAsMenuItem, saveMenuItem, sendToRobotItem, getFromRobotItem)

        val menuEdit = Menu("Edit")
        val undoMenuItem = MenuItem("Undo")
        undoMenuItem.accelerator = KeyCombination.keyCombination("Ctrl + Z")
        undoMenuItem.setOnAction {
            undo()
        }
        val redoMenuItem = MenuItem("Redo")
        redoMenuItem.accelerator = KeyCombination.keyCombination("Ctrl + Y")
        redoMenuItem.setOnAction {
            redo()
        }

        menuEdit.items.addAll(undoMenuItem, redoMenuItem)

//
//        val menuVisualize = Menu("Visualize")
//        toggleVisualizeActiveRobotMenuItem.setOnAction {
//            toggleVisualizeActiveRobot()
//        }
//        toggleVisualizeParralaxMenuItem.setOnAction {
//            toggleVisualizeParallax()
//        }
//        toggleVisualizeTargetMenuItem.setOnAction {
//            toggleVisualizeTarget()
//        }
//        toggleVisualizeRecordMenuItem.setOnAction {
//            toggleVisualizeRecord()
//        }
//        menuVisualize.items.addAll(toggleVisualizeActiveRobotMenuItem, toggleVisualizeParralaxMenuItem, toggleVisualizeTargetMenuItem, toggleVisualizeRecordMenuItem)

        menus.addAll(menuFile, menuEdit)//, menuVisualize)
    }

    fun toggleVisualizeActiveRobot() {
        FieldPane.displayActiveRobot = !FieldPane.displayActiveRobot
        toggleVisualizeActiveRobotMenuItem.text = if (FieldPane.displayActiveRobot) "☑ Display Active Robot" else "☐ Display Active Robot"

        // redraw screen in case we removed the arbitrary bot
        FieldPane.draw()
    }
    private fun toggleVisualizeParallax() {
        FieldPane.displayLimeLightRobot = !FieldPane.displayLimeLightRobot
        toggleVisualizeParralaxMenuItem.text = if (FieldPane.displayLimeLightRobot) "☑ Display Parallax" else "☐ Display Parallax"

        // redraw screen in case we removed the arbitrary bot
        FieldPane.draw()
    }
    private fun toggleVisualizeTarget() {
        FieldPane.displayParallax = !FieldPane.displayParallax
        toggleVisualizeTargetMenuItem.text = if (FieldPane.displayParallax) "☑ Display Target" else "☐ Display Target"

        // redraw screen in case we removed the arbitrary bot
        FieldPane.draw()
    }
    private fun toggleVisualizeRecord(){
        FieldPane.recording = !FieldPane.recording
        toggleVisualizeRecordMenuItem.text = if (FieldPane.recording) "Stop Recording" else "Start Recording"

        // redraw screen in case we removed the arbitrary bot
        FieldPane.draw()
    }
    private fun openFile(file: File) {
        try {
            val json = file.readText()
            ControlPanel.autonomi = Autonomi.fromJsonString(json)!!
            userPref.put(userFilenameKey, file.absolutePath)
        } catch (e: Exception) {
            System.err.println("Failed to find file ${file.absolutePath}")
            ControlPanel.autonomi = Autonomi()
        }

        ControlPanel.initializeParameters()  // since some of our older saved files don't have parameters, this prevents a bunch null references
        ControlPanel.refresh(true)
    }

    private fun readFromRobot(){
        try {
            val json = ControlPanel.autonomi.readFromNetworkTables(ControlPanel.networkTableInstance)
            println(json)
            ControlPanel.autonomi = Autonomi.fromJsonString(json)!!
            println("made it no error")
        } catch (e: Exception) {
            ControlPanel.autonomi = Autonomi()
        }

        ControlPanel.initializeParameters()  // since some of our older saved files don't have parameters, this prevents a bunch null references
        ControlPanel.refresh(true)
    }

    private fun saveAs() {
        val fileChooser = FileChooser()
        fileChooser.title = "Save Autonomi File As..."
        val extFilter = FileChooser.ExtensionFilter("Autonomi files (*.json)", "*.json")
        fileChooser.extensionFilters.add(extFilter)
        fileChooser.initialDirectory = File(System.getProperty("user.dir"))
        fileChooser.initialFileName = "Test.json"  // this is supposed to be saved in the registry, but it didn't work
        val file = fileChooser.showSaveDialog(PathVisualizer.stage)
        if (file != null) {
            userPref.put(userFilenameKey, file.absolutePath)
            fileName = file.absolutePath
            performSave()
        }
    }

    private fun performSave(){
        try {
            val file = File(fileName)
            fileWrite(fileName, ControlPanel.autonomi.toJsonString())
            if (ControlPanel.pathWeaverFormat) {

                val folder = file.parentFile.toPath()
                val autos = ControlPanel.autonomi.mapAutonomous
                for (currAutos in autos) {
                    for (currPath in currAutos.value.paths) {
                        val pathToSave = folder.resolve("AutoPW.${currAutos.key}.${currPath.key}.json")
                        println(pathToSave)
                        TrajectoryUtil.toPathweaverJson(currPath.value.trajectory(), pathToSave)
                    }
                }
            }
        } catch (ex : Exception) {
            println("Error during save: ${ex.message}")
        }
    }
    private fun fileWrite(writeToFile: String, json: String) {
        val file = File(writeToFile)
        val writer = PrintWriter(file)
        writer.append(json)
        writer.close()
    }
    //TopBar.undoStack.add(MovedPointAction(point, original))

    fun addUndo(action : Action) {
        undoStack.add(action)
        UndoPanel.refresh()
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        val action = undoStack.pop()
        action.undo()
        redoStack.push(action)
        FieldPane.draw()
        UndoPanel.refresh()
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val action = redoStack.pop()
        action.redo()
        undoStack.push(action)
        FieldPane.draw()
        UndoPanel.refresh()
    }

    interface Action {
        fun undo()
        fun redo()
        override fun toString() : String
    }
}


class SizedStack<T>(private val maxSize: Int) : Stack<T>() {

    override fun push(`object`: T): T {
        //If the stack is too big, remove elements until it's the right size.
        while (this.size >= maxSize) {
            this.removeAt(0)
        }
        return super.push(`object`)
    }
}