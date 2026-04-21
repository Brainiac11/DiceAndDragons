package src.screens;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import src.item.ItemEnum;
import src.item.VillageMarketplaceCatalog;
import src.networking.LobbyState;
import src.networking.PlayerInfo;
import src.players.DragonCatalog;
import src.skills.SkillEnum;

public class MarketplaceScreen extends JPanel {
    private final JLabel marketTitleLabel;
    private final JLabel dragonLabel;
    private final JLabel goldLabel;

    private final JTextArea chatArea;
    private final JTextField messageField;
    private final Consumer<String> messageSender;
    private final Consumer<String> itemBuyer;
    private final Consumer<String> itemSeller;
    private final Runnable startGameAction;
    private final String localHandle;

    private final JTable marketTable;
    private final DefaultTableModel marketTableModel;
    private final JButton buySelectedButton;
    private final JButton sellSelectedButton;
    private final JButton startGameButton;
    private final JPanel playerItemsPanel;
    private final ArrayList<String> localPurchasedItems;

    private int currentTeamGold;
    private String currentDragonSelection;

    public MarketplaceScreen(Consumer<String> messageSender, Consumer<String> itemBuyer,
            Consumer<String> itemSeller, Runnable startGameAction, boolean hostCanStart, String localHandle) {
        this.messageSender = messageSender;
        this.itemBuyer = itemBuyer;
        this.itemSeller = itemSeller;
        this.startGameAction = startGameAction;
        this.localHandle = localHandle == null ? "" : localHandle.trim();
        this.localPurchasedItems = new ArrayList<>();
        this.currentDragonSelection = null;

        setLayout(new BorderLayout(8, 8));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        marketTitleLabel = new JLabel("Village Marketplace");
        dragonLabel = new JLabel("Dragon: None");
        goldLabel = new JLabel("Team Gold: 0");
        startGameButton = new JButton("Start");
        startGameButton.setVisible(hostCanStart);
        startGameButton.setEnabled(hostCanStart);
        startGameButton.addActionListener(e -> moveToGameScreen());
        headerPanel.add(marketTitleLabel);
        headerPanel.add(dragonLabel);
        headerPanel.add(goldLabel);
        headerPanel.add(startGameButton);
        add(headerPanel, BorderLayout.NORTH);

        marketTableModel = new DefaultTableModel(new String[] { "Name", "Type", "Cost", "Quantity" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        marketTable = new JTable(marketTableModel);
        marketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        marketTable.getSelectionModel().addListSelectionListener(event -> updateActionButtonsState());

        buySelectedButton = new JButton("Buy Selected");
        buySelectedButton.setEnabled(false);
        buySelectedButton.addActionListener(e -> buySelectedItem());

        sellSelectedButton = new JButton("Sell Selected");
        sellSelectedButton.setEnabled(false);
        sellSelectedButton.addActionListener(e -> sellSelectedItem());

        JPanel marketButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        marketButtonsPanel.add(buySelectedButton);
        marketButtonsPanel.add(sellSelectedButton);

        JPanel marketPanel = new JPanel(new BorderLayout());
        marketPanel.setBorder(BorderFactory.createTitledBorder("Market (Items / Skills)"));
        marketPanel.add(new JScrollPane(marketTable), BorderLayout.CENTER);
        marketPanel.add(marketButtonsPanel, BorderLayout.SOUTH);

        chatArea = new JTextArea(15, 26);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        messageField = new JTextField(24);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendCurrentMessage());
        messageField.addActionListener(e -> sendCurrentMessage());

        JPanel chatInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chatInputPanel.add(messageField);
        chatInputPanel.add(sendButton);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        playerItemsPanel = new JPanel(new GridLayout(0, 2, 6, 7));
        JScrollPane playerItemsScrollPane = new JScrollPane(playerItemsPanel);
        playerItemsScrollPane.setBorder(BorderFactory.createTitledBorder("Players Purchases (Items / Skills)"));
        playerItemsScrollPane.setPreferredSize(new Dimension(300, 200));

        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 6, 7));
        rightPanel.add(playerItemsScrollPane);
        rightPanel.add(chatPanel);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 6, 7));
        centerPanel.add(marketPanel);
        centerPanel.add(rightPanel);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void buySelectedItem() {
        int selectedRow = marketTable.getSelectedRow();
        if (selectedRow < 0 || itemBuyer == null) {
            return;
        }

        int itemCost = parseCost(marketTableModel.getValueAt(selectedRow, 2));
        int itemQty = Integer.parseInt(String.valueOf(marketTableModel.getValueAt(selectedRow, 3)));

        if (itemCost > currentTeamGold || itemQty <= 0) {
            updateActionButtonsState();
            return;
        }

        String itemName = String.valueOf(marketTableModel.getValueAt(selectedRow, 0));
        itemBuyer.accept(itemName);
    }

    private void moveToGameScreen() {
        if (startGameAction == null) {
            return;
        }
        startGameAction.run();
    }

    private void sellSelectedItem() {
        int selectedRow = marketTable.getSelectedRow();
        if (selectedRow < 0 || itemSeller == null) {
            return;
        }

        String itemName = String.valueOf(marketTableModel.getValueAt(selectedRow, 0));
        if (!localPlayerOwnsItem(itemName)) {
            updateActionButtonsState();
            return;
        }

        itemSeller.accept(itemName);
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
        refreshMarket(state);
        refreshPlayerItemBoxes(state.players);
        refreshLocalInventory(state.players);

        ArrayList<String> lines = state.chat == null ? new ArrayList<>() : state.chat;
        StringBuilder chatBuilder = new StringBuilder();
        for (String line : lines) {
            chatBuilder.append(line).append("\n");
        }
        chatArea.setText(chatBuilder.toString());
        updateActionButtonsState();
    }

    private void setTeamGold(int teamGold) {
        currentTeamGold = teamGold;
        goldLabel.setText("Team Gold: " + teamGold);
    }

    private void refreshMarket(LobbyState state) {
        String dragonSelection = state.selectedDragon;
        DragonCatalog.DragonProfile dragon = DragonCatalog.findBySelection(dragonSelection);

        String previouslySelected = null;
        int selectedRow = marketTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < marketTableModel.getRowCount()) {
            previouslySelected = String.valueOf(marketTableModel.getValueAt(selectedRow, 0));
        }

        marketTableModel.setRowCount(0);

        if (dragon == null) {
            currentDragonSelection = null;
            marketTitleLabel.setText("Village Marketplace");
            dragonLabel.setText("Dragon: None");
            return;
        }

        String dragonId = dragon.getId();
        currentDragonSelection = dragonId;
        VillageMarketplaceCatalog.Marketplace market = VillageMarketplaceCatalog.forDragon(dragonId);
        if (market == null) {
            marketTitleLabel.setText("Village Marketplace");
            dragonLabel.setText("Dragon: " + dragon.getDisplayName());
            updateActionButtonsState();
            return;
        }

        marketTitleLabel.setText("Village Marketplace - " + market.getVillageName());
        dragonLabel.setText("Dragon: " + dragon.getDisplayName());

        int newSelectedIndex = -1;
        int index = 0;
        for (int pass = 0; pass < 2; pass++) {
            boolean showSkills = pass == 1;
            for (VillageMarketplaceCatalog.MarketplaceItem item : market.getItems()) {
                boolean isSkill = item.getType() == ItemEnum.SKILL;
                if (showSkills != isSkill) {
                    continue;
                }

                int remaining = item.getQuantity();
                if (state.players != null) {
                    for (PlayerInfo p : state.players) {
                        if (p.purchasedItems != null) {
                            for (String purchased : p.purchasedItems) {
                                if (purchased.equals(item.getName())) {
                                    remaining--;
                                }
                            }
                        }
                    }
                }

                marketTableModel.addRow(new Object[] {
                        item.getName(),
                        isSkill ? "Skill" : "Item",
                        item.getCost(),
                        remaining
                });

                if (item.getName().equals(previouslySelected)) {
                    newSelectedIndex = index;
                }
                index++;
            }
        }

        if (marketTableModel.getRowCount() > 0) {
            if (newSelectedIndex < 0) {
                newSelectedIndex = 0;
            }
            marketTable.setRowSelectionInterval(newSelectedIndex, newSelectedIndex);
        }
        updateActionButtonsState();
    }

    private void refreshPlayerItemBoxes(ArrayList<PlayerInfo> players) {
        ArrayList<PlayerInfo> safePlayers = players == null ? new ArrayList<>() : players;
        int columns = safePlayers.size() <= 1 ? 1 : 2;
        playerItemsPanel.setLayout(new GridLayout(0, columns, 8, 8));
        playerItemsPanel.removeAll();

        if (safePlayers.isEmpty()) {
            JPanel emptyPanel = new JPanel(new BorderLayout());
            emptyPanel.setBorder(BorderFactory.createTitledBorder("Players"));
            emptyPanel.add(new JLabel("No players connected."), BorderLayout.CENTER);
            playerItemsPanel.add(emptyPanel);
        } else {
            for (PlayerInfo player : safePlayers) {
                playerItemsPanel.add(createPlayerItemBox(player));
            }
        }

        playerItemsPanel.revalidate();
        playerItemsPanel.repaint();
    }

    private JPanel createPlayerItemBox(PlayerInfo player) {
        JPanel box = new JPanel(new BorderLayout());

        String playerHandle = "Unknown Player";
        if (player != null && player.handle != null && !player.handle.trim().isEmpty()) {
            playerHandle = player.handle.trim();
        }

        box.setBorder(BorderFactory.createTitledBorder(playerHandle));

        JTextArea itemsArea = new JTextArea(5, 18);
        itemsArea.setEditable(false);
        itemsArea.setLineWrap(true);
        itemsArea.setWrapStyleWord(true);
        itemsArea.setText(formatPurchasedItems(player));
        box.add(new JScrollPane(itemsArea), BorderLayout.CENTER);

        return box;
    }

    private String formatPurchasedItems(PlayerInfo player) {
        if (player == null || player.purchasedItems == null || player.purchasedItems.isEmpty()) {
            return "Items:\n- None\n\nSkills:\n- None";
        }

        StringBuilder itemText = new StringBuilder();
        StringBuilder skillText = new StringBuilder();

        for (String purchaseName : player.purchasedItems) {
            if (purchaseName == null) {
                continue;
            }
            String cleanName = purchaseName.trim();
            if (cleanName.isEmpty()) {
                continue;
            }

            if (isSkillPurchase(cleanName)) {
                skillText.append("- ").append(cleanName).append("\n");
            } else {
                itemText.append("- ").append(cleanName).append("\n");
            }
        }

        if (itemText.length() == 0) {
            itemText.append("- None\n");
        }
        if (skillText.length() == 0) {
            skillText.append("- None\n");
        }

        return "Items:\n" + itemText.toString().trim() + "\n\nSkills:\n" + skillText.toString().trim();
    }

    private boolean isSkillPurchase(String purchaseName) {
        VillageMarketplaceCatalog.MarketplaceItem marketItem = VillageMarketplaceCatalog.findItem(
                currentDragonSelection,
                purchaseName);
        if (marketItem != null) {
            return marketItem.getType() == ItemEnum.SKILL;
        }

        String normalized = purchaseName.trim().toUpperCase().replace(' ', '_');
        for (SkillEnum skill : SkillEnum.values()) {
            if (skill.name().equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void refreshLocalInventory(ArrayList<PlayerInfo> players) {
        localPurchasedItems.clear();
        if (localHandle.isEmpty() || players == null) {
            return;
        }
        for (PlayerInfo player : players) {
            if (player == null || player.handle == null || !player.handle.equalsIgnoreCase(localHandle)) {
                continue;
            }
            if (player.purchasedItems == null) {
                return;
            }
            for (String itemName : player.purchasedItems) {
                if (itemName == null) {
                    continue;
                }
                String cleanName = itemName.trim();
                if (!cleanName.isEmpty()) {
                    localPurchasedItems.add(cleanName);
                }
            }
            return;
        }
    }

    private boolean localPlayerOwnsItem(String itemName) {
        if (itemName == null) {
            return false;
        }

        String cleanName = itemName.trim();
        if (cleanName.isEmpty()) {
            return false;
        }

        for (String ownedItem : localPurchasedItems) {
            if (ownedItem.equals(cleanName)) {
                return true;
            }
        }

        return false;
    }

    private void updateActionButtonsState() {
        int selectedRow = marketTable.getSelectedRow();
        if (selectedRow < 0) {
            buySelectedButton.setEnabled(false);
            sellSelectedButton.setEnabled(false);
            return;
        }

        int itemCost = parseCost(marketTableModel.getValueAt(selectedRow, 2));
        int itemQty = Integer.parseInt(String.valueOf(marketTableModel.getValueAt(selectedRow, 3)));
        String itemName = String.valueOf(marketTableModel.getValueAt(selectedRow, 0));
        buySelectedButton.setEnabled(itemCost <= currentTeamGold && itemQty > 0);
        sellSelectedButton.setEnabled(localPlayerOwnsItem(itemName));
    }

    private int parseCost(Object rawValue) {
        if (rawValue instanceof Integer intValue) {
            return intValue;
        }
        try {
            return Integer.parseInt(String.valueOf(rawValue));
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }
}
