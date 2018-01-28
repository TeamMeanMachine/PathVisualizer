package org.team2471.frc.pathvisualizer

import org.team2471.frc.lib.motion_profiling.Path2D

object DefaultPath : Path2D() {
  init {
    name = "Circle"
    travelDirection = 1.0
    robotWidth = 36.5 / 12
    val tangentLength = 6.0

    addPointAndTangent(0.0, 0.0, 0.0, tangentLength)
    addPointAndTangent(4.0, 4.0, tangentLength, 0.0)
    addPointAndTangent(8.0, 0.0, 0.0, -tangentLength)
    addPointAndTangent(4.0, -4.0, -tangentLength, 0.0)
    addPointAndTangent(0.0, 0.0, 0.0, tangentLength)

    addEasePoint(0.0, 0.0)
    addEasePoint(8.0, 1.0)
  }
        //Path2D() {
//  init {
//    addPointAndTangent(0.0, 0.0, 0.0, 6.0)
//    addPointAndTangent(-4.2, 7.0, -6.0, 3.0)
//
//    addPointAndTangent(-4.2, 7.0, 6.0, -3.0)
//    addPointAndTangent(-0.0, 0.0, -0.0, -6.0)

//    addPointAndTangent(0.0,0.0,0.0,4.0)
//    addPointAndTangent(0.0,13.1,4.0,2.0)
//    addPointAndTangent(7.0, 13.1,2.0,0.0)

//    addPoint(0.0,0.0);
//    addPoint(0.0,13.0);
//    addPoint(7.0,13.0);

//    addEasePoint(0.0, 0.0)
//    addEasePoint(7.0, 1.0)
//}
}