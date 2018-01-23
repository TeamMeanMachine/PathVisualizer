package org.team2471.frc.pathvisualizer

import edu.wpi.first.wpilibj.networktables.NetworkTable
import java.awt.*
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.vector.Vector2
import org.team2471.frc.lib.motion_profiling.SharedAutonomousConfig

import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.File
import java.awt.BorderLayout
import java.awt.GridLayout

class PathVisualizer : JPanel() {

    private var selectedAutonomousConfig = SharedAutonomousConfig("Auto1")
    private var selectedPath: Path2D = DefaultPath //Path2D("Path1")  // DefaultPath

    private var blueSideImage: BufferedImage? = null
    private var redSideImage: BufferedImage? = null
    private val zoomTextField: JTextField
    private val sideSelection: JComboBox<String>
    private val autoSelection: JComboBox<String>
    private val pathSelection: JComboBox<String>
    private val timeTextField: JTextField

    private enum class Sides {
        BLUE, RED
    }

    private var timeInSeconds: Double = 5.0
        set(value) {
            timeInSeconds = value
            selectedPath.removeAllEasePoints()
            selectedPath.addEasePoint(0.0, 0.0)
            selectedPath.addEasePoint(timeInSeconds, 1.0)
        }

    private var sides: Sides = Sides.BLUE
    private val circleSize = 10
    private var zoom: Double = 20.0
    internal val offset = Vector2(915.0, 560.0)
    private val tangentLengthDrawFactor = 3.0
    internal var editPoint: Path2DPoint? = null
    internal var editVector: Vector2? = null
    internal var selectedPoint: Path2DPoint? = null

    internal enum class PointType {
        NONE, POINT, PREV_TANGENT, NEXT_TANGENT
    }

    internal var pointType = PointType.NONE

    init {
        NetworkTable.setServerMode()
        setSize(1024, 768)
        selectedAutonomousConfig.putPath(selectedPath.getName(), selectedPath!!)

        class MyListener : MouseInputAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                val mouseVec = Vector2(e!!.x.toDouble(), e.y.toDouble())
                var shortestDistance = 10000.0
                var closestPoint: Path2DPoint? = null

                //Find closest point
                var point: Path2DPoint? = selectedPath.xyCurve.headPoint
                while (point != null) {
                    val tPoint = world2Screen(point.position)
                    var dist = Vector2.length(Vector2.subtract(tPoint, mouseVec))
                    if (dist <= shortestDistance) {
                        shortestDistance = dist
                        closestPoint = point
                        pointType = PointType.POINT
                    }

                    if (point.prevPoint != null) {
                        val tanPoint1 = world2Screen(Vector2.subtract(point.position, Vector2.multiply(point.prevTangent, 1.0 / tangentLengthDrawFactor)))
                        dist = Vector2.length(Vector2.subtract(tanPoint1, mouseVec))
                        if (dist <= shortestDistance) {
                            shortestDistance = dist
                            closestPoint = point
                            pointType = PointType.PREV_TANGENT
                        }
                    }

                    if (point.nextPoint != null) {
                        val tanPoint2 = world2Screen(Vector2.add(point.position, Vector2.multiply(point.nextTangent, 1.0 / tangentLengthDrawFactor)))
                        dist = Vector2.length(Vector2.subtract(tanPoint2, mouseVec))
                        if (dist <= shortestDistance) {
                            shortestDistance = dist
                            closestPoint = point
                            pointType = PointType.NEXT_TANGENT
                        }
                    }
                    point = point.nextPoint
                    // find distance between point clicked and each point in the graph. Whichever one is the max gets to be assigned to the var.
                }
                if (shortestDistance <= circleSize / 2) {
                    selectedPoint = closestPoint
                } else {
                    if (closestPoint!=null) {
                        if (shortestDistance>50) // trying to deselect
                            selectedPoint = null
                        else
                            selectedPoint = selectedPath.addVector2After(screen2World(mouseVec), closestPoint)
                    }
                    else {
                        selectedPoint = selectedPath.addVector2(screen2World(mouseVec))
                    }
                }
                editPoint = selectedPoint
                repaint()
            }

            override fun mouseDragged(e: MouseEvent?) {
                if (editPoint != null) {
                    val worldPoint = screen2World(Vector2(e!!.x.toDouble(), e.y.toDouble()))
                    when (pointType) {
                        PathVisualizer.PointType.POINT -> editPoint?.position = worldPoint
                        PathVisualizer.PointType.PREV_TANGENT -> editPoint!!.prevTangent = Vector2.multiply(Vector2.subtract(worldPoint, editPoint!!.position), -tangentLengthDrawFactor)
                        PathVisualizer.PointType.NEXT_TANGENT -> editPoint!!.nextTangent = Vector2.multiply(Vector2.subtract(worldPoint, editPoint!!.position), tangentLengthDrawFactor)
                    }
                    repaint()
                }
            }

            override fun mouseReleased(e: MouseEvent?) {
                editPoint = null  // no longer editing
            }
        }

        val myListener = MyListener()
        addMouseListener(myListener)
        addMouseMotionListener(myListener)

        try {
            val classLoader = javaClass.classLoader
            val blueSideFile = File(classLoader.getResource("assets/HalfFieldDiagramBlue.png")!!.file)
            val redSideFile = File(classLoader.getResource("assets/HalfFieldDiagramRed.png")!!.file)
            blueSideImage = ImageIO.read(blueSideFile)
            redSideImage = ImageIO.read(redSideFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val toolBarPanel = JPanel(GridLayout(1, 7))
        val comboBoxNames = arrayOf("Blue Side", "Red Side")
        sideSelection = JComboBox(comboBoxNames)
        sideSelection.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                if (sideSelection.selectedIndex == 0) {
                    sides = Sides.BLUE
                } else if (sideSelection.selectedIndex == 1) {
                    sides = Sides.RED
                }
                repaint()
            }
        }

        // zoom text
        zoomTextField = JTextField(java.lang.Double.toString(zoom))
        zoomTextField.isEditable = true
        zoomTextField.addActionListener { e ->
            try {
                zoom = java.lang.Double.parseDouble(e.actionCommand)
                repaint()
            } catch (exception: NumberFormatException) {
                JOptionPane.showMessageDialog(this@PathVisualizer,
                        "The P.M.P. Section III Act 111.16.11, which you have violated, dictates that you must send one" +
                                " million dollars to the Prince of Nigeria or a jail sentence of 20 years of for-profit prison" +
                                " will be imposed.", "Police Alert", JOptionPane.ERROR_MESSAGE)
            }
        }

        // delete point button
        val deleteButton = JButton("Delete Point")
        deleteButton.addActionListener {
            if (selectedPoint != null && selectedPath != null) {
                selectedPath.removePoint(selectedPoint)
                selectedPoint = null
                repaint()
            }
        }

        // zoom out button
        val decrementButton = JButton("-")
        decrementButton.addActionListener {
            zoom--
            repaint()
            zoomTextField.text = java.lang.Double.toString(zoom)
        }

        // zoom in button
        val incrementButton = JButton("+")
        incrementButton.addActionListener {
            zoom++
            repaint()
            zoomTextField.text = java.lang.Double.toString(zoom)
        }

        // time in seconds
        timeTextField = JTextField(java.lang.Double.toString(timeInSeconds))
        zoomTextField.isEditable = true
        zoomTextField.addActionListener { e ->
            try {
                timeInSeconds = java.lang.Double.parseDouble(e.actionCommand)
                repaint()
            } catch (exception: NumberFormatException) {
                JOptionPane.showMessageDialog(this@PathVisualizer,
                        "The P.M.P. Section III Act 111.16.11, which you have violated, dictates that you must send one" +
                                " million dollars to the Prince of Nigeria or a jail sentence of 20 years of for-profit prison" +
                                " will be imposed.", "Police Alert", JOptionPane.ERROR_MESSAGE)
            }
        }

        // Autonomi (or autonopodes?)
        val autonomousNames: Array<String?>
        val numConfigs = SharedAutonomousConfig.configNames.size
        autonomousNames = arrayOfNulls<String>(numConfigs + 1)
        var currentIndex = -1
        for (i in 0..numConfigs - 1) {
            autonomousNames[i] = SharedAutonomousConfig.configNames[i]
            if (SharedAutonomousConfig(autonomousNames[i]!!)==selectedAutonomousConfig)
                currentIndex = i
        }
        autonomousNames[numConfigs] = "New Auto"
        autoSelection = JComboBox<String>(autonomousNames)
        autoSelection.selectedIndex = currentIndex
        autoSelection.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                if (autoSelection.selectedIndex == autoSelection.itemCount - 1) {
                    val s = JOptionPane.showInputDialog(
                            null,
                            "Autonomous Name:",
                            "New Autonomous",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            "") as String

                    if (s.length > 0) {
                        selectedAutonomousConfig = SharedAutonomousConfig(s)
                        autoSelection.insertItemAt(s, autoSelection.itemCount - 1)
                        autoSelection.selectedIndex = autoSelection.itemCount - 2
                    }
                } else {  // any autonomous chosen
                    selectedAutonomousConfig = SharedAutonomousConfig(autoSelection.selectedItem.toString())
                }
                repaint()
            }
        }

        // paths
        val pathSelectionNames: Array<String?>
        val numPaths = if (selectedAutonomousConfig != null) selectedAutonomousConfig!!.pathNames.size else 0
        pathSelectionNames = arrayOfNulls<String>(numPaths + 1)
        for (i in 0..numPaths - 1) {
            pathSelectionNames[i] = selectedAutonomousConfig!!.pathNames[i]
        }
        pathSelectionNames[numPaths] = "New Path"
        pathSelection = JComboBox<String>(pathSelectionNames)
        pathSelection.selectedIndex = -1
        pathSelection.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                if (pathSelection.selectedIndex == pathSelection.itemCount - 1) {
                    val s = JOptionPane.showInputDialog(
                            null,
                            "Path Name:",
                            "New Path",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            "") as String
                    if (s.isNotEmpty()) {
                        pathSelection.insertItemAt(s, pathSelection.itemCount - 1)
                        pathSelection.selectedIndex = pathSelection.itemCount - 2
                        selectedAutonomousConfig!!.putPath(s, Path2D())
                    }
                }   // any path chosen
                selectedPath = selectedAutonomousConfig.getPath(pathSelection.selectedItem.toString()) ?: selectedPath
                repaint()
            }
        }

        // add the tool bar items
        toolBarPanel.add(autoSelection)
        toolBarPanel.add(pathSelection)
        toolBarPanel.add(timeTextField)
        toolBarPanel.add(decrementButton)
        toolBarPanel.add(zoomTextField)
        toolBarPanel.add(incrementButton)
        toolBarPanel.add(sideSelection)
        toolBarPanel.add(deleteButton)

        add(toolBarPanel, BorderLayout.NORTH)
    }

    public override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        super.paintComponent(g2)

        if (sides == Sides.BLUE) {
            g2.drawImage(blueSideImage, 670 - ((zoom - 18) / 36 * blueSideImage!!.width).toInt(),
                    165 - ((zoom - 18) / 30 * (blueSideImage!!.height - 29)).toInt(),
                    blueSideImage!!.width + ((zoom - 18) / 18 * blueSideImage!!.width).toInt(),
                    ((blueSideImage!!.height + 29) * zoom / 18).toInt(), null)
        } else if (sides == Sides.RED) {
            g2.drawImage(redSideImage, 670 - ((zoom - 18) / 36 * redSideImage!!.width).toInt(),
                    165 - ((zoom - 18) / 30 * (redSideImage!!.height - 29)).toInt(),
                    redSideImage!!.width + ((zoom - 18) / 18 * redSideImage!!.width).toInt(),
                    ((redSideImage!!.height + 29) * zoom / 18).toInt(), null)
        }

        if (selectedAutonomousConfig != null) {
            val numPaths = selectedAutonomousConfig!!.pathNames.size
            for (i in 0..numPaths - 1) {
                val path2D = selectedAutonomousConfig!!.getPath(selectedAutonomousConfig!!.pathNames[i])
                DrawPath(g2, path2D)
            }
        }
        DrawSelectedPath(g2, selectedPath)
    }


    private fun DrawSelectedPath(g2: Graphics2D, path2D: Path2D?) {
        if (path2D==null || !path2D.hasPoints())
            return
        if (path2D.duration>0.0) {
            val deltaT = path2D.duration / 100.0
            var prevPos = path2D.getPosition(0.0)
            var prevLeftPos = path2D.getLeftPosition(0.0)
            var prevRightPos = path2D.getRightPosition(0.0)
            var pos: Vector2
            var leftPos: Vector2
            var rightPos: Vector2
            val MAX_SPEED = 8.0
            var t = deltaT
            while (t <= path2D.duration) {
                pos = path2D.getPosition(t)
                leftPos = path2D.getLeftPosition(t)
                rightPos = path2D.getRightPosition(t)

                // center line
                g2.color = Color.white
                drawPathLine(g2, prevPos, pos)

                // left wheel
                var leftSpeed = Vector2.length(Vector2.subtract(leftPos, prevLeftPos)) / deltaT
                leftSpeed /= MAX_SPEED  // MAX_SPEED is full green, 0 is full red.
                leftSpeed = Math.min(1.0, leftSpeed)
                val leftDelta = path2D.getLeftPositionDelta(t)
                if (leftDelta > 0)
                    g2.color = Color(((1.0 - leftSpeed) * 255).toInt(), (leftSpeed * 255).toInt(), 0)
                else {
                    g2.color = Color(0, 0, 255) //(int)blue));
                }
                drawPathLine(g2, prevLeftPos, leftPos)

                // right wheel
                var rightSpeed = Vector2.length(Vector2.subtract(rightPos, prevRightPos)) / deltaT / MAX_SPEED
                rightSpeed = Math.min(1.0, rightSpeed)
                val rightDelta = path2D.getRightPositionDelta(t)
                if (rightDelta > 0)
                    g2.color = Color(((1.0 - rightSpeed) * 255).toInt(), (rightSpeed * 255).toInt(), 0)
                else {
                    g2.color = Color(0, 0, 255) //(int)blue));
                }
                drawPathLine(g2, prevRightPos, rightPos)

                // set the prevs for the next loop
                prevPos.set(pos.x, pos.y)
                prevLeftPos.set(leftPos.x, leftPos.y)
                prevRightPos.set(rightPos.x, rightPos.y)
                t += deltaT
            }
        }

        // circles and lines for handles
        var point: Path2DPoint? = path2D.xyCurve.headPoint
        while (point != null) {
            if (point === selectedPoint && pointType == PointType.POINT)
                g2.color = Color.green
            else
                g2.color = Color.white

            val tPoint = world2Screen(point.position)
            g2.drawOval(tPoint.x.toInt() - circleSize / 2, tPoint.y.toInt() - circleSize / 2, circleSize, circleSize)
            if (point.prevPoint != null) {
                if (point === selectedPoint && pointType == PointType.PREV_TANGENT)
                    g2.color = Color.green
                else
                    g2.color = Color.white
                val tanPoint = world2Screen(Vector2.subtract(point.position, Vector2.multiply(point.prevTangent, 1.0 / tangentLengthDrawFactor)))
                g2.drawOval(tanPoint.x.toInt() - circleSize / 2, tanPoint.y.toInt() - circleSize / 2, circleSize, circleSize)
                g2.stroke = BasicStroke(2f)
                g2.drawLine(tPoint.x.toInt(), tPoint.y.toInt(), tanPoint.x.toInt(), tanPoint.y.toInt())
            }
            if (point.nextPoint != null) {
                if (point === selectedPoint && pointType == PointType.NEXT_TANGENT)
                    g2.color = Color.green
                else
                    g2.color = Color.white
                val tanPoint = world2Screen(Vector2.add(point.position, Vector2.multiply(point.nextTangent, 1.0 / tangentLengthDrawFactor)))
                g2.drawOval(tanPoint.x.toInt() - circleSize / 2, tanPoint.y.toInt() - circleSize / 2, circleSize, circleSize)
                g2.stroke = BasicStroke(2f)
                g2.drawLine(tPoint.x.toInt(), tPoint.y.toInt(), tanPoint.x.toInt(), tanPoint.y.toInt())
            }
            point = point.nextPoint
        }
        // draw the ease curve  // be nice to draw this beneath the map
        //    double prevEase = 0.0;
        //    g2.setStroke(new BasicStroke(3));
        //    for (double t = deltaT; t <= path2D.getDuration(); t += deltaT) {
        //      // draw the ease curve too
        //      g2.setColor(Color.black);
        //      double ease = path2D.getEaseCurve().getValue(t);
        //      double prevT = t - deltaT;
        //      g2.drawLine((int) (prevT * 40 + 100), (int) (prevEase * -200 + 700), (int) (t * 40 + 100), (int) (ease * -200 + 700));
        //      prevEase = ease;
        //    }
    }

    private fun DrawPath(g2: Graphics2D, path2D: Path2D?) {
        if (path2D==null)
            return
        val deltaT = path2D.duration / 100.0
        val prevPos = path2D.getPosition(0.0)
        var pos: Vector2

        g2.color = Color.white
        var t = deltaT
        while (t <= path2D.duration) {
            pos = path2D.getPosition(t)

            // center line
            drawPathLine(g2, prevPos, pos)
            prevPos.set(pos.x, pos.y)
            t += deltaT
        }
    }

    private fun drawPathLine(g2: Graphics2D, p1: Vector2, p2: Vector2) {

        val tp1 = world2Screen(p1)
        val tp2 = world2Screen(p2)

        g2.drawLine(tp1.x.toInt(), tp1.y.toInt(), tp2.x.toInt(), tp2.y.toInt())
    }

    private fun screen2World(point: Vector2): Vector2 {

        var xFlip = 1.0
        if (sides == Sides.RED) {
            xFlip = -1.0
        }
        val result = Vector2((point.x - offset.x) / xFlip / zoom, (point.y - offset.y) / -zoom)
        return result
    }

    private fun world2Screen(point: Vector2): Vector2 {

        var xFlip = 1.0
        if (sides == Sides.RED) {
            xFlip = -1.0
        }
        val result = Vector2(point.x * xFlip * zoom + offset.x, point.y * -zoom + offset.y)
        return result
    }
}



