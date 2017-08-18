package org.team2471.frc.pathvisualizer;

import javax.swing.*;

public class Main {
  public static void main(String[] args) {
      JFrame mainFrame = new JFrame("Path Visualizer");
      mainFrame.setSize(1024, 768);
      PathVisualizer pvFrame = new PathVisualizer();
      mainFrame.add(pvFrame);
      mainFrame.setVisible(true);
  }
}
