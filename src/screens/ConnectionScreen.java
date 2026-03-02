package src.screens;

import javax.swing.JButton;
import javax.swing.JFrame;

public class ConnectionScreen extends JFrame {
    public JButton button;

    public ConnectionScreen(){
        super("Test");
        setSize(800, 800);
        		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);
        button = new JButton("123");
        button.setBounds(100, 100, 50, 50);
        add(button);
    }

}
