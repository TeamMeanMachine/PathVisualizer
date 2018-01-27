import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.scene.shape.Line
import javafx.stage.Stage
import com.sun.javafx.robot.impl.FXRobotHelper.getChildren
import javafx.beans.property.DoubleProperty
import javafx.geometry.Rectangle2D
import javafx.scene.Group
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import org.team2471.frc.lib.vector.Vector2


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
        val box = HBox()
        val scene = Scene(box, 1600.0, 900.0)
        stage.scene = scene

        // load the image
        val image = Image("assets/HalfFieldDiagramBlue.png")
        println("Width: ${image.width}, Height: ${image.height}")
        imageWidthPixels = image.width
        imageHeightPixels = image.height

        // displays ImageView
        val imageView = ImageView()
        imageView.image = image
        val upperLeft = unProject(Vector2(0.0,0.0))
        val lowerRight = unProject(Vector2(image.width, image.height))
        val dimensions = lowerRight - upperLeft
        //imageView.viewport = Rectangle2D(upperLeft.x, upperLeft.y, dimensions.x, dimensions.y )

        box.children.add(imageView)

        stage.title = "Path Visualizer"
        stage.sizeToScene()
        stage.show()
    }
}