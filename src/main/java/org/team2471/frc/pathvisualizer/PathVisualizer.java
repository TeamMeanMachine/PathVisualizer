package org.team2471.frc.pathvisualizer;

import java.awt.*;
import org.team2471.frc.lib.motion_profiling.Path2D;
import org.team2471.frc.lib.motion_profiling.Path2DPoint;
import org.team2471.frc.lib.vector.Vector2;
import org.team2471.frc.lib.motion_profiling.SharedAutonomousConfig;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class PathVisualizer extends JPanel {

  private Path2D selectedPath = DefaultPath.INSTANCE;;
  private SharedAutonomousConfig selectedAutonomousConfig;

  private BufferedImage blueSideImage;
  private BufferedImage redSideImage;
  private JTextField zoomTextField;
  private JComboBox<String> sideSelection;
  private JComboBox<String> autoSelection;
  private JComboBox<String> pathSelection;

  private enum Sides{BLUE, RED}
  private Sides sides;
  private int circleSize = 10;
  private double zoom;
  final Vector2 offset = new Vector2( 295, 485);
  private final double tangentLengthDrawFactor = 3.0;
  Path2DPoint editPoint = null;
  Vector2 editVector = null;
  Path2DPoint selectedPoint = null;
  int pointType = 0;  // need an enum type

  public PathVisualizer() {
    setSize(1024, 768);
    zoom = 18;
    sides = Sides.BLUE;

    JFrame frame = new JFrame();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(this);
    frame.setVisible(true);
/*
    frame.addWindowListener(new WindowAdapter()
    {
      public void windowClosed(WindowEvent e)
      {
        System.exit(0); //calling the method is a must
      }
    });
*/

    class MyListener extends MouseInputAdapter {
      public void mousePressed(MouseEvent e) {
        Vector2 mouseVec = new Vector2(e.getX(), e.getY());
        double shortestDistance = 10000;
        Path2DPoint closestPoint = null;

        //Find closest point
        for (Path2DPoint point = selectedPath.getXYCurve().getHeadPoint(); point != null; point = point.getNextPoint()) {
          Vector2 tPoint = world2Screen(point.getPosition());
          double dist = Vector2.length(Vector2.subtract(tPoint, mouseVec));
          if (dist <= shortestDistance) {
            shortestDistance = dist;
            closestPoint = point;
            pointType = 0;
          }

          if (point.getPrevPoint() != null) {
            Vector2 tanPoint1 = world2Screen(Vector2.subtract(point.getPosition(), Vector2.multiply(point.getPrevTangent(), 1.0 / tangentLengthDrawFactor)));
            dist = Vector2.length(Vector2.subtract(tanPoint1, mouseVec));
            if (dist <= shortestDistance) {
              shortestDistance = dist;
              closestPoint = point;
              pointType = 1;
            }
          }

          if (point.getNextPoint() != null) {
            Vector2 tanPoint2 = world2Screen(Vector2.add(point.getPosition(), Vector2.multiply(point.getNextTangent(), 1.0 / tangentLengthDrawFactor)));
            dist = Vector2.length(Vector2.subtract(tanPoint2, mouseVec));
            if (dist <= shortestDistance) {
              shortestDistance = dist;
              closestPoint = point;
              pointType = 2;
            }
          }
          // find distance between point clicked and each point in the graph. Whichever one is the max gets to be assigned to the var.
        }
        if (shortestDistance <= circleSize / 2) {
          selectedPoint = closestPoint;
          editPoint = closestPoint;
        } else
          editVector = null;
      }

      public void mouseDragged(MouseEvent e) {
        if (editPoint != null) {
          Vector2 worldPoint = screen2World(new Vector2(e.getX(), e.getY()));
          switch (pointType) {
            case 0:
              editPoint.setPosition(worldPoint);
              break;
            case 1:
              editPoint.setPrevTangent(Vector2.multiply(Vector2.subtract(worldPoint, editPoint.getPosition()), -tangentLengthDrawFactor));
              break;
            case 2:
              editPoint.setNextTangent(Vector2.multiply(Vector2.subtract(worldPoint, editPoint.getPosition()), tangentLengthDrawFactor));
              break;
          }
          repaint();
        }
      }

      public void mouseReleased(MouseEvent e) {
        editPoint = null;  // no longer editing
      }
    }

    MyListener myListener = new MyListener();
    addMouseListener(myListener);
    addMouseMotionListener(myListener);

    try {
      ClassLoader classLoader = getClass().getClassLoader();
      blueSideImage = ImageIO.read(new File(classLoader.getResource("assets/HalfFieldDiagramBlue.png").getFile()));
      redSideImage = ImageIO.read(new File(classLoader.getResource("assets/HalfFieldDiagramRed.png").getFile()));
    } catch (Exception e) {
      e.printStackTrace();
    }

    JPanel toolBarPanel = new JPanel(new GridLayout(1, 7));
    String[] comboBoxNames = {"Blue Side", "Red Side"};
    sideSelection = new JComboBox<>(comboBoxNames);
    sideSelection.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        if (sideSelection.getSelectedIndex() == 0) {
          sides = Sides.BLUE;
        } else if (sideSelection.getSelectedIndex() == 1) {
          sides = Sides.RED;
        }
        repaint();
      }
    });

    // zoom out
    JButton decrementButton = new JButton("-");
    decrementButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        zoom--;
        repaint();
        zoomTextField.setText(Double.toString(zoom));
      }
    });

    // zoom in
    JButton incrementButton = new JButton("+");
    incrementButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        zoom++;
        repaint();
        zoomTextField.setText(Double.toString(zoom));
      }

    });

    // zoom
    zoomTextField = new JTextField(Double.toString(zoom));
    zoomTextField.setEditable(true);
    zoomTextField.addActionListener(e -> {
      try {
        zoom = Double.parseDouble(e.getActionCommand());
        repaint();
      } catch (NumberFormatException exception) {
        JOptionPane.showMessageDialog(PathVisualizer.this,
            "The P.M.P. Section III Act 111.16.11, which you have violated, dictates that you must send one" +
                " million dollars to the Prince of Nigeria or a jail sentence of 20 years of for-profit prison" +
                " will be imposed.", "Police Alert", JOptionPane.ERROR_MESSAGE);
      }
    });

    // Autonomi
    String[] autonomousNames;
    int numConfigs = SharedAutonomousConfig.Companion.getConfigNames().length;
    autonomousNames = new String[numConfigs + 1];
    for (int i = 0; i<numConfigs; i++) {
      autonomousNames[i] = SharedAutonomousConfig.Companion.getConfigNames()[i];
    }
    autonomousNames[numConfigs] = "New Auto";
    autoSelection = new JComboBox<>(autonomousNames);
    autoSelection.setSelectedIndex(-1);
    autoSelection.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        if (autoSelection.getSelectedIndex() == autoSelection.getItemCount() - 1) {
          String s = (String) JOptionPane.showInputDialog(
              null,
              "Autonomous Name:",
              "New Autonomous",
              JOptionPane.PLAIN_MESSAGE,
              null,
              null,
              "");

          if ((s != null) && (s.length() > 0)) {
            selectedAutonomousConfig = new SharedAutonomousConfig(s);
            autoSelection.insertItemAt(s, autoSelection.getItemCount() - 1);
            autoSelection.setSelectedIndex(autoSelection.getItemCount() - 2);
          }
        }
        else {  // any autonomous chosen
          selectedAutonomousConfig = new SharedAutonomousConfig(autoSelection.getSelectedItem().toString());
        }
        repaint();
      }
    });

    // paths
    String[] pathSelectionNames;
    int numPaths = selectedAutonomousConfig != null ? selectedAutonomousConfig.getPathNames().length : 0;
    pathSelectionNames = new String[numPaths + 1];
    for (int i = 0; i<numPaths; i++) {
      pathSelectionNames[i] = selectedAutonomousConfig.getPathNames()[i];
    }
    pathSelectionNames[numPaths] = "New Path";
    pathSelection = new JComboBox<>(pathSelectionNames);
    pathSelection.setSelectedIndex(-1);
    pathSelection.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        if (pathSelection.getSelectedIndex() == pathSelection.getItemCount() - 1) {
          String s = (String) JOptionPane.showInputDialog(
                  null,
                  "Path Name:",
                  "New Path",
                  JOptionPane.PLAIN_MESSAGE,
                  null,
                  null,
                  "");
          if ((s != null) && (s.length() > 0)) {
            pathSelection.insertItemAt(s, pathSelection.getItemCount() - 1);
            pathSelection.setSelectedIndex(pathSelection.getItemCount() - 2);
            if (selectedAutonomousConfig != null) {
              selectedAutonomousConfig.putPath(s, new Path2D());
            }
          }
        } else if (selectedAutonomousConfig != null) {  // any path chosen
          selectedPath = selectedAutonomousConfig.getPath(pathSelection.getSelectedItem().toString());
        }
        repaint();
      }
    });

    // add the tool bar items
    toolBarPanel.add(autoSelection);
    toolBarPanel.add(pathSelection);
    toolBarPanel.add(decrementButton);
    toolBarPanel.add(zoomTextField);
    toolBarPanel.add(incrementButton);
    toolBarPanel.add(sideSelection);

    add(toolBarPanel, BorderLayout.NORTH);
  }

  @Override
  public void paintComponent (Graphics g){
    Graphics2D g2 = (Graphics2D) g;
    super.paintComponent(g2);

    if (sides == Sides.BLUE) {
      g2.drawImage(blueSideImage, 0 - (int) ((zoom - 18) / 36 * blueSideImage.getWidth()),
          0 - (int) ((zoom - 18) / 18 * (blueSideImage.getHeight() - 29)),
          blueSideImage.getWidth() + (int) ((zoom - 18) / 18 * blueSideImage.getWidth()),
          (int) ((blueSideImage.getHeight() + 29) * zoom / 18), null);
    } else if (sides == Sides.RED) {
      g2.drawImage(redSideImage, 0 - (int) ((zoom - 18) / 36 * redSideImage.getWidth()),
          0 - (int) ((zoom - 18) / 18 * (redSideImage.getHeight() - 29)),
          redSideImage.getWidth() + (int) ((zoom - 18) / 18 * redSideImage.getWidth()),
          (int) ((redSideImage.getHeight() + 29) * zoom / 18), null);
    }

    if (selectedAutonomousConfig!=null) {
      int numPaths = selectedAutonomousConfig.getPathNames().length;
      for (int i = 0; i < numPaths; i++) {
        Path2D path2D = selectedAutonomousConfig.getPath(selectedAutonomousConfig.getPathNames()[i]);
        DrawPath(g2, path2D);
      }
    }
    DrawSelectedPath(g2, selectedPath);
  }

  
  private void DrawSelectedPath(Graphics2D g2, Path2D path2D) {
    double deltaT = path2D.getDuration() / 100.0;
    Vector2 prevPos = path2D.getPosition(0.0);
    Vector2 prevLeftPos = path2D.getLeftPosition(0.0);
    Vector2 prevRightPos = path2D.getRightPosition(0.0);
    Vector2 pos, leftPos, rightPos;
    final double MAX_SPEED = 8.0;
    for (double t = deltaT; t <= path2D.getDuration(); t += deltaT) {
      pos = path2D.getPosition(t);
      leftPos = path2D.getLeftPosition(t);
      rightPos = path2D.getRightPosition(t);
/*
      // center line
      g2.setColor(Color.white);
      drawPathLine(g2, prevPos, pos);
*/
      // left wheel
      double leftSpeed = Vector2.length(Vector2.subtract(leftPos, prevLeftPos)) / deltaT;
      leftSpeed /= MAX_SPEED;  // MAX_SPEED is full green, 0 is full red.
      leftSpeed = Math.min(1.0, leftSpeed);
      double leftDelta = path2D.getLeftPositionDelta(t);
      if (leftDelta>0)
        g2.setColor(new Color((int) ((1.0 - leftSpeed) * 255), (int) (leftSpeed * 255), 0));
      else {
        g2.setColor(new Color(0, 0, 255)); //(int)blue));
      }
      drawPathLine(g2, prevLeftPos, leftPos);

      // right wheel
      double rightSpeed = Vector2.length(Vector2.subtract(rightPos, prevRightPos)) / deltaT / MAX_SPEED;
      rightSpeed = Math.min(1.0, rightSpeed);
      double rightDelta = path2D.getRightPositionDelta(t);
      if (rightDelta>0)
        g2.setColor(new Color((int) ((1.0 - rightSpeed) * 255), (int) (rightSpeed * 255), 0));
      else {
        g2.setColor(new Color(0, 0, 255)); //(int)blue));
      }
      drawPathLine(g2, prevRightPos, rightPos);

      // set the prevs for the next loop
      prevPos.set(pos.x, pos.y);
      prevLeftPos.set(leftPos.x, leftPos.y);
      prevRightPos.set(rightPos.x, rightPos.y);
    }

    // circles and lines for handles
    for(Path2DPoint point = path2D.getXYCurve().getHeadPoint(); point != null; point = point.getNextPoint()) {
      if (point==selectedPoint && pointType==0)
        g2.setColor(Color.green);
      else
        g2.setColor(Color.white);

      Vector2 tPoint = world2Screen(point.getPosition());
      g2.drawOval(((int)tPoint.x - circleSize/2),((int)tPoint.y - circleSize/2), circleSize,circleSize);
      if(point.getPrevPoint() != null) {
        if (point==selectedPoint && pointType==1)
          g2.setColor(Color.green);
        else
          g2.setColor(Color.white);
        Vector2 tanPoint = world2Screen(Vector2.subtract(point.getPosition(), Vector2.multiply(point.getPrevTangent(),1.0/tangentLengthDrawFactor)));
        g2.drawOval(((int)tanPoint.x - circleSize/2),((int)tanPoint.y - circleSize/2), circleSize,circleSize);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine((int)tPoint.x,(int)tPoint.y,(int)tanPoint.x,(int)tanPoint.y);
      }
      if(point.getNextPoint() != null) {
        if (point==selectedPoint && pointType==2)
          g2.setColor(Color.green);
        else
          g2.setColor(Color.white);
        Vector2 tanPoint = world2Screen(Vector2.add(point.getPosition(), Vector2.multiply(point.getNextTangent(),1.0/tangentLengthDrawFactor)));
        g2.drawOval(((int)tanPoint.x - circleSize/2),((int)tanPoint.y - circleSize/2), circleSize,circleSize);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine((int)tPoint.x,(int)tPoint.y,(int)tanPoint.x,(int)tanPoint.y);
      }
    }
// draw the ease curve
//    double prevEase = 0.0;
//    g2.setStroke(new BasicStroke(3));
//    for (double t = deltaT; t <= path2D.getDuration(); t += deltaT) {
//      // draw the ease curve too
//      g2.setColor(Color.black);
//      double ease = path2D.getEaseCurve().getValue(t);
//      double prevT = t - deltaT;
//      g2.drawLine((int) (prevT * 40 + 100), (int) (prevEase * -200 + 700), (int) (t * 40 + 100), (int) (ease * -200 + 700));
//      prevEase = ease;
//    }
  }

  private void DrawPath(Graphics2D g2, Path2D path2D) {
    double deltaT = path2D.getDuration() / 100.0;
    Vector2 prevPos = path2D.getPosition(0.0);
    Vector2 pos;

    g2.setColor(Color.white);
    for (double t = deltaT; t <= path2D.getDuration(); t += deltaT) {
      pos = path2D.getPosition(t);

      // center line
      drawPathLine(g2, prevPos, pos);
      prevPos.set(pos.x, pos.y);
    }
  }

  private void drawPathLine( Graphics2D g2, Vector2 p1, Vector2 p2 ) {

    Vector2 tp1 = world2Screen(p1);
    Vector2 tp2 = world2Screen(p2);

    g2.drawLine( (int)(tp1.x), (int)(tp1.y), (int)(tp2.x), (int)(tp2.y) );
  }

  private Vector2 screen2World(Vector2 point){

    double xFlip = 1.0;
    if(sides == Sides.RED){
      xFlip = -1.0;
    }
    Vector2 result = new Vector2 ((point.x-offset.x)/xFlip/zoom, (point.y-offset.y)/-zoom);
    return result;
  }

  private Vector2 world2Screen(Vector2 point){

    double xFlip = 1.0;
    if(sides == Sides.RED){
      xFlip = -1.0;
    }
    Vector2 result = new Vector2 (point.x*xFlip*zoom + offset.x, point.y*-zoom + offset.y);
    return result;
  }
}



