package org.team2471.frc.pathvisualizer;

import java.awt.*;
import org.team2471.frc.lib.motion_profiling.Path2D;
import org.team2471.frc.lib.motion_profiling.Path2DPoint;
import org.team2471.frc.lib.vector.Vector2;
import org.team2471.frc.motion_profiling.SharedAutonomousConfig;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Vector;


public class PathVisualizer extends JPanel{

  private Path2D m_path = DefaultPath.INSTANCE;
  private BufferedImage blueSideImage;
  private BufferedImage redSideImage;
  private JTextField scaleTextField;
  private JComboBox<String> sideSelection;
  private JComboBox<String> autoSelection;
  private JComboBox pathSelection;
  private String currentAutonomous;
  //Map<String, Path2D> map;

  private enum Sides{BLUE, RED}
  private Sides sides;
  private double scale;
  private int circleSize = 10;
  private ArrayList sharedAutonomousConfigsList;
  final double xOffset = 295;
  final double yOffset = 485;
  private final double tangentLengthDrawFactor = 3.0;
  Path2DPoint editPoint = null;
  Vector2 editVector = null;
  Path2DPoint selectedPoint = null;
  int winner = 0;  // need an enum type

  public PathVisualizer() {
    sharedAutonomousConfigsList = new ArrayList();
    setSize(1024, 768);
    scale = 18;
    sides = Sides.BLUE;

    class MyListener extends MouseInputAdapter {

      public void testPoint( Vector2 point ) {

      }

      public void mousePressed(MouseEvent e) {
        Vector2 mouseVec = new Vector2(e.getX(), e.getY());
        double shortestDistance = 10000;
        Path2DPoint closestPoint = null;

        //Find closest point
        for(Path2DPoint point = m_path.getXYCurve().getHeadPoint(); point != null; point = point.getNextPoint()) {
          Vector2 tPoint = world2Screen(point.getPosition());
          double dist = Vector2.length(Vector2.subtract(tPoint, mouseVec));
          if(dist <= shortestDistance){
            shortestDistance = dist;
            closestPoint = point;
            winner = 1;
          }

          if(point.getPrevPoint() != null) {
            Vector2 tanPoint1 = world2Screen(Vector2.subtract(point.getPosition(), Vector2.multiply(point.getPrevTangent(),1.0/tangentLengthDrawFactor)));
            dist = Vector2.length(Vector2.subtract(tanPoint1, mouseVec));
            if(dist <= shortestDistance){
              shortestDistance = dist;
              closestPoint = point;
              winner = 2;
            }
          }

          if(point.getNextPoint() != null) {
            Vector2 tanPoint2 = world2Screen(Vector2.add(point.getPosition(), Vector2.multiply(point.getNextTangent(),1.0/tangentLengthDrawFactor)));
            dist = Vector2.length(Vector2.subtract(tanPoint2, mouseVec));
            if(dist <= shortestDistance){
              shortestDistance = dist;
              closestPoint = point;
              winner = 3;
            }
          }
          // find distance between point clicked and each point in the graph. Whichever one is the max gets to be assigned to the var.
        }
        if(shortestDistance <= circleSize/2 ){
          selectedPoint = closestPoint;
          editPoint = closestPoint;
        }
        else
          editVector = null;
      }

      public void mouseDragged(MouseEvent e) {
        if(editPoint != null) {
          Vector2 worldPoint = screen2World(new Vector2(e.getX(), e.getY()));
          switch (winner) {
            case 1:
              editPoint.setPosition( worldPoint );
              break;
            case 2:
              editPoint.setPrevTangent( Vector2.multiply( Vector2.subtract(worldPoint, editPoint.getPosition()), -tangentLengthDrawFactor ));
              break;
            case 3:
              editPoint.setNextTangent( Vector2.multiply( Vector2.subtract(worldPoint, editPoint.getPosition()), tangentLengthDrawFactor ));
              break;
          }
          repaint();
        }
      }

      public void mouseReleased(MouseEvent e) {
        //System.out.println("mouseReleased");
        editPoint = null;
      }
    }

    MyListener myListener = new MyListener();
    addMouseListener(myListener);
    addMouseMotionListener(myListener);

    try {
      ClassLoader classLoader = getClass().getClassLoader();
      blueSideImage = ImageIO.read(new File(classLoader.getResource("assets/HalfFieldDiagramBlue.png").getFile()));
      redSideImage = ImageIO.read(new File(classLoader.getResource("assets/HalfFieldDiagramRed.png").getFile()));
    } catch(Exception e) {
      e.printStackTrace();
    }

    JPanel toolBarPanel = new JPanel(new GridLayout(1, 7));
    String[] comboBoxNames = {"Blue Side", "Red Side"};
    sideSelection = new JComboBox<>(comboBoxNames);
    sideSelection.addItemListener(e -> {
      if(e.getStateChange() == ItemEvent.SELECTED){
        if (sideSelection.getSelectedIndex() == 0){
          sides = Sides.BLUE;
        }
        else if(sideSelection.getSelectedIndex() == 1){
          sides = Sides.RED;
        }
        repaint();
      }
    });

//    class JDialogExample extends JFrame
//    {
//      JDialog d1;
//
//      public JDialogExample()
//      {
//        createAndShowGUI();
//      }
//
//      private void createAndShowGUI()
//      {
//        setTitle("JDialog Example");
//        setDefaultCloseOperation(EXIT_ON_CLOSE);
//        setLayout(new FlowLayout());
//
//        // Must be called before creating JDialog for
//        // the desired effect
//        JDialog.setDefaultLookAndFeelDecorated(true);
//
//        // A perfect constructor, mostly used.
//        // A dialog with current frame as parent
//        // a given title, and modal
//        d1=new JDialog(this,"New Autonomous",true);
//
//        // Set size
//        d1.setSize(400,100);
//
//        // Set some layout
//        d1.setLayout(new FlowLayout());
//
//        d1.add(new JLabel("Autonomous Name"));
//        d1.add(new JTextField(20));
//        d1.add(new JButton("OK"));
//        d1.add(new JButton("Cancel"));
//
//        setSize(400,100);
//        setVisible(true);
//
//        // Like JFrame, JDialog isn't visible, you'll
//        // have to make it visible
//        // Remember to show JDialog after its parent is
//        // shown so that its parent is visible
//        d1.setVisible(true);
//      }
//    }

    String[] autonomousNames;
    if (sharedAutonomousConfigsList.size() == 0) {
      String tempNames[] = {"New Auto"};
      autonomousNames = tempNames;
    }
    else {
      autonomousNames = new String[sharedAutonomousConfigsList.size()+1];
      for (int i = 0; i<sharedAutonomousConfigsList.size(); i++) {
        autonomousNames[i] = sharedAutonomousConfigsList.get(i).toString();
      }
      autonomousNames[sharedAutonomousConfigsList.size()] = "New Auto";
    }
    autoSelection = new JComboBox<>(autonomousNames);
    autoSelection.setSelectedIndex(-1);
    autoSelection.addItemListener(e -> {
      if(e.getStateChange() == ItemEvent.SELECTED){
        if (autoSelection.getSelectedIndex() == autoSelection.getItemCount()-1) {
//          new JDialogExample();
          String s = (String)JOptionPane.showInputDialog(
              null,
              "Autonomous Name:",
              "New Autonomous",
              JOptionPane.PLAIN_MESSAGE,
              null,
              null,
              "");

//If a string was returned, say so.
          if ((s != null) && (s.length() > 0)) {
            sharedAutonomousConfigsList.add(s);
            autoSelection.insertItemAt(s, autoSelection.getItemCount()-1);
            autoSelection.setSelectedIndex(autoSelection.getItemCount()-2);
          }
        }
        repaint();
      }
    });

    JButton decrementButton = new JButton("-");


    decrementButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        scale--;
        repaint();
        scaleTextField.setText(Double.toString(scale));
      }
    });
    JButton incrementButton = new JButton("+");
    incrementButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        scale++;
        repaint();
        scaleTextField.setText(Double.toString(scale));
      }

    });

    scaleTextField = new JTextField(Double.toString(scale));
    scaleTextField.setEditable(true);
    scaleTextField.addActionListener(e -> {
      try {
        scale = Double.parseDouble(e.getActionCommand());
        repaint();
      } catch(NumberFormatException exception){
        JOptionPane.showMessageDialog(PathVisualizer.this,
                "The P.M.P. Section III Act 111.16.11, which you have violated, dictates that you must send one" +
                        " million dollars to the Prince of Nigeria or a jail sentence of 20 years of for-profit prison" +
                        " will be imposed.", "Police Alert", JOptionPane.ERROR_MESSAGE);
      }
    });
    /*String[] autonomousSelectionNames = {"Placeholder", "Placeholder2", "Placeholder3"};
    currentAutonomous = autonomousSelectionNames[0];
    Path2D[] path2DS = new Path2D[3];
    map = new HashMap();
    for (int i=0; i<autonomousSelectionNames.length; i++){
      map.put(autonomousSelectionNames[i], path2DS[i]);
    }
    pathSelection = new JComboBox(autonomousSelectionNames);
    pathSelection.addItemListener(e -> {
      if(e.getStateChange() == ItemEvent.SELECTED){
        currentAutonomous = autonomousSelectionNames[pathSelection.getSelectedIndex()];
        repaint();
      }
    });*/
    toolBarPanel.add(autoSelection);
    toolBarPanel.add(decrementButton);
    toolBarPanel.add(scaleTextField);
    toolBarPanel.add(incrementButton);
    toolBarPanel.add(sideSelection);
    //toolBarPanel.add(pathSelection);

    add(toolBarPanel, BorderLayout.NORTH);
  }

  @Override
  public void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    super.paintComponent(g2);

    if(sides == Sides.BLUE){
      g2.drawImage(blueSideImage, 0 - (int)((scale-18)/36 * blueSideImage.getWidth()),
              0 - (int)((scale-18)/18 * (blueSideImage.getHeight() - 29)),
              blueSideImage.getWidth() + (int)((scale-18)/18 * blueSideImage.getWidth()),
              (int)((blueSideImage.getHeight() + 29) * scale/18) , null);
    }
    else if(sides == Sides.RED){
      g2.drawImage(redSideImage, 0 - (int)((scale-18)/36 * redSideImage.getWidth()),
              0 - (int)((scale-18)/18 * (redSideImage.getHeight() - 29)),
              redSideImage.getWidth() + (int)((scale-18)/18 * redSideImage.getWidth()),
              (int)((redSideImage.getHeight() + 29) * scale/18) , null);
    }
    g2.setStroke(new BasicStroke(2));
    //g2.drawLine((int)(blueSideImage.getWidth(null) * scale / 18) + 15, 0,
    //      (int)(blueSideImage.getWidth(null) * scale / 18) + 15, (int)(blueSideImage.getHeight(null) * scale / 18) + 15);
    //g2.drawLine(5);
    g2.drawLine(0, blueSideImage.getHeight()-29, blueSideImage.getWidth()/2, blueSideImage.getHeight()-29);


    // get the stuff ready for the path drawing loop
    double deltaT = m_path.getDuration() / 100.0;
    Vector2 prevPos = m_path.getPosition(0.0);
    Vector2 prevLeftPos = m_path.getLeftPosition(0.0);
    Vector2 prevRightPos = m_path.getRightPosition(0.0);
    Vector2 pos, leftPos, rightPos;
    double prevEase = 0.0;
    final double MAX_SPEED = 8.0;

    for (double t = deltaT; t <= m_path.getDuration(); t += deltaT) {
      pos = m_path.getPosition(t);
      leftPos = m_path.getLeftPosition(t);
      rightPos = m_path.getRightPosition(t);

      // center line
      g2.setColor(Color.white);
      drawPathLine(g2, prevPos, pos);
      //needs to be implemented
      //drawPath2DLine(g2, map.get(currentAutonomous));

      // left wheel
      double leftSpeed = Vector2.length(Vector2.subtract(leftPos, prevLeftPos)) / deltaT;
      leftSpeed /= MAX_SPEED;  // MAX_SPEED is full green, 0 is full red.
      leftSpeed = Math.min(1.0, leftSpeed);
      double leftDelta = m_path.getLeftPositionDelta(t);
      if (leftDelta>0)
        g2.setColor(new Color((int) ((1.0 - leftSpeed) * 255), (int) (leftSpeed * 255), 0));
      else {
        g2.setColor(new Color(0, 0, 255)); //(int)blue));
      }
      drawPathLine(g2, prevLeftPos, leftPos);

      // right wheel
      double rightSpeed = Vector2.length(Vector2.subtract(rightPos, prevRightPos)) / deltaT / MAX_SPEED;
      rightSpeed = Math.min(1.0, rightSpeed);
      double rightDelta = m_path.getRightPositionDelta(t);
      System.out.println("Right: " + rightDelta);
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

    for(Path2DPoint point = m_path.getXYCurve().getHeadPoint(); point != null; point = point.getNextPoint()) {
      g2.setColor(Color.green);
      Vector2 tPoint = world2Screen(point.getPosition());
      g2.drawOval(((int)tPoint.x - circleSize/2),((int)tPoint.y - circleSize/2), circleSize,circleSize);
      if(point.getPrevPoint() != null) {
        g2.setColor(Color.blue);
        Vector2 tanPoint = world2Screen(Vector2.subtract(point.getPosition(), Vector2.multiply(point.getPrevTangent(),1.0/tangentLengthDrawFactor)));
        g2.drawOval(((int)tanPoint.x - circleSize/2),((int)tanPoint.y - circleSize/2), circleSize,circleSize);
        g2.setColor(Color.cyan);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine((int)tPoint.x,(int)tPoint.y,(int)tanPoint.x,(int)tanPoint.y);
      }
      if(point.getNextPoint() != null) {
        g2.setColor(Color.blue);
        Vector2 tanPoint = world2Screen(Vector2.add(point.getPosition(), Vector2.multiply(point.getNextTangent(),1.0/tangentLengthDrawFactor)));
        g2.drawOval(((int)tanPoint.x - circleSize/2),((int)tanPoint.y - circleSize/2), circleSize,circleSize);
        g2.setColor(Color.cyan);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine((int)tPoint.x,(int)tPoint.y,(int)tanPoint.x,(int)tanPoint.y);
      }
    }

// draw the ease curve
//    g2.setStroke(new BasicStroke(3));
//    for (double t = deltaT; t <= m_path.getDuration(); t += deltaT) {
//      // draw the ease curve too
//      g2.setColor(Color.black);
//      double ease = m_path.getEaseCurve().getValue(t);
//      double prevT = t - deltaT;
////      g2.drawLine((int) (prevT * 40 + 600), (int) (prevEase * -200 + 700), (int) (t * 40 + 600), (int) (ease * -200 + 700));
//      g2.drawLine((int) (prevT * 40 + 100), (int) (prevEase * -200 + 700), (int) (t * 40 + 100), (int) (ease * -200 + 700));
//      prevEase = ease;
//    }
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
    Vector2 result = new Vector2 ((point.x-xOffset)/xFlip/scale, (point.y-yOffset)/-scale);
    return result;
  }
  private Vector2 world2Screen(Vector2 point){

    double xFlip = 1.0;
    if(sides == Sides.RED){
      xFlip = -1.0;
    }
    Vector2 result = new Vector2 (point.x*xFlip*scale+ xOffset, point.y*-scale+ yOffset);
    return result;
  }
}



