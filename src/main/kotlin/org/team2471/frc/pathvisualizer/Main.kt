import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.event.ActionEvent
import javafx.event.EventHandler
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
import org.team2471.frc.pathvisualizer.TestMoshi
import java.text.DecimalFormat
import java.text.NumberFormat

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

    // class state variables

    private var mapAutonomous: MutableMap<String, Autonomous> = mutableMapOf()
    private var selectedAutonomous: Autonomous? = null
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

    val moshi = Moshi.Builder()
            // Add any other JsonAdapter factories.
            .add(KotlinJsonAdapterFactory())
            .build()

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

        // set up the paths and autos
        selectedAutonomous = Autonomous("Auto1")
        mapAutonomous.put(selectedAutonomous!!.name, selectedAutonomous!!)
        selectedPath = DefaultPath
        selectedAutonomous!!.putPath(selectedPath!!.name, selectedPath!!)

        // setup the layout
        val buttonsBox = VBox()
        buttonsBox.spacing = 10.0
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

        // test json writing
        TestMoshi()
    }

// todo: javaFX UI controls //////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun addControlsToButtonsBox(buttonsBox: VBox) {

        // path combo box
        val pathChooserHBox = HBox()
        val pathChooserName = Text("Path:  ")
        val pathChooserBox = ComboBox<String>()
        fillPathCombo(pathChooserBox)
        pathChooserBox.valueProperty().addListener({_, _, newText ->
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
                    selectedAutonomous!!.putPath(newPathName, newPath)
                    pathChooserBox.items.add(pathChooserBox.items.count()-1, newPathName)
                }
                else {
                    newPathName = selectedPath?.name
                }
            }
            selectedPath = selectedAutonomous!!.getPath(newPathName)
            pathChooserBox.value = newPathName
            selectedPoint = null
            repaint()
        })
        pathChooserHBox.children.addAll(pathChooserName, pathChooserBox)

        // autonomous combo box
        val autoChooserHBox = HBox()
        val autoChooserName = Text("Auto:  ")
        val autoChooserBox = ComboBox<String>()
        autoChooserBox.items.clear()
        for (kvAuto in mapAutonomous) {
            autoChooserBox.items.add(kvAuto.key)
            if (kvAuto.value == selectedAutonomous) {
                autoChooserBox.value = kvAuto.key
            }
        }
        autoChooserBox.items.add("New Auto")
        autoChooserBox.valueProperty().addListener({_, _, newText ->
            var newAutoName = newText
            if (newAutoName=="New Auto") {
                var defaultName = "Auto"
                var count = 1
                while (mapAutonomous.containsKey(defaultName+count))
                    count++
                val dialog = TextInputDialog(defaultName+count)
                dialog.title = "Auto Name"
                dialog.headerText = "Enter the name for your new autonomous"
                dialog.contentText = "Auto name:"
                val result = dialog.showAndWait()
                if (result.isPresent) {
                    newAutoName = result.get()
                    val newAuto = Autonomous(newAutoName)
                    mapAutonomous[newAutoName] = newAuto
                    autoChooserBox.items.add(autoChooserBox.items.count()-1, newAutoName)
                }
                else {
                    newAutoName = selectedAutonomous?.name
                }
            }
            selectedAutonomous = mapAutonomous[newAutoName]
            autoChooserBox.value = newAutoName
            selectedPath = null
            selectedPoint = null
            fillPathCombo(pathChooserBox)
            repaint()
        })
        autoChooserHBox.children.addAll(autoChooserName, autoChooserBox)

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
        val saveAsButton = Button("Save As")
        saveAsButton.setOnAction { _: ActionEvent ->
            val json = getAutonomiJson()
            println(json)
            // todo: bring up system save as dialog
        }
        // todo: also neeed buttons for save and open  // could put these in a file menu, or should they at least be at the top?
        // should display the name of this config?
        filesBox.children.addAll(saveAsButton)

// todo: save to network tables for pathvisualizer
        val sendToRobot = Button("Send To Robot")
        sendToRobot.setOnAction { _: ActionEvent ->
            //val json = getAutonomiJson()
        }

        buttonsBox.children.addAll(
                autoChooserHBox,
                pathChooserHBox,
                zoomHBox,
                panHBox,
                deletePoint,
                mirroredCheckBox,
                speedHBox,
                robotDirectionHBox,
                widthHBox,
                lengthHBox,
                widthFudgeFactorHBox,
                filesBox
                )
    }

    // todo: UI helper functions //////////////////////////////////////////////////////////////////////////////////////////////////////

    fun fillPathCombo(pathChooserBox: ComboBox<String>) {
        pathChooserBox.items.clear()
        if (selectedAutonomous!=null) {
            val paths = selectedAutonomous!!.paths
            for (kvPath in paths) {
                pathChooserBox.items.add(kvPath.key)
                if (kvPath.value == selectedPath) {
                    pathChooserBox.value = kvPath.key
                }
            }
            pathChooserBox.items.add("New Path")
        }
    }

    fun getAutonomiJson() : String {
        var json = String()
        for (kvAuto in mapAutonomous) {
            val auto = kvAuto.value
            for (kvPath in auto.paths) {
                val path = kvPath.value
                json += path.toJSonString()
            }
        }
        println(json)
        return json
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
//  : mirrored,
//  : speed,
//  : travel direction,
//  : robot width, length, fudgefactor
// : change the displayed value of robot width and length to inch (it is currently in feet)
// todo: round the length and width number to about 3 or 4 digits after the decimal point
// todo: edit boxes for position and tangents of selected point
// todo: edit box for duration of path
// todo: save to file, load from file
// todo: save to network tables for pathvisualizer
// todo: load from network tables for robot

// todo: add button beside auto and path combos to edit their names
// todo: upres or repaint a new high res field image
// todo: clicking on path should select it
// todo: make a separate and larger radius for selecting points compared to drawing them
// todo: pan with mouse with a pan button or middle mouse button
// todo: zoom with the mouse wheel
// todo: arrow keys to nudge selected path points
// todo: draw ease curve in bottom panel, use another SplitPane horizontal
// todo: playback of robot travel
// todo: add partner1 and partner2 auto combos - draw cyan, magenta, yellow?
// todo: editing of ease curve
// todo: multi-select path points by dragging selection dashed rectangle
