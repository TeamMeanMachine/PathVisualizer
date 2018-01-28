import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import org.team2471.frc.lib.vector.Vector2
import javafx.scene.canvas.GraphicsContext
import javafx.scene.shape.ArcType
import javafx.scene.text.Text


class PathVisualizer : Application() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(PathVisualizer::class.java,*args)

/*
          // use this to run the old Swing Visualizer
            val application = JFrame("Path Visualizer")
            application.setSize(1924, 1020)
            application.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            val pvPanel = PathVisualizer()
            application.add(pvPanel)
            application.isVisible = true
*/
        }
    }

    var imageWidthPixels = 562.0
    var imageHeightPixels = 684.0
    val imageWidthFeet = 27.0
    fun feetToPixels(feet: Double) : Double = feet * imageWidthPixels / imageWidthFeet
    fun pixelsToFeet(pixels: Double) : Double = pixels * imageWidthFeet / imageWidthPixels

    var zoom: Double = feetToPixels(1.0)  // initially draw at 1:1
    var offset: Vector2 = Vector2(0.0, 0.0)
    val zoomPivot: Vector2 = Vector2(imageWidthPixels/2.0, 534.0)  // the location in the image where the zoom origin will originate

    private fun project(vector2: Vector2) : Vector2 {  // converts a point from world units to screen pixels
        val temp = vector2 * zoom
        temp.y = -temp.y
        return temp + zoomPivot + offset
    }

    private fun unProject(vector2: Vector2) : Vector2 {  // converts a point from screen pixels to world units
        val temp = vector2 - offset - zoomPivot
        temp.y = -temp.y
        return temp / zoom
    }

    override fun start(stage: Stage) {
        val outerHBox = HBox()
        val scene = Scene(outerHBox, 1600.0, 900.0)
        stage.scene = scene
        val rightVBox = VBox()

        // load the image
        val image = Image("assets/HalfFieldDiagramBlue.png")
        println("Width: ${image.width}, Height: ${image.height}")

        // calculate ImageView corners
        //offset = Vector2(100.0, 100.0)
        //zoom *= 2.0
        val upperLeft = project(Vector2(-13.5,27.0))
        val lowerRight = project(Vector2(13.5, -5.0))
        val dimensions = lowerRight - upperLeft

        val canvas = Canvas(800.0, 600.0)
        val gc = canvas.graphicsContext2D
        gc.drawImage(image, 0.0,0.0, image.width, image.height, upperLeft.x, upperLeft.y, dimensions.x, dimensions.y )
        //drawShapes(gc)
        //drawPaths()
        outerHBox.children.add(canvas)

        // scrolling stuff
//        // add a stackpane to use in the scroll pane
//        val stackPane = StackPane()
//        stackPane.children.setAll( canvas )
//        // wrap the stackPane contents in a pannable scroll pane
//        val scroll = createScrollPane(stackPane)
//        // add the scroll to the outer HBox
//        outerHBox.children.add(scroll)
//        outerHBox.children.add(buttonBox)
//        // bind the preferred size of the scroll area to the size of the scene.
//        scroll.prefWidthProperty().bind(stackPane.widthProperty())
//        scroll.prefHeightProperty().bind(stackPane.widthProperty())
//        // center the scroll contents.
//        scroll.setHvalue(scroll.getHmin() + (scroll.getHmax() - scroll.getHmin()) / 2)
//        scroll.setVvalue(scroll.getVmin() + (scroll.getVmax() - scroll.getVmin()) / 2)

        val autoChooserHBox = HBox()
        val autoChooserName = Text("Auto Chooser  ")
        val autoChooserBox = ComboBox<String>()
        autoChooserBox.items.addAll(
                "hitler",
                "asdf",
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
        zoomMinus.setOnAction{ _ : ActionEvent -> //set what you want the buttons to do here
            zoom-- // so the zoom code or calls to a zoom function or whatever go here
            zoomAdjust.text = zoom.toString()
        }
        val zoomPlus = Button("+")
        zoomPlus.setOnAction { _: ActionEvent ->// same as above
            zoom++
            zoomAdjust.text = zoom.toString()
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
        val panYAdjust = TextField( "0")
        panHBox.children.addAll(
                panName,
                panXName,
                panXAdjust,
                panYName,
                panYAdjust)

        val buttonsBox = VBox()
        buttonsBox.children.addAll(
                autoChooserHBox,
                pathChooserHBox,
                zoomHBox,
                panHBox)
        buttonsBox.spacing = 10.0
        outerHBox.children.add(buttonsBox)

        stage.title = "Path Visualizer"
        stage.sizeToScene()
        stage.show()
    }

    private fun createKillButton(): Button {
        val killButton = Button("Kill the evil witch")
        killButton.style = "-fx-base: firebrick;"
        killButton.translateX = 15.0
        killButton.translateY = -13.0
        killButton.onAction = EventHandler {
            killButton.style = "-fx-base: forestgreen;"
            killButton.text = "Ding-Dong! The Witch is Dead"
        }
        return killButton
    }

    private fun createScrollPane(stackPane: Pane): ScrollPane {
        val scroll = ScrollPane()
        scroll.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.isPannable = true
        scroll.setPrefSize(800.0, 600.0)
        scroll.content = stackPane
        return scroll
    }

 private fun drawShapes(gc: GraphicsContext) {
        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(5.0);
        gc.strokeLine(40.0, 10.0, 10.0, 40.0);
        gc.fillOval(10.0, 60.0, 30.0, 30.0);
        gc.strokeOval(60.0, 60.0, 30.0, 30.0);
        gc.fillRoundRect(110.0, 60.0, 30.0, 30.0, 10.0, 10.0);
        gc.strokeRoundRect(160.0, 60.0, 30.0, 30.0, 10.0, 10.0);
        gc.fillArc(10.0, 110.0, 30.0, 30.0, 45.0, 240.0, ArcType.OPEN);
        gc.fillArc(60.0, 110.0, 30.0, 30.0, 45.0, 240.0, ArcType.CHORD);
        gc.fillArc(110.0, 110.0, 30.0, 30.0, 45.0, 240.0, ArcType.ROUND);
        gc.strokeArc(10.0, 160.0, 30.0, 30.0, 45.0, 240.0, ArcType.OPEN);
        gc.strokeArc(60.0, 160.0, 30.0, 30.0, 45.0, 240.0, ArcType.CHORD);
        gc.strokeArc(110.0, 160.0, 30.0, 30.0, 45.0, 240.0, ArcType.ROUND);
        gc.fillPolygon(doubleArrayOf(10.0, 40.0, 10.0, 40.0), doubleArrayOf(210.0, 210.0, 240.0, 240.0), 4);
        gc.strokePolygon(doubleArrayOf(60.0, 90.0, 60.0, 90.0), doubleArrayOf(210.0, 210.0, 240.0, 240.0), 4);
        gc.strokePolyline(doubleArrayOf(110.0, 140.0, 110.0, 140.0), doubleArrayOf(210.0, 210.0, 240.0, 240.0), 4);
    }
}