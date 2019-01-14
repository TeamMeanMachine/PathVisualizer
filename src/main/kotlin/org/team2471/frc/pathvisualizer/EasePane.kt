package org.team2471.frc.pathvisualizer

import javafx.event.EventHandler
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.team2471.frc.lib.motion_profiling.MotionKey
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.vector.Vector2

object EasePane : StackPane() {
    private val canvas = ResizableCanvas()

    init {
        children.add(canvas)
        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())

        canvas.onMousePressed = EventHandler<MouseEvent> { onMousePressed(it) }
        canvas.onMouseDragged = EventHandler<MouseEvent> { onMouseDragged(it) }
        canvas.onMouseReleased = EventHandler<MouseEvent> { onMouseReleased() }
    }

    private fun onMousePressed(e: MouseEvent) {

    }

    private fun onMouseDragged(e: MouseEvent) {

    }

    private fun onMouseReleased() {

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

/*
        while (t <= selectedPath!!.durationWithSpeed) {
            ease = selectedPath!!.easeCurve.getValue(t)
            var distance = ease * pathLength
            var position = selectedPath!!.xyCurve.getPositionAtDistance(distance)
            var velocity = (position - prevPosition) / deltaT
            var acceleration = (velocity - prevVelocity) / deltaT

            var newEase = false
            if (Vector2.length(acceleration) > maxAcceleration) {
                acceleration = Vector2.normalize(acceleration) * maxAcceleration
                velocity = prevVelocity + acceleration * deltaT
                newEase = true
            }
            if (Vector2.length(velocity) > maxVelocity) {
                velocity = Vector2.normalize(velocity) * maxVelocity
                newEase = true
            }
            if (newEase) {
                position = prevPosition + velocity * deltaT
                distance = Vector2.length(position - prevPosition)
                ease = distance / pathLength
                selectedPath!!.easeCurve.storeValue(t,ease)
            }
            t += deltaT
            prevVelocity = velocity
        }
*/


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
        gc.strokeLine(currentTimeX, 0.0, currentTimeX, canvas.height)
        gc.stroke = Color.BLACK
        gc.strokeOval(currentTimeX - 5.0, canvas.height - 10.0, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)

    }

    private fun drawEaseLine(gc: GraphicsContext, p1: Vector2, p2: Vector2, yScale: Double) {
        gc.strokeLine(p1.x, p1.y * yScale, p2.x, p2.y * yScale)
    }
}