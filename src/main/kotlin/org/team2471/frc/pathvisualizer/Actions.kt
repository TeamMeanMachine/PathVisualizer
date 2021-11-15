package org.team2471.frc.pathvisualizer

import org.team2471.frc.lib.math.Vector2
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import javax.naming.ldap.Control

object Actions {
    class ChangedDurationAction(private val path : Path2D, private val from: Double, private val to: Double) : TopBar.Action {
        override fun undo(){
            // select path in UI
            ControlPanel.setSelectedPath(path)
            path.scaleEasePoints(from)
            path.duration = from
        }

        override fun redo() {
            ControlPanel.setSelectedPath(path)
            path.scaleEasePoints(to)
            path.duration = to
        }
        override fun toString() : String {
            return "Speed Change : ${from.round(1)} to ${to.round(1)} for ${path.autonomous.name}-${path.name}"
        }
    }

    class MovedPointAction(private val path: Path2D, private val point: Path2DPoint, private val from: Vector2, private val pointType: Path2DPoint.PointType) : TopBar.Action {
        private val to = when(pointType) {
            Path2DPoint.PointType.POINT -> point.position
            Path2DPoint.PointType.PREV_TANGENT -> point.prevTangent
            Path2DPoint.PointType.NEXT_TANGENT -> point.nextTangent
        }

        override fun undo() {
            ControlPanel.setSelectedPath(path)
            when (pointType) {
                Path2DPoint.PointType.POINT -> point.position = from
                Path2DPoint.PointType.PREV_TANGENT -> point.prevTangent = from
                Path2DPoint.PointType.NEXT_TANGENT -> point.nextTangent = from
            }
        }

        override fun redo() {
            println("redo")
            ControlPanel.setSelectedPath(path)
            when (pointType) {
                Path2DPoint.PointType.POINT -> point.position = to
                Path2DPoint.PointType.PREV_TANGENT -> point.prevTangent = to
                Path2DPoint.PointType.NEXT_TANGENT -> point.nextTangent = to
            }
            //point.prevTangent = to.prevTangent
            //point.nextTangent = to.nextTangent
        }
        override fun toString() : String {
            return "Move Point ($pointType) : $from to $to"
        }
    }
}