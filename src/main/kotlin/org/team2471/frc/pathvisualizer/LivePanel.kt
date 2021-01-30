package org.team2471.frc.pathvisualizer

import edu.wpi.first.networktables.EntryListenerFlags
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Slider
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import java.io.File
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.*


object LivePanel : VBox() {
    private val viewActiveRobotCheckBox = CheckBox("Odometry Robot")
    private val viewLimelightRobot = CheckBox("Limelight Robot")
    private val viewParralaxCheckBox = CheckBox("Parralax")
    private val recordButton = Button("Start Recording")
    private val txtRecordTime = Text("00:00")
    private val playbackSlider = Slider(0.0, 100.0, 0.0)
    private val playButton = Button("Playback")
    private val selectRecordingForPlayback = ChoiceBox<String>()
    private val selectAuto = ChoiceBox<String>()
    private val selectAutoTest = ChoiceBox<String>()
    private val savePath =  System.getProperty("user.dir") + "/../"
    val smartDashboardTable = ControlPanel.networkTableInstance.getTable("SmartDashboard")
    val autosTable = smartDashboardTable.getSubTable("Autos")
    val autoTestsTable = smartDashboardTable.getSubTable("Tests")

    init {
        spacing = 10.0
        padding = Insets(10.0, 10.0, 10.0, 10.0)

        smartDashboardTable.getEntry("autoStatus").addListener( { event ->
            println("saw status change in autoStatus ${event.value.string}")
            // set recording to start or complete on status change
            FieldPane.recording = (event.value.string == "init")
        }, EntryListenerFlags.kImmediate or EntryListenerFlags.kNew or EntryListenerFlags.kUpdate)

        viewActiveRobotCheckBox.setOnAction{
            FieldPane.displayActiveRobot = viewActiveRobotCheckBox.isSelected
        }
        viewLimelightRobot.setOnAction {
            FieldPane.displayLimeLightRobot = viewLimelightRobot.isSelected
        }
        viewParralaxCheckBox.setOnAction {
            FieldPane.displayParallax = viewLimelightRobot.isSelected
        }

        playButton.setOnAction {
            val willPlay = !FieldPane.playing
            playButton.text = if (willPlay) "Stop Playback" else "Start Playback"

            // redraw screen in case we removed the arbitrary bot
            FieldPane.draw()

            if (willPlay) {
                val selectedRecording = selectRecordingForPlayback.value
                val selectedRecordingTS = selectedRecording.split(" : ").first()
                println("starting playback for ${selectedRecordingTS}")
                
            }
            FieldPane.playing = willPlay
        }

        recordButton.setOnAction {
            FieldPane.recording = !FieldPane.recording
            recordButton.text = if (FieldPane.recording) "Stop Recording" else "Start Recording"
            if (!FieldPane.recording) {
                // recording ended . refresh list of recordings
                refreshRecordingsList()
            }
            // redraw screen in case we removed the arbitrary bot
            FieldPane.draw()
        }



        refreshRecordingsList()
        children.addAll(
                viewActiveRobotCheckBox,
                viewLimelightRobot,
                viewParralaxCheckBox,
                recordButton,
                txtRecordTime,
                playButton,
                selectRecordingForPlayback,
                playbackSlider,
                selectAuto,
                selectAutoTest
        )
    }
    fun refresh(){
        setValues()
        loadAutosList()
        refreshRecordingsList()
    }
    fun setValues(){
        viewParralaxCheckBox.isSelected = FieldPane.displayParallax
        viewLimelightRobot.isSelected = FieldPane.displayLimeLightRobot
        viewActiveRobotCheckBox.isSelected = FieldPane.displayActiveRobot
    }
    fun loadAutosList(){
        selectAuto.setOnAction{}
        selectAutoTest.setOnAction {  }
        val autoOptionString = autosTable.getEntry("options").getStringArray(null)
        val selectedEntry = autosTable.getEntry("selected").getString("")

        val autoTestOptions = autoTestsTable.getEntry("options").getStringArray(null)
        val selectedTest = autoTestsTable.getEntry("selected").getString("")
        val selectedTestDefault = autoTestsTable.getEntry("default").getString("")

        if (autoOptionString != null) {
            selectAuto.items.clear()
            selectAuto.items.addAll(autoOptionString)
        }

        if (autoTestOptions != null) {
            selectAutoTest.items.clear()
            selectAutoTest.items.addAll(autoTestOptions)
            if (selectedTest.isNotEmpty()) {
                selectAutoTest.selectionModel.select(selectedTest)
                println("autos test selected = $selectedTest")
            } else if (selectedTestDefault.isNotEmpty()) {
                selectAutoTest.selectionModel.select(selectedTestDefault)
                println("autos test default = $selectedTestDefault")
            }
            selectAutoTest.setOnAction {
                println("Selected new auto test: ${selectAutoTest.value}")
                autoTestsTable.getEntry("selected").setString(selectAutoTest.value)
            }
        }

        if (selectedEntry.isNotEmpty()) {
            selectAuto.selectionModel.select(selectedEntry)
            selectAutoTest.isVisible = (selectedEntry == "Tests")
            selectAuto.setOnAction {
                println("Selected a new auto: ${selectAuto.value}")
                selectAutoTest.isVisible = (selectedEntry == "Tests")
                autosTable.getEntry("selected").setString(selectAuto.value)
            }
        }
    }

    fun refreshRecordingsList(){
        selectRecordingForPlayback.items.clear()
        val folder = File(savePath)
        var files = folder.list()
                .filter{it.startsWith("pathVisualizer_") && it.endsWith(".json")}
                .map{
                    val arrParts = it.replace("pathVisualizer_", "")
                            .replace(".json", "").split("_")
                    var pathName = arrParts[0]
                    var timeStamp = arrParts[arrParts.size-1]
                    var testName = if (arrParts.size == 3) {" : " + arrParts[1]} else ""
                    convertLongToTime(timeStamp
                                .toLong()*1000) + " ($pathName$testName)"
                }

        selectRecordingForPlayback.items.addAll(files.sorted().reversed())
        if (selectRecordingForPlayback.items.count() > 0) {
            selectRecordingForPlayback.selectionModel.selectFirst()
        }
    }
    fun convertLongToTime(time: Long): String {

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        val format = SimpleDateFormat("MM.dd HH:mm:ss")
        return format.format(calendar.time)
    }

    fun getCurrentAuto() : String {
        val selAuto = selectAuto.value
        val testValue = if (selAuto == "Tests") {"_" + selectAutoTest.value } else ""
        return selAuto + testValue
    }

}