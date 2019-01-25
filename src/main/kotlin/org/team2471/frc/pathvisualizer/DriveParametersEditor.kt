package org.team2471.frc.pathvisualizer
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import org.team2471.frc.lib.motion_profiling.following.DrivetrainParameters
import java.lang.IllegalStateException
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

// this function is magical -tyler
inline fun <reified T : DrivetrainParameters> DriveParametersEditor(params: T): Node {
    val paramsClass = T::class

    @Suppress("UNCHECKED_CAST")
    val props = paramsClass
            .memberProperties
            .mapNotNull { it as? KMutableProperty1<T, Any?> }

    val pane = FlowPane()

    for (prop in props) {
        val node: Node = when (prop.returnType) {
            Double::class.starProjectedType -> {
                HBox().apply {
                    val label = Label(prop.name.capitalize())
                    val textField = TextField().apply {
                        setOnKeyTyped { key ->
                            if (key.character == "\n" || key.character == "\t") {
                                val value = text.toDouble()

                                prop.set(params, value)
                            }
                        }
                    }
                    children.addAll(label, textField)
                }
            }
            Boolean::class.starProjectedType -> HBox().apply {
                val label = Label(prop.name.capitalize())
                val textField = TextField().apply {
                    setOnKeyTyped { key ->
                        if (key.character == "\n" || key.character == "\t") {
                            val value = text?.toBoolean() ?: return@setOnKeyTyped

                            prop.set(params, value)
                        }
                    }
                }
                children.addAll(label, textField)
            }
            else -> throw IllegalStateException("Unknown type: ${prop.returnType}")
        }

        pane.children.add(node)
    }

    return pane
}
