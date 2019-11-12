package org.team2471.frc.pathvisualizer

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.math.Vector2
import org.team2471.frc.lib.motion_profiling.MotionKey
import org.team2471.frc.lib.motion.following.ArcadePath
import org.team2471.frc.lib.motion_profiling.following.ArcadeParameters

private fun drawPathLine(gc: GraphicsContext, p1: Vector2, p2: Vector2) {
    val tp1 = world2Screen(p1)
    val tp2 = world2Screen(p2)
    gc.strokeLine(tp1.x, tp1.y, tp2.x, tp2.y)
}

fun drawPaths(gc: GraphicsContext, paths: Iterable<Path2D>?, selectedPath: Path2D?, selectedPoint: Path2DPoint?, selectedPointType: Path2DPoint.PointType) {
    if (paths == null) return

    for (path in paths) {
        drawPath(gc, path)
    }
    if (selectedPath != null && ControlPanel.autonomi.drivetrainParameters != null) {
        drawSelectedPath(gc, selectedPath, selectedPoint, selectedPointType)
    }
}

private fun drawPath(gc: GraphicsContext, path: Path2D?) {
    if (path == null || path.duration == 0.0)
        return
    val deltaT = path.durationWithSpeed / 200.0
    var prevPos = path.getPosition(0.0)
    var pos: Vector2

    gc.stroke = Color.WHITE
    var t = deltaT
    while (t <= path.durationWithSpeed) {
        val ease = t / path.durationWithSpeed
        pos = path.getPosition(t)

        // center line
        gc.stroke = Color(ease * Color.WHITE.red, ease * Color.WHITE.green, ease * Color.WHITE.blue, 1.0)
        drawPathLine(gc, prevPos, pos)
        prevPos = Vector2(pos.x, pos.y)
        t += deltaT
    }
}

private fun drawSelectedPath(gc: GraphicsContext, path: Path2D?, selectedPoint: Path2DPoint?, selectedPointType: Path2DPoint.PointType) {
    if (path == null || !path.hasPoints())
        return

    val parameters = (ControlPanel.autonomi.drivetrainParameters as? ArcadeParameters) ?: return

    val arcadePath = ArcadePath(path, parameters.trackWidth *
            parameters.scrubFactor)

    if (path.durationWithSpeed > 0.0) {
        val deltaT = path.durationWithSpeed / 200.0
        var prevLeftPos = arcadePath.getLeftPosition(0.0)
        var prevRightPos = arcadePath.getRightPosition(0.0)
        var leftPos: Vector2
        var rightPos: Vector2
        val maxSpeed = 8.0
        var t = deltaT
        arcadePath.resetDistances()
        while (t <= path.durationWithSpeed) {
            leftPos = arcadePath.getLeftPosition(t)
            rightPos = arcadePath.getRightPosition(t)

            // left wheel
            var leftSpeed = (leftPos - prevLeftPos).length / deltaT / maxSpeed
            leftSpeed = Math.max(Math.min(1.0, leftSpeed),0.0)
            val leftDelta = arcadePath.getLeftPositionDelta(t)
            if (leftDelta >= 0) {
                gc.stroke = Color(1.0 - leftSpeed, leftSpeed, 0.0, 1.0)  // green fast, red slow
            } else {
                gc.stroke = Color(1.0 - leftSpeed, 0.0, leftSpeed, 1.0)  // blue fast, red slow
            }
            drawPathLine(gc, prevLeftPos, leftPos)

            // right wheel
            var rightSpeed = (rightPos - prevRightPos).length / deltaT / maxSpeed
            rightSpeed = Math.max(Math.min(1.0, rightSpeed),0.0)
            val rightDelta = arcadePath.getRightPositionDelta(t)
            if (rightDelta >= 0) {
                gc.stroke = Color(1.0 - rightSpeed, rightSpeed, 0.0, 1.0)
            } else {
                gc.stroke = Color(1.0 - rightSpeed, 0.0, rightSpeed, 1.0)
            }
            drawPathLine(gc, prevRightPos, rightPos)

            // set the prevs for the next loop
            prevLeftPos = leftPos.copy()
            prevRightPos = rightPos.copy()
            t += deltaT
        }
    }

    // circles and lines for handles
    var point: Path2DPoint? = path.xyCurve.headPoint
    while (point != null) {
        if (point === selectedPoint && selectedPointType == Path2DPoint.PointType.POINT)
            gc.stroke = Color.LIMEGREEN
        else
            gc.stroke = Color.WHITE

        val tPoint = world2ScreenWithMirror(point.position, path.isMirrored)
        gc.strokeOval(tPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2, tPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2,
                PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
        if (point.prevPoint != null) {
            if (point === selectedPoint && selectedPointType == Path2DPoint.PointType.PREV_TANGENT)
                gc.stroke = Color.LIMEGREEN
            else
                gc.stroke = Color.WHITE
            val tanPoint = world2ScreenWithMirror(point.position - point.prevTangent * (1.0 / PathVisualizer.TANGENT_DRAW_FACTOR), path.isMirrored)
            gc.strokeOval(tanPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2,
                    tanPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2,
                    PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
            gc.lineWidth = 2.0
            gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
        }
        if (point.nextPoint != null) {
            if (point === selectedPoint && selectedPointType == Path2DPoint.PointType.NEXT_TANGENT)
                gc.stroke = Color.LIMEGREEN
            else
                gc.stroke = Color.WHITE
            val tanPoint = world2ScreenWithMirror(point.position + point.nextTangent *
                    1.0 / PathVisualizer.TANGENT_DRAW_FACTOR, path.isMirrored)
            gc.strokeOval(tanPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2,
                    tanPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2,
                    PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
            gc.lineWidth = 2.0
            gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
        }
        point = point.nextPoint
    }

    drawRobot(gc, path)
}

fun drawRobot(gc: GraphicsContext, selectedPath: Path2D) {
    gc.stroke = Color.YELLOW
    val corners = FieldPane.getWheelPositions(ControlPanel.currentTime)
    corners[0] = world2ScreenWithMirror(corners[0], selectedPath.isMirrored)
    corners[1] = world2ScreenWithMirror(corners[1], selectedPath.isMirrored)
    corners[2] = world2ScreenWithMirror(corners[2], selectedPath.isMirrored)
    corners[3] = world2ScreenWithMirror(corners[3], selectedPath.isMirrored)

    gc.strokeLine(corners[0].x, corners[0].y, corners[1].x, corners[1].y)
    gc.strokeText("F", (corners[1].x + corners[0].x)/2, (corners[1].y + corners[0].y)/2)
    gc.stroke = Color.BLUE
    gc.strokeLine(corners[1].x, corners[1].y, corners[2].x, corners[2].y)
    gc.strokeLine(corners[2].x, corners[2].y, corners[3].x, corners[3].y)
    gc.strokeLine(corners[3].x, corners[3].y, corners[0].x, corners[0].y)
}

fun drawWheelPaths(gc: GraphicsContext, selectedPath: Path2D?) {
    gc.stroke = Color.GHOSTWHITE
    for (i in 1..selectedPath!!.xyCurve.length.toInt()) {
        val corners = FieldPane.getWheelPositions(i/selectedPath.duration)
        corners[0] = world2ScreenWithMirror(corners[0], selectedPath.isMirrored)
        corners[1] = world2ScreenWithMirror(corners[1], selectedPath.isMirrored)
        corners[2] = world2ScreenWithMirror(corners[2], selectedPath.isMirrored)
        corners[3] = world2ScreenWithMirror(corners[3], selectedPath.isMirrored)

        val prevCorners = FieldPane.getWheelPositions((i - 1)/selectedPath.duration)
        prevCorners[0] = world2ScreenWithMirror(prevCorners[0], selectedPath.isMirrored)
        prevCorners[1] = world2ScreenWithMirror(prevCorners[1], selectedPath.isMirrored)
        prevCorners[2] = world2ScreenWithMirror(prevCorners[2], selectedPath.isMirrored)
        prevCorners[3] = world2ScreenWithMirror(prevCorners[3], selectedPath.isMirrored)

        gc.strokeLine(prevCorners[0].x, prevCorners[0].y, corners[0].x, corners[0].y)
        gc.strokeLine(prevCorners[1].x, prevCorners[1].y, corners[1].x, corners[1].y)
        gc.strokeLine(prevCorners[2].x, prevCorners[2].y, corners[2].x, corners[2].y)
        gc.strokeLine(prevCorners[3].x, prevCorners[3].y, corners[3].x, corners[3].y)

    }
}

fun drawHeadingCurve(path: Path2D?) {
    val gc = EasePane.canvas.graphicsContext2D
    if (gc.canvas.width == 0.0)
        return

    val selectedPath = path ?: return

    val deltaT = selectedPath.durationWithSpeed / gc.canvas.width
    var t = 0.0
    var ease = selectedPath.headingCurve.getValue(t)
    var x = t / selectedPath.durationWithSpeed * gc.canvas.width
    var y = (180.0 - ease) * gc.canvas.height
    var pos = org.team2471.frc.lib.vector.Vector2(x, y)
    var prevPos = pos

    t = deltaT
    while (t <= selectedPath.durationWithSpeed) {
        ease = selectedPath.headingCurve.getValue(t)
        x = t / selectedPath.durationWithSpeed * gc.canvas.width
        y = (180.0 - ease) / 180.0
        pos = org.team2471.frc.lib.vector.Vector2(x, y)
        val blue = Math.max(Math.min(ease * Color.BLUE.blue, 1.0), 0.0)

        gc.stroke = Color(0.0, 0.0, blue, 1.0)
        gc.strokeLine(prevPos.x, prevPos.y * gc.canvas.height, pos.x, pos.y * gc.canvas.height)


        prevPos = pos
        t += deltaT
    }
}

fun drawEaseCurve(path: Path2D?) {
    val gc = EasePane.canvas.graphicsContext2D
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
    var prevVelocity = org.team2471.frc.lib.vector.Vector2(0.0, 0.0)
    var ease = 0.0

    deltaT = selectedPath.durationWithSpeed / gc.canvas.width
    t = 0.0
    ease = selectedPath.easeCurve.getValue(t)
    var x = t / selectedPath.durationWithSpeed * gc.canvas.width
    var y = (1.0 - ease) * gc.canvas.height
    var pos = org.team2471.frc.lib.vector.Vector2(x, y)
    var prevPos = pos
    var prevSpeed = 0.0
    var prevAccel = 0.0
    t = deltaT
    while (t <= selectedPath.durationWithSpeed) {
        ease = selectedPath.easeCurve.getValue(t)
        x = t / selectedPath.durationWithSpeed * gc.canvas.width
        y = (1.0 - ease)
        pos = org.team2471.frc.lib.vector.Vector2(x, y)
        val red = Math.max(Math.min(ease * Color.RED.red, 1.0), 0.0)
        val redGreen = Math.max(Math.min(ease * Color.RED.green, 1.0), 0.0)
        val redBlue = Math.max(Math.min(ease * Color.RED.blue, 1.0), 0.0)

        gc.stroke = Color(red, redGreen, redBlue, 1.0)
        gc.strokeLine(prevPos.x, prevPos.y * gc.canvas.height, pos.x, pos.y * gc.canvas.height)

        prevPos = pos
        t += deltaT
    }

    // circles and lines for handles
    var point: MotionKey? = selectedPath.easeCurve.headKey
    while (point != null) {
        if (point === EasePane.selectedPoint && EasePane.selectedPointType == Path2DPoint.PointType.POINT)
            gc.stroke = Color.LIMEGREEN
        else
            gc.stroke = Color.WHITE

        val tPoint = org.team2471.frc.lib.vector.Vector2(easeWorld2ScreenX(point.time), easeWorld2ScreenY(point.value))
        gc.strokeOval(tPoint.x - PathVisualizer.DRAW_CIRCLE_SIZE / 2, tPoint.y - PathVisualizer.DRAW_CIRCLE_SIZE / 2, PathVisualizer.DRAW_CIRCLE_SIZE, PathVisualizer.DRAW_CIRCLE_SIZE)
        if (point.prevKey != null) {
            if (point === EasePane.selectedPoint && EasePane.selectedPointType == Path2DPoint.PointType.PREV_TANGENT)
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
            if (point === EasePane.selectedPoint && EasePane.selectedPointType == Path2DPoint.PointType.NEXT_TANGENT)
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
}
