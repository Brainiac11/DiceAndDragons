package src.screens;

import src.networking.GameClient;
import src.networking.GameServer;
import src.networking.GameSubscriber;

import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionScreen extends JFrame {
    public JButton button;

    private GameServer server;
    private ArrayList<GameClient> clients = new ArrayList<>();

    private final JLabel receivedLabel = new JLabel("recieved ");
    private final JLabel serverIdLabel = new JLabel("serve id ");
    private final JLabel statusLabel = new JLabel("statis ");
    private final static JLabel dataLabel = new JLabel("Data");
    private final JLabel sentLabel = new JLabel("sent ");

    private final JTextField ipField = new JTextField("localhost", 12);
    private final JTextField portField = new JTextField(7);
    private final JButton hostBtn = new JButton("Host Host");
    private final JButton joinBtn = new JButton("Join Join");
    private final JButton startGameBtn = new JButton("Start Game");
    private JTextArea chatLogArea = new JTextArea("", 20, 20);
    private JTextField messageTextSendArea = new JTextField("", 20);

    public ConnectionScreen() {
        super("Dice and Dragons");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new
                FlowLayout());



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
        dataLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(dataLabel);
        add(new JLabel("Chat Log:"));
        chatLogArea.setAlignmentX(SwingConstants.CENTER);
        chatLogArea.setEditable(false);
        chatLogArea.setLineWrap(true);
        for (int i = 0; i < 100; i++){
            chatLogArea.setText(chatLogArea.getText() + " Hello");
        }
        add(chatLogArea);







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
                Thread thread = new Thread(server);
                thread.start();
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
            clients.add(new GameClient());
            clients.getLast().connectAsync(ip, port,
                    received -> {
                        statusLabel.setText("connected");
                        sentLabel.setText("sent conencton susccesful");
                        receivedLabel.setText("recieved " + received);
                    },
                    error -> statusLabel.setText("static failure + =" + error));

        });

        startGameBtn.addActionListener(e -> {
            try {
                if (server==null || !server.hasConnections()) {
                    System.out.println("NO CONNECTIONS");
                    dataLabel.setText("None");
                }
                else{
                    ArrayList<String> spp = new ArrayList<>( List.of("h", "e", "l", "l", "o", " ", String.valueOf(Math.random())));
                    for (GameSubscriber subscriber: server.getConnections()){
                        subscriber.send(spp);
                    }
                    dataLabel.setText(spp.toString());
                    System.out.println("aada");
                }
            } catch(Exception ea){
                System.out.println("Need to start game");
                ea.printStackTrace();
            }
        });

        button = new JButton("123, ABC");
    }

    public static JLabel dataLabel(){
        return dataLabel;
    }

}