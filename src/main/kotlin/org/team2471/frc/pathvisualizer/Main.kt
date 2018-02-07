import edu.wpi.first.networktables.NetworkTable
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import org.team2471.frc.lib.vector.Vector2
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.text.Text
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.pathvisualizer.DefaultPath
import kotlin.math.round
import javafx.scene.control.SplitPane
import javafx.scene.layout.StackPane
import javafx.scene.control.TextInputDialog
import javafx.scene.control.CheckBox
import java.text.DecimalFormat
import javafx.stage.FileChooser
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Autonomous
import java.io.File
import java.io.PrintWriter

// todo: Autonomous - class to hold multiple paths /////////////////////////////////////////////////////////////////////

class Autonomous( var name: String ) {
    var paths: MutableMap<String, Path2D> = mutableMapOf()

    fun putPath( name: String, path2D: Path2D) {
        paths.put(name, path2D)
    }

    fun getPath( name: String ) : Path2D? {
        return paths.get(name)
    }
}


// todo: main application class ////////////////////////////////////////////////////////////////////////////////////////

class PathVisualizer : Application() {

    // todo: the companion object which starts this class //////////////////////////////////////////////////////////////
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(PathVisualizer::class.java, *args)
        }
    }

    // todo: class state - vars and vals ///////////////////////////////////////////////////////////////////////////////
    // javaFX state which needs saved around
    private val canvas = ResizableCanvas(this)
    private val image = Image("assets/2018Field.PNG")
    private var stage: Stage? = null
    private var fileName = String()

    // class state variables

    private var autonomi = Autonomi()
    var selectedAutonomous: Autonomous? = null
    private var selectedPath: Path2D? = null

    // image stuff - measure your image with paint and enter these first 3
    private val upperLeftOfFieldPixels = Vector2(39.0, 58.0)
    private val lowerRightOfFieldPixels = Vector2(624.0, 701.0)
    private val zoomPivot = Vector2(366.0, 701.0)  // the location in the image where the zoom origin will originate
    private val fieldDimensionPixels = lowerRightOfFieldPixels - upperLeftOfFieldPixels
    private val fieldDimensionFeet = Vector2(27.0, 27.0)

    // view settings
    private var zoom: Double = round(feetToPixels(1.0))  // initially draw at 1:1
    var offset: Vector2 = Vector2(0.0, 0.0)

    // location of image extremes in world units
    private val upperLeftFeet = screen2World(Vector2(0.0, 0.0))  // calculate these when zoom is 1:1, and offset is 0,0
    private val lowerRightFeet = screen2World(Vector2(image.width, image.height))

    // drawing
    private val circleSize = 10.0
    private val tangentLengthDrawFactor = 3.0

    // point editing
    private var editPoint: Path2DPoint? = null
    private var selectedPoint: Path2DPoint? = null

    // custom types
    private enum class PointType {
        NONE, POINT, PREV_TANGENT, NEXT_TANGENT
    }

    private var pointType = PointType.NONE

    private enum class MouseMode {
        EDIT, PAN
    }

    private var mouseMode = MouseMode.EDIT

// todo: helper functions //////////////////////////////////////////////////////////////////////////////////////////////

    fun feetToPixels(feet: Double): Double = feet * fieldDimensionPixels.x / fieldDimensionFeet.x

    inline fun <T:Any, R> whenNotNull(input: T?, callback: (T)->R): R? {
        return input?.let(callback)
    }

    private fun world2Screen(vector2: Vector2): Vector2 {
        val temp = vector2 * zoom
        temp.y = -temp.y
        return temp + zoomPivot + offset
    }

    private fun screen2World(vector2: Vector2): Vector2 {
        val temp = vector2 - offset - zoomPivot
        temp.y = -temp.y
        return temp / zoom
    }

    fun Double.format(fracDigits: Int): String {
        val fd = DecimalFormat()
        fd.maximumFractionDigits = fracDigits
        fd.minimumFractionDigits = fracDigits
        return fd.format(this)
    }

// todo: start /////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun start(stage: Stage) {
        stage.title = "Path Visualizer"
        this.stage = stage

        // set up the paths and autos
        selectedAutonomous = Autonomous("Auto1")
        autonomi.put(selectedAutonomous!!)
        selectedPath = DefaultPath
        selectedAutonomous!!.putPath(selectedPath!!)

        // setup the layout
        val buttonsBox = VBox()
        buttonsBox.spacing = 10.0
        buttonsBox.padding = Insets(10.0, 10.0, 10.0, 10.0)
        addControlsToButtonsBox(buttonsBox)

        val stackPane = StackPane(canvas)
        val splitPane = SplitPane(stackPane, buttonsBox)
        splitPane.setDividerPositions(0.7)

        stage.scene = Scene(splitPane, 1600.0, 900.0)
        stage.sizeToScene()

        //val easeCanvas = Canvas()
        //anchorPane.bottomAnchor = easeCanvas

        repaint()
        stage.show()

        canvas.onMousePressed = EventHandler<MouseEvent> { onMousePressed(it) }
        canvas.onMouseDragged = EventHandler<MouseEvent> { onMouseDragged(it) }
        canvas.onMouseReleased = EventHandler<MouseEvent> { onMouseReleased() }
    }

// todo: javaFX UI controls //////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun addControlsToButtonsBox(buttonsBox: VBox) {

        // path combo box
        val pathComboHBox = HBox()
        val pathComboName = Text("Path:  ")
        val pathComboBox = ComboBox<String>()
        refreshPathCombo(pathComboBox)
        pathComboBox.valueProperty().addListener({_, _, newText ->
            var newPathName = newText
            if (newPathName=="New Path") {
                var defaultName = "Path"
                var count = 1
                while (selectedAutonomous!!.paths.containsKey(defaultName+count))
                    count++
                val dialog = TextInputDialog(defaultName+count)
                dialog.title = "Path Name"
                dialog.headerText = "Enter the name for your new path"
                dialog.contentText = "Path name:"
                val result = dialog.showAndWait()
                if (result.isPresent) {
                    newPathName = result.get()
                    val newPath = Path2D(newPathName)
                    newPath.addEasePoint(0.0, 0.0); newPath.addEasePoint(5.0,1.0); // always begin with an ease curve
                    selectedAutonomous!!.putPath(newPath)
                    pathComboBox.items.add(pathComboBox.items.count()-1, newPathName)
                }
                else {
                    newPathName = selectedPath?.name
                }
            }
            if (selectedAutonomous!=null) {
                selectedPath = selectedAutonomous!!.getPath(newPathName)
            }
            pathComboBox.value = newPathName
            selectedPoint = null
            repaint()
        })
        pathComboHBox.children.addAll(pathComboName, pathComboBox)

        // autonomous combo box
        val autoComboHBox = HBox()
        val autoComboName = Text("Auto:  ")
        val autoComboBox = ComboBox<String>()
        refreshAutoCombo(autoComboBox)
        autoComboBox.valueProperty().addListener({_, _, newText ->
            var newAutoName = newText
            if (newAutoName=="New Auto") {
                var defaultName = "Auto"
                var count = 1
                while (autonomi.mapAutonomous.containsKey(defaultName+count))
                    count++
                val dialog = TextInputDialog(defaultName+count)
                dialog.title = "Auto Name"
                dialog.headerText = "Enter the name for your new autonomous"
                dialog.contentText = "Auto name:"
                val result = dialog.showAndWait()
                if (result.isPresent) {
                    newAutoName = result.get()
                    val newAuto = Autonomous(newAutoName)
                    autonomi.put(newAuto)
                    autoComboBox.items.add(autoComboBox.items.count()-1, newAutoName)
                }
                else {
                    newAutoName = selectedAutonomous?.name
                }
            }
            selectedAutonomous = autonomi.get(newAutoName)
            autoComboBox.value = newAutoName
            selectedPath = null
            selectedPoint = null
            refreshPathCombo(pathComboBox)
            repaint()
        })
        autoComboHBox.children.addAll(autoComboName, autoComboBox)

        val zoomHBox = HBox()
        val zoomName = Text("Zoom  ")
        val zoomAdjust = TextField(zoom.toString())
        zoomAdjust.textProperty().addListener({ _, _, newText ->
            zoom = newText.toDouble()
            repaint()
        })
        val zoomMinus = Button("-")
        zoomMinus.setOnAction { _: ActionEvent ->
            //set what you want the buttons to do here
            zoom-- // so the zoom code or calls to a zoom function or whatever go here
            zoomAdjust.text = zoom.toString()
            repaint()
        }
        val zoomPlus = Button("+")
        zoomPlus.setOnAction { _: ActionEvent ->
            // same as above
            zoom++
            zoomAdjust.text = zoom.toString()
            repaint()
        }
        zoomHBox.children.addAll(
                zoomName,
                zoomMinus,
                zoomAdjust,
                zoomPlus)

        val panHBox = HBox()
        val panName = Text("Pan  ")
        val panXName = Text("X = ")
        val panYName = Text("Y = ")
        val panXAdjust = TextField(offset.x.toString())
        panXAdjust.textProperty().addListener({ _, _, newText ->
            offset.x = newText.toDouble()
            repaint()
        })
        val panYAdjust = TextField(offset.y.toString())
        panYAdjust.textProperty().addListener({ _, _, newText ->
            offset.y = newText.toDouble()
            repaint()
        })
        panHBox.children.addAll(
                panName,
                panXName,
                panXAdjust,
                panYName,
                panYAdjust)

        val deletePoint = Button("Delete Point")
        deletePoint.setOnAction { _: ActionEvent ->
            if (selectedPoint != null && selectedPath != null) {
                selectedPath?.removePoint(selectedPoint)
                selectedPoint = null
                repaint()
            }
        }

        val mirroredCheckBox = CheckBox("Mirrored")
        mirroredCheckBox.isSelected = selectedPath!!.isMirrored

        val robotDirectionHBox = HBox()
        val robotDirectionName = Text("Robot Direction:  ")
        val robotDirectionBox = ComboBox<String>()
        robotDirectionBox.items.add("Forward")
        robotDirectionBox.items.add("Backward")
        robotDirectionBox.value = if (selectedPath!!.travelDirection>0) "Forward" else "Backward"
        robotDirectionBox.valueProperty().addListener({ _, _, newText ->
            selectedPath?.travelDirection = if (newText=="Forward") 1.0 else -1.0
        })
        robotDirectionHBox.children.addAll(robotDirectionName, robotDirectionBox)

        val speedHBox = HBox()
        val speedName = Text("Speed:  ")
        val speedText = TextField(selectedPath?.speed.toString())
        speedText.textProperty().addListener ({ _, _, newText ->
            selectedPath?.speed = newText.toDouble()
            repaint()
        })
        speedHBox.children.addAll(speedName, speedText)

        val widthHBox = HBox()
        val widthName = Text("Width:  ")
        //this might perpetually throw an exception at every moment there isn't a path
        // todo: experiment with this and change accordingly
        val widthText = TextField((selectedPath!!.robotWidth * 12.0).format(1))
        widthText.textProperty().addListener({ _, _, newText ->
            selectedPath?.robotWidth = (newText.toDouble()) / 12.0
            //widthText.text = (selectedPath!!.robotWidth * 12.0).format(1)
            repaint()
        })
        val widthUnit = Text(" inches")
        widthHBox.children.addAll(widthName, widthText, widthUnit)

        val lengthHBox = HBox()
        val lengthName = Text("Length:  ")
        val lengthText = TextField((selectedPath!!.robotLength * 12.0).format(1))
        lengthText.textProperty().addListener({ _, _, newText ->
            selectedPath?.robotLength = newText.toDouble()
            //lengthText.text = (selectedPath!!.robotLength * 12.0).format(1)
            repaint()
        })
        val lengthUnit = Text("inches")
        lengthHBox.children.addAll(lengthName, lengthText, lengthUnit)

        val widthFudgeFactorHBox = HBox()
        val widthFudgeFactorName = Text("Width Fudge Factor:  ")
        val widthFudgeFactorText = TextField(selectedPath?.widthFudgeFactor.toString())
        widthFudgeFactorText.textProperty().addListener({ _, _, newText ->
            selectedPath?.widthFudgeFactor = newText.toDouble()
            repaint()
        })
        widthFudgeFactorHBox.children.addAll(widthFudgeFactorName, widthFudgeFactorText)

        // todo: edit boxes for position and tangents of selected point

        val filesBox = HBox()
        filesBox.spacing = 10.0
        val saveAsButton = Button("Save As")
        saveAsButton.setOnAction { _: ActionEvent ->
            val fileChooser = FileChooser()
            fileChooser.setTitle("Save Autonomi File As...")
            val extFilter = FileChooser.ExtensionFilter("Autonomi files (*.json)", "*.json")
            fileChooser.extensionFilters.add(extFilter)
            val file = fileChooser.showSaveDialog(stage)
            if (file!=null) {
                fileName = file.name
                val json = autonomi.toJsonString()
                val writer = PrintWriter(file)
                writer.append(json)
                writer.close()
            }
        }
        val saveButton = Button("Save")
        saveButton.setOnAction { _: ActionEvent ->
            if (!fileName.isEmpty()) {
                val file = File(fileName)
                val json = autonomi.toJsonString()
                val writer = PrintWriter(file)
                writer.append(json)
                writer.close()
            }
        }
        val openButton = Button("Open")
        openButton.setOnAction { _: ActionEvent ->
            val fileChooser = FileChooser()
            fileChooser.setTitle("Open Autonomi File...")
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Autonomi files (*.json)", "*.json"))
            val file = fileChooser.showOpenDialog(stage)
            if (file != null) {
                var json: String = file.readText()
                autonomi = Autonomi.fromJsonString(json)
                refreshEverything(autoComboBox, pathComboBox)
            }
        }
        filesBox.children.addAll(saveAsButton, saveButton, openButton)

        val robotHBox = HBox()
        val sendToRobotButton = Button("Send To Robot")
        sendToRobotButton.setOnAction { _: ActionEvent ->
            autonomi.publishToNetworkTables()
        }
        val addressName = Text("  IP Address:  ")
        val addressText = TextField("10.24.71.100")
        addressText.textProperty().addListener({ _, _, newText ->
//            autonomi.IPAddress = newText
        })
        robotHBox.children.addAll(sendToRobotButton, addressName, addressText)

        buttonsBox.children.addAll(
                zoomHBox,
                panHBox,
                autoComboHBox,
                pathComboHBox,
                deletePoint,
                mirroredCheckBox,
                speedHBox,
                robotDirectionHBox,
                widthHBox,
                lengthHBox,
                widthFudgeFactorHBox,
                filesBox,
                robotHBox
                )
    }

    // todo: UI helper functions //////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun refreshAutoCombo(autoComboBox: ComboBox<String>) {
        autoComboBox.items.clear()
        for (kvAuto in autonomi.mapAutonomous) {
            autoComboBox.items.add(kvAuto.key)
            if (kvAuto.value == selectedAutonomous) {
                autoComboBox.value = kvAuto.key
            }
        }
        autoComboBox.items.add("New Auto")
        if (selectedAutonomous==null) {
            selectedAutonomous = autonomi.mapAutonomous.values.firstOrNull()
            autoComboBox.value = selectedAutonomous?.name
        }
    }

    private fun refreshPathCombo(pathComobBox: ComboBox<String>) {
        pathComobBox.items.clear()
        if (selectedAutonomous!=null) {
            val paths = selectedAutonomous!!.paths
            for (kvPath in paths) {
                pathComobBox.items.add(kvPath.key)
                if (kvPath.value == selectedPath) {
                    pathComobBox.value = kvPath.key
                }
            }
            pathComobBox.items.add("New Path")
            if (selectedPath==null) {
                selectedPath = paths.values.firstOrNull()
                pathComobBox.value = selectedPath?.name
            }
        }
    }

    private fun refreshEverything(autoComboBox: ComboBox<String>, pathComboBox: ComboBox<String>) {
        refreshAutoCombo(autoComboBox)
        refreshPathCombo(pathComboBox)
    }

// todo: draw functions ////////////////////////////////////////////////////////////////////////////////////////////////

    fun repaint() {
        val gc = canvas.graphicsContext2D
        gc.fill = Color.LIGHTGRAY
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)

        // calculate ImageView corners
        val upperLeftPixels = world2Screen(upperLeftFeet)
        val lowerRightPixels = world2Screen(lowerRightFeet)
        val dimensions = lowerRightPixels - upperLeftPixels
        gc.drawImage(image, 0.0, 0.0, image.width, image.height, upperLeftPixels.x, upperLeftPixels.y, dimensions.x, dimensions.y)

        drawPaths(gc)
    }

    private fun drawPaths(gc: GraphicsContext) {
        if (selectedAutonomous != null) {
            for (path2D in selectedAutonomous!!.paths) {
                drawPath(gc, path2D.value)
            }
        }
        drawSelectedPath(gc, selectedPath)
    }

    private fun drawPath(gc: GraphicsContext, path2D: Path2D?) {
        if (path2D == null || path2D.duration==0.0)
            return
        val deltaT = path2D.duration / 100.0
        val prevPos = path2D.getPosition(0.0)
        var pos: Vector2

        gc.stroke = Color.WHITE
        var t = deltaT
        while (t <= path2D.duration) {
            pos = path2D.getPosition(t)

            // center line
            drawPathLine(gc, prevPos, pos)
            prevPos.set(pos.x, pos.y)
            t += deltaT
        }
    }

    private fun drawPathLine(gc: GraphicsContext, p1: Vector2, p2: Vector2) {
        val tp1 = world2Screen(p1)
        val tp2 = world2Screen(p2)
        gc.strokeLine(tp1.x, tp1.y, tp2.x, tp2.y)
    }

    private fun drawSelectedPath(gc: GraphicsContext, path2D: Path2D?) {
        if (path2D == null || !path2D.hasPoints())
            return
        if (path2D.duration > 0.0) {
            val deltaT = path2D.duration / 300.0
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
                gc.stroke = Color.WHITE
                drawPathLine(gc, prevPos, pos)

                // left wheel
                var leftSpeed = Vector2.length(Vector2.subtract(leftPos, prevLeftPos)) / deltaT
                leftSpeed /= MAX_SPEED  // MAX_SPEED is full GREEN, 0 is full red.
                leftSpeed = Math.min(1.0, leftSpeed)
                val leftDelta = path2D.getLeftPositionDelta(t)
                if (leftDelta > 0)
                    gc.stroke = Color(1.0 - leftSpeed, leftSpeed, 0.0, 1.0)
                else {
                    gc.stroke = Color.BLUE
                }
                drawPathLine(gc, prevLeftPos, leftPos)

                // right wheel
                var rightSpeed = Vector2.length(Vector2.subtract(rightPos, prevRightPos)) / deltaT / MAX_SPEED
                rightSpeed = Math.min(1.0, rightSpeed)
                val rightDelta = path2D.getRightPositionDelta(t)
                if (rightDelta > 0)
                    gc.stroke = Color(1.0 - rightSpeed, rightSpeed, 0.0, 1.0)
                else {
                    gc.stroke = Color.BLUE
                }
                drawPathLine(gc, prevRightPos, rightPos)

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
                gc.stroke = Color.LIMEGREEN
            else
                gc.stroke = Color.WHITE

            val tPoint = world2Screen(point.position)
            gc.strokeOval(tPoint.x - circleSize / 2, tPoint.y - circleSize / 2, circleSize, circleSize)
            if (point.prevPoint != null) {
                if (point === selectedPoint && pointType == PointType.PREV_TANGENT)
                    gc.stroke = Color.LIMEGREEN
                else
                    gc.stroke = Color.WHITE
                val tanPoint = world2Screen(Vector2.subtract(point.position, Vector2.multiply(point.prevTangent, 1.0 / tangentLengthDrawFactor)))
                gc.strokeOval(tanPoint.x - circleSize / 2, tanPoint.y - circleSize / 2, circleSize, circleSize)
                gc.lineWidth = 2.0
                gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
            }
            if (point.nextPoint != null) {
                if (point === selectedPoint && pointType == PointType.NEXT_TANGENT)
                    gc.stroke = Color.LIMEGREEN
                else
                    gc.stroke = Color.WHITE
                val tanPoint = world2Screen(Vector2.add(point.position, Vector2.multiply(point.nextTangent, 1.0 / tangentLengthDrawFactor)))
                gc.strokeOval(tanPoint.x - circleSize / 2, tanPoint.y - circleSize / 2, circleSize, circleSize)
                gc.lineWidth = 2.0
                gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
            }
            point = point.nextPoint
        }
    }

    fun drawEaseCurve() {
        // draw the ease curve  // be nice to draw this beneath the map
        //    double prevEase = 0.0;
        //    gc.setStroke(new BasicStroke(3));
        //    for (double t = deltaT; t <= path2D.getDuration(); t += deltaT) {
        //      // draw the ease curve too
        //      gc.setColor(Color.black);
        //      double ease = path2D.getEaseCurve().getValue(t);
        //      double prevT = t - deltaT;
        //      gc.drawLine((int) (prevT * 40 + 100), (int) (prevEase * -200 + 700), (int) (t * 40 + 100), (int) (ease * -200 + 700));
        //      prevEase = ease;
        //    }
    }

    var startMouse = Vector2(0.0, 0.0)

// todo: mouse functions ///////////////////////////////////////////////////////////////////////////////////////////////

    fun onMousePressed(e: MouseEvent) {
        val mouseVec = Vector2(e.x, e.y)
        startMouse = mouseVec

        var shortestDistance = 10000.0
        var closestPoint: Path2DPoint? = null

        //Find closest point
        var point: Path2DPoint? = selectedPath?.xyCurve?.headPoint
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
            if (closestPoint != null) {
                if (shortestDistance > 50) // trying to deselect?
                    selectedPoint = null
                else
                    selectedPoint = selectedPath?.addVector2After(screen2World(mouseVec), closestPoint)
            } else {  // first point on a path?
//                val path2DPoint = selectedPath?.addVector2(screen2World(mouseVec)-Vector2(0.0,0.25)) // add a pair of points, initially on top of one another
//                selectedPoint = selectedPath?.addVector2After(screen2World(mouseVec), path2DPoint)
                selectedPath?.addVector2(screen2World(mouseVec))
            }
        }
        editPoint = selectedPoint
        repaint()
    }

    fun onMouseDragged(e: MouseEvent) {
        if (editPoint != null) {
            val worldPoint = screen2World(Vector2(e.x, e.y))
            when (pointType) {
                PathVisualizer.PointType.POINT -> editPoint?.position = worldPoint
                PathVisualizer.PointType.PREV_TANGENT -> editPoint!!.prevTangent = Vector2.multiply(Vector2.subtract(worldPoint, editPoint!!.position), -tangentLengthDrawFactor)
                PathVisualizer.PointType.NEXT_TANGENT -> editPoint!!.nextTangent = Vector2.multiply(Vector2.subtract(worldPoint, editPoint!!.position), tangentLengthDrawFactor)
                else -> {}
            }
            repaint()
        }
    }

    fun onMouseReleased() {
        editPoint = null  // no longer editing
    }
}

// todo: resizable canvas //////////////////////////////////////////////////////////////////////////////////////////////

class ResizableCanvas(pv: PathVisualizer) : Canvas() {

    private var pathVisualizer = pv

    override fun isResizable() = true

    override fun prefWidth(height: Double) = width

    override fun prefHeight(width: Double) = height

    override fun minHeight(width: Double): Double {
        return 64.0
    }

    override fun maxHeight(width: Double): Double {
        return 1000.0
    }

    override fun minWidth(height: Double): Double {
        return 0.0
    }

    override fun maxWidth(height: Double): Double {
        return 10000.0
    }

    override fun resize(_width: Double, _height: Double) {
        width = _width
        height = _height
        pathVisualizer.repaint()
    }
}

// todo list  //////////////////////////////////////////////////////////////////////////////////////////////////////

// : mouse routines - down, move, up
// : edit boxes respond - zoom, and pan
// : investigate why mirrored is not working
// : try layoutpanel for making buttons follow size of window on right - used splitpane and resizable
// : get path combo working
// : handle cancel on new dialogs
// : generate unique name for Auto and Path
// : new path draws blank
// : get autonomous combo working
// : delete point button
// : add path properties - ...
// : mirrored,
// : speed,
// : travel direction,
// : robot width, length, fudgefactor
// : change the displayed value of robot width and length to inch (it is currently in feet)
// : mirrored,
// : speed,
// : travel direction,
// : robot width, length, fudgefactor
// : convert robot width and length to inches - Duy
// : save to file, load from file
// : save to network tables for pathvisualizer
// : load from network tables for robot

// todo: draw ease curve in bottom panel, use another SplitPane horizontal
// todo: edit box for duration of path, place in bottom corner of ease canvas using StackPane

// todo: add rename button beside auto and path combos to edit their names -- Duy
// todo: add delete buttons beside auto and path for deleting them
// todo: add text box for team number or ip
// todo: change path combo to a list box
// todo: add edit box for coloring maximum speed
// todo: upres or repaint a new high res field image
// todo: clicking on path should select it
// todo: make a separate and larger radius for selecting points compared to drawing them
// todo: pan with mouse with a pan button or middle mouse button
// todo: zoom with the mouse wheel -- Julian
// todo: arrow keys to nudge selected path points
// todo: playback of robot travel - this should be broken into sub tasks
// todo: add partner1 and partner2 auto combos - draw cyan, magenta, yellow
// todo: editing of ease curve
// todo: multi-select path points by dragging selecting with dashed rectangle
// todo: draw ease curve in bottom panel, use another SplitPane horizontal
// todo: add pause and turn in place path types (actions)
// todo: decide what properties should be saved locally and save them to the registry or local folder
