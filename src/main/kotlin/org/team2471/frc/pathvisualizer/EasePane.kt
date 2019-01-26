package org.team2471.frc.pathvisualizer

import javafx.event.EventHandler
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.team2471.frc.lib.motion_profiling.MotionKey
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.vector.Vector2
import org.team2471.frc.pathvisualizer.ControlPanel.refresh
import org.team2471.frc.pathvisualizer.FieldPane.draw
import org.team2471.frc.pathvisualizer.FieldPane.selectedPath
import kotlin.math.absoluteValue


private var startMouse = Vector2(0.0, 0.0)
private var mouseMode = PathVisualizer.MouseMode.EDIT

var selectedPointType = Path2DPoint.PointType.POINT
    private set
private var editPoint: MotionKey? = null
var selectedPoint: MotionKey? = null
    private set

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
            if (Math.abs(mouseVec.x - currentTimeX) < PathVisualizer.CLICK_CIRCLE_SIZE) {
                mouseMode = PathVisualizer.MouseMode.DRAG_TIME
            }
        }
        selectedPoint = null

        val mouseVec = Vector2(e.x, e.y)
        startMouse = mouseVec

        var shortestDistance = 10000.0
        var closestPoint: MotionKey? = null

        //Find closest point
        var point: MotionKey? = selectedPath?.easeCurve?.headKey
        while (point != null) {
            val tPoint = Vector2(easeWorld2ScreenX(point.time), easeWorld2ScreenY(point.value))
            var dist = Vector2.length(Vector2.subtract(tPoint, mouseVec))
            if (dist <= shortestDistance) {
                shortestDistance = dist
                closestPoint = point
                selectedPointType = Path2DPoint.PointType.POINT
            }

            if (point.prevKey != null) {
                val prevTanPoint = point.timeAndValue - point.prevTangent / PathVisualizer.TANGENT_DRAW_FACTOR
                val tanPoint = Vector2(easeWorld2ScreenX(prevTanPoint.x), easeWorld2ScreenY(prevTanPoint.y))
                dist = Vector2.length(Vector2.subtract(tanPoint, mouseVec))
                if (dist <= shortestDistance) {
                    shortestDistance = dist
                    closestPoint = point
                    selectedPointType = Path2DPoint.PointType.PREV_TANGENT
                }
            }

            if (point.nextKey != null) {
                val prevTanPoint = point.timeAndValue + point.nextTangent / PathVisualizer.TANGENT_DRAW_FACTOR
                val tanPoint = Vector2(easeWorld2ScreenX(prevTanPoint.x), easeWorld2ScreenY(prevTanPoint.y))
                dist = Vector2.length(Vector2.subtract(tanPoint, mouseVec))
                if (dist <= shortestDistance) {
                    shortestDistance = dist
                    closestPoint = point
                    selectedPointType = Path2DPoint.PointType.NEXT_TANGENT
                }
            }

            point = point.nextKey
            // find distance between point clicked and each point in the graph. Whichever one is the max gets to be assigned to the var.
        }
        if (shortestDistance <= PathVisualizer.CLICK_CIRCLE_SIZE / 2) {
            selectedPoint = closestPoint
        } else {

            if ((selectedPath!!.easeCurve.getValue(easeScreen2WorldX(e.x)) - easeScreen2WorldY(e.y)).absoluteValue * 25 < 0.5) {
                val worldPosition = Vector2(easeScreen2WorldX(e.x), easeScreen2WorldY(e.y))
                selectedPath!!.addEasePoint(worldPosition.x, worldPosition.y)
            }
            else if (closestPoint != null) {
                if (shortestDistance > PathVisualizer.CLICK_CIRCLE_SIZE * 2) // trying to deselect?
                    selectedPoint = null
            }
        }

        if ((e.isMiddleButtonDown || e.isSecondaryButtonDown) && shortestDistance >= PathVisualizer.CLICK_CIRCLE_SIZE * 2) {
            //fieldCanvas.cursor = Cursor.CROSSHAIR
            mouseMode = PathVisualizer.MouseMode.PAN
        }

        when (mouseMode) {
           PathVisualizer.MouseMode.EDIT -> {
                editPoint = selectedPoint
                draw()
            }
           PathVisualizer.MouseMode.PAN -> {
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
            PathVisualizer.MouseMode.EDIT -> {
                if (editPoint != null) {
                    val worldPoint = Vector2(easeScreen2WorldX(e.x), easeScreen2WorldY(e.y))
                    when (selectedPointType) {
                        Path2DPoint.PointType.POINT -> {
                            editPoint?.timeAndValue = worldPoint
                        }
                        Path2DPoint.PointType.PREV_TANGENT -> {
                            editPoint!!.prevMagnitude *= Vector2.length(worldPoint - editPoint!!.timeAndValue) * 3.0 / Vector2.length(editPoint!!.prevTangent)
                            editPoint!!.angle = (Math.atan((worldPoint.y - editPoint!!.value) / (worldPoint.x - editPoint!!.time)))
                        }
                        Path2DPoint.PointType.NEXT_TANGENT -> {
                            editPoint!!.nextMagnitude *= Vector2.length(worldPoint - editPoint!!.timeAndValue) * 3.0 / Vector2.length(editPoint!!.nextTangent)
                            editPoint!!.angle = (Math.atan((worldPoint.y - editPoint!!.value) / (worldPoint.x - editPoint!!.time)))
                        }
                    }
                    draw()
                }
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
            val red = if (ease * Color.RED.red < 1.0) ease * Color.RED.red else 1.0
            val redGreen = if (ease * Color.RED.green < 1.0) ease * Color.RED.green else 1.0
            val redBlue = if (ease * Color.RED.blue < 1.0) ease * Color.RED.blue else 1.0

            gc.stroke = Color(red, redGreen, redBlue, 1.0)
            drawEaseLine(gc, prevPos, pos, gc.canvas.height)

            prevPos = pos
            t += deltaT
        }

        // circles and lines for handles
        var point: MotionKey? = selectedPath.easeCurve.headKey
        while (point != null) {
            if (point === selectedPoint && selectedPointType == Path2DPoint.PointType.POINT)
                gc.stroke = Color.LIMEGREEN
            else
            gc.stroke = Color.WHITE

            val tPoint = Vector2(easeWorld2ScreenX(point.time), easeWorld2ScreenY(point.value))
            gc.strokeOval(tPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2, tPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
            if (point.prevKey != null) {
                if (point === selectedPoint && selectedPointType == Path2DPoint.PointType.PREV_TANGENT)
                    gc.stroke = Color.LIMEGREEN
                else
                gc.stroke = Color.WHITE
                val tanPoint = point.timeAndValue - point.prevTangent / PathVisualizer.TANGENT_DRAW_FACTOR
                tanPoint.set(easeWorld2ScreenX(tanPoint.x), easeWorld2ScreenY(tanPoint.y))
                gc.strokeOval(tanPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2, tanPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
                gc.lineWidth = 2.0
                gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
            }

            if (point.nextKey != null) {
                if (point === selectedPoint && selectedPointType == Path2DPoint.PointType.NEXT_TANGENT)
                    gc.stroke = Color.LIMEGREEN
                else
                gc.stroke = Color.WHITE
                val tanPoint = point.timeAndValue + point.nextTangent / PathVisualizer.TANGENT_DRAW_FACTOR
                tanPoint.set(easeWorld2ScreenX(tanPoint.x), easeWorld2ScreenY(tanPoint.y))
                gc.strokeOval(tanPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2, tanPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
                gc.lineWidth = 2.0
                gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
            }

            point = point.nextKey
        }

        val currentTimeX = easeWorld2ScreenX(ControlPanel.currentTime)
        gc.stroke = Color.YELLOW
        gc.strokeLine(currentTimeX, 10.0, currentTimeX, canvas.height-10.0)
        gc.strokeOval(currentTimeX - 5.0, 0.0, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
        gc.strokeOval(currentTimeX - 5.0, canvas.height - 10.0, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
    }

    private fun drawEaseLine(gc: GraphicsContext, p1: Vector2, p2: Vector2, yScale: Double) {
        gc.strokeLine(p1.x, p1.y * yScale, p2.x, p2.y * yScale)
    }
}