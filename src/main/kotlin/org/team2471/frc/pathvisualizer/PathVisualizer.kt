package org.team2471.frc.pathvisualizer

import javafx.application.Application
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Screen
import javafx.stage.Stage
import org.team2471.frc.lib.motion_profiling.*

class PathVisualizer : Application() {

    companion object {
        // drawing
        const val DRAW_CIRCLE_SIZE = 10.0
        const val CLICK_CIRCLE_SIZE = 15.0
        const val TANGENT_DRAW_FACTOR = 3.0

        lateinit var stage: Stage

        @JvmStatic
        fun main(args: Array<String>) {
            launch(PathVisualizer::class.java, *args)
        }
    }

    enum class PointType {
        POINT, PREV_TANGENT, NEXT_TANGENT
    }

    enum class MouseMode {
        EDIT, PAN
    }

    override fun start(stage: Stage) {
        stage.title = "Path Visualizer"
        PathVisualizer.stage = stage

        // set up the paths and autos
        val testAuto = Autonomous("Tests")
        ControlPanel.autonomi.put(testAuto)
        testAuto.putPath(EightFootStraight)
        testAuto.putPath(EightFootCircle)
        testAuto.putPath(FourFootCircle)
        testAuto.putPath(TwoFootCircle)
        testAuto.putPath(AngleAndMagnitudeBug)

        // setup the layout
        val verticalSplitPane = SplitPane(FieldPane, EasePane)
        verticalSplitPane.orientation = Orientation.VERTICAL
        verticalSplitPane.setDividerPositions(0.85)

        val horizontalSplitPane = SplitPane(verticalSplitPane, ControlPanel)
        horizontalSplitPane.setDividerPositions(0.68)

        val screen = Screen.getPrimary()
        val bounds = screen.visualBounds

        stage.scene = Scene(horizontalSplitPane, bounds.width, bounds.height)
        FieldPane.draw()
        stage.sizeToScene()
        stage.isMaximized = true
        FieldPane.zoom = FieldPane.width / PixelsToFeet(FieldPane.image.width) // zoom fit
        FieldPane.offset.x = -FieldPane.upperLeftOfFieldPixels.x
        stage.show()
        ControlPanel.refresh()
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
// : rename robotWidth in path to trackWidth, add robotLength and robotWidth to Autonomous for drawing
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

// todo: playback of robot travel
// todo: editing of ease curve and heading curve - Julian
// todo: Be able to type heading of robot
// todo: Be able to turn Robot heading on field
// todo: Be able to create wheel paths for swerves - use swerve modules

// todo: navigation for graph panel
// todo: place path duration in bottom corner of ease canvas using StackPane
// todo: place edit box for magnitude of ease curve - just share the same one for points
// todo: add edit box for what speed is colored maximum green
// todo: clicking on path should select it
// todo: make an add mode for adding a new point to a path

// todo: add partner1 and partner2 auto combos - draw cyan, magenta, yellow
// todo: multi-select path points by dragging selecting with dashed rectangle
// todo: add pause and turn in place path types (actions)
// todo: decide what properties should be saved locally and save them to the registry or local folder

// todo: create robot and derivatives for abstaction of drive trains - arcade, curvature, swerve, mecanum, kiwi
