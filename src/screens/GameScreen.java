package src.screens;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;

public class GameScreen extends JPanel {
    BufferedImage image;
    BufferedImage image1;
    public GameScreen(){
        setSize(800, 670);
        makeImages();
    }
    public void makeImages(){
        try {
            image = ImageIO.read(new File("imgs/blue_dragon.png"));
            image1 = ImageIO.read(new File("imgs/young_red_dragon.png"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, image.getWidth() /  4,image.getHeight() / 4,this);
        g.drawImage(image1, 100, 100, image1.getWidth() /  4,image1.getHeight() / 4,this);

    }
}
