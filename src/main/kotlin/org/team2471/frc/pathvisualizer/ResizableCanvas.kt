package org.team2471.frc.pathvisualizer

import javafx.scene.canvas.Canvas

class ResizableCanvas : Canvas() {
    override fun isResizable() = true

    override fun prefWidth(height: Double) = width

    override fun prefHeight(width: Double) = height

    override fun minHeight(width: Double): Double {
        return 64.0
    }

    override fun maxHeight(width: Double): Double {
        return 10000.0
    }

    override fun minWidth(height: Double): Double {
        return 0.0
    }

    override fun maxWidth(height: Double): Double {
        return 10000.0
    }

    override fun resize(_width: Double, _height: Double) {
//        width = _width
//        height = _height
        FieldPane.draw()
    }
}