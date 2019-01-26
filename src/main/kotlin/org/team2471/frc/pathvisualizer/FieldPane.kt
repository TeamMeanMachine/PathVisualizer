package org.team2471.frc.pathvisualizer

import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.ImageCursor
import javafx.scene.image.Image
import javafx.scene.input.*
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.math.Vector2
import org.team2471.frc.pathvisualizer.ControlPanel
import kotlin.math.round

object FieldPane : StackPane() {
    private val canvas = ResizableCanvas()
    private val image = Image("assets/2019Field.PNG")
    private val upperLeftOfFieldPixels = Vector2(79.0, 0.0)
    private val lowerRightOfFieldPixels = Vector2(1421.0, 1352.0)

    val zoomPivot = Vector2(750.0, 1352.0)  // the location in the image where the zoom origin will originate
    val fieldDimensionPixels = lowerRightOfFieldPixels - upperLeftOfFieldPixels
    val fieldDimensionFeet = Vector2(27.0, 27.0)

    // view settings
    var zoom: Double = round(feetToPixels(1.0))  // initially draw at 1:1 pixel in image = pixel on screen

    var selectedPointType = Path2DPoint.PointType.POINT
        private set

    var selectedPath: Path2D? = null
        set(value) {
            field = value
            selectedPoint = null
            EasePane.selectedPoint = null
        }

    private var editPoint: Path2DPoint? = null
    var selectedPoint: Path2DPoint? = null

    private var oCoord: Vector2 = Vector2(0.0, 0.0)
    var offset = Vector2(0.0, 0.0)
        private set

    private val upperLeftFeet = screen2World(Vector2(0.0, 0.0))  // calculate these when zoom is 1:1, and offset is 0,0
    private val lowerRightFeet = screen2World(Vector2(image.width, image.height))
    private var startMouse = Vector2(0.0, 0.0)
    private var mouseMode = PathVisualizer.MouseMode.EDIT

    init {
        children.add(canvas)
        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())
        canvas.onMousePressed = EventHandler<MouseEvent>(::onMousePressed)
        canvas.onMouseDragged = EventHandler<MouseEvent>(::onMouseDragged)
        canvas.onMouseReleased = EventHandler<MouseEvent> { onMouseReleased() }
        canvas.onZoom = EventHandler<ZoomEvent>(::onZoom)
        canvas.onKeyPressed = EventHandler<KeyEvent>(::onKeyPressed)
        canvas.onScroll = EventHandler<ScrollEvent>(::onScroll)
    }

    fun zoomFit() {
        val hZoom = width / PixelsToFeet(image.width)
        val vZoom = height / PixelsToFeet(image.height)

        zoom = 1.0
        zoom = Math.min(hZoom, vZoom)
        offset = Vector2(0.0, 0.0)

        val upperLeftPixels = world2Screen(upperLeftFeet)
        val lowerRightPixels = world2Screen(lowerRightFeet)
        val centerPixels = (upperLeftPixels + lowerRightPixels) / 2.0
        val centerOfCanvas = Vector2(canvas.width, canvas.height) / 2.0

        offset = centerOfCanvas - centerPixels

        draw()
    }

    fun setSelectedPathMirrored(mirrored: Boolean) {
        selectedPath?.autonomous?.isMirrored = mirrored
        draw()
    }

    fun setSelectedPathDuration(seconds: Double) {
        selectedPath?.duration = seconds
        draw()
    }

    fun setSelectedPathSpeed(speed: Double) {
        selectedPath?.speed = speed
        draw()
    }

    fun setSelectedPathRobotDirection(robotDirection: Path2D.RobotDirection) {
        selectedPath?.robotDirection = robotDirection
        draw()
    }

    fun setSelectedPointX(x: Double) {
        when (selectedPointType) {
            Path2DPoint.PointType.POINT -> {
                selectedPoint!!.position.x = x
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
                selectedPoint?.position?.y = y
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

    fun setSelectedSlopeMethod(slopeMode: Path2DPoint.SlopeMethod) {
        selectedPoint?.prevSlopeMethod = slopeMode
        selectedPoint?.nextSlopeMethod = slopeMode
        draw()
    }

    fun setSelectedCurveType(curveType: Path2D.CurveType) {
        selectedPath!!.curveType = curveType
    }


    fun deleteSelectedPoint() {
        if (selectedPoint != null && selectedPath != null) {
            FieldPane.selectedPath?.removePoint(selectedPoint)
            selectedPoint = null
        }
        draw()
    }


    private fun onMousePressed(e: MouseEvent) {
        val mouseVec = Vector2(e.x, e.y)
        startMouse = mouseVec

        var shortestDistance = 10000.0
        var closestPoint: Path2DPoint? = null

        //Find closest point
        var point: Path2DPoint? = selectedPath?.xyCurve?.headPoint
        while (point != null) {
            val tPoint = world2ScreenWithMirror(point.position, selectedPath!!.isMirrored)
            var dist = (tPoint - mouseVec).length
            if (dist <= shortestDistance) {
                shortestDistance = dist
                closestPoint = point
                selectedPointType = Path2DPoint.PointType.POINT
            }

            if (point.prevPoint != null) {
                val tanPoint1 = world2ScreenWithMirror(point.position -
                        point.prevTangent * (1.0 / PathVisualizer.TANGENT_DRAW_FACTOR), selectedPath!!.isMirrored)
                dist = (tanPoint1 - mouseVec).length
                if (dist <= shortestDistance) {
                    shortestDistance = dist
                    closestPoint = point
                    selectedPointType = Path2DPoint.PointType.PREV_TANGENT
                }
            }

            if (point.nextPoint != null) {
                val tanPoint2 = world2ScreenWithMirror(point.position +
                        point.nextTangent * (1.0 / PathVisualizer.TANGENT_DRAW_FACTOR), selectedPath!!.isMirrored)
                dist = (tanPoint2 - mouseVec).length
                if (dist <= shortestDistance) {
                    shortestDistance = dist
                    closestPoint = point
                    selectedPointType = Path2DPoint.PointType.NEXT_TANGENT
                }
            }
            point = point.nextPoint
            // find distance between point clicked and each point in the graph. Whichever one is the max gets to be assigned to the var.
        }
        if (shortestDistance <= PathVisualizer.CLICK_CIRCLE_SIZE) {
            selectedPoint = closestPoint
        } else {
            if (closestPoint != null) {
                selectedPoint = if (shortestDistance > PathVisualizer.CLICK_CIRCLE_SIZE * 2) {// trying to deselect?
                    null
                } else {
                    selectedPath?.addVector2After(screen2World(mouseVec), closestPoint)
                }
            } else {  // first point on a path?
                //                val path2DPoint = selectedPath?.addVector2(screen2World(mouseVec)-Vector2(0.0,0.25)) // add a pair of points, initially on top of one another
                //                selectedPoint = selectedPaath?.addVector2After(screen2World(mouseVec), path2DPoint)
                selectedPath?.addVector2(screen2World(mouseVec))
            }
        }

        if ((e.isMiddleButtonDown || e.isSecondaryButtonDown) && shortestDistance >= PathVisualizer.CLICK_CIRCLE_SIZE * 2) {
            canvas.cursor = Cursor.CROSSHAIR
            mouseMode = PathVisualizer.MouseMode.PAN
        }

        when (mouseMode) {
            PathVisualizer.MouseMode.EDIT -> {
                editPoint = selectedPoint
                draw()
                ControlPanel.refresh()
            }
            PathVisualizer.MouseMode.PAN -> {
                canvas.cursor = ImageCursor.CROSSHAIR
                oCoord = Vector2(e.x, e.y) - offset
            }
        }
    }

    private fun onMouseDragged(e: MouseEvent) {
        when (mouseMode) {
            PathVisualizer.MouseMode.EDIT -> {
                if (editPoint != null) {
                    val worldPoint = screen2WorldWithMirror(Vector2(e.x, e.y), selectedPath!!.isMirrored)
                    when (selectedPointType) {
                        Path2DPoint.PointType.POINT -> editPoint?.position = worldPoint
                        Path2DPoint.PointType.PREV_TANGENT -> editPoint!!.prevTangent = (worldPoint - editPoint!!.position) * -PathVisualizer.TANGENT_DRAW_FACTOR
                        Path2DPoint.PointType.NEXT_TANGENT -> editPoint!!.nextTangent = (worldPoint - editPoint!!.position) * PathVisualizer.TANGENT_DRAW_FACTOR
                    }
                    draw()
                    ControlPanel.refresh()
                }
            }
            PathVisualizer.MouseMode.PAN -> {
                offset.x = e.x - oCoord.x
                offset.y = e.y - oCoord.y
                draw()
            }
        }
    }

    private fun onMouseReleased() {
        when (mouseMode) {
            PathVisualizer.MouseMode.EDIT -> editPoint = null  // no longer editing
            PathVisualizer.MouseMode.PAN -> mouseMode = PathVisualizer.MouseMode.EDIT
        }
        canvas.cursor = Cursor.DEFAULT
        canvas.requestFocus()
    }

    fun draw() {
        var gc = canvas.graphicsContext2D
        gc.fill = Color.LIGHTGRAY
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)

        // calculate ImageView corners
        val upperLeftPixels = world2Screen(upperLeftFeet)
        val lowerRightPixels = world2Screen(lowerRightFeet)
        val dimensions = lowerRightPixels - upperLeftPixels
        gc.drawImage(image, 0.0, 0.0, image.width, image.height, upperLeftPixels.x, upperLeftPixels.y, dimensions.x, dimensions.y)
        drawPaths(gc, ControlPanel.selectedAutonomous?.paths?.values, selectedPath, selectedPoint, selectedPointType)

        gc = EasePane.canvas.graphicsContext2D
        if (gc.canvas.width == 0.0)
            return
        gc.fill = Color.LIGHTGRAY
        gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)
        gc.lineWidth = 2.0


        when (selectedPath!!.curveType) {
            Path2D.CurveType.EASE -> {
                EasePane.drawEaseCurve(selectedPath)
            }
            Path2D.CurveType.HEADING -> {
                EasePane.drawHeadingCurve(selectedPath)
            }
            Path2D.CurveType.BOTH -> {
                EasePane.drawEaseCurve(selectedPath)
                EasePane.drawHeadingCurve(selectedPath)
            }
            null -> {}
        }
        EasePane.drawTimeScrubber()
    }

    private fun onZoom(e: ZoomEvent) {
        zoom *= e.zoomFactor
        draw()
    }

    private fun onKeyPressed(e: KeyEvent) {

        //monitoring keyboard input for "p", if pressed, will enable pan ability
        when (e.text) {
            "a" -> {
                mouseMode = PathVisualizer.MouseMode.ADD
            }
            "p" -> {
                canvas.cursor = ImageCursor.CROSSHAIR
                mouseMode = PathVisualizer.MouseMode.PAN
            }
            "f" -> {
                zoomFit()
            }
            "=" -> {
                zoom *= if (e.isControlDown)
                    1.01
                else
                    1.10
                draw()
            }
            "-" -> {
                zoom /= if (e.isControlDown)
                    1.01
                else
                    1.10
                draw()
            }
        }
        if (selectedPoint != null && e.isControlDown) {
            val offset = Vector2(0.0, 0.0)

            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (e.code) {
                KeyCode.UP -> offset.y += 1.0 / 12.0
                KeyCode.DOWN -> offset.y -= 1.0 / 12.0
                KeyCode.LEFT -> offset.x -= 1.0 / 12.0
                KeyCode.RIGHT -> offset.x += 1.0 / 12.0
            }
            when (selectedPointType) {
                Path2DPoint.PointType.POINT -> {
                    selectedPoint!!.position += offset
                }
                Path2DPoint.PointType.PREV_TANGENT -> {
                    selectedPoint!!.prevTangent += offset * -PathVisualizer.TANGENT_DRAW_FACTOR
                }
                Path2DPoint.PointType.NEXT_TANGENT -> {
                    selectedPoint!!.nextTangent += offset * PathVisualizer.TANGENT_DRAW_FACTOR
                }
            }
            if (offset != Vector2(0.0, 0.0)) {
                draw()
                canvas.requestFocus()
            }
            ControlPanel.refresh()
        }
    }

    private fun onScroll(e: ScrollEvent) {
        if (mouseMode != PathVisualizer.MouseMode.PAN) {
            zoom += e.deltaY / 25 * -1
            draw()
        }
    }

    fun getWheelPositions(time: Double): Array<Vector2> {  // offset can be positive or negative (half the width of the robot)
        val centerPosition = selectedPath!!.getPosition(time)
        var tangent = selectedPath!!.getTangent(time)
        tangent = tangent!!.normalize()
        val heading = selectedPath!!.headingCurve.getValue(time)
        tangent = tangent.rotateDegrees(-heading)

        var perpendicularToPath = tangent.perpendicular()
        val robotLength = ControlPanel.autonomi.robotParameters.robotLength / 2.0
        tangent *= robotLength
        val robotWidth = ControlPanel.autonomi.robotParameters.robotWidth / 2.0
        perpendicularToPath *= robotWidth

        return arrayOf(
                centerPosition + tangent + perpendicularToPath,
                centerPosition + tangent - perpendicularToPath,
                centerPosition - tangent - perpendicularToPath,
                centerPosition - tangent + perpendicularToPath
        )
    }
}