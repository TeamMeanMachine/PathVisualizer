package org.team2471.frc.pathvisualizer

import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import javafx.util.Duration

class FontAwesome {
    enum class Icon(val string: String) {
        Align("\uf247"),
        Delete("\uf1f8"),
        Add("\u002b"),
        Edit("\uf044"),
        Play("\uf04b"),
        AngleDown("\uf107"),
        AngleUp("\uf106"),
        ForwardStep("\uf051"),
        BackwardStep("\uf048")

    }
    companion object {
        fun cssClass(icon: Icon): String {
            return "${icon.name.lowercase()}-button"
        }
    }
}
fun Button.fontAwesome(fontUnicode: FontAwesome.Icon, tooltip: String) : Button {
    this.font = PathVisualizer.fontAwesome
    this.text = fontUnicode.string
    this.standardizedTooltip(tooltip)
    this.styleClass.add(FontAwesome.cssClass(fontUnicode))
    return this
}
fun Button.fontAwesome(fontUnicode: FontAwesome.Icon, tooltip: String, styleClass: String) : Button {
    this.font = PathVisualizer.fontAwesome
    this.text = fontUnicode.string
    this.standardizedTooltip(tooltip)
    this.styleClass.add(styleClass)
    return this
}
fun Button.standardizedTooltip(text:String) {
    val tooltip = Tooltip(text)
    tooltip.style = "-fx-font-size: 12";
    tooltip.showDelay = Duration(400.0)
    this.tooltip = tooltip
}
