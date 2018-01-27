import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.scene.shape.Line
import javafx.stage.Stage
import com.sun.javafx.robot.impl.FXRobotHelper.getChildren
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Rectangle2D
import javafx.scene.Group
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javax.swing.Action


class PathVisualizer : Application() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = launch(PathVisualizer::class.java,*args)
    }

    override fun start(stage: Stage) {
        val box = HBox()
        val scene = Scene(box, 1600.0, 900.0)
        stage.scene = scene

        // load the image
        val image = Image("assets/HalfFieldDiagramBlue.png")

        // simple displays ImageView the image as is
        val iv1 = ImageView()
        iv1.image = image
        // iv1.viewport = Rectangle2D(40.0, 35.0, 110.0, 110.0)

        val buttonNameBox = VBox()
        val buttonsBox = VBox()
        val zoomFormatter = HBox()
        val panFormatter = HBox()

        val autoChooserName = Text()
        autoChooserName.text = "Auto Chooser  "
        val autoChooserBox = ComboBox<String>()
        autoChooserBox.items.addAll(
                "hitler",
                "asdf",
                "dothething",
                "wooowee")
        autoChooserBox.value = "hitler"

        val pathChooserName = Text()
        pathChooserName.text = "Path Chooser  "

        val pathChooserBox = ComboBox<String>()
        pathChooserBox.items.addAll(
                "sliiide to the left",
                "sliiiide to the right",
                "two hops this time!",
                "reverse, reverse!"
        )
        pathChooserBox.value = "sliiiide to the right"

        var zoomMultiplier = 1

        val zoomName = Text()
        zoomName.text = "Zoom  "
        val zoomAdjust = TextField()
        zoomAdjust.text = zoomMultiplier.toString()
        val zoomMinus = Button()
        zoomMinus.text = "-"
        zoomMinus.setOnAction{ _ : ActionEvent -> //set what you want the buttons to do here
            zoomMultiplier-- // so the zoom code or calls to a zoom function or whatever go here
            zoomAdjust.text = zoomMultiplier.toString()
        }

        val zoomPlus = Button()
        zoomPlus.text = "+"
        zoomPlus.setOnAction { _: ActionEvent ->// same as above
            zoomMultiplier++
            zoomAdjust.text = zoomMultiplier.toString()
        }

        val panName = Text()
        panName.text = "Pan  "

        val panXName = Text()
        panXName.text = "X = "
        val panYName = Text()
        panYName.text = "Y = "
        val panXAdjust = TextField()
        val panYAdjust = TextField()
        panXAdjust.text = "0"
        panYAdjust.text = "0"


        box.children.add(iv1)
        box.children.add(buttonNameBox)
        box.children.add(buttonsBox)

        buttonNameBox.children.addAll(
                autoChooserName,
                pathChooserName,
                zoomName,
                panName)
        buttonsBox.children.addAll(
                autoChooserBox,
                pathChooserBox,
                zoomFormatter,
                panFormatter)
        zoomFormatter.children.addAll(
                zoomMinus,
                zoomAdjust,
                zoomPlus)
        panFormatter.children.addAll(
                panXName,
                panXAdjust,
                panYName,
                panYAdjust)

        stage.title = "Path Visualizer"
        stage.sizeToScene()
        stage.show()
    }
}