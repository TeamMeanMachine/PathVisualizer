# PathVisualizer
Interactive spline editing program for FRC path following

For autonomous, we have all of these features, plus we add smooth path following for driving around without the aid of a human operator.

Here is a picture of our path visualizer, it is written in Kotlin using JavaFX, and shares the same meanlib path code which is used by our robot.  Meanlib is still written in Java, though we’re likely to change it to Kotlin over the summer.



In this picture, the selected autonomous displays all centerline paths as black at the start, white at the end.  The selected path is shown with wheel paths.  These indicate color which varies with speed.  Red is slow, green is fast forward, blue is fast backwards.  The control points and tangent points are also drawn for the selected path.  They can be adjusted with the mouse, arrow keys, or type into the edit boxes.  

The reddish curve at the bottom represents the ease along the path.  It shows how the robot starts and ends slow, but is faster in the middle.

Meanlib is available on our github account.  We have been using it for the last year and a half or so.  There is example code from 2017 plus the past two bunny bot competitions.  We will release our 2018 robot code when our competitions are over.  

Our path library has a few unique features:
It has animation curves for controlling arms and other motors simultaneously via keyframes.
The path curves (just the points, tangents and properties) are sent via a json blob over the network tables directly to the robot, they are cached there on eprom for future use.  This file is human readable, so it can be manually edited if necessary.
The curves are turned into path points on the fly in realtime on the RIO as the path is being driven using Fast Forward Differencing (FFD) to perform a single sub step along the path with just 4 floating point add operations.
The interactive path visualizer can visualize your paths directly on a field drawing.  It is expanding all the time, but still has a long to do list.  It allows very rapid tuning.

Unfortunately, our libraries are not yet well documented, but if you don't mind following some code trails, some links are here: 

https://github.com/TeamMeanMachine/meanlib

https://github.com/TeamMeanMachine/FRC2017

We use the new auxiliary feed forward feature that is now standard in the Talon SRX motor controller..  It works really well, and allowed us to go back to position control, and still get velocity or even acceleration feed forwards added in trivially.  We tested these estimated feed forwards by clearing out the SRX position constants, which essentially allowed us to observe how close our curves were by running them open loop.  Then you add the closed loop back and the robot is just more responsive.  We did try using speed control as well, but found the position was a bit easier to tune.

Much experimentation and improvement has happened with the motor side of the path following since Wilsonville, (our first district event, where we used SRX PD position control on each side). We tried several new architectures, including something much like team 254’s path following setup (minus the pure pursuit). What we settled on for now is a gyro PI controller that adjusts the set points of SRX PD position controllers with velocity and turning scrub feed forwards.

We use the Analog Devices gyro: ADIS16448 FRC IMU.  They are also a team sponsor.
