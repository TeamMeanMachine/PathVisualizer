import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.scene.shape.Line
import javafx.stage.Stage
import com.sun.javafx.robot.impl.FXRobotHelper.getChildren
import javafx.geometry.Rectangle2D
import javafx.scene.Group
import javafx.scene.control.ComboBox
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.paint.Color


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

        val buttonsBox = VBox()

        val autoChooserBox = ComboBox<String>()
        autoChooserBox.items.addAll(
                "asfd",
                "dothething",
                "woooowee")


        box.children.add(iv1)
        box.children.add(buttonsBox)

        buttonsBox.children.add(autoChooserBox)



        stage.title = "Path Visualizer"
        stage.sizeToScene()
        stage.show()
    }
}