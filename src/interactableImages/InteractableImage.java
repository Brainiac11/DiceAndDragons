package src.interactableImages;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;

public class InteractableImage extends JLabel implements MouseListener {
    private BufferedImage image;
    private int x, y;
    private int width, height;

    private Consumer<MouseEvent> mouseEventListener;

    public InteractableImage(String pathName, Consumer<MouseEvent> mouseEventListener) throws IOException {
        image = ImageIO.read(new File(pathName));
        this.mouseEventListener = mouseEventListener;
        super.setIcon(new ImageIcon(image));
        width = image.getWidth();
        height = image.getHeight();
        this.addMouseListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(image, x, y, width, height, this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseEventListener.accept(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setCoordinates(int x, int y) {
        this.y = y;
        this.x = x;
    }

    @Override
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setDimensions(int width, int height) {
        this.height = height;
        this.width = width;
    }

    /**
     * Maintains aspect ratio
     * 
     * @param proportion the proportion to scale the height and width by. between
     *                   0.0<proportion<=1.0
     *
     */
    public void scaleImageDimensions(double proportion) {
        this.height = (int) ((int) image.getHeight() * proportion);
        this.width = (int) ((int) image.getWidth() * proportion);
    }

    public Consumer<MouseEvent> getMouseEventListener() {
        return mouseEventListener;
    }

    public void setMouseEventListener(Consumer<MouseEvent> mouseEventListener) {
        this.mouseEventListener = mouseEventListener;
    }
}
