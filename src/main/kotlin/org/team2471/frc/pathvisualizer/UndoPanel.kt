package org.team2471.frc.pathvisualizer

import javafx.geometry.Insets
import javafx.scene.control.ListView
import javafx.scene.layout.VBox
import javafx.scene.text.Text

object UndoPanel : VBox() {
    private val undoListView = ListView<String>()
    private val redoListView = ListView<String>()
    init{
        spacing = 10.0
        padding = Insets(10.0, 10.0, 10.0, 10.0)
        val undoText = Text("Undo List")
        val redoText = Text("Redo List")
        children.addAll(undoText, undoListView, redoText, redoListView)


    }
    fun refresh(){
        println("refreshing view")
        undoListView.items.clear()
        for (action in TopBar.undoStack) {
            undoListView.items.add(action.toString())
        }
        undoListView.refresh()

        redoListView.items.clear()
        for (action in TopBar.redoStack) {
            redoListView.items.add(action.toString())
        }
        redoListView.refresh()

    }
}