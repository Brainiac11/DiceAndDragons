package src.screens;

import src.interactableImages.InteractableImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;

public class GameScreen extends JPanel {
    private InteractableImage image;
    public GameScreen(){
        setSize(800, 670);
        setLayout(new FlowLayout());
        makeImages();
    }
    public void makeImages(){
        try {
            image = new InteractableImage("imgs/blank_sheet.png", this::mouseRegister);
            image.scaleImageDimensions(0.3);
            this.add(image);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void mouseRegister(MouseEvent e){
        System.out.println(e.getY());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
//        image.paint(g);

    }
}
