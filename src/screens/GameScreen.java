package src.screens;

import java.awt.*;
import java.awt.event.MouseEvent;
import javax.swing.*;
import src.interactableImages.InteractableImage;

public class GameScreen extends JPanel {
    private InteractableImage image;

    public GameScreen() {
        setSize(800, 670);
        setLayout(new FlowLayout());
        makeImages();
    }

    public void makeImages() {
        try {
            image = new InteractableImage("imgs/blank_sheet.png", this::mouseRegister);
            image.scaleImageDimensions(0.3);
            this.add(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mouseRegister(MouseEvent e) {
        System.out.println(e.getY());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // image.paint(g);

    }
}
