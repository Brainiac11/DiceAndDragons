package src.screens;

import src.networking.GameClient;
import src.networking.GameServer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ConnectionScreen extends JFrame {
    public JButton button;

    private GameServer server;
    private GameClient client;

    public ConnectionScreen() {
        super("Dice and Dragons");
        setSize(560, 267);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        JLabel serverIdLabel = new JLabel("serve id ");
        JLabel statusLabel = new JLabel("statis ");
        JLabel sentLabel = new JLabel("sent ");
        JLabel receivedLabel = new JLabel("recieved ");
        JLabel dataLabel = new JLabel("Data");
        JTextField ipField = new JTextField("localhost", 12);
        JTextField portField = new JTextField(7);
        JButton hostBtn = new JButton("Host Host");
        JButton joinBtn = new JButton("Join Join");
        JButton startGameBtn = new JButton("Start Game");

        add(new JLabel("ip "));
        add(ipField);
        add(new JLabel("port "));
        add(portField);
        add(hostBtn);
        add(joinBtn);
        add(serverIdLabel);
        add(statusLabel);
        add(sentLabel);
        add(receivedLabel);
        add(startGameBtn);
        add(dataLabel);

        hostBtn.addActionListener(e -> {
            try {
                server = new GameServer();
                serverIdLabel.setText("serve id " + server.getPort());
                statusLabel.setText("statis connection wait");
                server.setOnConnected(received -> {
                    statusLabel.setText("statis connected");
                    sentLabel.setText("sent connection successful ");
                    receivedLabel.setText("recieved " + received);
                });
                server.startListening();
            } catch (IOException ex) {
                statusLabel.setText("static " + ex.getMessage());
            }
        });

        joinBtn.addActionListener(e -> {
            String ip = ipField.getText().trim(),
                    portText = portField.getText().trim();
            if (ip.isEmpty() || portText.isEmpty()) {
                statusLabel.setText("static porta and ip");
                return;
            }
            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException ex) {
                statusLabel.setText("Static port has to be number or else u are stupid");
                return;
            }
            statusLabel.setText("statiuc connecting ");
            client = new GameClient();
            client.connectAsync(ip, port,
                    received -> {
                        statusLabel.setText("connected");
                        sentLabel.setText("sent conencton susccesful");
                        receivedLabel.setText("recieved " + received);
                    },
                    error -> statusLabel.setText("static failure + =" + error));
        });

        startGameBtn.addActionListener(e -> {
            try {
                if (!server.hasConnections()) {
                    System.out.println("NO CONNECTIONS");
                    dataLabel.setText("None");
                }
                else{
                    client.send("Hello");
                    dataLabel.setText("Hello");
                }
            } catch(Exception ea){
                System.out.println("Need to start game");
                ea.printStackTrace();
            }
        });

        button = new JButton("123, ABC");
    }
}