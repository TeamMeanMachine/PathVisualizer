package org.team2471.frc.pathvisualizer

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.math.Vector2
import org.team2471.frc.lib.motion_profiling.following.ArcadePath

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
    if (selectedPath!=null && ControlPanel.autonomi.drivetrainParameters!=null)
        drawSelectedPath(gc, selectedPath, selectedPoint, selectedPointType)
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

    val arcadePath = ArcadePath(path, ControlPanel.autonomi.arcadeParameters.trackWidth *
            ControlPanel.autonomi.arcadeParameters.scrubFactor)

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
    gc.strokeLine(corners[1].x, corners[1].y, corners[2].x, corners[2].y)
    gc.strokeLine(corners[2].x, corners[2].y, corners[3].x, corners[3].y)
    gc.strokeLine(corners[3].x, corners[3].y, corners[0].x, corners[0].y)
}
