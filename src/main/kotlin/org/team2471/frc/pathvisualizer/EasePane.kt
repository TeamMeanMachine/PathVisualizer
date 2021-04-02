package org.team2471.frc.pathvisualizer

import javafx.event.EventHandler
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.BackgroundPosition
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.team2471.frc.lib.motion_profiling.MotionKey
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.vector.Vector2
import org.team2471.frc.pathvisualizer.ControlPanel.refresh
import org.team2471.frc.pathvisualizer.FieldPane.draw
import org.team2471.frc.pathvisualizer.FieldPane.selectedPath
import javax.naming.ldap.Control
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.round


private var startMouse = Vector2(0.0, 0.0)

var selectedPointType = Path2DPoint.PointType.POINT
var selectedPointHeading = false
    private set
private var editPoint: MotionKey? = null

object EasePane : StackPane() {

    var zoomPivot = Vector2(750.0, 0.0)  // the location in the image where the zoom origin will originate
    var zoom: Double = 1.0  // initially draw at 1:1 pixel in image = pixel on screen

    val canvas = ResizableCanvas()
    private var mouseMode = PathVisualizer.MouseMode.EDIT
    var selectedPoint: MotionKey? = null
    var selectedPointType = Path2DPoint.PointType.POINT
        private set

    init {
        children.add(canvas)
        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())

        canvas.onMousePressed = EventHandler<MouseEvent> { onMousePressed(it) }
        canvas.onMouseDragged = EventHandler<MouseEvent> { onMouseDragged(it) }
        canvas.onScroll = EventHandler<ScrollEvent> {(EasePane::onScroll)}
        canvas.onMouseReleased = EventHandler<MouseEvent> { onMouseReleased() }
    }

    private fun onMousePressed(e: MouseEvent) {
        if (selectedPath!=null) {
            val mouseVec = Vector2(e.x, e.y)
            val currentTimeX = ControlPanel.currentTime / selectedPath!!.durationWithSpeed * canvas.width
            if (abs(mouseVec.x - currentTimeX) < PathVisualizer.CLICK_CIRCLE_SIZE) {
                mouseMode = PathVisualizer.MouseMode.DRAG_TIME
            }
        }
        selectedPoint = null
        FieldPane.selectedPoint = null

        val mouseVec = Vector2(e.x, e.y)
        startMouse = mouseVec

        var shortestDistance = 10000.0
        var closestPoint: MotionKey? = null

        selectedPointHeading = false

        // Find closest point (heading curve)
        var point: MotionKey? = selectedPath?.headingCurve?.headKey

        if (selectedPath?.curveType == Path2D.CurveType.BOTH || selectedPath?.curveType == Path2D.CurveType.HEADING) {
            while (point != null) {
                println("found a point ${point.value}")
                val tPoint = Vector2(easeWorld2ScreenX(point.time), headingWorld2ScreenY(point.value))
                var dist = Vector2.length(Vector2.subtract(tPoint, mouseVec))
                if (dist <= shortestDistance) {
                    shortestDistance = dist
                    closestPoint = point
                    selectedPointType = Path2DPoint.PointType.POINT
                    selectedPointHeading = true
                }
                point = point.nextKey
            }
        }

        if (selectedPath?.curveType == Path2D.CurveType.BOTH || selectedPath?.curveType == Path2D.CurveType.EASE) {

            //Find closest point (ease curve)
            point = selectedPath?.easeCurve?.headKey
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

        if (selectedPoint != null) {
            ControlPanel.currentTime = selectedPoint?.time!!
            refresh()
            draw()
        }
        if (selectedPoint != null && e.isShiftDown) {
            // deleting selected point
            if (selectedPointHeading) {
                println("removing ${selectedPoint?.time} ${selectedPoint?.value}")
                selectedPath?.removeHeadingPoint(selectedPoint)
            } else {
                selectedPath?.removeEasePoint(selectedPoint)
            }
            draw()
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
                    ControlPanel.refreshPoints()
                }
            }
        }
    }

    private fun onScroll(e: ScrollEvent) {
        this.zoom += e.deltaX
    }

    private fun onMouseReleased() {
        when (mouseMode) {
            PathVisualizer.MouseMode.DRAG_TIME -> {
                mouseMode = PathVisualizer.MouseMode.EDIT
            }
        }
    }

    fun drawTimeScrubber() {
        val gc = canvas.graphicsContext2D
        if (gc.canvas.width == 0.0)
            return

        val currentTimeX = easeWorld2ScreenX(ControlPanel.currentTime)
        gc.stroke = Color.YELLOW
        gc.strokeLine(currentTimeX, 10.0, currentTimeX, canvas.height-10.0)
        gc.strokeOval(currentTimeX - 5.0, 0.0, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
        gc.strokeOval(currentTimeX - 5.0, canvas.height - 10.0, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
    }

    fun setSelectedPointX(x: Double) {
        when (selectedPointType) {
            Path2DPoint.PointType.POINT -> {
                selectedPoint!!.time = x
            }
            Path2DPoint.PointType.PREV_TANGENT -> {
                selectedPoint!!.prevTangent = Vector2(x * -PathVisualizer.TANGENT_DRAW_FACTOR, selectedPoint!!.prevTangent.y)
            }
            Path2DPoint.PointType.NEXT_TANGENT -> {
                selectedPoint!!.nextTangent = Vector2(x * PathVisualizer.TANGENT_DRAW_FACTOR, selectedPoint!!.nextTangent.y)
            }
        }

        selectedPoint?.onPositionChanged()
        draw()
    }

    fun setSelectedPointY(y: Double) {
        when (selectedPointType) {
            Path2DPoint.PointType.POINT -> {
                selectedPoint?.value = y
            }
            Path2DPoint.PointType.PREV_TANGENT -> {
                selectedPoint!!.prevTangent = Vector2(selectedPoint!!.prevTangent.x, y * -PathVisualizer.TANGENT_DRAW_FACTOR)
            }
            Path2DPoint.PointType.NEXT_TANGENT -> {
                selectedPoint!!.nextTangent = Vector2(selectedPoint!!.nextTangent.x, y * PathVisualizer.TANGENT_DRAW_FACTOR)
            }
        }
        selectedPoint?.onPositionChanged()
        draw()
    }
    fun setSelectedPointAngle(angle: Double) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (selectedPointType) {
            Path2DPoint.PointType.PREV_TANGENT -> {
                selectedPoint!!.prevAngleAndMagnitude = Vector2(angle, selectedPoint!!.prevMagnitude)
            }
            Path2DPoint.PointType.NEXT_TANGENT -> {
                selectedPoint!!.nextAngleAndMagnitude = Vector2(angle, selectedPoint!!.nextMagnitude)
            }
        }
        draw()
    }
    fun setSelectedPointMagnitude(magnitude: Double) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (selectedPointType) {
            Path2DPoint.PointType.PREV_TANGENT -> {
                selectedPoint!!.prevAngleAndMagnitude = Vector2(selectedPoint!!.prevAngle, magnitude)
            }
            Path2DPoint.PointType.NEXT_TANGENT -> {
                selectedPoint!!.nextAngleAndMagnitude = Vector2(selectedPoint!!.nextAngle, magnitude)
            }
        }
        draw()
    }

    fun isBelowEaseValue(mousePosition: org.team2471.frc.lib.math.Vector2) {

    }
}