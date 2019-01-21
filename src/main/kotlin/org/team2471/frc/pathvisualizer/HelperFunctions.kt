package org.team2471.frc.pathvisualizer

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import org.team2471.frc.lib.vector.Vector2
import java.text.DecimalFormat

fun feetToPixels(feet: Double): Double = feet * FieldPane.fieldDimensionPixels.x / FieldPane.fieldDimensionFeet.x
fun PixelsToFeet(pixels: Double): Double = pixels * FieldPane.fieldDimensionFeet.x / FieldPane.fieldDimensionPixels.x

inline fun <T : Any, R> whenNotNull(input: T?, callback: (T) -> R): R? {
    return input?.let(callback)
}

fun world2Screen(vector2: Vector2): Vector2 {
    val temp = vector2 * FieldPane.zoom
    temp.y = -temp.y
    return temp + FieldPane.zoomPivot + FieldPane.offset
}

fun screen2World(vector2: Vector2): Vector2 {
    val temp = vector2 - FieldPane.offset - FieldPane.zoomPivot
    temp.y = -temp.y
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
    return xPoint / FieldPane.selectedPath!!.durationWithSpeed * EasePane.width
}

fun easeWorld2ScreenY(yPoint: Double): Double {
    return (1.0 - yPoint) * EasePane.height
}

fun easeScreen2WorldX(xPoint: Double): Double {
    return xPoint * FieldPane.selectedPath!!.durationWithSpeed / EasePane.width
}

fun easeScreen2WorldY(yPoint: Double): Double {
    return -1 * (yPoint / EasePane.height - 1)
}

fun compressAngle(prevAngle: Double) : Double {
    var angle = prevAngle
    if (angle < -Math.PI / 2) {
        while (angle < -Math.PI / 2) {
            angle += Math.PI / 2
        }
    }
    if (angle > Math.PI / 2) {
        while (angle > Math.PI / 2) {
            angle -= Math.PI / 2
        }
    }
    return angle
}


fun Double.format(fracDigits: Int): String {
    val fd = DecimalFormat()
    fd.maximumFractionDigits = fracDigits
    fd.minimumFractionDigits = fracDigits
    return fd.format(this)
}
