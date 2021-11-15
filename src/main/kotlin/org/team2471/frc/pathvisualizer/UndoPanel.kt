package org.team2471.frc.pathvisualizer

import javafx.scene.control.ComboBox
import javafx.scene.layout.VBox

object UndoPanel : VBox() {
    private val undoComboBox = ComboBox<String>()
    init{

    }
    fun refreshCombo(){
        undoComboBox.items.clear()

        for (action in TopBar.undoStack) {
            undoComboBox.items.add(action.toString())
        }


    }
}