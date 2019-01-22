package org.team2471.frc.pathvisualizer

import edu.wpi.first.networktables.NetworkTableInstance
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.FileChooser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Autonomous
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.motion_profiling.Path2DPoint
import org.team2471.frc.lib.util.Timer
import java.io.File
import java.io.PrintWriter
import java.util.prefs.Preferences
import org.team2471.frc.pathvisualizer.FieldPane.draw
import org.team2471.frc.pathvisualizer.FieldPane.selectedPath
import javafx.scene.input.KeyCode



object ControlPanel : VBox() {
    private val autoComboBox = ComboBox<String>()
    private val pathListView = ListView<String>()
    private val mirroredCheckBox = CheckBox("Mirrored")
    private val robotDirectionBox = ComboBox<String>()
    private val secondsText = TextField()
    private val speedText = TextField()
    private val trackWidthText = TextField()
    private val widthText = TextField()
    private val lengthText = TextField()
    private val scrubFactorText = TextField()
    private val xPosText = TextField()
    private val yPosText = TextField()
    private val angleText = TextField()
    private val magnitudeText = TextField()
    private val slopeModeCombo = ComboBox<String>()
    private val pathLengthText = TextField()
    private var refreshing = false
    private val currentTimeText = TextField()
    private val headingAngleText = TextField()
    private val easePositionText = TextField()
    private val userPref = Preferences.userRoot()
    private const val userFilenameKey = "org-frc2471-PathVisualizer-FileName"
    private var fileName = userPref.get(userFilenameKey, "")
    private val networkTableInstance = NetworkTableInstance.create()

    private var connectionJob: Job? = null

    var autonomi = Autonomi()
        private set

    var currentTime = 0.0
        set(value) {
            field = value
            FieldPane.draw()
        }


    var selectedAutonomous: Autonomous? = null
        private set

    init {
        spacing = 10.0
        padding = Insets(10.0, 10.0, 10.0, 10.0)

        if (!fileName.isEmpty())
            openFile(File(fileName))


        pathListView.prefHeight = 180.0
        val pathListViewHBox = HBox()
        pathListViewHBox.spacing = 10.0
        val pathListViewName = Text("Path:  ")

        pathListView.selectionModel.selectedItemProperty().addListener { _, _, pathName ->
            if (refreshing) return@addListener
            setSelectedPath(pathName)
        }

        val newPathButton = Button("New Path")
        newPathButton.setOnAction {

        }

        val deletePathButton = Button("Delete Path")
        deletePathButton.setOnAction {
            if (FieldPane.selectedPath != null && selectedAutonomous != null) {
                deleteSelectedPath()
            }
        }

        val renamePathButton = Button("Rename Path")
        renamePathButton.setOnAction {
            val selectedPath = FieldPane.selectedPath ?: return@setOnAction

            val dialog = TextInputDialog(selectedPath.name)
            dialog.title = "Path Name"
            dialog.headerText = "Enter the name for your path"
            dialog.contentText = "Path name:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                renamePath(selectedPath, result.get())
            }
        }

        pathListViewHBox.children.addAll(pathListViewName, pathListView, deletePathButton, renamePathButton)

        // autonomous combo box
        val autoComboHBox = HBox()
        autoComboHBox.spacing = 10.0
        val autoComboName = Text("Auto:  ")

        autoComboBox.valueProperty().addListener { _, _, newText ->
            if (refreshing) return@addListener

            setAuto(newText)
        }

        val renameAutoButton = Button("Rename Auto")
        renameAutoButton.setOnAction {
            val auto = selectedAutonomous ?: return@setOnAction
            val dialog = TextInputDialog(auto.name)
            dialog.title = "Auto Name"
            dialog.headerText = "Enter the name for your autonomous"
            dialog.contentText = "Auto name:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                renameAuto(auto, result.get())
            }
        }

        val deleteAutoButton = Button("Delete Auto")
        deleteAutoButton.setOnAction {
            if (selectedAutonomous != null) {
                deleteSelectedAuto()
            }
        }

        autoComboHBox.children.addAll(autoComboName, autoComboBox, deleteAutoButton, renameAutoButton)

        val miscHBox = HBox()
        val deletePoint = Button("Delete Point")
        deletePoint.setOnAction { FieldPane.deleteSelectedPoint() }
        val pathLengthLabel = Text("   Path Length:  ")
        val pathLengthUnits = Text("   Feet")
        miscHBox.children.addAll(deletePoint, pathLengthLabel, pathLengthText, pathLengthUnits)

        mirroredCheckBox.isSelected = if (selectedAutonomous != null) selectedAutonomous!!.isMirrored else false
        mirroredCheckBox.setOnAction {
            if (!refreshing) {
                FieldPane.setSelectedPathMirrored(mirroredCheckBox.isSelected)
            }
        }

        val robotDirectionHBox = HBox()
        val robotDirectionName = Text("Robot Direction:  ")
        robotDirectionBox.items.add("Forward")
        robotDirectionBox.items.add("Backward")
        robotDirectionBox.value = if (FieldPane.selectedPath == null || FieldPane.selectedPath!!.robotDirection == Path2D.RobotDirection.FORWARD) "Forward" else "Backward"
        robotDirectionBox.valueProperty().addListener { _, _, newText ->
            if (!refreshing) {
                FieldPane.setSelectedPathRobotDirection(if (newText == "Forward") Path2D.RobotDirection.FORWARD else Path2D.RobotDirection.BACKWARD)
            }
        }
        robotDirectionHBox.children.addAll(robotDirectionName, robotDirectionBox)



        val speedHBox = HBox()
        val speedName = Text("Speed Multiplier:  ")
        speedText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                val speed = speedText.text.toDouble()
                FieldPane.setSelectedPathSpeed(speed)
            }
        }
        speedHBox.children.addAll(speedName, speedText)

        val pointPosHBox = HBox()
        val posLabel = Text("Position:  ")
        xPosText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                FieldPane.setSelectedPointX(xPosText.text.toDouble())
                refresh()
            }
        }
        yPosText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                FieldPane.setSelectedPointY(yPosText.text.toDouble())
                refresh()
            }
        }
        val posUnit = Text(" feet")
        pointPosHBox.children.addAll(posLabel, xPosText, yPosText, posUnit)

        val tangentHBox = HBox()
        val tangentLabel = Text("Tangent:  ")
        angleText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                FieldPane.setSelectedPointAngle(angleText.text.toDouble())
                refresh()
            }
        }
        magnitudeText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                FieldPane.setSelectedPointMagnitude(magnitudeText.text.toDouble())
                refresh()
            }
        }
        val angleUnit = Text(" degrees")
        val magnitudeUnit = Text(" magnitude")
        tangentHBox.children.addAll(tangentLabel, angleText, angleUnit, magnitudeText, magnitudeUnit)

        val slopeMethodHBox = HBox()
        val slopeComboLabel = Text("Slope Method:  ")
        slopeModeCombo.items.addAll("Smooth", "Manual", "None")
        slopeModeCombo.valueProperty().addListener { _, _, newText ->
            if (refreshing) return@addListener

            val method = when (newText) {
                "Smooth" -> Path2DPoint.SlopeMethod.SLOPE_SMOOTH
                "Manual" -> Path2DPoint.SlopeMethod.SLOPE_MANUAL
                "Linear" -> Path2DPoint.SlopeMethod.SLOPE_LINEAR
                else -> throw IllegalStateException("Invalid slope method $newText")
            }
            FieldPane.setSelectedSlopeMethod(method)

        }
        slopeMethodHBox.children.addAll(slopeComboLabel, slopeModeCombo)

        val trackWidthHBox = HBox()
        val trackWidthName = Text("Track Width:  ")
        trackWidthText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                ControlPanel.trackWidthText.text = (ControlPanel.selectedAutonomous!!.trackWidth * 12.0).format(1)
                ControlPanel.selectedAutonomous?.trackWidth = (trackWidthText.text.toDouble()) / 12.0
                FieldPane.draw()
            }
        }
        val trackWidthUnit = Text(" inches")
        trackWidthHBox.children.addAll(trackWidthName, trackWidthText, trackWidthUnit)

        val scrubFactorHBox = HBox()
        val scrubFactorName = Text("Width Scrub Factor:  ")
        scrubFactorText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                selectedAutonomous?.scrubFactor = scrubFactorText.text.toDouble()
                FieldPane.draw()
            }
        }
        scrubFactorHBox.children.addAll(scrubFactorName, scrubFactorText)

        val widthHBox = HBox()
        val widthName = Text("Robot Width:  ")
        widthText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                selectedAutonomous?.robotWidth = widthText.text.toDouble() / 12.0
                FieldPane.draw()
            }
        }
        val widthUnit = Text(" inches")
        widthHBox.children.addAll(widthName, widthText, widthUnit)

        val lengthHBox = HBox()
        val lengthName = Text("Robot Length:  ")
        lengthText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                selectedAutonomous?.robotLength = lengthText.text.toDouble() / 12.0
                FieldPane.draw()
            }
        }
        val lengthUnit = Text(" inches")
        lengthHBox.children.addAll(lengthName, lengthText, lengthUnit)

        val filesBox = HBox()
        filesBox.spacing = 10.0
        val openButton = Button("Open")
        openButton.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.title = "Open Autonomi File..."
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Autonomi files (*.json)", "*.json"))
            fileChooser.initialDirectory = File(System.getProperty("user.dir"))
            fileChooser.initialFileName = "Test.json"  // this is supposed to be saved in the registry, but it didn't work
            val file = fileChooser.showOpenDialog(PathVisualizer.stage)
            if (file != null) {
                fileName = file.absolutePath
                openFile(file)
            }
        }
        val saveAsButton = Button("Save As")
        saveAsButton.setOnAction {
            saveAs()
        }
        val saveButton = Button("Save")
        saveButton.setOnAction {
            if (fileName.isEmpty()) {
                saveAs()
            } else {
                val file = File(fileName)
                val json = autonomi.toJsonString()
                val writer = PrintWriter(file)
                writer.append(json)
                writer.close()
            }
        }
        filesBox.children.addAll(openButton, saveAsButton, saveButton)

        val robotHBox = HBox()
        val easeCurveFuntions = HBox()
        val sendToRobotButton = Button("Send To Robot")
        sendToRobotButton.setOnAction { _: ActionEvent ->
            autonomi.publishToNetworkTables(networkTableInstance)
        }
        val addressName = Text("  IP Address:  ")  // this is a great candidate to be saved in the registry, so that other teams only have to change it once
        var ipAddress = "10.24.71.2"
        val addressText = TextField(ipAddress)
        addressText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                ipAddress = addressText.text
                connect(ipAddress)
            }
        }
        connect(ipAddress)

        val playButton = Button("Play")
        var animationJob: Job? = null
        playButton.setOnAction {
            if (selectedPath != null) {
                animationJob?.cancel()

                val timer = Timer()
                timer.start()

                animationJob = GlobalScope.launch {
                    while (timer.get() < selectedPath!!.durationWithSpeed) {
                        if (!isActive) return@launch

                        Platform.runLater {
                            currentTime = timer.get()
                            draw()
                            refresh()
                        }

                        // Playback @ approx 30fps (1000ms/30fps = 33ms)
                        delay(1000L / 30L)
                    }

                    Platform.runLater { currentTime = selectedPath!!.durationWithSpeed }
                }
            }
        }

        robotHBox.children.addAll(sendToRobotButton, addressName, addressText)

        val secondsHBox = HBox()
        secondsHBox.spacing = 10.0
        val secondsName = Text("Path Duration:")
        val currentTimeName = Text("Current Time:")
        secondsText.prefWidth = 100.0
        secondsText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                val seconds = secondsText.text.toDouble()
                FieldPane.setSelectedPathDuration(seconds)
            }
        }
        currentTimeText.prefWidth = 100.0
        currentTimeText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                currentTime = currentTimeText.text.toDouble()
                refresh()
            }
        }

        secondsHBox.children.addAll( currentTimeName, currentTimeText, playButton,  secondsName, secondsText)

        val easeAndHeadingHBox = HBox()
        easeAndHeadingHBox.spacing = 10.0
        val easeValue = Text("Current Ease Value: ")
        val headingValue = Text("Current Heading value: ")

        easePositionText.prefWidth = 100.0
        easePositionText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                if (selectedPath!= null) {
                    selectedPath!!.getEaseCurve().storeValue(currentTime, easePositionText.text.toDouble()/100.0 )
                    println("Edited Ease: ${selectedPath!!.getEaseCurve().getValue(currentTime)}")
                    draw()
                }
            }
        }

        headingAngleText.prefWidth = 100.0
        headingAngleText.setOnKeyPressed { event ->
            if (event.code === KeyCode.ENTER) {
                if (selectedPath!= null && !headingAngleText.text.isEmpty()) {
                    selectedPath!!.getHeadingCurve().storeValue(currentTime, headingAngleText.text.toDouble() )
                    draw()
                }
            }
        }

        easeAndHeadingHBox.children.addAll(easeValue, easePositionText, headingValue, headingAngleText)

        children.addAll(
                autoComboHBox,
                pathListViewHBox,
                miscHBox,
                Separator(),
                mirroredCheckBox,
                speedHBox,
                robotDirectionHBox,
                Separator(),
                pointPosHBox,
                tangentHBox,
                slopeMethodHBox,
                Separator(),
                trackWidthHBox,
                scrubFactorHBox,
                widthHBox,
                lengthHBox,
                Separator(),
                filesBox,
                robotHBox,
                Separator(),
                secondsHBox,
                easeAndHeadingHBox
        )

        refresh()
        setAuto("Tests")
    }

    private fun connect(address: String) {
        println("Connecting to address $address")

        connectionJob?.cancel()

        connectionJob = GlobalScope.launch {
            // shut down previous server, if connected
            if (networkTableInstance.isConnected) {
                networkTableInstance.stopDSClient()
                networkTableInstance.stopClient()
                networkTableInstance.deleteAllEntries()
            }

            // reconnect with new address
            networkTableInstance.setNetworkIdentity("PathVisualizer")

            if (address.matches("[1-9](\\d{1,3})?".toRegex())) {
                networkTableInstance.startClientTeam(address.toInt(), NetworkTableInstance.kDefaultPort)
            } else {
                networkTableInstance.startClient(address, NetworkTableInstance.kDefaultPort)
            }
        }
    }

    private fun openFile(file: File) {
        try {
            val json = file.readText()
            autonomi = Autonomi.fromJsonString(json)
            userPref.put(userFilenameKey, file.absolutePath)
        } catch (e: Exception) {
            System.err.println("Failed to find file ${file.absolutePath}")
            autonomi = Autonomi()
        }
        refresh()
    }

    private fun saveAs() {
        val fileChooser = FileChooser()
        fileChooser.title = "Save Autonomi File As..."
        val extFilter = FileChooser.ExtensionFilter("Autonomi files (*.json)", "*.json")
        fileChooser.extensionFilters.add(extFilter)
        fileChooser.initialDirectory = File(System.getProperty("user.dir"))
        fileChooser.initialFileName = "Test.json"  // this is supposed to be saved in the registry, but it didn't work
        val file = fileChooser.showSaveDialog(PathVisualizer.stage)
        if (file != null) {
            userPref.put(userFilenameKey, file.absolutePath)
            fileName = file.absolutePath
            val json = autonomi.toJsonString()
            val writer = PrintWriter(file)
            writer.append(json)
            writer.close()
        }
    }

    private fun deleteSelectedPath() {
        selectedAutonomous!!.paths.remove(FieldPane.selectedPath!!.name, FieldPane.selectedPath)
        FieldPane.selectedPath = null
        refresh()
    }

    private fun deleteSelectedAuto() {
        autonomi.mapAutonomous.remove(selectedAutonomous!!.name)
        selectedAutonomous = null
        FieldPane.selectedPath = null
        refresh()
    }

    private fun renamePath(path: Path2D, newName: String) {
        selectedAutonomous?.paths?.remove(path.name)
        path.name = newName
        selectedAutonomous?.paths?.put(newName, path)
        refresh()
    }

    private fun renameAuto(autonomous: Autonomous, newName: String) {
        autonomi.mapAutonomous.remove(autonomous.name)
        autonomous.name = newName
        autonomi.mapAutonomous[newName] = autonomous
        refresh()
    }

    private fun setAuto(auto: String?) {
        var newAutoName = auto
        if (newAutoName == "New Auto") {
            val defaultName = "Auto"
            var count = 1
            while (autonomi.mapAutonomous.containsKey(defaultName + count))
                count++
            val dialog = TextInputDialog(defaultName + count)
            dialog.title = "Auto Name"
            dialog.headerText = "Enter the name for your new autonomous"
            dialog.contentText = "Auto name:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                newAutoName = result.get()
                val newAuto = Autonomous(newAutoName)
                autonomi.put(newAuto)
                autoComboBox.items.add(autoComboBox.items.count() - 1, newAutoName)
            } else {
                newAutoName = selectedAutonomous?.name
            }
        }
        selectedAutonomous = autonomi.get(newAutoName)
        autoComboBox.value = newAutoName
        FieldPane.selectedPath = null
        refresh()
    }

    private fun setSelectedPath(pathName: String?) {
        var newPathName: String? = pathName

        if (pathName == "New Path") {
            val dialog = TextInputDialog("Path")
            dialog.title = "Path Name"
            dialog.headerText = "Enter the name for your new path"
            dialog.contentText = "Path name:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                newPathName = result.get()
                val newPath = Path2D(newPathName)
                newPath.addEasePoint(0.0, 0.0); newPath.addEasePoint(5.0, 1.0) // always begin with an ease curve
                selectedAutonomous!!.putPath(newPath)
                pathListView.items.add(pathListView.items.count() - 1, newPathName)
            } else {
                newPathName = FieldPane.selectedPath?.name
            }
        }
        if (selectedAutonomous != null) {
            FieldPane.selectedPath = selectedAutonomous!![newPathName]
        }

        currentTime = 0.0
        pathListView.selectionModel.select(newPathName)
        refresh()
        FieldPane.draw()
    }

    fun refresh() {
        refreshing = true
        // refresh auto combo
        autoComboBox.items.clear()
        for (kvAuto in autonomi.mapAutonomous) {
            autoComboBox.items.add(kvAuto.key)
            if (kvAuto.value == selectedAutonomous) {
                autoComboBox.value = kvAuto.key
            }
        }
        autoComboBox.items.add("New Auto")
        if (selectedAutonomous == null) {
            selectedAutonomous = autonomi.mapAutonomous.values.firstOrNull()
            autoComboBox.value = selectedAutonomous?.name
        }

        // refresh path list view
        pathListView.items.clear()
        if (selectedAutonomous != null) {
            val paths = selectedAutonomous!!.paths
            for (kvPath in paths) {
                pathListView.items.add(kvPath.key)
                if (kvPath.value == FieldPane.selectedPath) {
                    pathListView.selectionModel.select(kvPath.key)
                }
            }
            pathListView.items.add("New Path")
            if (FieldPane.selectedPath == null) {
                FieldPane.selectedPath = paths.values.firstOrNull()
                pathListView.selectionModel.select(FieldPane.selectedPath?.name)
            }
        }

        if (FieldPane.selectedPath != null) {
            mirroredCheckBox.isSelected = selectedAutonomous!!.isMirrored
            robotDirectionBox.value = if (FieldPane.selectedPath!!.robotDirection == Path2D.RobotDirection.FORWARD) "Forward" else "Backward"
            secondsText.text = FieldPane.selectedPath!!.duration.format(1)
            speedText.text = FieldPane.selectedPath!!.speed.format(1)
            pathLengthText.text = FieldPane.selectedPath!!.length.format(2)
        }
        if (selectedAutonomous != null) {
            trackWidthText.text = (selectedAutonomous!!.trackWidth * 12.0).format(1)
            widthText.text = (selectedAutonomous!!.robotWidth * 12.0).format(1)
            lengthText.text = (selectedAutonomous!!.robotLength * 12.0).format(1)
            scrubFactorText.text = selectedAutonomous!!.scrubFactor.format(3)
        }

        currentTimeText.text = currentTime.format(1)
        if (selectedPath != null) {
            easePositionText.text = (selectedPath!!.getEaseCurve().getValue(currentTime) * 100.0).format(1)
            headingAngleText.text = selectedPath!!.getHeadingCurve().getValue(currentTime).format(1)
        }

        refreshPoints()
        FieldPane.draw()
        refreshing = false
    }

    private fun refreshPoints() {
        val selectedPoint = FieldPane.selectedPoint

        refreshing = true
        if (selectedPoint == null) {
            xPosText.text = ""
            yPosText.text = ""
            angleText.text = ""
            magnitudeText.text = ""
            slopeModeCombo.selectionModel.select("None")
        } else {
            when (FieldPane.selectedPointType) {
                PathVisualizer.PointType.POINT -> {
                    xPosText.text = (selectedPoint.position.x).format(2)
                    yPosText.text = (selectedPoint.position.y).format(2)
                    angleText.text = ""
                    magnitudeText.text = ""
                    slopeModeCombo.selectionModel.select("None")
                }
                PathVisualizer.PointType.PREV_TANGENT -> {
                    xPosText.text = (selectedPoint.prevTangent.x / -PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                    yPosText.text = (selectedPoint.prevTangent.y / -PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                    angleText.text = (selectedPoint.prevAngleAndMagnitude.x).format(1)
                    magnitudeText.text = (selectedPoint.prevAngleAndMagnitude.y).format(2)
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (selectedPoint.prevSlopeMethod) {
                        Path2DPoint.SlopeMethod.SLOPE_SMOOTH -> slopeModeCombo.selectionModel.select("Smooth")
                        Path2DPoint.SlopeMethod.SLOPE_MANUAL -> slopeModeCombo.selectionModel.select("Manual")
                    }
                }
                PathVisualizer.PointType.NEXT_TANGENT -> {
                    xPosText.text = (selectedPoint.nextTangent.x / PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                    yPosText.text = (selectedPoint.nextTangent.y / PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                    angleText.text = (selectedPoint.nextAngleAndMagnitude.x).format(1)
                    magnitudeText.text = (selectedPoint.nextAngleAndMagnitude.y).format(2)
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (selectedPoint.nextSlopeMethod) {
                        Path2DPoint.SlopeMethod.SLOPE_SMOOTH -> slopeModeCombo.selectionModel.select("Smooth")
                        Path2DPoint.SlopeMethod.SLOPE_MANUAL -> slopeModeCombo.selectionModel.select("Manual")
                    }
                }
            }

            val selectedPath = FieldPane.selectedPath ?: return
            pathLengthText.text = selectedPath.length.format(2)
        }
        refreshing = false
    }
}
