package org.team2471.frc.pathvisualizer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

public class PathVisualizer extends JFrame {
    private BufferedImage blueSideImage;
    private BufferedImage redSideImage;
    private JComboBox sideSelection;
    private boolean blueTrueRedFalse;
    PathVisualizer(){
        blueTrueRedFalse = true;
        String[] comboBoxNames = {"Blue Side", "Red Side"};
        sideSelection = new JComboBox(comboBoxNames);
        try{
            blueSideImage = ImageIO.read(new File("HalfFieldDiagramBlue.png"));
        } catch (IOException e){}
        try {
            redSideImage = ImageIO.read(new File("HalfFieldDiagramRed.PNG"));
        } catch(IOException e){}

    }
    public void paintComponent(Graphics g){
        if(blueTrueRedFalse) g.drawImage(blueSideImage, 0, 0, null);
        else g.drawImage(redSideImage, 0, 0, null);
    }
    private class onClickHandler extends MouseInputAdapter{

    }
}
