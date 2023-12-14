package org.team2471.frc.pathvisualizer

import edu.wpi.first.math.trajectory.Trajectory
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import org.team2471.frc.lib.math.Vector2
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.units.asMeters
import org.team2471.frc.lib.units.feet
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.reflect.KClass

fun feetToPixels(feet: Double): Double = feet * FieldPane.fieldDimensionPixels.x / FieldPane.fieldDimensionFeet.x
fun pixelsToFeet(pixels: Double): Double = pixels * FieldPane.fieldDimensionFeet.x / FieldPane.fieldDimensionPixels.x

inline fun <T : Any, R> whenNotNull(input: T?, callback: (T) -> R): R? {
    return input?.let(callback)
}

fun world2Screen(vector2: Vector2): Vector2 {
    val temp = (vector2 * FieldPane.zoom).mirrorYAxis()
    return temp + FieldPane.zoomPivot + FieldPane.offset
}

fun screen2World(vector2: Vector2): Vector2 {
    val temp = (vector2 - FieldPane.offset - FieldPane.zoomPivot).mirrorYAxis()
    return temp / FieldPane.zoom
}

fun world2ScreenWithMirror(vector2: Vector2, mirror: Boolean): Vector2 {
    val temp = vector2 * FieldPane.zoom
    temp.y = -temp.y
    if (mirror)
        temp.x = -temp.x
    return temp + FieldPane.zoomPivot + FieldPane.offset
}

fun screen2WorldWithMirror(vector2: Vector2, mirror: Boolean): Vector2 {
    val temp = vector2 - FieldPane.offset - FieldPane.zoomPivot
    temp.y = -temp.y
    if (mirror)
        temp.x = -temp.x
    return temp / FieldPane.zoom
}

fun easeWorld2ScreenX(xPoint: Double): Double {
    if(FieldPane.selectedPath != null)
        return xPoint / FieldPane.selectedPath!!.durationWithSpeed * EasePane.width
    return 0.0
}

fun easeWorld2ScreenY(yPoint: Double): Double {
    return (1.0 - yPoint) * EasePane.height
}
fun headingWorld2ScreenY(yPoint: Double) : Double {
    return getHeadingYVal(yPoint) * EasePane.canvas.height
}

fun easeScreen2WorldX(xPoint: Double): Double {
    return xPoint * (FieldPane.selectedPath?.durationWithSpeed ?: 0.0) / EasePane.width /// EasePane.zoom
}

fun easeScreen2WorldY(yPoint: Double): Double {
    return -1 * (yPoint / EasePane.height - 1) // EasePane.zoom
}

fun Double.format(fracDigits: Int): String {
    val fd = DecimalFormat()
    fd.maximumFractionDigits = fracDigits
    fd.minimumFractionDigits = fracDigits
    return fd.format(this)
}
fun Double.round(fracDigits : Int) : Double {
    return if (this.equals(0.0)) {
        0.0
    } else {
        val powerOf: Double = 10.0.pow(fracDigits)
        kotlin.math.round(this * powerOf) / (powerOf)
    }
}
fun Path2D.trajectory() : Trajectory? {
    return this.generateTrajectory(ControlPanel.maxVelocity.feet.asMeters, ControlPanel.maxAcceleration.feet.asMeters)
}
fun <T:Any> TextField.setChangeHandler(dataValidate : KClass<T>, allowBlank: Boolean = true, changeFunc : () -> Unit ) {
    val verifyAndApplyChange = {
        if (this.promptText != this.text) {
            var allowChange = true
            if (dataValidate == Double::class) {
                allowChange = false
                if (this.text.isBlank() || this.text.toDoubleOrNull() == null) {
                    this.styleClass.add("textfield-error")
                } else {
                    allowChange = true
                    this.styleClass.remove("textfield-error")
                }
            }
            if (dataValidate == String::class && !allowBlank && this.text.isEmpty()) {
                allowChange = false
            }
            if (allowChange) {
                changeFunc()
            }
        }
    }

    this.setOnKeyPressed { event ->
        if (event.code === KeyCode.ENTER) {
            verifyAndApplyChange()
            this.promptText = this.text
        }
    }
    this.focusedProperty().addListener { _, _, newVal ->
        if (newVal) {
            this.promptText = this.text
        } else {
            verifyAndApplyChange()
            this.promptText = ""
        }
    }
}
fun <T:Any> TextField.setChangeHandler(dataValidate : KClass<T>, changeFunc : () -> Unit ) {
    setChangeHandler(dataValidate, true, changeFunc )
}
fun TextField.setChangeHandler(changeFunc : () -> Unit ) {
    setChangeHandler(String::class, true, changeFunc )
}
