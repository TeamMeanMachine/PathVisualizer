package org.team2471.frc.pathvisualizer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

public class PathVisualizer extends JFrame {
    private BufferedImage blueSideImage;
    private BufferedImage redSideImage;
    private JComboBox sideSelection;
    private enum Sides{BLUE, RED}
    Sides sides;
    PathVisualizer(){
        sides = Sides.BLUE;
        String[] comboBoxNames = {"Blue Side", "Red Side"};
        sideSelection = new JComboBox(comboBoxNames);
        sideSelection.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED){
                    if (sideSelection.getSelectedIndex() == 0){
                        sides = Sides.BLUE;
                    }
                    else if(sideSelection.getSelectedIndex() == 1){
                        sides = Sides.RED;
                    }
                }
            }
        });
        add(sideSelection);

        try{
            blueSideImage = ImageIO.read(new File("HalfFieldDiagramBlue.png"));
        } catch (IOException e){}
        try {
            redSideImage = ImageIO.read(new File("HalfFieldDiagramRed.PNG"));
        } catch(IOException e){}

    }
    public void paintComponent(Graphics g){
        if(sides == Sides.BLUE) g.drawImage(blueSideImage, 0, 0, null);
        else if(sides == Sides.RED) g.drawImage(redSideImage, 0, 0, null);
    }
    private class onClickHandler extends MouseInputAdapter{

    }
}
