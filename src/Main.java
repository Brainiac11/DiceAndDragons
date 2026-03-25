package src;

import src.screens.ConnectionScreen;
import src.screens.GameScreen;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        ConnectionScreen screen = new ConnectionScreen();
//        JFrame screen =  new JFrame();
//        screen.setSize(800,800);
//        screen.add(new GameScreen());
        screen.setVisible(true);

    }
}
