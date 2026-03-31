package src.screens;

import java.awt.FlowLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import src.networking.GameClient;
import src.networking.GameMessage;
import src.networking.GameServer;
import src.networking.LobbyState;
import src.networking.PlayerInfo;
import src.players.Heroes;

// MAKE SURE TO CHANGE THE HOST NAME TAKEN ERROR SCREEN TO DISPLAY THAT THE HOST NAME IS TAKEn

public class ConnectionScreen extends JFrame {
    private static final String HERO_PLACEHOLDER = "Select an Hero";

    private GameServer s;
    private GameClient c;
    private boolean host;
    private String me;
    private String myPick;
    private boolean noHeroEvent;

    private Timer t;

    private JPanel p1;
    private JPanel p2;

    private JLabel stat;
    private JLabel top;
    private JLabel sub;

    private JTextField name;
    private JTextField ip;
    private JTextField port;
    private JTextArea ppl;
    private JComboBox<String> hero;
    private JTextArea chat;
    private JTextField msg;

    private JButton start;
    private MarketplaceScreen marketplace;
    private boolean inMarketplace;
    private Consumer<String> dragonPicker;
    private JComboBox<String> dragonSelector;
    private JLabel dragonLabel;
    private boolean noDragonEvent;

    public ConnectionScreen() {
        super("Dice and Dragons");
        setSize(800, 670);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // allah
        makeMain();
        makeLobby();
        openMain();
    }

    private void makeMain() {
        p1 = new JPanel(new FlowLayout());
        name = new JTextField(20);
        ip = new JTextField("localhost", 20);
        port = new JTextField(20);

        JButton b1 = new JButton("Host Game");
        JButton b2 = new JButton("Join Game");
        stat = new JLabel(" ");
        p1.add(new JLabel("Handle"));
        p1.add(name);
        p1.add(new JLabel("IP"));
        p1.add(ip);
        p1.add(new JLabel("Port"));
        p1.add(port);
        p1.add(b1);
        p1.add(b2);
        p1.add(stat);
        b1.addActionListener(e -> doHost());
        b2.addActionListener(e -> doJoin());
    }

    private void makeLobby() {
        p2 = new JPanel(new FlowLayout());

        top = new JLabel("Lobby");
        sub = new JLabel("watiing for the ost.....");
        this.dragonPicker = dragonPicker;
        this.noDragonEvent = false;
        // 67
        ppl = new JTextArea(12, 22);
        ppl.setEditable(false);

        // placeholders for now when i acc get everything oorkingn
        hero = new JComboBox<>(new String[] { HERO_PLACEHOLDER, Heroes.WARRIOR, Heroes.WIZARD, Heroes.CLERIC,
                Heroes.RANGER, Heroes.ROGUE });

        chat = new JTextArea(16, 37);
        chat.setEditable(false);
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        msg = new JTextField(25);
        JButton send = new JButton("Send");
        dragonSelector = new JComboBox<>(new String[] { "DRAGON_1", "DRAGON_2" });



        dragonLabel = new JLabel("Selected: None");


        // only host can start the game, but everyone can see the button, need to patch
        start = new JButton("Start Game");
        start.setVisible(false);
        JButton leave = new JButton("Leave Lobby");

        p2.add(top);
        p2.add(sub);
        p2.add(new JLabel("Players"));
        p2.add(new JScrollPane(ppl));
        p2.add(new JLabel("Hero"));
        p2.add(hero);
        p2.add(new JLabel("Chat"));
        p2.add(new JScrollPane(chat));
        p2.add(msg);
        p2.add(send);
        p2.add(start);
        p2.add(leave);
        p2.add(dragonSelector);
        dragonSelector.setEnabled(host);
        p2.add(new JLabel("Dragon:"));
        p2.add(dragonLabel);
        dragonPicker = this::selectBoss;

        hero.addActionListener(e -> pickHero());
        send.addActionListener(e -> sendMsg());
        msg.addActionListener(e -> sendMsg());
        start.addActionListener(e -> pressStart());
        leave.addActionListener(e -> leaveRoom());
        dragonSelector.addActionListener(e -> selectDragon());
    }

    private void openMain() {
        inMarketplace = false;
        marketplace = null;
        setContentPane(p1);
        revalidate();
        repaint();
    }

    private void openLobby() {
        inMarketplace = false;

        makeLobby();
        setContentPane(p2);
        revalidate();
        repaint();
    }

    private void openMarketplace(LobbyState state) {
        if (marketplace == null) {
            marketplace = new MarketplaceScreen(this::sendMarketplaceMessage, this::buyMarketplaceItem, host
                    );
        }
        marketplace.updateFromLobbyState(state);
        inMarketplace = true;
        setContentPane(marketplace);
        revalidate();
        repaint();
    }
    private void selectDragon() {
        if (noDragonEvent) {
            return;
        }
        String selected = (String) dragonSelector.getSelectedItem();
        if (selected == null || selected.isEmpty()) {
            return;
        }
        if (dragonPicker != null) {
            dragonPicker.accept(selected);
            dragonLabel.setText(selected);
        }
    }

    private void doHost() {
        String n = name.getText().trim();

        if (n.isEmpty()) {
            stat.setText("Enter ur hangle");
            return;
        }
        stopAll();
        host = true;
        try {
            s = new GameServer(n);
            me = n;
            myPick = null;


            port.setText(String.valueOf(s.getPort()));
            s.startListening();

            openLobby();
            top.setText("Host: " + me + " Port: " + s.getPort());
            sub.setText("Waiitng for platers......");
            start.setVisible(true);

            // TODO: BANADAID AHH Solutions for updatiign state need to get this done right
            t = new Timer(500, event -> {
                if (s != null) {
                    updateThePaintOnLobby(new GameMessage(GameMessage.LOBBY_UPDATE, s.getCurrentLobbyState()));
                }
            });
            t.start();
        } catch (IOException e) {
            stat.setText("sever failed to start:" + e.getMessage());
        }
    }

    private void doJoin() {
        String n = name.getText().trim();
        String ipx = ip.getText().trim();
        String px = port.getText().trim();

        if (n.isEmpty()) {
            stat.setText("Enter ur hangle");
            return;
        }
        if (ipx.isEmpty() || px.isEmpty()) {
            stat.setText("Enter ip and port-");
            return;
        }

        int p;
        try {
            p = Integer.parseInt(px);
        } catch (NumberFormatException ex) {
            stat.setText("port has to be a number");
            return;
        }
        stopAll();
        me = n;
        myPick = null;
        host = false;
        stat.setText("conneting.....");
        GameClient c2 = new GameClient();

        Thread thread = new Thread(() -> {
            try {
                LobbyState first = c2.connect(ipx, p, n);
                c = c2;
                openLobby();
                top.setText("Player: " + me + " Host: " + ipx + ":" + p);
                sub.setText("Connected to lobby.");
                updateThePaintOnLobby(new GameMessage(GameMessage.LOBBY_UPDATE, first));
                listenClient();
            } catch (Exception e) {
                stat.setText("Failed to COnnect");
                JOptionPane.showMessageDialog(ConnectionScreen.this, "Failed to Conect", "Join Failed",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void pickHero() {
        if (noHeroEvent) {
            return;
        }
        String h = (String) hero.getSelectedItem();
        if (h == null || h.equals(HERO_PLACEHOLDER)) {
            h = null;
        }
        myPick = h;
        try {
            if (host && s != null) {
                s.setHostHero(h);
            } else if (!host && c != null) {
                c.selectHero(h == null ? "" : h);
            }
        } catch (IOException e) {
            sub.setText("Error");
        }
    }

    private void sendMsg() {
        String m = msg.getText().trim();
        System.out.println("Mesaage: " + m);
        if (sendChatMessage(m)) {
            msg.setText("");
        }
    }

    private void sendMarketplaceMessage(String message) {
        sendChatMessage(message);
    }

    private void buyMarketplaceItem(String itemName) {
        String cleanItem = itemName == null ? "" : itemName.trim();
        if (cleanItem.isEmpty()) {
            return;
        }

        if (host && s != null) {
            s.buyItem(cleanItem, me);
            return;
        }

        if (!host && c != null) {
            try {
                c.buyItem(cleanItem);
            } catch (IOException e) {
                sub.setText("Error buying item");
            }
        }
    }

    private void selectBoss(String bossName) {
        String cleanBoss = bossName == null ? "" : bossName.trim();
        if (cleanBoss.isEmpty()) {
            return;
        }

        if (host && s != null) {
            s.selectBoss(cleanBoss);
            return;
        }

        if (!host && c != null) {
            try {
                c.selectBoss(cleanBoss);
            } catch (IOException e) {
                sub.setText("Error selecting boss");
            }
        }
    }

    private boolean sendChatMessage(String rawText) {
        String m = rawText == null ? "" : rawText.trim();
        if (m.isEmpty()) {
            return false;
        }

        try {
            if (host && s != null) {
                s.sendHostChatMessage(m);
                return true;
            }
            if (!host && c != null) {
                c.sendChatMessage(m);
                return true;
            }
        } catch (IOException e) {
            if (!inMarketplace) {
                sub.setText("Error when trying to send messaage");
            }
        }
        return false;
    }

    private void pressStart() {
        if (!host || s == null) {
            return;
        }
        LobbyState st = s.getCurrentLobbyState();
        if (!st.allReady) {
            sub.setText(
                    "not all players sleected hero, make sure they do to start the game otherwise you can't start the game ");
            return;
        }
        s.startGame();
        sub.setText("Game started@");
        openMarketplace(s.getCurrentLobbyState());
    }

    private void leaveRoom() {
        stopAll();
        myPick = null;
        openMain();
        stat.setText("Lobby left");
    }

    // this is so cancer
    private void updateThePaintOnLobby(GameMessage gt) {
        if (gt == null) {
            return;
        }

        LobbyState state = gt.lobbyState;

        if (GameMessage.START.equals(gt.type)) {
            if (state == null && host && s != null) {
                state = s.getCurrentLobbyState();
            }
            openMarketplace(state);
            return;
        }

        if (state == null) {
            return;
        }

        if (inMarketplace) {
            if (marketplace != null) {
                marketplace.updateFromLobbyState(state);
            }
            return;
        }
        if (state.selectedDragon != null && !state.selectedDragon.isEmpty()) {
            noDragonEvent = true;
            dragonSelector.setSelectedItem(state.selectedDragon);
            noDragonEvent = false;
            dragonLabel.setText("Selected: " + state.selectedDragon);
        } else {
            dragonLabel.setText("Selected: None");
        }

        ArrayList<PlayerInfo> players = state.players == null ? new ArrayList<>() : state.players;
        ArrayList<String> chatLines = state.chat == null ? new ArrayList<>() : state.chat;

        StringBuilder playerText = new StringBuilder();
        for (PlayerInfo p : players) {
            String hh = (p.hero == null || p.hero.isEmpty()) ? "No Hero" : p.hero;
            String rr;

            if (!host && p.handle != null && me != null && p.handle.equalsIgnoreCase(me) && hh.equals("No Hero")
                    && myPick != null && !myPick.isEmpty()) {
                hh = myPick;
            }

            if (p.ready) {
                rr = "Ready";
            } else {
                rr = "Not Ready";
            }

            if (!host && p.handle != null && me != null && p.handle.equalsIgnoreCase(me) && myPick != null
                    && !myPick.isEmpty() && hh.equals(myPick)) {
                rr = "Ready";
            }

            playerText.append(p.handle).append(" --- ").append(hh).append(" --- ").append(rr).append("\n");
        }
        ppl.setText(playerText.toString());

        StringBuilder chatText = new StringBuilder();
        for (String line : chatLines) {
            chatText.append(line).append("\n");
        }
        chat.setText(chatText.toString());

        String myHero = null;
        for (PlayerInfo p : players) {
            // System.out.println(p.handle);
            if (p.handle != null && me != null && p.handle.equalsIgnoreCase(me)) {
                myHero = p.hero;
                break;
            }
        }

        if (myHero != null && !myHero.isEmpty()) {
            myPick = myHero;
        }
        noHeroEvent = true;

        if (myHero != null && !myHero.isEmpty()) {
            hero.setSelectedItem(myHero);
        } else if (host) {
            hero.setSelectedIndex(0);
        } else if (myPick != null && !myPick.isEmpty()) {
            hero.setSelectedItem(myPick);
        }
        noHeroEvent = false;
        start.setVisible(host);
        start.setEnabled(host && state.allReady);
    }

    private void listenClient() {
        Thread th = new Thread(() -> {
            try {
                while (true) {
                    // LobbyState st = c.readUpdateToLobby();
                    // updateThePaintOnLobby(st);
                    GameMessage gt = c.readUpdateToGameMessage();

                    updateThePaintOnLobby(gt);
                }
            } catch (Exception e) {
                stopAll();
                openMain();
                stat.setText("Lobby disconcnneted");
            }
        });
        th.setDaemon(true);
        th.start();
    }

    private void stopAll() {
        System.out.println("STOPPP");
        myPick = null;
        if (t != null) {
            t.stop();
            t = null;
        }
        if (c != null) {
            c.close();
            c = null;
        }
        if (s != null) {
            s.close();
            s = null;
        }
    }

    @Override
    public void dispose() {
        stopAll();
        super.dispose();
    }
}
