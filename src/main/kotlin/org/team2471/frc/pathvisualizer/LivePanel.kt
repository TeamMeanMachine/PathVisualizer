package org.team2471.frc.pathvisualizer

import com.google.gson.Gson
import edu.wpi.first.networktables.EntryListenerFlags
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Slider
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import kotlinx.coroutines.*
import org.team2471.frc.lib.util.Timer
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.HashMap


object LivePanel : VBox() {
    private val viewActiveRobotCheckBox = CheckBox("Odometry Robot")
    private val viewLimelightRobot = CheckBox("Limelight Robot")
    private val viewParralaxCheckBox = CheckBox("Parralax")
    private val viewRecordingPathCheckBox = CheckBox("Recording")
    private val recordButton = Button("Start Recording")
    private val txtRecordTime = Text("00:00")
    private val playbackSlider = Slider(0.0, 100.0, 0.0)
    private val playButton = Button("Start Playback")
    private val selectRecordingForPlayback = ChoiceBox<String>()
    private val selectAuto = ChoiceBox<String>()
    private val selectAutoTest = ChoiceBox<String>()
    private val savePath =  System.getProperty("user.dir") + "/../"
    private var files  = LinkedHashMap<String,String>()
    var currRecording : RobotRecording? = null
    val smartDashboardTable = ControlPanel.networkTableInstance.getTable("SmartDashboard")
    val autosTable = smartDashboardTable.getSubTable("Autos")
    val autoTestsTable = smartDashboardTable.getSubTable("Tests")
    val recording_lookup = HashMap<String, String>()
    var playBackTime = 0.0
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
        viewRecordingPathCheckBox.setOnAction{
            FieldPane.displayRecording = viewRecordingPathCheckBox.isSelected
            FieldPane.draw()
        }

        playbackSlider.valueProperty().addListener { _, _, newValue ->
            // adjust current time for playback
            playBackTime = newValue.toDouble()
            println("set time to ${playBackTime}")
        }

        playButton.setOnAction {
            val willPlay = !FieldPane.playing
            playButton.text = if (willPlay) "Stop Playback" else "Start Playback"

            // redraw screen in case we removed the arbitrary bot
            FieldPane.draw()

            if (willPlay) {
                var animationJob: Job? = null

                if (currRecording != null && currRecording!!.recordings.size > 0) {
                    val playbackRecordings = currRecording!!.recordings
                    animationJob?.cancel()

                    val timer = Timer()

                    animationJob = GlobalScope.launch {
                            for (recording in playbackRecordings) {
                                playbackSlider.value = 0.0
                                timer.start()

                                while (timer.get() < (FieldPane.selectedPath?.durationWithSpeed ?: 0.0)) {
                                    if (!isActive) return@launch

                                    Platform.runLater {
                                        ControlPanel.currentTime = timer.get()
                                        FieldPane.draw()
                                        ControlPanel.refresh()
                                    }

                                    // Playback @ approx 30fps (1000ms/30fps = 33ms)
                                    delay(1000L / 30L)
                                }
                            }
                            if (FieldPane.selectedPath != null) {
                                Platform.runLater { ControlPanel.currentTime = FieldPane.selectedPath!!.durationWithSpeed }
                            }
                        }
                    }



                currRecording?.recordings?.forEach {
                    println("recording: x: ${it.x} y: ${it.y} h:${it.h} ts: ${it.ts}")
                }
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

        playbackSlider.valueProperty().addListener { _, _, newValue ->
            newSliderSelected(newValue.toDouble())
        }

        refreshRecordingsList()
        children.addAll(
                viewActiveRobotCheckBox,
                viewLimelightRobot,
                viewParralaxCheckBox,
                viewRecordingPathCheckBox,
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
        var files2 = folder.list()
                .filter{it.startsWith("pathVisualizer_") && it.endsWith(".json")}
                .map{
                    val arrParts = it.replace("pathVisualizer_", "")
                            .replace(".json", "").split("_")
                    var pathName = arrParts[0]
                    var timeStamp = arrParts[arrParts.size-1]
                    var testName = if (arrParts.size == 3) {" : " + arrParts[1]} else ""
                    convertLongToTime(timeStamp.toLong()*1000) + " ($pathName$testName)"
                }

        for(recordingfill in folder.list().filter{it.startsWith("pathVisualizer_") && it.endsWith(".json")}){
            val arrParts = recordingfill.replace("pathVisualizer_", "")
                    .replace(".json", "").split("_")
            var pathName = arrParts[0]
            var timeStamp = arrParts[arrParts.size-1]
            var testName = if (arrParts.size == 3) {" : " + arrParts[1]} else ""
            val longTime = convertLongToTime(timeStamp.toLong()*1000) + " ($pathName$testName)"
            recording_lookup.set(longTime, recordingfill)
        }
        files.clear()
        folder.listFiles().forEach {
           if (it.name.startsWith("pathVisualizer_") && it.name.endsWith(".json")) {

               val arrParts = it.name.toString().replace("pathVisualizer_", "")
                       .replace(".json", "").split("_")
               val pathName = arrParts[0]
               val timeStamp = arrParts[arrParts.size-1]
               val testName = if (arrParts.size == 3) {" : " + arrParts[1]} else ""
               files[convertLongToTime(timeStamp
                       .toLong()*1000) + " ($pathName$testName)"] = it.path.toString()
            }
        }

        selectRecordingForPlayback.items.addAll(files.keys.sorted().reversed())
        if (selectRecordingForPlayback.items.count() > 0) {
            selectRecordingForPlayback.setOnAction {
                loadRecording()
            }
            selectRecordingForPlayback.selectionModel.selectFirst()
            selectRecordingForPlayback.setOnAction {
                playbackSlider.value = 0.0
                try {
                    val fileReader = FileReader("${files[selectRecordingForPlayback.value]}")
                    println("new playback selected: ${files[selectRecordingForPlayback.value]}")
                    currRecording = Gson().fromJson(fileReader, RobotRecording::class.java)
                    println("loaded ${currRecording?.name} auto")
                    if (currRecording?.recordings != null && currRecording!!.recordings.size > 0) {
                        val recs = currRecording!!.recordings
                        playbackSlider.isDisable = false
                        playbackSlider.min = 0.0
                        playbackSlider.max = (recs[recs.size-1].ts.asSeconds - recs[0].ts.asSeconds)
                        println("set slider playback to ${playbackSlider.max} seconds")
                        playbackSlider.value = 0.0
                    } else {
                        playbackSlider.isDisable = true
                    }
                } catch (ex: Exception) {
                    println("Exception encountered when opening json: $ex")
                }
            }
        }
    }
    fun loadRecording() {
        val currRecording = selectRecordingForPlayback.value
        if (currRecording != null) {
            val filename = recording_lookup[currRecording]
            val jsonKjkjed = File("$savePath$filename" ).readLines()
            println (jsonKjkjed)

            println("recording: ${selectRecordingForPlayback.value} $filename")
            val startTime = Instant.ofEpochMilli(1629255631201)
            val endTime = Instant.ofEpochMilli(1629255633712)
            endTime.minusMillis(startTime.toEpochMilli())
            val timeDifference = endTime.minusMillis(startTime.toEpochMilli())
            println(timeDifference.toEpochMilli())
            playbackSlider.max = timeDifference.toEpochMilli().toDouble()
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
fun newSliderSelected(sliderValue: Double) {
  println("slider value: $sliderValue")
}
}