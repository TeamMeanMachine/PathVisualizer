package org.team2471.frc.pathvisualizer

import org.team2471.frc.lib.units.Time

data class RobotRecording (
    val name  : String,
    val recordings  : ArrayList<RobotInTime>
)
data class RobotInTime (
    var ts : Time,
    var x : Double,
    var y: Double,
    var h: Double
)
