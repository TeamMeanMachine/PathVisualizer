package org.team2471.frc.pathvisualizer

import javax.swing.JFrame

fun main(args: Array<String>) {
  val application = JFrame("Path Visualizer")
  application.setSize(1024, 768)

  val pvPanel = PathVisualizer()
  application.add(pvPanel)
  application.isVisible = true
}