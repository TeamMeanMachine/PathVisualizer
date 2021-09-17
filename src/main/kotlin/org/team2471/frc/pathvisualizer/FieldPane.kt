package org.team2471.frc.pathvisualizer

import edu.wpi.first.networktables.ConnectionInfo
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.ImageCursor
import javafx.scene.control.TableView
import javafx.scene.image.Image
import javafx.scene.input.*
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color

import org.team2471.frc.lib.coroutines.periodic
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.math.Vector2
import org.team2471.frc.lib.units.Time
import java.io.BufferedWriter
import java.io.File
import java.nio.file.Path
import java.util.*
object FieldPane : StackPane() {
    private val canvas = ResizableCanvas()
    private val arbitraryCanvas = ResizableCanvas()
    private val replayCanvas = ResizableCanvas()
    private val arbitraryGC = arbitraryCanvas.graphicsContext2D
    private val replayGC = replayCanvas.graphicsContext2D
    //When updating image change upperLeftOfFieldPixels, lowerRightOfFieldPixels, and zoomPivot
    private val image = Image("assets/2020Field.png")
    private val upperLeftOfFieldPixels = Vector2(105.0, 820.0)
    private val lowerRightOfFieldPixels = Vector2(2175.0, 4850.0)

    var zoomPivot = Vector2(1565.0, 821.0)  // the location in the image where the zoom origin will originate
    val fieldDimensionPixels = lowerRightOfFieldPixels - upperLeftOfFieldPixels
    val fieldDimensionFeet = Vector2(27.0, 54.0)
    var displayActiveRobot = false
    var displayLimeLightRobot = true
    var displayParallax = false
    var playing = false
    var recording = false
    var wasRecording = false
    var recordedPlayback = false
    var recordingFile : BufferedWriter? = null
    val recordingTimer = Timer()
    var currentRecordedTime = 0
    var firstRecordedTime : Time = Time(0.0)

    // view settings
    var zoom: Double = kotlin.math.round(feetToPixels(1.0))  // initially draw at 1:1 pixel in image = pixel on screen

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
    private var different = false
    private var from: Vector2? = null
    private val positionTable = ControlPanel.networkTableInstance.getTable("Drive")
    val limelightTable = ControlPanel.networkTableInstance.getTable("limelight-front")
    val shooterTable = ControlPanel.networkTableInstance.getTable("Shooter")
    init {
        children.add(canvas)
        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())
        arbitraryCanvas.onMousePressed = EventHandler<MouseEvent>(::onMousePressed)
        arbitraryCanvas.onMouseDragged = EventHandler<MouseEvent>(::onMouseDragged)
        arbitraryCanvas.onMouseReleased = EventHandler<MouseEvent> { onMouseReleased() }
        arbitraryCanvas.onZoom = EventHandler<ZoomEvent>(::onZoom)
        arbitraryCanvas.onKeyPressed = EventHandler<KeyEvent>(::onKeyPressed)
        arbitraryCanvas.onScroll = EventHandler<ScrollEvent>(::onScroll)
        initActiveRobotDraw()
        initPlaybackRobotDraw()
        initConnectionStatusDraw()
    }

    private fun initConnectionStatusDraw(){
        drawConnectionStatus(canvas.graphicsContext2D, ControlPanel.networkTableInstance.isConnected)
        val updateFrequencyInSeconds = 1
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                // check network table connection
                drawConnectionStatus(canvas.graphicsContext2D, ControlPanel.networkTableInstance.isConnected)
            }
        }, 1, 1000L * updateFrequencyInSeconds)
    }

    private fun initPlaybackRobotDraw() {
        children.add(replayCanvas)
        replayCanvas.widthProperty().bind(widthProperty())
        replayCanvas.heightProperty().bind(heightProperty())
        replayGC.clearRect(0.0, 0.0, replayCanvas.width, replayCanvas.height)
        val framesPerSecond = 20L
        val timer = Timer()
        if (LivePanel.currRecording != null && LivePanel.currRecording!!.recordings.size > 0) {
            firstRecordedTime = LivePanel.currRecording!!.recordings[0].ts
        } else {
            firstRecordedTime = Time(0.0)
        }
        currentRecordedTime = 0
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (recordedPlayback) {
                    //println("displaying arbitrary robot")

                    replayGC.clearRect(0.0, 0.0, replayCanvas.width, replayCanvas.height)
                    if (LivePanel.currRecording != null) {
                        val recording = LivePanel.currRecording!!.recordings[currentRecordedTime]
                        drawReplayRobot(arbitraryGC, Vector2(recording.x, recording.y), ControlPanel.autonomi.robotParameters.robotLength, ControlPanel.autonomi.robotParameters.robotWidth, recording.y)
                    }
                }
            }
        }, 1, 1000L/framesPerSecond)
    }

    private fun initActiveRobotDraw(){
        children.add(arbitraryCanvas)
        arbitraryCanvas.widthProperty().bind(widthProperty())
        arbitraryCanvas.heightProperty().bind(heightProperty())
        val framesPerSecond = 20L
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (playing) {
                    //println("displaying arbitrary robot")

                    arbitraryGC.clearRect(0.0, 0.0, arbitraryCanvas.width, arbitraryCanvas.height);
                    drawArbitraryRobot(arbitraryGC, Vector2(positionTable.getEntry("X").getDouble(0.0), positionTable.getEntry("Y").getDouble(0.0)), ControlPanel.autonomi.robotParameters.robotLength, ControlPanel.autonomi.robotParameters.robotWidth, positionTable.getEntry("Heading").getDouble(0.0))
                }
            }
        }, 1, 1000L/framesPerSecond)
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
        val prevDuration = selectedPath?.duration
        selectedPath?.scaleEasePoints(seconds)
        println("new duration is ${selectedPath?.duration}")
        selectedPath?.duration = seconds
        println("new duration explicitly set is ${selectedPath?.duration}")
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
        selectedPath?.curveType = curveType
    }


    fun deleteSelectedPoint() {
        if (selectedPoint != null) {
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

        var selectPathFlag = true

        //Find closest point
        var point: Path2DPoint? = selectedPath?.xyCurve?.headPoint
        while (point != null) {
            val tPoint = world2ScreenWithMirror(point.position, selectedPath?.isMirrored ?: false)
            var dist = (tPoint - mouseVec).length
            if (dist <= shortestDistance) {
                shortestDistance = dist
                closestPoint = point
                selectedPointType = Path2DPoint.PointType.POINT
            }

            if (point.prevPoint != null) {
                val tanPoint1 = world2ScreenWithMirror(point.position -
                        point.prevTangent * (1.0 / PathVisualizer.TANGENT_DRAW_FACTOR), selectedPath?.isMirrored ?: false)
                dist = (tanPoint1 - mouseVec).length
                if (dist <= shortestDistance) {
                    shortestDistance = dist
                    closestPoint = point
                    selectedPointType = Path2DPoint.PointType.PREV_TANGENT
                }
            }

            if (point.nextPoint != null) {
                val tanPoint2 = world2ScreenWithMirror(point.position +
                        point.nextTangent * (1.0 / PathVisualizer.TANGENT_DRAW_FACTOR), selectedPath?.isMirrored ?: false)
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
            selectPathFlag = false
        } else {
            if (closestPoint != null) {
                if (shortestDistance > PathVisualizer.CLICK_CIRCLE_SIZE * 2) {// trying to deselect?
                    selectedPoint = null
                } else {
                    selectedPath?.addVector2After(screen2World(mouseVec), closestPoint)
                    selectPathFlag = false
                }
            } else {  // first point on a path
                selectedPath?.addVector2(screen2World(mouseVec))
                selectPathFlag = false
            }
        }

        if ((e.isMiddleButtonDown || e.isSecondaryButtonDown) && shortestDistance >= PathVisualizer.CLICK_CIRCLE_SIZE * 2) {
            canvas.cursor = Cursor.CROSSHAIR
            mouseMode = PathVisualizer.MouseMode.PAN
        }

        when (mouseMode) {
            PathVisualizer.MouseMode.EDIT -> {
                editPoint = selectedPoint
                if (!selectPathFlag) from = when(selectedPointType){
                    Path2DPoint.PointType.POINT -> selectedPoint?.position
                    Path2DPoint.PointType.NEXT_TANGENT -> selectedPoint?.nextTangent
                    Path2DPoint.PointType.PREV_TANGENT -> selectedPoint?.prevTangent
                }
                draw()
                ControlPanel.refresh()
            }
            PathVisualizer.MouseMode.PAN -> {
                canvas.cursor = ImageCursor.CROSSHAIR
                oCoord = Vector2(e.x, e.y) - offset
            }
        }

        /*
            This code loops through each path, compares
            the mouse click with different incremental points
            on each path, and find the closest incremental point
            to the mouse click. Whichever path that point is on
            gets selected
         */
        if (selectPathFlag) {
            val gc = canvas.graphicsContext2D
            var nearestPath: Path2D? = null
            var nearestDistance = 10000.0

            //first, iterate through each path of the selected autonomous
            for (path in ControlPanel.selectedAutonomous!!.paths.values) {
                //iterate through each path with an increment of 200 steps
                if (path.duration == 0.0)
                    continue
                val deltaT = path.durationWithSpeed / 200.0
                var t = 0.0
                while (t <= path.durationWithSpeed) {
                    val ease = t / path.durationWithSpeed
                    var pos = path.getPosition(t)
                    t += deltaT
                    val comparePoint = world2Screen(pos)

                    //comparing mouse click to the nearest comparison point
                    val distance = (comparePoint - mouseVec).length
                    if (distance<nearestDistance) {
                        nearestDistance = distance
                        nearestPath = path
                        //println("" + nearestPoint.x + " " + nearestPoint.y)
                        //println("" + e.x + " " + e.y)
//                        gc.stroke = Color.CYAN
//                        gc.strokeOval(comparePoint.x-10.0, comparePoint.y-PathVisualizer.CLICK_CIRCLE_SIZE/2, PathVisualizer.CLICK_CIRCLE_SIZE, PathVisualizer.CLICK_CIRCLE_SIZE)
                    }
                    else {
//                        gc.stroke = Color.YELLOW
//                        gc.strokeOval(comparePoint.x - 10.0, comparePoint.y - PathVisualizer.CLICK_CIRCLE_SIZE/2, PathVisualizer.CLICK_CIRCLE_SIZE, PathVisualizer.CLICK_CIRCLE_SIZE)
                    }
                }
            }
            if (nearestDistance < PathVisualizer.CLICK_CIRCLE_SIZE) {
                ControlPanel.setSelectedPath(nearestPath?.name)
            }
//            gc.stroke = Color.MAGENTA
//            gc.strokeOval(e.x-1.5, e.y-1.5, 3.0, 3.0)
        }
    }
    private fun onMouseDragged(e: MouseEvent) {
        when (mouseMode) {
            PathVisualizer.MouseMode.EDIT -> {
                if (editPoint != null) {
                    val worldPoint = screen2WorldWithMirror(Vector2(e.x, e.y), selectedPath?.isMirrored ?: false)
                    if (!different) different = true
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
            PathVisualizer.MouseMode.EDIT -> {
                if (different && editPoint != null) {
                    TopBar.redoStack.clear()
                    TopBar.undoStack.add(TopBar.MovedPointAction(editPoint!!, from!!, selectedPointType))
                }
                editPoint = null
            }  // no longer editing
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
        //val positionTable = ControlPanel.networkTableInstance.getTable("Drive")
        gc.drawImage(image, 0.0, 0.0, image.width, image.height, upperLeftPixels.x, upperLeftPixels.y, dimensions.x, dimensions.y)

        if (!displayActiveRobot) {
            drawPaths(gc, ControlPanel.selectedAutonomous?.paths?.values, selectedPath, selectedPoint, selectedPointType)

            gc = EasePane.canvas.graphicsContext2D
            if (gc.canvas.width == 0.0)
                return
            gc.fill = Color.LIGHTGRAY
            gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)
            gc.lineWidth = 2.0

            when (selectedPath?.curveType) {
                Path2D.CurveType.EASE -> {
                    drawEaseCurve(selectedPath)
                }
                Path2D.CurveType.HEADING -> {
                    drawHeadingCurve(selectedPath)
                }
                Path2D.CurveType.BOTH -> {
                    drawEaseCurve(selectedPath)
                    drawHeadingCurve(selectedPath)
                }
                null -> {
                }
            }
            EasePane.drawTimeScrubber()
        }
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
            "z" -> {
                if (e.isControlDown) {
                    if (e.isShiftDown) TopBar.redo()
                    else TopBar.undo()
                }
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
        val centerPosition = selectedPath?.getPosition(time) ?: Vector2(0.0, 0.0)
        val heading = selectedPath?.headingCurve?.getValue(time) ?: 0.0
        return getWheelPositions(centerPosition, heading)
    }

    fun getWheelPositions(centerPosition: Vector2, heading: Double) : Array<Vector2> {
        var tangent = Vector2(0.0, 1.0)
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
