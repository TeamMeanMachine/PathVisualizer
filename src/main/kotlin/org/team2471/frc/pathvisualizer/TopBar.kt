package org.team2471.frc.pathvisualizer

import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
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
        saveAsMenuItem.setOnAction {
            saveAs()
        }
        val saveMenuItem = MenuItem("Save")
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

        menuFile.items.addAll(openMenuItem, saveAsMenuItem, saveMenuItem)

        val menuEdit = Menu("Edit")
        val undoMenuItem = MenuItem("Undo")
        undoMenuItem.setOnAction {
            undo()
        }
        val redoMenuItem = MenuItem("Redo")
        redoMenuItem.setOnAction {
            redo()
        }

        menuEdit.items.addAll(undoMenuItem, redoMenuItem)

        //val menuHelp = Menu("Help")
        menus.addAll(menuFile, menuEdit/*, menuHelp*/)
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

    class MovedPointAction(private val point: Path2DPoint, private val from: Vector2) : Action {
        private val to = point.position

        override fun undo() {
            point.position = from
        }

        override fun redo() {
            point.position = to
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