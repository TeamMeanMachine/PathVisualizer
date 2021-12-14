package org.team2471.frc.pathvisualizer

import edu.wpi.first.networktables.NetworkTableInstance
import javafx.application.Platform
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.util.Duration
import kotlinx.coroutines.*
import org.team2471.frc.lib.motion_profiling.*
import org.team2471.frc.lib.motion_profiling.following.ArcadeParameters
import org.team2471.frc.lib.motion_profiling.following.RobotParameters
import org.team2471.frc.lib.motion_profiling.following.SwerveParameters
import org.team2471.frc.lib.units.asMeters
import org.team2471.frc.lib.units.feet
import org.team2471.frc.lib.util.Timer
import org.team2471.frc.pathvisualizer.FieldPane.draw
import org.team2471.frc.pathvisualizer.FieldPane.selectedPath
import org.team2471.frc.pathvisualizer.FieldPane.selectedPoint
import kotlin.math.roundToInt


object ControlPanel : VBox() {
    private val autoComboBox = ComboBox<String>()
    private val pathListView = ListView<String>()
    private val mirroredCheckBox = CheckBox("Mirrored")
    private val pathWeaverFormatCheckBox = CheckBox("PathWeaver")
    private val robotDirectionBox = ComboBox<String>()
    private val secondsText = TextField()
    private val displayOnlySelectedPath = CheckBox("")
    private val speedText = TextField()
    private val trackWidthText = TextField()
    private val widthText = TextField()
    private val lengthText = TextField()
    private val displaySwerveTracks = CheckBox("Show Swerve Tracks")
    private val scrubFactorText = TextField()
    private val xPosText = TextField()
    private val yPosText = TextField()
    private val angleText = TextField()
    private val magnitudeText = TextField()
    private val slopeModeCombo = ComboBox<String>()
    private val pathLengthText = TextField()
    private var refreshing = false
    private val currentTimeText = TextField()
    private val autosTitlePane = TitledPane()
    private val pathsTitlePane = TitledPane()
    private val pointsAndTangentsTitlePane = TitledPane()
    private val easeAndHeadingTitlePane = TitledPane()
    private val headingAngleText = TextField()
    private val easePositionText = TextField()
    private val fieldWidth = TextField()
    private val fieldHeight = TextField()
    private val fieldOverlayOpacitySlider = Slider(0.0, 1.0, 0.5)
    private val maxAccelerationText = TextField()
    private val maxVelocityText = TextField()
    private val fieldTopLeftX = TextField()
    private val fieldTopLeftY = TextField()
    private val curveTypeCombo = ComboBox<String>()
    private val driveTrainTypeCombo = ComboBox<String>()
    private val deletePointButton = Button().fontAwesome(FontAwesome.Icon.Delete,"Delete Point")
    private var animationJob: Job? = null
    private var connectionJob: Job? = null
    var selectedEasePointType = ""

    var ipAddress = ""
    var pathWeaverFormat = false
    val displayFieldOverlay = CheckBox("Field overlay")
    val networkTableInstance : NetworkTableInstance = NetworkTableInstance.create()
    var autonomi = Autonomi()
    var pathDuration : Double
        get() {
            return secondsText.text.toDoubleOrNull() ?: 0.0
        }
        set (value) {
            secondsText.text = value.toString()
        }
    var currentTime = 0.0
        set(value) {
            field = value
            draw()
        }
    var robotWidth : Double
        get() {
            return widthText.text.toDoubleOrNull() ?: 0.0
        }
        set (value) {
            widthText.text = value.toString()
        }
    var robotLength : Double
        get() {
            return lengthText.text.toDoubleOrNull() ?: 0.0
        }
        set (value) {
            lengthText.text = value.toString()
        }
    var trackWidth : Double
        get() {
            return trackWidthText.text.toDoubleOrNull() ?: 0.0
        }
        set (value) {
            trackWidthText.text = value.toString()
        }
    var trackScrubFactor : Double
        get() {
            return scrubFactorText.text.toDoubleOrNull() ?: 0.0
        }
        set (value) {
            scrubFactorText.text = value.toString()
        }

    val trackDisplaySwerve : Boolean
        get() {
            return displaySwerveTracks.isSelected
        }
    val maxVelocity : Double
        get(){
            return maxVelocityText.text.toDoubleOrNull() ?: 20.0
        }
    val maxAcceleration : Double
        get(){
            return maxAccelerationText.text.toDoubleOrNull() ?: 20.0
        }

    val displayOnlyCurrentPath : Boolean
     get() {
         return displayOnlySelectedPath.isSelected
     }

    val fieldOverlayOpacity : Double
        get() {
            return fieldOverlayOpacitySlider.value
        }

    var selectedAutonomous: Autonomous? = null
        private set

    fun initializeParameters() {
        if (autonomi.robotParameters.robotWidth > 0 && autonomi.robotParameters.robotLength > 0) {
            robotWidth = (autonomi.robotParameters.robotWidth * 12)
            robotLength = (autonomi.robotParameters.robotLength * 12)
        } else {
            // provide default robotParameters if none exist
            autonomi.robotParameters = RobotParameters(
                robotWidth = 28.0 / 12.0,
                robotLength = 32.0 / 12.0
            )
        }
        when (val driveParams = autonomi.drivetrainParameters) {
            is ArcadeParameters -> {
                trackWidth = driveParams.trackWidth * 12.0
                trackScrubFactor = driveParams.scrubFactor
            }
            is SwerveParameters -> {
                trackWidth = robotWidth
                trackScrubFactor = 1.0
            }
            else -> {
                autonomi.drivetrainParameters = ArcadeParameters(
                    trackWidth = 25.0,
                    scrubFactor = 1.12,
                    leftFeedForwardCoefficient = 0.070541988198899,
                    leftFeedForwardOffset = 0.021428882425651,
                    rightFeedForwardCoefficient = 0.071704891069425,
                    rightFeedForwardOffset = 0.020459379452296
                )
            }
        }
        setDrivetrainType()
    }

    init {
        // get ipAddress from preferences
        ipAddress = PathVisualizer.pref.get("ipAddress", "10.24.71.2")
        pathWeaverFormat = false //pref.getBoolean("pathWeaverFormat", false)


        spacing = 10.0
        padding = Insets(10.0, 10.0, 10.0, 10.0)

        initializeParameters()



        // autonomous management
        val autoComboHBox = HBox()
        autoComboHBox.spacing = 10.0
        autoComboBox.isEditable = false
        autoComboBox.valueProperty().addListener { _, _, newText ->
            if (!refreshing) {
                setAuto(newText)
            }
        }

        val newAutoButton = Button().fontAwesome(FontAwesome.Icon.Add,"New Auto")
        newAutoButton.setOnAction {
            val newAutoName: String
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
                newAutoName = selectedAutonomous?.name ?: ""
            }
            setAuto(newAutoName)
        }

        val renameAutoButton = Button().fontAwesome(FontAwesome.Icon.Edit,"Rename Auto")
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

        val deleteAutoButton = Button().fontAwesome(FontAwesome.Icon.Delete,"Delete Auto")
        deleteAutoButton.setOnAction {
            if (selectedAutonomous != null) {
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.contentText = "Are you sure you want to delete the entire auto?"
                alert.showAndWait()
                if (alert.result == ButtonType.OK) {
                    deleteSelectedAuto()
                }
            }
        }

       val playAutoButton = Button().fontAwesome(FontAwesome.Icon.Play,"Play Auto")
       playAutoButton.setOnAction {

            if (selectedAutonomous != null) {
                animationJob?.cancel()

                val timer = Timer()

                animationJob = GlobalScope.launch {
                    if (selectedAutonomous != null) {
                        val paths = selectedAutonomous!!.paths
                        val sortedPaths = paths.toSortedMap()
                        for (kvPath in sortedPaths) {
                            selectedPath = kvPath.value
                            animateSelectedPath()
                        }
                        if (selectedPath != null) {
                            Platform.runLater { currentTime = selectedPath!!.durationWithSpeed }
                        }
                    }
                }
            }
        }


        mirroredCheckBox.isSelected = selectedAutonomous?.isMirrored ?: false
        mirroredCheckBox.setOnAction {
            if (!refreshing) {
                FieldPane.setSelectedPathMirrored(mirroredCheckBox.isSelected)
            }
        }
        pathWeaverFormatCheckBox.isSelected = pathWeaverFormat
        pathWeaverFormatCheckBox.setOnAction {
            pathWeaverFormat = pathWeaverFormatCheckBox.isSelected
            PathVisualizer.pref.putBoolean("pathWeaverFormat", pathWeaverFormat)
            updatePathWeaverDisplay()
            draw()
        }
        val spacerAutoDelete = Region()
        spacerAutoDelete.prefWidth = 35.0
        val autoButtonsHBox = HBox()
        autoButtonsHBox.spacing = 10.0
        autoButtonsHBox.children.addAll( newAutoButton, renameAutoButton,playAutoButton,spacerAutoDelete,deleteAutoButton)
        autoComboHBox.alignment = Pos.CENTER_LEFT
        autoComboHBox.children.addAll(autoComboBox,mirroredCheckBox, pathWeaverFormatCheckBox)
        val autosGridPane = GridPane()
        autosGridPane.vgap = 4.0
        autosGridPane.padding = Insets(5.0, 5.0, 5.0, 5.0)
        // row 1
        autosGridPane.add(autoComboHBox, 0,0)
        // row 2
        autosGridPane.add(autoButtonsHBox,0,1)

        autosTitlePane.text = "Autos - " + selectedAutonomous?.name
        autosTitlePane.content = autosGridPane
        autosTitlePane.expandedProperty().addListener {_,_,newExpanded ->
            PathVisualizer.pref.putBoolean("autosPaneExpanded", newExpanded)
        }
        autosTitlePane.isExpanded = PathVisualizer.pref.getBoolean("autosPaneExpanded", true)



        // paths management
        pathListView.prefHeight = 180.0
        pathListView.selectionModel.selectedItemProperty().addListener { _, _, pathName ->
            if (refreshing) return@addListener
            setSelectedPath(pathName)
        }

        val newPathButton = Button().fontAwesome(FontAwesome.Icon.Add,"New Path")
        newPathButton.setOnAction {
            val newPathName: String?
            val defaultName = "Path"
            var count = 1
            while (selectedAutonomous!!.paths.containsKey(defaultName + count))
                count++
            val dialog = TextInputDialog(defaultName + count)
            dialog.title = "Path Name"
            dialog.headerText = "Enter the name for your new path"
            dialog.contentText = "Path name:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                newPathName = result.get()
                val newPath = Path2D(newPathName)
                newPath.addEasePoint(0.0, 0.0);
                newPath.addEasePoint(5.0, 1.0) // always begin with an ease curve
                selectedAutonomous!!.putPath(newPath)
                if (pathListView.items.isEmpty()) pathListView.items.add(pathListView.items.count(), newPathName)
            }
        }

        val deletePathButton = Button().fontAwesome(FontAwesome.Icon.Delete,"Delete Path")
        deletePathButton.setOnAction {
            if (selectedPath != null && selectedAutonomous != null) {
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.contentText = "Are you sure you want to delete this path?"
                alert.showAndWait()
                if (alert.result == ButtonType.OK) {
                    deleteSelectedPath()
                }
            }
        }
        val alignPathButton = Button().fontAwesome(FontAwesome.Icon.Align,"Align Path to Previous")
        alignPathButton.setOnAction {
            if (selectedPath != null && selectedAutonomous != null && pathListView.selectionModel.selectedIndex > 0) {
                val prevPath = selectedAutonomous!!.paths[pathListView.items[pathListView.selectionModel.selectedIndex-1]]
                if (prevPath != null) {
                    selectedPath!!.xyCurve.headPoint.position = prevPath.xyCurve.tailPoint.position
//                    selectedPath!!.headingCurve.headKey.angle = prevPath.headingCurve.tailKey.angle
                    println("prev ${prevPath.headingCurve.getValue(prevPath.duration)}")
                    selectedPath!!.headingCurve.storeValue(selectedPath!!.headingCurve.headKey.time,prevPath.headingCurve.getValue(prevPath.duration))
                }
                refresh()
                draw( )
            }
        }

        val renamePathButton = Button().fontAwesome(FontAwesome.Icon.Edit,"Rename Path")
        renamePathButton.setOnAction {
            val selectedPath = selectedPath ?: return@setOnAction

            val dialog = TextInputDialog(selectedPath.name)
            dialog.title = "Path Name"
            dialog.headerText = "Enter the name for your path"
            dialog.contentText = "Path name:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                val from = selectedPath.name
                val to = result.get()
                renamePath(selectedPath, to)
                TopBar.addUndo(Actions.RenamePathAction(selectedPath, from, to))
            }
        }

        val playPathButton = Button().fontAwesome(FontAwesome.Icon.Play,"Play Path")
        playPathButton.setOnAction {
            playSelectedPath()
        }
        val spacerPathDelete = Region()
        spacerPathDelete.prefWidth = 35.0

        val pathButtonsVBox = HBox()
        pathButtonsVBox.spacing = 10.0
        pathButtonsVBox.children.addAll(newPathButton, renamePathButton, playPathButton,spacerPathDelete, deletePathButton, alignPathButton)

        val pathLengthLabel = Text("Length:")
        val pathLengthUnits = Text("feet")
        pathLengthText.prefWidth = 60.0
        pathLengthText.isEditable = false
        pathLengthText.styleClass.add("textfield-readonly")

        val robotDirectionName = Text("Robot Direction:")
        robotDirectionBox.items.add(Path2D.RobotDirection.FORWARD.name.lowercase().capitalize())
        robotDirectionBox.items.add(Path2D.RobotDirection.BACKWARD.name.lowercase().capitalize())
        robotDirectionBox.value = selectedPath?.robotDirection?.name?.lowercase()?.capitalize() ?: Path2D.RobotDirection.BACKWARD.name.lowercase().capitalize()
        //if (FieldPane.selectedPath == null || FieldPane.selectedPath!!.robotDirection == Path2D.RobotDirection.FORWARD) "Forward" else "Backward"
        robotDirectionBox.valueProperty().addListener { _, _, newText ->
            if (!refreshing) {
                FieldPane.setSelectedPathRobotDirection(if (newText.uppercase() == Path2D.RobotDirection.BACKWARD.name) Path2D.RobotDirection.BACKWARD else Path2D.RobotDirection.FORWARD)
            }
        }

        val speedName = Text("Speed Multiplier:")
        speedText.prefWidth = 60.0
        speedText.setChangeHandler (Double::class){ FieldPane.setSelectedPathSpeed(speedText.text.toDouble())}

        val secondsName = Text("Path Duration:")
        secondsText.prefWidth = 60.0
        secondsText.setChangeHandler (Double::class) { FieldPane.setSelectedPathDuration(secondsText.text.toDouble())}

        val displayOnlySelectedPathName = Text("Hide other paths")
        displayOnlySelectedPath.setOnAction {
            draw()
        }


        // set up Paths GridPane

        val pathsGridPane = GridPane()
        pathsGridPane.vgap = 5.0
        pathsGridPane.padding = Insets(5.0, 5.0, 5.0, 5.0)
        pathsGridPane.hgap = 5.0
        pathsGridPane.isGridLinesVisible = false

        // row 1
        pathsGridPane.add(pathListView, 0,0,1,6) // spans path data rows on right
        pathsGridPane.add(pathLengthLabel,1,0)
        pathsGridPane.add(pathLengthText,2,0)
        pathsGridPane.add(pathLengthUnits, 3,0)

        // row 2
        pathsGridPane.add(secondsName,1,1)
        pathsGridPane.add(secondsText,2,1)

        // row 3
        pathsGridPane.add(speedName,1,2)
        pathsGridPane.add(speedText,2,2)

        // row 4
        pathsGridPane.add(robotDirectionName,1,3)
        pathsGridPane.add(robotDirectionBox,2,3)

        // row 5
        pathsGridPane.add(displayOnlySelectedPathName, 1, 4)
        pathsGridPane.add(displayOnlySelectedPath, 2, 4 )



        pathsGridPane.add(pathButtonsVBox, 0,7,4,1)
        pathsTitlePane.text = "Paths - " + selectedPath?.name
        pathsTitlePane.content = pathsGridPane
        pathsTitlePane.expandedProperty().addListener {_,_,newExpanded ->
            PathVisualizer.pref.putBoolean("pathsPaneExpanded", newExpanded)
        }
        pathsTitlePane.isExpanded = PathVisualizer.pref.getBoolean("pathsPaneExpanded", true)


        // Point Management


        val posLabelX = Text("Position x:")
        xPosText.prefWidth = 100.0
        xPosText.setChangeHandler (Double::class) {
            if(selectedPoint != null) {
                FieldPane.setSelectedPointX(xPosText.text.toDouble())
                refresh()
            } else if (EasePane.selectedPoint != null) {
                EasePane.setSelectedPointX(xPosText.text.toDouble())
                refresh()
            }
        }
        val posLabelY = Text("      y:")
        yPosText.prefWidth = 100.0
        yPosText.setChangeHandler (Double::class) {
            if(selectedPoint != null) {
                FieldPane.setSelectedPointY(yPosText.text.toDouble())
                refresh()
            } else if (EasePane.selectedPoint != null) {
                EasePane.setSelectedPointY(yPosText.text.toDouble())
                refresh()
            }
        }
        val posUnit = Text("feet")
        deletePointButton.setOnAction {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.contentText = "Are you sure you want to delete this point?"
            alert.showAndWait()
            if (alert.result == ButtonType.OK) {
                if (selectedPoint != null) {
                    FieldPane.deleteSelectedPoint()
                } else if (EasePane.selectedPoint != null){
                    if (selectedEasePointType == "Heading") {
                        selectedPath?.removeHeadingPoint(EasePane.selectedPoint)
                    } else if (selectedEasePointType == "Ease") {
                        selectedPath?.removeEasePoint(EasePane.selectedPoint)
                    }
                }
            }
        }


        val tangentHBox = HBox()
        tangentHBox.alignment = Pos.CENTER_LEFT
        val tangentLabel = Text("Tangent:")
        angleText.prefWidth = 100.0
        angleText.setChangeHandler (Double::class) {
            if (selectedPoint != null) {
                FieldPane.setSelectedPointAngle(angleText.text.toDouble())
                refresh()
            } else if (EasePane.selectedPoint != null) {
                EasePane.setSelectedPointAngle(angleText.text.toDouble())
                refresh()
            }
        }
        magnitudeText.prefWidth = 100.0
        magnitudeText.setChangeHandler (Double::class) {
            if (selectedPoint != null) {
                FieldPane.setSelectedPointMagnitude(magnitudeText.text.toDouble())
                refresh()
            } else if (EasePane.selectedPoint != null) {
                EasePane.setSelectedPointMagnitude(magnitudeText.text.toDouble())
                refresh()
            }
        }
        val angleUnit = Text("deg")
        val magnitudeUnit = Text("magnitude")
        tangentHBox.children.addAll()

        val slopeComboLabel = Text("Slope:")
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



        val pointsAndTangentsGridPane = GridPane()
        val col0 = ColumnConstraints()
        col0.hgrow = Priority.SOMETIMES
        val col1 = ColumnConstraints()
        col1.hgrow = Priority.SOMETIMES
        val col2 = ColumnConstraints()
        col2.hgrow = Priority.SOMETIMES
        val col3 = ColumnConstraints()
        col3.hgrow = Priority.SOMETIMES
        val col4 = ColumnConstraints()
        col3.hgrow = Priority.SOMETIMES
        val rightCol = ColumnConstraints()
        rightCol.hgrow = Priority.ALWAYS
        rightCol.halignment = HPos.RIGHT
        pointsAndTangentsGridPane.columnConstraints.addAll(col0,col1,col2,col3,col4,rightCol)
        pointsAndTangentsGridPane.vgap = 5.0
        pointsAndTangentsGridPane.padding = Insets(5.0, 5.0, 5.0, 5.0)
        pointsAndTangentsGridPane.hgap = 5.0
        pointsAndTangentsGridPane.isGridLinesVisible = false

        // row 1
        pointsAndTangentsGridPane.add(posLabelX, 0,0)
        pointsAndTangentsGridPane.add(xPosText, 1,0)
        pointsAndTangentsGridPane.add(posLabelY, 2,0)
        pointsAndTangentsGridPane.add(yPosText, 3,0)
        pointsAndTangentsGridPane.add(posUnit, 4,0)
        pointsAndTangentsGridPane.add(deletePointButton, 5,0)
        // row 2
        pointsAndTangentsGridPane.add(tangentLabel,0,1)
        pointsAndTangentsGridPane.add(angleText,1,1)
        pointsAndTangentsGridPane.add(angleUnit,2,1)
        pointsAndTangentsGridPane.add(magnitudeText,3,1)
        pointsAndTangentsGridPane.add(magnitudeUnit,4,1)

        pointsAndTangentsGridPane.add(slopeComboLabel,0,2)
        pointsAndTangentsGridPane.add(slopeModeCombo,1,2)


        pointsAndTangentsTitlePane.text = "Selected Point / Tangent"
        pointsAndTangentsTitlePane.content = pointsAndTangentsGridPane
        pointsAndTangentsTitlePane.isExpanded = false





        // Ease and Headings

        val currentTimeName = Text("Time:")
        currentTimeText.prefWidth = 100.0
        currentTimeText.setChangeHandler (Double::class) {
            currentTime = currentTimeText.text.toDouble()
            refresh()
        }

        val easeValue = Text("Ease:")
        val headingValue = Text("Heading:")

        easePositionText.prefWidth = 100.0
        easePositionText.setChangeHandler (Double::class) {
            selectedPath?.easeCurve?.storeValue(currentTime, easePositionText.text.toDouble() / 100.0)
//            println("Edited Ease: ${selectedPath?.easeCurve?.getValue(currentTime)}")
            draw()
        }

        headingAngleText.prefWidth = 100.0
        headingAngleText.setChangeHandler (Double::class) {
            if (headingAngleText.text.isNotEmpty()) {
                selectedPath?.headingCurve?.storeValue(currentTime, headingAngleText.text.toDouble())
                draw()
            }
        }

        val curveTypeHBox = HBox()
        val curveTypeLabel = Text("Curve Type:  ")
        curveTypeCombo.items.addAll("Ease", "Heading", "Both")
        curveTypeCombo.value = "Both"
        curveTypeCombo.valueProperty().addListener { _, _, newText ->
            if (refreshing) return@addListener

            val method = when (newText) {
                "Ease" -> Path2D.CurveType.EASE
                "Heading" -> Path2D.CurveType.HEADING
                "Both" -> Path2D.CurveType.BOTH
                else -> throw IllegalStateException("Invalid slope method $newText")
            }
            FieldPane.setSelectedCurveType(method)
            draw()
        }
        curveTypeHBox.children.addAll(curveTypeLabel, curveTypeCombo)

        val easeAndHeadingsGridPane = GridPane()
        easeAndHeadingsGridPane.vgap = 5.0
        easeAndHeadingsGridPane.padding = Insets(5.0, 5.0, 5.0, 5.0)
        easeAndHeadingsGridPane.hgap = 5.0
        easeAndHeadingsGridPane.isGridLinesVisible = false

        // row 1
        easeAndHeadingsGridPane.add(currentTimeName, 1,0)
        easeAndHeadingsGridPane.add(easeValue, 2,0)
        easeAndHeadingsGridPane.add(headingValue, 3,0)

        // row 2
        easeAndHeadingsGridPane.add(currentTimeText,1,1)
        easeAndHeadingsGridPane.add(easePositionText,2,1)
        easeAndHeadingsGridPane.add(headingAngleText,3,1)

        easeAndHeadingsGridPane.add(curveTypeLabel,0,2)
        easeAndHeadingsGridPane.add(curveTypeHBox,1,2)

        easeAndHeadingTitlePane.text = "Ease and Headings"
        easeAndHeadingTitlePane.content = easeAndHeadingsGridPane
        easeAndHeadingTitlePane.isExpanded = true


        // Robot Parameters

        val addressName = Text("Robot Address:")
        val addressText = TextField(ipAddress)
        addressText.setChangeHandler {
            ipAddress = addressText.text
            PathVisualizer.pref.put("ipAddress", ipAddress)
            connect()
        }
        connect()

        val widthName = Text("Robot Width:  ")
        widthText.setChangeHandler (Double::class) {
            autonomi.robotParameters.robotWidth = widthText.text.toDouble() / 12.0
            draw()
        }
        val widthUnit = Text(" inches")

        val lengthName = Text("Robot Length:")
        lengthText.setChangeHandler (Double::class) {
            autonomi.robotParameters.robotLength = lengthText.text.toDouble() / 12.0
            draw()
        }
        val lengthUnit = Text(" inches")

        val driveTrainTypeComboText = Text("Drive Type:")
        driveTrainTypeCombo.items.addAll("Arcade", "Swerve")
        driveTrainTypeCombo.value = if (autonomi.drivetrainParameters is ArcadeParameters) {"Arcade"} else {"Swerve"}
        driveTrainTypeCombo.valueProperty().addListener { _, _, newText ->
            if (refreshing) return@addListener
           updateDriveTrainParams()
           setDrivetrainType()
           draw()
        }
        displaySwerveTracks.isSelected = PathVisualizer.pref.getBoolean("displaySwerveTracks", false)
        displaySwerveTracks.setOnAction {
            PathVisualizer.pref.putBoolean("displaySwerveTracks", displaySwerveTracks.isSelected)
            draw()
        }

        val maxAccelerationName = Text("Max Acceleration:")
        val maxAccelerationUnitTextFlow = TextFlow()
        val maxAccelerationUnit = Text(" ft/s")
        val maxAccelerationUnitSuper = Text("2")
        maxAccelerationUnitSuper.translateY = maxAccelerationUnit.font.size * -0.3
        val smallerFontSize = (maxAccelerationUnit.font.size * 0.6).roundToInt()
        maxAccelerationUnitSuper.style = "-fx-font-size: $smallerFontSize;"
        maxAccelerationUnitTextFlow.children.addAll(maxAccelerationUnit, maxAccelerationUnitSuper)
        maxAccelerationText.text = PathVisualizer.pref.getDouble("maxAcceleration", 20.0).toString()
        maxAccelerationText.setChangeHandler (Double::class)  {
            PathVisualizer.pref.putDouble("maxAcceleration", maxAcceleration)
            draw()
        }

        val maxVelocityName = Text("Max Velocity:")
        val maxVelocityUnit = Text(" ft/s")
        maxVelocityText.text = PathVisualizer.pref.getDouble("maxVelocity", 20.0).toString()
        maxVelocityText.setChangeHandler (Double::class)  {
            PathVisualizer.pref.putDouble("maxVelocity", maxVelocity)
            draw()
        }

        /* ROBOT SPECIFIC PROPERTIES - USE REFLECTION ALONG WITH CURRENT ROBOT TO POPULATE THESE CONTROLS */
        val trackWidthName = Text("Track Width:")
        trackWidthText.setChangeHandler (Double::class)  {
            (autonomi.drivetrainParameters as? ArcadeParameters)?.trackWidth = (trackWidth / 12.0)
            draw()
        }
        val trackWidthUnit = Text(" inches")

        val scrubFactorName = Text("Width Scrub Factor:  ")
        scrubFactorText.setChangeHandler(Double::class)  {
            (autonomi.drivetrainParameters as? ArcadeParameters)?.scrubFactor = trackScrubFactor
            draw()
        }



        /* END - ROBOT SPECIFIC PROPERTIES - USE REFLECTION ALONG WITH CURRENT ROBOT TO POPULATE THESE CONTROLS */


        val robotParamsTitledPane = TitledPane()
        val robotParamsGridPane = GridPane()
        var currRow = 0
        robotParamsGridPane.vgap = 4.0
//        robotParamsGridPane.alignment = Pos.CENTER_LEFT
        robotParamsGridPane.padding = Insets(5.0, 5.0, 5.0, 5.0)
        robotParamsGridPane.add(addressName, 0,currRow)
        robotParamsGridPane.add(addressText, 1,currRow)
        currRow++

        robotParamsGridPane.add(driveTrainTypeComboText,0,currRow)
        robotParamsGridPane.add(driveTrainTypeCombo,1,currRow)
        robotParamsGridPane.add(displaySwerveTracks,2,currRow)
        currRow++

        robotParamsGridPane.add(widthName,0,currRow)
        robotParamsGridPane.add(widthText,1,currRow)
        robotParamsGridPane.add(widthUnit,2,currRow)
        currRow++

        robotParamsGridPane.add(lengthName,0,currRow)
        robotParamsGridPane.add(lengthText,1,currRow)
        robotParamsGridPane.add(lengthUnit,2,currRow)
        currRow++

        robotParamsGridPane.add(maxAccelerationName,0,currRow)
        robotParamsGridPane.add(maxAccelerationText,1,currRow)
        robotParamsGridPane.add(maxAccelerationUnitTextFlow,2,currRow)
        currRow++

        robotParamsGridPane.add(maxVelocityName,0,currRow)
        robotParamsGridPane.add(maxVelocityText,1,currRow)
        robotParamsGridPane.add(maxVelocityUnit,2,currRow)
        currRow++

        robotParamsGridPane.add(trackWidthName,0,currRow)
        robotParamsGridPane.add(trackWidthText,1,currRow)
        robotParamsGridPane.add(trackWidthUnit,2,currRow)
        currRow++

        robotParamsGridPane.add(scrubFactorName,0,currRow)
        robotParamsGridPane.add(scrubFactorText,1,currRow)


        robotParamsTitledPane.text = "Robot Parameters"
        robotParamsTitledPane.content = robotParamsGridPane
        robotParamsTitledPane.expandedProperty().addListener {_,_,newExpanded ->
            PathVisualizer.pref.putBoolean("robotParamsExpanded", newExpanded)
        }
        robotParamsTitledPane.isExpanded = PathVisualizer.pref.getBoolean("robotParamsExpanded", true)



        // field parameters
        displayFieldOverlay.isSelected = PathVisualizer.pref.getBoolean("displayFieldOverlay", true)
        displayFieldOverlay.setOnAction {
            PathVisualizer.pref.putBoolean("displayFieldOverlay", displayFieldOverlay.isSelected)
            draw()
        }

        // fieldOpacity
        val fieldOverlayOpacityText = Text("        Opacity:")
        fieldOverlayOpacitySlider.value = PathVisualizer.pref.getDouble("fieldOverlayOpacity", 0.5)
        fieldOverlayOpacitySlider.valueProperty().addListener { _, _, newValue ->
            if (displayFieldOverlay.isSelected) {
                println(newValue)
                PathVisualizer.pref.putDouble("fieldOverlayOpacity", newValue.toDouble())
                draw()
            }
        }

        val fieldWidthName = Text("Field Width:  ")
        val fieldWidthDimens = Text(" feet")
        fieldWidth.prefWidth = 60.0
        fieldWidth.text = FieldPane.fieldDimensionFeet.x.toString()
        fieldWidth.setChangeHandler (Double::class) {
            PathVisualizer.pref.putDouble("fieldWidth", fieldWidth.text.toDouble())
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Restart required"
            alert.headerText = "Warning"
            alert.contentText = "Restart required for field dimension changes"
            alert.show()
        }

        val fieldHeightName = Text("Field Height:  ")
        val fieldHeightDimens = Text(" feet")
        fieldHeight.prefWidth = 60.0
        fieldHeight.text = FieldPane.fieldDimensionFeet.y.toString()
        fieldHeight.setChangeHandler (Double::class) {
            PathVisualizer.pref.putDouble("fieldHeight", fieldHeight.text.toDouble())
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Restart required"
            alert.headerText = "Warning"
            alert.contentText = "Restart required for field dimension changes"
            alert.show()
        }

        val fieldParamsTitledPane = TitledPane()
        val fieldParamsGridPane = GridPane()
        fieldParamsGridPane.vgap = 4.0
//        robotParamsGridPane.alignment = Pos.CENTER_LEFT
        fieldParamsGridPane.padding = Insets(5.0, 5.0, 5.0, 5.0)

        // row 1

        fieldParamsGridPane.add(displayFieldOverlay, 0,0)
        fieldParamsGridPane.add(fieldOverlayOpacityText, 1, 0)
        fieldParamsGridPane.add(fieldOverlayOpacitySlider, 2,0)
        // row 1
        fieldParamsGridPane.add(fieldWidthName, 0,1)
        fieldParamsGridPane.add(fieldWidth, 1,1)
        fieldParamsGridPane.add(fieldWidthDimens, 2,1)

        // row 2
        fieldParamsGridPane.add(fieldHeightName, 0,2)
        fieldParamsGridPane.add(fieldHeight, 1,2)
        fieldParamsGridPane.add(fieldHeightDimens, 2,2)

        fieldParamsTitledPane.content = fieldParamsGridPane
        fieldParamsTitledPane.text = "Field  Parameters"
        fieldParamsTitledPane.isExpanded = false


        children.addAll(
                autosTitlePane,
                pathsTitlePane,
                easeAndHeadingTitlePane,
                pointsAndTangentsTitlePane,
                robotParamsTitledPane,
                fieldParamsTitledPane
        )

        refresh()
    }

//    private fun rebuildFieldPane(){
//        FieldPane.recalcFieldDimens()
//        draw()
//    }
    private fun playSelectedPath() {
        if (selectedPath != null) {
            animationJob?.cancel()

            val timer = Timer()
            timer.start()

            animationJob = GlobalScope.launch {
                animateSelectedPath()
                if (selectedPath != null) {
                    Platform.runLater { currentTime = selectedPath!!.durationWithSpeed }
                }
            }
        }
    }
    private suspend fun animateSelectedPath(){
        var pathDuration = selectedPath?.durationWithSpeed ?: 0.0
        if (pathWeaverFormat) {
            pathDuration = selectedPath?.generateTrajectory(maxVelocity.feet.asMeters, maxAcceleration.feet.asMeters)?.totalTimeSeconds ?: 0.0
        }
        currentTime = 0.0
        val timer = Timer()
        timer.start()

        while (timer.get() < pathDuration ) {
            Platform.runLater {
                currentTime = timer.get()
                refresh()
            }
            // Playback @ approx 30fps (1000ms/30fps = 33ms)
            delay(1000L / 30L)
        }
    }

    fun connect() {
        val address = ipAddress
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

/*
    private fun openFile(file: File) {
        try {
            val json = file.readText()
            autonomi = Autonomi.fromJsonString(json)
            userPref.put(userFilenameKey, file.absolutePath)
        } catch (e: Exception) {
            System.err.println("Failed to find file ${file.absolutePath}")
            autonomi = Autonomi()
        }
        if (autonomi.drivetrainParameters==null) {  // fix up after load since these parameters were not present.
            autonomi.arcadeParameters = ArcadeParameters(
                    trackWidth = 25.0/12.0,
                    scrubFactor = 1.12,
                    leftFeedForwardCoefficient = 0.070541988198899,
                    leftFeedForwardOffset = 0.021428882425651,
                    rightFeedForwardCoefficient = 0.071704891069425,
                    rightFeedForwardOffset = 0.020459379452296
            )
            autonomi.robotParameters = RobotParameters(
                    robotWidth = 28.0 / 12.0,
                    robotLength = 32.0 /12.0
            )

            autonomi.drivetrainParameters = autonomi.arcadeParameters
        }

        refresh()
    }
*/

/*
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
*/

    private fun deleteSelectedPath() {
        selectedAutonomous?.paths?.remove(selectedPath?.name, selectedPath)
        selectedPath = null
        refresh()
    }

    private fun deleteSelectedAuto() {
        autonomi.mapAutonomous.remove(selectedAutonomous?.name)
        selectedAutonomous = null
        selectedPath = null
        refresh()
    }

    fun renamePath(path: Path2D, newName: String) {
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

    private fun setAuto(auto: String) {
        PathVisualizer.pref.put("selectedAutonomousName", auto ?: "")
        selectedAutonomous = auto.let { autonomi[it] }
        autosTitlePane.text = "Auto - $auto"
        selectedPath = null
        refresh()
    }

    fun setSelectedPath(path: Path2D) {
        selectedAutonomous = path.autonomous
        setSelectedPath(path.name)
    }

    fun setSelectedPath(pathName: String?) {
        if (selectedAutonomous != null) {
            selectedPath = selectedAutonomous?.get(pathName)
        }
        currentTime = 0.0
        pathListView.selectionModel.select(pathName)
        pathsTitlePane.text = "Path - $pathName"
        refresh()
        draw()
    }

    fun refresh(newAutonomi : Boolean = false) {
        refreshing = true
        // refresh auto combo
        if (newAutonomi) {
            autoComboBox.items.clear()
            for (kvAuto in autonomi.mapAutonomous) {
                autoComboBox.items.add(kvAuto.key)
                if (kvAuto.value.name == selectedAutonomous?.name) {
                    println("set auto to ${kvAuto.key}")
                    autoComboBox.selectionModel.select(kvAuto.key)
                }
            }
        }
        if (selectedAutonomous == null && autonomi.mapAutonomous.isNotEmpty()) {
            selectedAutonomous = autonomi[PathVisualizer.pref.get("selectedAutonomousName", "")]
            if (selectedAutonomous == null) {
                selectedAutonomous = autonomi.mapAutonomous.values.firstOrNull()
            }
            autoComboBox.selectionModel.select(selectedAutonomous?.name)
            autosTitlePane.text = "Auto - ${selectedAutonomous?.name}"
            println("null auto selection. set auto to ${selectedAutonomous?.name}")
        }

        // refresh path list view
        pathListView.items.clear()
        if (selectedAutonomous != null) {
            val paths = selectedAutonomous!!.paths
            val sortedPaths = paths.toSortedMap()
            for (kvPath in sortedPaths) {
                pathListView.items.add(kvPath.key)
                if (kvPath.value == selectedPath) {
                    pathListView.selectionModel.select(kvPath.key)
                }
            }
            if (selectedPath == null) {
                selectedPath = paths.values.firstOrNull()
                pathListView.selectionModel.select(selectedPath?.name)
            }
        }

        if (selectedPath != null) {
            mirroredCheckBox.isSelected = selectedAutonomous?.isMirrored ?: false
            robotDirectionBox.value = selectedPath?.robotDirection?.name?.toLowerCase()?.capitalize() ?: Path2D.RobotDirection.BACKWARD.name.toLowerCase().capitalize()
            secondsText.text = selectedPath?.duration?.format(1)
            speedText.text = selectedPath?.speed?.format(1)
            pathLengthText.text = selectedPath?.length?.format(2)
            pathsTitlePane.text = "Path - ${selectedPath?.name ?: ""}"
            curveTypeCombo.value = selectedPath?.curveType.toString().capitalize()
        }
//
//        trackWidthText.text = (autonomi.arcadeParameters.trackWidth * 12.0).format(1)
//        scrubFactorText.text = autonomi.arcadeParameters.scrubFactor.format(3)
//        widthText.text = (autonomi.robotParameters.robotWidth * 12.0).format(1)
//        lengthText.text = (autonomi.robotParameters.robotLength * 12.0).format(1)

        currentTimeText.text = currentTime.format(1)
        if (selectedPath != null) {
            easePositionText.text = (selectedPath!!.easeCurve.getValue(currentTime) * 100.0).format(1)
            headingAngleText.text = selectedPath!!.headingCurve.getValue(currentTime).format(1)
        }
        updatePathWeaverDisplay()
        refreshPoints()
        draw()
        refreshing = false
    }

    fun refreshPoints() {
        val fieldPaneSelectedPoint = selectedPoint
        val easePaneSelectedPoint = EasePane.selectedPoint
        refreshing = true
        magnitudeText.isDisable = true
        angleText.isDisable = true
        slopeModeCombo.isDisable = true
        xPosText.isDisable = true
        yPosText.isDisable = true
        deletePointButton.isDisable = true
        if (fieldPaneSelectedPoint == null && easePaneSelectedPoint == null) {
            xPosText.text = ""
            yPosText.text = ""
            angleText.text = ""
            magnitudeText.text = ""
            slopeModeCombo.selectionModel.select("None")
            pointsAndTangentsTitlePane.isExpanded = false
        } else {
            pointsAndTangentsTitlePane.isExpanded = true
            if(fieldPaneSelectedPoint != null) {
                when (FieldPane.selectedPointType) {
                    Path2DPoint.PointType.POINT -> {
                        xPosText.text = (fieldPaneSelectedPoint.position.x).format(2)
                        yPosText.text = (fieldPaneSelectedPoint.position.y).format(2)
                        angleText.text = ""
                        magnitudeText.text = ""
                        slopeModeCombo.selectionModel.select("None")
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                        deletePointButton.isDisable = (selectedPoint == selectedPath?.xyCurve?.headPoint || selectedPoint == selectedPath?.xyCurve?.tailPoint)
                    }
                    Path2DPoint.PointType.PREV_TANGENT -> {
                        xPosText.text = (fieldPaneSelectedPoint.prevTangent.x / -PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                        yPosText.text = (fieldPaneSelectedPoint.prevTangent.y / -PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                        angleText.text = (fieldPaneSelectedPoint.prevAngleAndMagnitude.x).format(1)
                        magnitudeText.text = (fieldPaneSelectedPoint.prevAngleAndMagnitude.y).format(2)
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                        slopeModeCombo.isDisable = false
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                        @Suppress("NON_EXHAUSTIVE_WHEN")
                        when (fieldPaneSelectedPoint.prevSlopeMethod) {
                            Path2DPoint.SlopeMethod.SLOPE_SMOOTH -> slopeModeCombo.selectionModel.select("Smooth")
                            Path2DPoint.SlopeMethod.SLOPE_MANUAL -> slopeModeCombo.selectionModel.select("Manual")
                        }
                    }
                    Path2DPoint.PointType.NEXT_TANGENT -> {
                        xPosText.text = (fieldPaneSelectedPoint.nextTangent.x / PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                        yPosText.text = (fieldPaneSelectedPoint.nextTangent.y / PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                        angleText.text = (fieldPaneSelectedPoint.nextAngleAndMagnitude.x).format(1)
                        magnitudeText.text = (fieldPaneSelectedPoint.nextAngleAndMagnitude.y).format(2)
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                        slopeModeCombo.isDisable = false
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                        @Suppress("NON_EXHAUSTIVE_WHEN")
                        when (fieldPaneSelectedPoint.nextSlopeMethod) {
                            Path2DPoint.SlopeMethod.SLOPE_SMOOTH -> slopeModeCombo.selectionModel.select("Smooth")
                            Path2DPoint.SlopeMethod.SLOPE_MANUAL -> slopeModeCombo.selectionModel.select("Manual")
                        }
                    }
                }
            } else if (easePaneSelectedPoint != null) {
                pointsAndTangentsTitlePane.isExpanded = (EasePane.selectedPointType != Path2DPoint.PointType.POINT)
                deletePointButton.isDisable = (EasePane.selectedPointType != Path2DPoint.PointType.POINT)
                    when (EasePane.selectedPointType){
                    Path2DPoint.PointType.POINT -> {
                        xPosText.text = ""
                        yPosText.text = ""
                        angleText.text = ""
                        magnitudeText.text = ""
                        slopeModeCombo.selectionModel.select("None")
                    }
                    Path2DPoint.PointType.PREV_TANGENT -> {
                        xPosText.text = (easePaneSelectedPoint.prevTangent.x / -PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                        yPosText.text = (easePaneSelectedPoint.prevTangent.y / -PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                        angleText.text = (easePaneSelectedPoint.prevAngleAndMagnitude.x).format(1)
                        magnitudeText.text = (easePaneSelectedPoint.prevAngleAndMagnitude.y).format(2)
                        @Suppress("NON_EXHAUSTIVE_WHEN")
                        when (easePaneSelectedPoint.prevSlopeMethod) {
                            MotionKey.SlopeMethod.SLOPE_SMOOTH -> slopeModeCombo.selectionModel.select("Smooth")
                            MotionKey.SlopeMethod.SLOPE_MANUAL -> slopeModeCombo.selectionModel.select("Manual")
                        }
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                        slopeModeCombo.isDisable = false
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                    }
                    Path2DPoint.PointType.NEXT_TANGENT -> {
                        xPosText.text = (easePaneSelectedPoint.nextTangent.x / PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                        yPosText.text = (easePaneSelectedPoint.nextTangent.y / PathVisualizer.TANGENT_DRAW_FACTOR).format(2)
                        angleText.text = (easePaneSelectedPoint.nextAngleAndMagnitude.x).format(1)
                        magnitudeText.text = (easePaneSelectedPoint.nextAngleAndMagnitude.y).format(2)
                        @Suppress("NON_EXHAUSTIVE_WHEN")
                        when (easePaneSelectedPoint.nextSlopeMethod) {
                            MotionKey.SlopeMethod.SLOPE_SMOOTH -> slopeModeCombo.selectionModel.select("Smooth")
                            MotionKey.SlopeMethod.SLOPE_MANUAL -> slopeModeCombo.selectionModel.select("Manual")
                        }
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                        slopeModeCombo.isDisable = false
                        xPosText.isDisable = false
                        yPosText.isDisable = false
                    }
                }
            }
            val selectedPath = selectedPath ?: return
            pathLengthText.text = selectedPath.length.format(2)

        }
        refreshing = false
    }
    private fun standardizedTooltip (text: String) : Tooltip {
        val tooltip = Tooltip(text)
        tooltip.style = "-fx-font-size: 12";
        tooltip.showDelay = Duration(400.0)
        return tooltip
    }
    private fun setDrivetrainType() {
        val isSwerve = (autonomi.drivetrainParameters is SwerveParameters)
        trackWidthText.isDisable = isSwerve
        scrubFactorText.isDisable = isSwerve
        driveTrainTypeCombo.selectionModel.select(if (isSwerve) {"Swerve"} else {"Arcade"})
    }
    private fun updateDriveTrainParams(){
        var driveParams = autonomi.drivetrainParameters
        when(driveTrainTypeCombo.value) {
            "Arcade"-> {
                if (driveParams is ArcadeParameters) {
                   driveParams.trackWidth = trackWidth / 12.0
                   driveParams.scrubFactor = trackScrubFactor
                } else {
                    driveParams = ArcadeParameters(
                        trackWidth / 12.0,
                        trackScrubFactor,
                        leftFeedForwardCoefficient = 0.070541988198899,
                        leftFeedForwardOffset = 0.021428882425651,
                        rightFeedForwardCoefficient = 0.071704891069425,
                        rightFeedForwardOffset = 0.020459379452296
                    )
                }
            }
            "Swerve"-> {
                if (driveParams is SwerveParameters) {
                    driveParams.alignRobotToPath = false
                } else {
                    driveParams = SwerveParameters(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
                }
            }
        }
        autonomi.drivetrainParameters = driveParams
    }
    private fun updatePathWeaverDisplay() {
        if (pathWeaverFormat) {
            secondsText.style = "-fx-background-color: LightGrey;"
            secondsText.isEditable = false
            secondsText.text = selectedPath?.generateTrajectory(maxVelocity.feet.asMeters, maxAcceleration.feet.asMeters)?.totalTimeSeconds?.round(2).toString()
        } else {
            secondsText.style = "-fx-background-color: White;"
            secondsText.isEditable = true
            secondsText.text = selectedPath?.durationWithSpeed.toString()
        }
    }
}
