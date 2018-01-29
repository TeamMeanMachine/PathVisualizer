import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.event.ActionEvent
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import org.team2471.frc.lib.vector.Vector2
import javafx.scene.canvas.GraphicsContext
import javafx.scene.text.Text
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.motion_profiling.SharedAutonomousConfig
import org.team2471.frc.pathvisualizer.DefaultPath

class PathVisualizer : Application() {

    // the companion object which starts this class
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(PathVisualizer::class.java, *args)
        }
    }

    // javaFX state which needs saved around
    val canvas = Canvas(800.0, 900.0)
    val gc = canvas.graphicsContext2D
    val image = Image("assets/2018Field.png")

    // class state variables
    var mapAutonomous: MutableMap<String, SharedAutonomousConfig> = mutableMapOf()
    var selectedAutonomous: SharedAutonomousConfig? = null
    var selectedPath: Path2D? = null

    // image stuff - measure your image with paint and enter these first 3
    private val upperLeftOfFieldPixels = Vector2(39.0, 58.0)
    private val lowerRightOfFieldPixels = Vector2(624.0, 701.0)
    private val zoomPivot = Vector2(366.0, 701.0)  // the location in the image where the zoom origin will originate
    private val fieldDimensionPixels = lowerRightOfFieldPixels - upperLeftOfFieldPixels
    private val fieldDimensionFeet = Vector2(27.0, 27.0)

    // view settings
    private var zoom: Double = feetToPixels(1.0)  // initially draw at 1:1
    var offset: Vector2 = Vector2(0.0, 0.0)

    // location of image extremes in world units
    private val upperLeftFeet = screen2World(Vector2(0.0, 0.0))  // calculate when zoom is 1:1, and offset is 0,0
    private val lowerRightFeet = screen2World(Vector2(image.width, image.height))

    // drawing
    private val circleSize = 10.0
    private val tangentLengthDrawFactor = 3.0

    // point editing
    private var editPoint: Path2DPoint? = null
    private var editVector: Vector2? = null
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

    // helper functions
    fun feetToPixels(feet: Double): Double = feet * fieldDimensionPixels.x / fieldDimensionFeet.x

    fun pixelsToFeet(pixels: Double): Double = pixels * fieldDimensionFeet.x / fieldDimensionPixels.x

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

    // get started - essentially the initialization function
    override fun start(stage: Stage) {
        stage.title = "Path Visualizer"
        val outerHBox = HBox()
        stage.scene = Scene(outerHBox, 1600.0, 900.0)
        stage.sizeToScene()

        // set up the paths and autos
        selectedAutonomous = SharedAutonomousConfig("Auto1")
        mapAutonomous.put(selectedAutonomous!!.name, selectedAutonomous!!)
        selectedPath = DefaultPath
        selectedAutonomous!!.paths.put(selectedPath!!.name, selectedPath!!)

        // setup the layout
        outerHBox.children.add(canvas)
        val buttonsBox = VBox()
        buttonsBox.spacing = 10.0
        outerHBox.children.add(buttonsBox)
        addControlsToButtonsBox(buttonsBox)

        refreshScreen()
        stage.show()
    }

    // add all of the javaFX UI controls
    private fun addControlsToButtonsBox(buttonsBox: VBox) {
        val autoChooserHBox = HBox()
        val autoChooserName = Text("Auto Chooser  ")
        val autoChooserBox = ComboBox<String>()
        autoChooserBox.items.addAll(
                "hitler",
                "communism",
                "dothething",
                "wooowee")
        autoChooserBox.value = "hitler"
        autoChooserHBox.children.addAll(autoChooserName, autoChooserBox)

        val pathChooserHBox = HBox()
        val pathChooserName = Text("Path Chooser  ")
        val pathChooserBox = ComboBox<String>()
        pathChooserBox.items.addAll(
                "sliiide to the left",
                "sliiiide to the right",
                "two hops this time!",
                "reverse, reverse!"
        )
        pathChooserBox.value = "sliiiide to the right"
        pathChooserHBox.children.addAll(pathChooserName, pathChooserBox)

        val zoomHBox = HBox()
        val zoomName = Text("Zoom  ")
        val zoomAdjust = TextField(zoom.toString())
        val zoomMinus = Button("-")
        zoomMinus.setOnAction { _: ActionEvent ->
            //set what you want the buttons to do here
            zoom-- // so the zoom code or calls to a zoom function or whatever go here
            zoomAdjust.text = zoom.toString()
            refreshScreen()
        }
        val zoomPlus = Button("+")
        zoomPlus.setOnAction { _: ActionEvent ->
            // same as above
            zoom++
            zoomAdjust.text = zoom.toString()
            refreshScreen()
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
        val panXAdjust = TextField("0")
        val panYAdjust = TextField("0")
        panHBox.children.addAll(
                panName,
                panXName,
                panXAdjust,
                panYName,
                panYAdjust)

        buttonsBox.children.addAll(
                autoChooserHBox,
                pathChooserHBox,
                zoomHBox,
                panHBox)
    }

    private fun refreshScreen() {
        gc.fill = Color.WHITE
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
        if (path2D == null)
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
                gc.stroke = Color.GREEN
            else
                gc.stroke = Color.WHITE

            val tPoint = world2Screen(point.position)
            gc.strokeOval(tPoint.x - circleSize / 2, tPoint.y - circleSize / 2, circleSize, circleSize)
            if (point.prevPoint != null) {
                if (point === selectedPoint && pointType == PointType.PREV_TANGENT)
                    gc.stroke = Color.GREEN
                else
                    gc.stroke = Color.WHITE
                val tanPoint = world2Screen(Vector2.subtract(point.position, Vector2.multiply(point.prevTangent, 1.0 / tangentLengthDrawFactor)))
                gc.strokeOval(tanPoint.x - circleSize / 2, tanPoint.y - circleSize / 2, circleSize, circleSize)
                gc.lineWidth = 2.0
                gc.strokeLine(tPoint.x, tPoint.y, tanPoint.x, tanPoint.y)
            }
            if (point.nextPoint != null) {
                if (point === selectedPoint && pointType == PointType.NEXT_TANGENT)
                    gc.stroke = Color.GREEN
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
}

// todo: investigate why mirrored is not working
// todo: edit boxes respond - zoom, and pan
// todo: mouse routines - down, move, up
// todo: try layoutpanel for making buttons follow size of window on right
// todo: autonomous and path combos working
// todo: delete point button
// todo: add path properties - robot width, height, speed, direction, mirrored
// todo: save to file, load from file
// todo: save to network tables for pathvisualizer
// todo: load from network tables for robot
// todo: draw ease curve in bottom panel
// todo: edit boxes for position and tangents of selected point
// todo: editing of ease curve
// todo: playback of robot travel
