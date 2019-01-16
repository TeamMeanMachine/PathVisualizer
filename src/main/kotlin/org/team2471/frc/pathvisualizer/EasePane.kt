package org.team2471.frc.pathvisualizer

import javafx.event.EventHandler
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.team2471.frc.lib.motion_profiling.MotionKey
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.vector.Vector2
import org.team2471.frc.pathvisualizer.ControlPanel.refresh
import org.team2471.frc.pathvisualizer.FieldPane.draw
import org.team2471.frc.pathvisualizer.FieldPane.selectedPath

object EasePane : StackPane() {
    private val canvas = ResizableCanvas()
    private var mouseMode = PathVisualizer.MouseMode.EDIT

    init {
        children.add(canvas)
        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())

        canvas.onMousePressed = EventHandler<MouseEvent> { onMousePressed(it) }
        canvas.onMouseDragged = EventHandler<MouseEvent> { onMouseDragged(it) }
        canvas.onMouseReleased = EventHandler<MouseEvent> { onMouseReleased() }
    }

    private fun onMousePressed(e: MouseEvent) {
        if (selectedPath!=null) {
            val mouseVec = Vector2(e.x, e.y)
            val currentTimeX = ControlPanel.currentTime / selectedPath!!.durationWithSpeed * canvas.width
            val topTimeKnob = Vector2(currentTimeX, 5.0)
            val bottomTimeKnob = Vector2(currentTimeX, canvas.height - 5.0)
            if (Vector2.length(mouseVec - topTimeKnob) < PathVisualizer.CLICK_CIRCLE_SIZE ||
                    Vector2.length(mouseVec - bottomTimeKnob) < PathVisualizer.CLICK_CIRCLE_SIZE) {
                mouseMode = PathVisualizer.MouseMode.DRAG_TIME
            }
        }
    }

    private fun onMouseDragged(e: MouseEvent) {
        when (mouseMode) {
            PathVisualizer.MouseMode.DRAG_TIME -> {
                ControlPanel.currentTime = e.x * selectedPath!!.durationWithSpeed / canvas.width
                refresh()
                draw()
            }
        }
    }

    private fun onMouseReleased() {
        when (mouseMode) {
            PathVisualizer.MouseMode.DRAG_TIME -> {
                mouseMode = PathVisualizer.MouseMode.EDIT
            }
        }
    }

    fun drawEaseCurve(path: Path2D?) {
        val gc = canvas.graphicsContext2D
        if (gc.canvas.width == 0.0)
            return
        gc.fill = Color.LIGHTGRAY
        gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)
        gc.lineWidth = 2.0

        val selectedPath = path ?: return

        val maxVelocity = 12.5  // feet per sec
        val maxAcceleration = 5.0  // feet per sec^2
        val maxCurveAcceleration = 5.0  // feet per sec^2
        val speedFactor = 0.75
        val accelerationFactor = 0.75
        val curveFactor = 0.75

        val pathLength = selectedPath.length
        var deltaT = 1.0 / 50.0
        var t = deltaT
        var prevPosition = selectedPath.xyCurve.getPositionAtDistance(0.0)
        var prevVelocity = Vector2(0.0, 0.0)
        var ease = 0.0

        deltaT = selectedPath.durationWithSpeed / gc.canvas.width
        t = 0.0
        ease = selectedPath.easeCurve.getValue(t)
        var x = t / selectedPath.durationWithSpeed * gc.canvas.width
        var y = (1.0 - ease) * gc.canvas.height
        var pos = Vector2(x, y)
        var prevPos = pos
        var prevSpeed = 0.0
        var prevAccel = 0.0
        t = deltaT
        while (t <= selectedPath.durationWithSpeed) {
            ease = selectedPath.easeCurve.getValue(t)
            x = t / selectedPath.durationWithSpeed * gc.canvas.width
            y = (1.0 - ease)
            pos = Vector2(x, y)
            gc.stroke = Color(ease * Color.RED.red, ease * Color.RED.green, ease * Color.RED.blue, 1.0)
            drawEaseLine(gc, prevPos, pos, gc.canvas.height)

            prevPos = pos
            t += deltaT
        }

        // circles and lines for handles
        var point: MotionKey? = selectedPath.easeCurve.headKey
        while (point != null) {
//            if (point === selectedPoint && pointType == PointType.POINT)
//                gc.stroke = Color.LIMEGREEN
//            else
            gc.stroke = Color.WHITE

            val tPoint = Vector2(point.time / selectedPath.durationWithSpeed * canvas.width, (1.0 - point.value) * canvas.height)
            gc.strokeOval(tPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2, tPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
            if (point.prevKey != null) {
//                if (point === selectedPoint && pointType == PointType.PREV_TANGENT)
//                    gc.stroke = Color.LIMEGREEN
//                else
                gc.stroke = Color.WHITE
                val tanPoint = Vector2.subtract(point.timeAndValue, Vector2.multiply(point.prevTangent, 1.0 / PathVisualizer.TANGENT_DRAW_FACTOR))
                tanPoint.set(tanPoint.x / selectedPath.durationWithSpeed * canvas.width, (1.0 - tanPoint.y) * canvas.height)
                gc.strokeOval(tanPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2, tanPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
                gc.lineWidth = 2.0
                gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
            }

            if (point.nextKey != null) {
//                if (point === selectedPoint && pointType == PointType.NEXT_TANGENT)
//                    gc.stroke = Color.LIMEGREEN
//                else
                gc.stroke = Color.WHITE
                val tanPoint = Vector2.add(point.timeAndValue, Vector2.multiply(point.nextTangent, 1.0 / PathVisualizer.TANGENT_DRAW_FACTOR))
                tanPoint.set(tanPoint.x / selectedPath.durationWithSpeed * canvas.width, (1.0 - tanPoint.y) * canvas.height)
                gc.strokeOval(tanPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2, tanPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
                gc.lineWidth = 2.0
                gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
            }

            point = point.nextKey
        }

        val currentTimeX = ControlPanel.currentTime / selectedPath.durationWithSpeed * canvas.width
        gc.stroke = Color.YELLOW
        gc.strokeLine(currentTimeX, 10.0, currentTimeX, canvas.height-10.0)
        gc.strokeOval(currentTimeX - 5.0, 0.0, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
        gc.strokeOval(currentTimeX - 5.0, canvas.height - 10.0, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
    }

    private fun drawEaseLine(gc: GraphicsContext, p1: Vector2, p2: Vector2, yScale: Double) {
        gc.strokeLine(p1.x, p1.y * yScale, p2.x, p2.y * yScale)
    }
}