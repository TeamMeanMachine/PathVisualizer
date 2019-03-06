package org.team2471.frc.pathvisualizer

import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCombination
import javafx.stage.FileChooser
import org.team2471.frc.lib.math.Vector2
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Path2DPoint
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

    init {
        if (!fileName.isEmpty())
            openFile(File(fileName))

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
                val file = File(fileName)
                val json = ControlPanel.autonomi.toJsonString()
                val writer = PrintWriter(file)
                writer.append(json)
                writer.close()
            }
        }
        val sendToRobotItem = MenuItem("Send to Robot")
        sendToRobotItem.accelerator = KeyCombination.keyCombination("Ctrl + R")
        sendToRobotItem.setOnAction {
            ControlPanel.autonomi.publishToNetworkTables(ControlPanel.networkTableInstance)
            println("Sent to robot...")
        }

        menuFile.items.addAll(openMenuItem, saveAsMenuItem, saveMenuItem, sendToRobotItem)

        val menuEdit = Menu("Edit")
        val undoMenuItem = MenuItem("Undo")
        undoMenuItem.accelerator = KeyCombination.keyCombination("Ctrl + Z")
        undoMenuItem.setOnAction {
            undo()
        }
        val redoMenuItem = MenuItem("Redo")
        redoMenuItem.accelerator = KeyCombination.keyCombination("Ctrl + Shift + Z")
        redoMenuItem.setOnAction {
            redo()
        }

        menuEdit.items.addAll(undoMenuItem, redoMenuItem)
        menus.addAll(menuFile, menuEdit)
    }


    private fun openFile(file: File) {
        try {
            val json = file.readText()
            ControlPanel.autonomi = Autonomi.fromJsonString(json)
            userPref.put(userFilenameKey, file.absolutePath)
        } catch (e: Exception) {
            System.err.println("Failed to find file ${file.absolutePath}")
            ControlPanel.autonomi = Autonomi()
        }

        ControlPanel.initializeParameters()  // since some of our older saved files don't have parameters, this prevents a bunch null references
        ControlPanel.refresh()
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
            val json = ControlPanel.autonomi.toJsonString()
            val writer = PrintWriter(file)
            writer.append(json)
            writer.close()
        }
    }
    //TopBar.undoStack.add(MovedPointAction(point, original))

    fun undo() {
        if (undoStack.isEmpty()) return

        val action = undoStack.pop()
        action.undo()
        redoStack.push(action)
        FieldPane.draw()
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val action = redoStack.pop()
        action.redo()
        undoStack.push(action)
        FieldPane.draw()
    }

    interface Action {
        fun undo()
        fun redo()
    }

    class MovedPointAction(private val point: Path2DPoint, private val from: Vector2, private val pointType: Path2DPoint.PointType) : Action {
        private val to = when(pointType) {
            Path2DPoint.PointType.POINT -> point.position
            Path2DPoint.PointType.PREV_TANGENT -> point.prevTangent
            Path2DPoint.PointType.NEXT_TANGENT -> point.nextTangent
        }

        override fun undo() {
            when (pointType) {
                Path2DPoint.PointType.POINT -> point.position = from
                Path2DPoint.PointType.PREV_TANGENT -> point.prevTangent = from
                Path2DPoint.PointType.NEXT_TANGENT -> point.nextTangent = from
            }
        }

        override fun redo() {
            println("redo")
            when (pointType) {
                Path2DPoint.PointType.POINT -> point.position = to
                Path2DPoint.PointType.PREV_TANGENT -> point.prevTangent = to
                Path2DPoint.PointType.NEXT_TANGENT -> point.nextTangent = to
            }
            //point.prevTangent = to.prevTangent
            //point.nextTangent = to.nextTangent
        }
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