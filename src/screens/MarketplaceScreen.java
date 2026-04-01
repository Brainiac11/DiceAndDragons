package src.screens;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.TableColumnModel;

import src.networking.LobbyState;

public class MarketplaceScreen extends JPanel {
    private final JLabel goldLabel;
    private final JTextArea chatArea;
    private final JTextField messageField;
    private final Consumer<String> messageSender;
    private final Consumer<String> itemBuyer;
    private final boolean isHost;
    private final JButton item1Button;
    private final JButton item2Button;


    private JScrollPane market;
    private JLabel smallHealingPotion;
    private JTable marketTable;



    public MarketplaceScreen(Consumer<String> messageSender, Consumer<String> itemBuyer, boolean isHost) {
        this.messageSender = messageSender;
        this.itemBuyer = itemBuyer;
        this.isHost = isHost;


        setLayout(new FlowLayout());

        add(new JLabel("Marketplace"));

        goldLabel = new JLabel("Team Gold: 0");
        add(goldLabel);

        item1Button = new JButton("Buy ITEM_1 (1 gold)");
        item2Button = new JButton("Buy ITEM_2 (2 gold)");
        item1Button.addActionListener(e -> buyItem("ITEM_1"));
        item2Button.addActionListener(e -> buyItem("ITEM_2"));
        add(item1Button);
        add(item2Button);




        chatArea = new JTextArea(15, 26);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane);

        messageField = new JTextField();
        messageField.setColumns(24);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendCurrentMessage());
        messageField.addActionListener(e -> sendCurrentMessage());

        add(messageField);
        add(sendButton);

        market = new JScrollPane();
        market.add(new JLabel("HELO"));
        add(market);
        marketTable = new JTable(new TableColumnModel())
        smallHealingPotion = new JLabel("Small Healing Potion - ")
    }

    private void buyItem(String itemName) {
        if (itemBuyer == null) {
            return;
        }
        itemBuyer.accept(itemName);
    }



    private void sendCurrentMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        if (messageSender != null) {
            messageSender.accept(message);
        }
        messageField.setText("");
    }

    public void updateFromLobbyState(LobbyState state) {
        if (state == null) {
            return;
        }

        setTeamGold(state.teamGold);
        item1Button.setEnabled(state.teamGold > 0);
        item2Button.setEnabled(state.teamGold > 0);



        ArrayList<String> lines = state.chat == null ? new ArrayList<>() : state.chat;
        StringBuilder chatBuilder = new StringBuilder();
        for (String line : lines) {
            chatBuilder.append(line).append("\n");
        }
        chatArea.setText(chatBuilder.toString());
    }

    private void setTeamGold(int teamGold) {
        goldLabel.setText("Team Gold: " + teamGold);
    }
}
