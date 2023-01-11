package org.team2471.frc.pathvisualizer

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.input.DragEvent
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.text.Font
import javafx.stage.Screen
import javafx.stage.Stage
import java.util.prefs.Preferences
class PathVisualizer : Application() {

    companion object {
        // drawing
        const val DRAW_CIRCLE_SIZE = 10.0
        const val CLICK_CIRCLE_SIZE = 15.0
        const val TANGENT_DRAW_FACTOR = 3.0

        lateinit var stage: Stage
        lateinit var fontAwesome : Font
        val pref = Preferences.userNodeForPackage(::PathVisualizer.javaClass)
        var windowResizing = false
        val verticalSplitPane = SplitPane()
        @JvmStatic
        fun main(args: Array<String>) {
            launch(PathVisualizer::class.java, *args)
        }
    }

    enum class MouseMode {
        EDIT, PAN, ADD, DRAG_TIME
    }

    override fun start(stage: Stage) {
        fontAwesome = Font.loadFont(Thread.currentThread().contextClassLoader.getResourceAsStream("assets/fAwesome6FreeSolid.otf"), 16.0)
        stage.title = "Path Visualizer"
        PathVisualizer.stage = stage

        // setup the layout
        verticalSplitPane.items.addAll(FieldPane, EasePane)
        verticalSplitPane.orientation = Orientation.VERTICAL
        verticalSplitPane.setDividerPositions(0.85)

        val pathScrollPane = ScrollPane(ControlPanel)
        pathScrollPane.hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        pathScrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED

        val liveScrollPane = ScrollPane(LivePanel)
        liveScrollPane.hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        liveScrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED

        val undoScrollPane = ScrollPane(UndoPanel)
        undoScrollPane.hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        undoScrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED

        val tabPane = TabPane()
        val tabScroll = Tab("Path Editing")
        tabScroll.content = pathScrollPane
        tabScroll.isClosable = false

        val tabLive = Tab("Live View")
        tabLive.isClosable = false
        tabLive.content = liveScrollPane
        tabLive.setOnSelectionChanged {
            TopBar.toggleVisualizeActiveRobot()
            (liveScrollPane.content as LivePanel).refresh()
        }

        val tabUndo = Tab("Undo List")
        tabUndo.isClosable = false
        tabUndo.content = undoScrollPane
        tabUndo.setOnSelectionChanged {
            (undoScrollPane.content as UndoPanel).refresh()
        }
        tabPane.tabs.addAll(tabScroll, tabLive, tabUndo)

        tabPane.addEventFilter(KeyEvent.ANY) { event: KeyEvent ->
            if (event.code.isArrowKey && event.target === tabPane) {
                event.consume()
            }
        }

        val horizontalSplitPane = SplitPane(verticalSplitPane, tabPane)
        val preferredPanelSize = minOf(pref.getDouble("horizontalPanelPercent", 0.68), .95)
        horizontalSplitPane.setDividerPositions(preferredPanelSize)
        horizontalSplitPane.dividers[0].positionProperty().addListener {  _, oldPos, newPos -> dividerResized(oldPos, newPos) }

        val borderPane = BorderPane(horizontalSplitPane)
        borderPane.top = TopBar

        val screen = Screen.getPrimary()

        //There is a bug where the default height of the stage leaks under the tool bar in windows 10
        val bounds = Rectangle2D(screen.visualBounds.minX, screen.visualBounds.minY, screen.visualBounds.width, screen.visualBounds.height - 30)
        stage.scene = Scene(borderPane, bounds.width, bounds.height)
        stage.scene.stylesheets.add("assets/theme.css");
        stage.scene.addPreLayoutPulseListener {
            windowResizing = true
            Platform.runLater {
                windowResizing = false
            }
        }
        FieldPane.draw()
        stage.sizeToScene()
        stage.isMaximized = true
        stage.show()
        ControlPanel.refresh()
        LivePanel.refresh()
        FieldPane.zoomFit()
    }
    private fun dividerResized(
        oldPos: Number,
        newPos: Number
    ) {
        if (windowResizing) {
            return
        }
        pref.putDouble("horizontalPanelPercent", newPos.toDouble())
       // println("User moved divider position to $newPos from $oldPos")
    }
    private fun horizontalSplitDone(dragEvent: DragEvent?) {
        println("drag is done")
        println(dragEvent?.sceneX)
    }

}

// todo list  //////////////////////////////////////////////////////////////////////////////////////////////////////

// : mouse routines - down, move, up
// : edit boxes respond - zoom, and pan
// : investigate why mirrored is not working
// : try layoutpanel for making buttons follow size of window on right - used splitpane and resizable
// : get path combo working
// : handle cancel on new dialogs
// : generate unique name for Auto and Path
// : new path draws blank
// : get autonomous combo working
// : delete point button
// : add path properties - ...
// : mirrored,
// : speed,
// : travel direction,
// : robot width, length, fudgefactor
// : change the displayed value of robot width and length to inch (it is currently in feet)
// : mirrored,
// : speed,
// : travel direction,
// : robot width, length, fudgefactor
// : convert robot width and length to inches - Duy
// : save to file, load from file
// : save to network tables for pathvisualizer
// : load from network tables for robot
// : add text box for team number or ip
// : pan with mouse with a pan button or middle mouse button -- Julian
// : zoom with the mouse wheel -- Julian
// : make a separate and larger radius for selecting points compared to drawing them
// : edit box for duration of path,
// : support mirror state of the path (where?  in getPosition() or in worldToScreen()?)
// : fix robotDirection and speed properties
// : rename robotWidth in path to trackWidth, add trackWidth and robotWidth to Autonomous for drawing
// : set initial folder to the output folder for open and save
// : change path combo to a list box
// : invert pinch zooming - Julian
// : add edit boxes for x, y coordinate of selected point and magnitude and angle of tangent points
// : add combo box for tangent modes of selected point
// : arrow keys to nudge selected path points
// : upres or repaint a new high res field image
// : add path length field for measuring the field drawing, etc...
// : draw ease curve in bottom panel, use another SplitPane horizontal
// : remember last loaded/saved file in registry and automatically load it at startup
// : New field drawing 2019 - thanks SERT
// : add delete buttons beside auto and path for deleting them - James
// : add rename button beside auto and path combos to edit their names - Qui and Jonah
// : playback of robot travel
// : Be able to type heading of robot
// : Add buttons for new auto and new path, remove list items - Jonah
// : playback of entire autonomous
// : top bar - Ryan
// : add a file and edit menu for open, save, save as, undo and redo

// : Be able to turn Robot heading on field
// : Scrolling on Control Panel
// : Have an indicator for the front of the robot
// : make tangent adjustment points a different color than the point

// todo: Be able to create wheel paths for swerves - use swerve modules
// todo: Be able to add a point anywhere on a path by clicking on it
// todo: editing of ease curve and heading curve - Julian
// todo: Ability to add heading curve plus other curves for the robot to the graph and edit them

// todo: navigation for graph panel - pan and zoom
// todo: clicking on path should select it
// todo: make an add mode for adding a new point to a path or graph

// todo: add partner1 and partner2 auto combos - draw cyan, magenta, yellow
// todo: multi-select path points by dragging selecting with dashed rectangle
// todo: add pause and turn in place path types (actions)
// todo: undo operations - Ryan

// todo: create robot and derivatives for abstraction of drive trains - arcade, swerve, curvature, mecanum, kiwi

// todo: figure out why when there is no file, selectedPath is null and visualizer crashes
// todo: fix Unrecognized PGCanvas token: 68 (use Platform.runLater() to execute UI updates on Application thread)

