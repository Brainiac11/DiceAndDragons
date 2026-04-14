package src.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import src.networking.LobbyState;
import src.networking.PlayerInfo;
import src.players.Heroes;

public class GameScreen extends JPanel {
    private static final int kBoardWidth = 320;
    private static final int kBoardHeight = 460;
    private static final int kItemSlotCount = 2;
    private static final int kSkillSlotCount = 6;
    private static final String kBoardBakupPath = "imgs/blank_sheet.png";

    private static final Map<String, String> kHeroToSheetPaths = createHeroToSheetPathMap();
    private static final Map<String, String[]> kHeroToSkills = createHeroToSkillsMap();
    private static final Map<String, String[]> kHeroToItems = createHeroToItemsMap();

    private JPanel boardsPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private Consumer<String> chatSender;

    private Random alllahuakbar;
    private Map<String, BoardState> boardStateByPlayer;
    private Map<String, ImageIcon> boardIconCache;

    public GameScreen() {
        this(null, null, null);
    }

    // bs'd all the values for ts
    public GameScreen(LobbyState lobbyState, String localHandle, Consumer<String> chatSender) {
        this.chatSender = chatSender;
        this.alllahuakbar = new Random();
        this.boardStateByPlayer = new LinkedHashMap<>();
        this.boardIconCache = new HashMap<>();

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        boardsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        JScrollPane boardScrollPane = new JScrollPane(boardsPanel);
        boardScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(boardScrollPane, BorderLayout.CENTER);

        JPanel sidePanel = new JPanel(new BorderLayout(8, 8));
        sidePanel.setPreferredSize(new Dimension(360, 10));

        chatArea = new JTextArea(14, 28);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        chatInput = new JTextField(20);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendChatMessage());
        chatInput.addActionListener(e -> sendChatMessage());

        JPanel chatInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chatInputPanel.add(chatInput);
        chatInputPanel.add(sendButton);

        JPanel chatPanel = new JPanel(new BorderLayout(6, 6));
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        sidePanel.add(chatPanel, BorderLayout.CENTER);

        add(sidePanel, BorderLayout.EAST);

        rebuildBoards(lobbyState, localHandle);
        refreshChat(lobbyState);
    }

    public void updateFromLobbyState(LobbyState lobbyState) {
        rebuildBoards(lobbyState, null);
        refreshChat(lobbyState);
    }

    private void sendChatMessage() {
        String text = "";
        if (chatInput.getText() != null) {
            text = chatInput.getText().trim();
        }

        if (text.isEmpty()) {
            return;
        }

        if (chatSender != null) {
            chatSender.accept(text);
        } else {
            chatArea.append("Me: " + text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }

        chatInput.setText("");
    }

    private void refreshChat(LobbyState lobbyState) {
        if (lobbyState == null || lobbyState.chat == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String line : lobbyState.chat) {
            sb.append(line).append("\n");
        }
        chatArea.setText(sb.toString());
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void rebuildBoards(LobbyState lobbyState, String localHandle) {
        boardsPanel.removeAll();

        ArrayList<PlayerInfo> players = new ArrayList<>();
        if (lobbyState != null && lobbyState.players != null) {
            players = lobbyState.players;
        }

        LinkedHashMap<String, BoardState> nextBoardStateMap = new LinkedHashMap<>();

        if (players.isEmpty()) {
            PlayerInfo fallbackPlayer = new PlayerInfo(
                    localHandle == null || localHandle.isBlank() ? "Player" : localHandle);
            fallbackPlayer.hero = Heroes.WARRIOR;
            BoardState state = buildBoardState(fallbackPlayer, 0);
            nextBoardStateMap.put(state.handle, state);
            boardsPanel.add(new PlayerBoardPanel(state));
        } else {
            int index = 0;
            for (PlayerInfo player : players) {
                if (player == null) {
                    continue;
                }

                String handle = cleanHandle(player.handle, index);
                String hero = cleanHero(player.hero);
                BoardState existing = boardStateByPlayer.get(handle);

                BoardState boardState;
                if (existing == null || !existing.hero.equals(hero)) {
                    PlayerInfo copy = new PlayerInfo(handle);
                    copy.hero = hero;
                    if (player.purchasedItems != null) {
                        copy.purchasedItems = new ArrayList<>(player.purchasedItems);
                    }
                    boardState = buildBoardState(copy, index);
                } else {
                    boardState = existing;
                    updateBoardItemsFromPurchases(boardState, player.purchasedItems);
                }

                nextBoardStateMap.put(handle, boardState);
                boardsPanel.add(new PlayerBoardPanel(boardState));
                index++;
            }
        }

        boardStateByPlayer.clear();
        boardStateByPlayer.putAll(nextBoardStateMap);

        boardsPanel.revalidate();
        boardsPanel.repaint();
    }

    private BoardState buildBoardState(PlayerInfo player, int index) {
        String handle = cleanHandle(player.handle, index);
        String hero = cleanHero(player.hero);

        BoardState state = new BoardState();
        state.handle = handle;
        state.hero = hero;
        state.sheetPath = resolveSheetPath(hero);

        state.level = randomBetween(1, 5);
        state.exp = randomBetween(8, 30);
        state.gold = randomBetween(0, 15);
        state.hitPoints = randomBetween(35, 95);
        state.armourClass = randomBetween(0, 3);
        state.initiative = randomBetween(1, 12);

        String[] heroSkills = getSkillsForHero(hero);
        int activeSkillCount = randomBetween(2, Math.min(heroSkills.length, kSkillSlotCount));
        state.skills = new SkillSlot[kSkillSlotCount];

        for (int i = 0; i < kSkillSlotCount; i++) {
            SkillSlot slot = new SkillSlot();
            if (i < activeSkillCount) {
                slot.name = heroSkills[i % heroSkills.length];
                slot.locked = false;
                slot.covered = alllahuakbar.nextBoolean();
            } else {
                slot.name = "Locked";
                slot.locked = true;
                slot.covered = true;
            }
            state.skills[i] = slot;
        }

        updateBoardItemsFromPurchases(state, player.purchasedItems);
        return state;
    }

    private void updateBoardItemsFromPurchases(BoardState boardState, ArrayList<String> purchasedItems) {
        String purchasedSignature = buildPurchasedSignature(purchasedItems);
        if (purchasedSignature.equals(boardState.purchasedSignature)) {
            return;
        }

        boardState.purchasedSignature = purchasedSignature;
        boardState.items = new String[kItemSlotCount];

        int slotIndex = 0;
        if (purchasedItems != null) {
            for (String item : purchasedItems) {
                if (item == null) {
                    continue;
                }
                String clean = item.trim();
                if (clean.isEmpty()) {
                    continue;
                }
                if (slotIndex >= kItemSlotCount) {
                    break;
                }
                boardState.items[slotIndex] = clean;
                slotIndex++;
            }
        }

        while (slotIndex < kItemSlotCount) {
            if (alllahuakbar.nextBoolean()) {
                boardState.items[slotIndex] = randomItemForHero(boardState.hero);
            } else {
                boardState.items[slotIndex] = "Empty";
            }
            slotIndex++;
        }
    }

    private String buildPurchasedSignature(ArrayList<String> purchasedItems) {
        if (purchasedItems == null || purchasedItems.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String item : purchasedItems) {
            if (item == null) {
                continue;
            }
            String clean = item.trim();
            if (clean.isEmpty()) {
                continue;
            }
            sb.append(clean).append("|");
        }
        return sb.toString();
    }

    private String cleanHandle(String handle, int index) {
        if (handle == null || handle.isBlank()) {
            return "Player " + (index + 1);
        }
        return handle.trim();
    }

    private String cleanHero(String hero) {
        if (hero == null || hero.isBlank()) {
            return "No Hero";
        }
        return hero.trim();
    }

    private int randomBetween(int min, int maxInclusive) {
        if (maxInclusive <= min) {
            return min;
        }
        return min + alllahuakbar.nextInt((maxInclusive - min) + 1);
    }

    private String resolveSheetPath(String hero) {
        String path = kHeroToSheetPaths.get(hero);
        if (path == null) {
            return kBoardBakupPath;
        }
        return path;
    }

    private String[] getSkillsForHero(String hero) {
        String[] skills = kHeroToSkills.get(hero);
        if (skills == null || skills.length == 0) {
            return new String[] { "Strike", "Critical Hit", "Defend", "Counter", "Focus", "Brace" };
        }
        return skills;
    }

    private String randomItemForHero(String hero) {
        String[] heroItems = kHeroToItems.get(hero);
        if (heroItems == null || heroItems.length == 0) {
            return "Potion";
        }
        return heroItems[alllahuakbar.nextInt(heroItems.length)];
    }

    private ImageIcon getBoardIcon(String path) {
        String cleanPath = path == null || path.isBlank() ? kBoardBakupPath : path;
        ImageIcon cached = boardIconCache.get(cleanPath);
        if (cached != null) {
            return cached;
        }

        ImageIcon icon = null;
        try {
            BufferedImage raw = ImageIO.read(new File(cleanPath));
            if (raw != null) {
                Image scaled = raw.getScaledInstance(kBoardWidth, kBoardHeight, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaled);
            }
        } catch (IOException e) {
            icon = null;
        }

        if (icon == null) {
            try {
                BufferedImage raw = ImageIO.read(new File(kBoardBakupPath));
                if (raw != null) {
                    Image scaled = raw.getScaledInstance(kBoardWidth, kBoardHeight, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaled);
                }
            } catch (IOException e) {
                icon = null;
            }
        }

        if (icon == null) {
            BufferedImage placeholder = new BufferedImage(kBoardWidth, kBoardHeight, BufferedImage.TYPE_INT_ARGB);
            icon = new ImageIcon(placeholder);
        }

        boardIconCache.put(cleanPath, icon);
        return icon;
    }

    private static Map<String, String> createHeroToSheetPathMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(Heroes.WARRIOR, "imgs/warrior_sheet.png");
        map.put(Heroes.WIZARD, "imgs/wizard_sheet.png");
        map.put(Heroes.CLERIC, "imgs/cleric_sheet.png");
        map.put(Heroes.RANGER, "imgs/ranger_sheet.png");
        map.put(Heroes.ROGUE, "imgs/rogue_sheet.png");
        return map;
    }

    private static Map<String, String[]> createHeroToSkillsMap() {
        LinkedHashMap<String, String[]> map = new LinkedHashMap<>();

        map.put(Heroes.WARRIOR,
                new String[] { "Slash", "Smashing Blow", "Savage Attack", "Parry", "Strike", "Critical Hit" });

        map.put(Heroes.WIZARD,
                new String[] { "Lightning Storm", "Genie", "Magic Bolt", "Fireball", "Shield", "Drain Life" });

        map.put(Heroes.CLERIC,
                new String[] { "Blessing", "Smite", "Healing Hands", "Holy Storm", "Holy Water", "Heal" });

        map.put(Heroes.RANGER,
                new String[] { "Pin Down", "Bestial Pounce", "Throwing Axe", "Accurate Shot", "Dual Shot",
                        "Crossfire" });

        map.put(Heroes.ROGUE,
                new String[] { "Sneak Attack", "Deflect", "Stab", "Flanking Blow", "Sudden Death", "Poison Tip" });

        return map;
    }

    private static Map<String, String[]> createHeroToItemsMap() {
        LinkedHashMap<String, String[]> map = new LinkedHashMap<>();

        map.put(Heroes.WARRIOR,
                new String[] { "Steel Shield", "Great Haste Potion", "Blessed Hammer", "Gauntlets of Power" });

        map.put(Heroes.WIZARD,
                new String[] { "Mana Potion", "Magic Staff", "Scroll of Knowledge", "Magic Bracelet" });

        map.put(Heroes.CLERIC,
                new String[] { "Healing Potion", "Holy Water", "Staff of Healing", "Scroll of Knowledge" });

        map.put(Heroes.RANGER,
                new String[] { "Pimpout Crossbow", "Vision Potion", "Stealth Potion", "Great Haste Potion" });

        map.put(Heroes.ROGUE,
                new String[] { "Stealth Cloak", "Stealth Potion", "Magic Sword", "Haste Potion" });

        return map;
    }

    private class PlayerBoardPanel extends JPanel {
        private BoardState boardState;

        private JButton lvlButton;
        private JButton expButton;
        private JButton goldButton;
        private JButton hpButton;
        private JButton armourButton;
        private JButton initiativeButton;

        private JButton[] skillButtons;
        private JButton[][] skillSpotButtons;
        private JButton[] itemButtons;

        private PlayerBoardPanel(BoardState boardState) {
            this.boardState = boardState;

            setLayout(new BorderLayout(4, 4));
            setBorder(BorderFactory.createTitledBorder(boardState.handle + " - " + boardState.hero));

            JLayeredPane boardLayer = new JLayeredPane();
            boardLayer.setLayout(null);
            boardLayer.setPreferredSize(new Dimension(kBoardWidth, kBoardHeight));

            JLabel backgroundLabel = new JLabel(getBoardIcon(boardState.sheetPath));
            backgroundLabel.setBounds(0, 0, kBoardWidth, kBoardHeight);
            boardLayer.add(backgroundLabel, 0);

            lvlButton = createSpotButton();
            lvlButton.setBounds(230, 48, 60, 22);
            lvlButton.addActionListener(e -> {
                boardState.level = randomBetween(1, 10);
                refreshBoardText();
            });

            expButton = createSpotButton();
            expButton.setBounds(230, 83, 60, 22);
            expButton.addActionListener(e -> {
                boardState.exp = randomBetween(8, 30);
                refreshBoardText();
            });

            goldButton = createSpotButton();
            goldButton.setBounds(230, 118, 60, 22);
            goldButton.addActionListener(e -> {
                boardState.gold = randomBetween(0, 15);
                refreshBoardText();
            });

            hpButton = createSpotButton();
            hpButton.setBounds(160, 80, 45, 30);
            hpButton.addActionListener(e -> {
                boardState.hitPoints = randomBetween(35, 95);
                refreshBoardText();
            });

            armourButton = createSpotButton();
            armourButton.setBounds(73, 80, 45, 30);
            armourButton.addActionListener(e -> {
                boardState.armourClass = randomBetween(0, 3);
                refreshBoardText();
            });

            initiativeButton = createSpotButton();
            initiativeButton.setBounds(20, 20, 50, 20);
            initiativeButton.addActionListener(e -> {
                boardState.initiative = randomBetween(1, 12);
                refreshBoardText();
            });

            boardLayer.add(lvlButton, 1);
            boardLayer.add(expButton, 1);
            boardLayer.add(goldButton, 1);
            boardLayer.add(hpButton, 1);
            boardLayer.add(armourButton, 1);
            boardLayer.add(initiativeButton, 1);

            skillButtons = new JButton[kSkillSlotCount];
            skillSpotButtons = new JButton[kSkillSlotCount][4];
            int skillY = 175;
            for (int i = 0; i < kSkillSlotCount; i++) {
                final int skillIndex = i;
                JButton skillButton = createSpotButton();
                skillButton.setBounds(15, skillY, 115, 22);
                skillButton.addActionListener(e -> {
                    SkillSlot slot = boardState.skills[skillIndex];
                    if (slot.locked) {
                        slot.locked = false;
                        slot.name = getSkillsForHero(boardState.hero)[alllahuakbar
                                .nextInt(getSkillsForHero(boardState.hero).length)];
                        slot.covered = true;
                    } else {
                        slot.covered = !slot.covered;
                    }
                    refreshBoardText();
                });
                skillButtons[i] = skillButton;
                boardLayer.add(skillButton, 1);

                int spotX = 135;
                for (int j = 0; j < 4; j++) {
                    int spotIndex = j;
                    JButton spotButton = createSpotButton();
                    spotButton.setBounds(spotX, skillY, 25, 22);
                    spotButton.addActionListener(e -> {
                        SkillSlot slot = boardState.skills[skillIndex];
                        if (!slot.locked && !slot.covered) {
                            String[] possible = { "", "S", "H", "M", "B" };
                            String current = slot.spots[spotIndex];
                            if (current == null) {
                                current = "";
                            }

                            int nextIdx = 0;
                            for (int k = 0; k < possible.length; k++) {
                                if (possible[k].equals(current)) {
                                    nextIdx = (k + 1) % possible.length;
                                    break;
                                }
                            }
                            slot.spots[spotIndex] = possible[nextIdx];
                            refreshBoardText();
                        }
                    });
                    skillSpotButtons[i][j] = spotButton;
                    boardLayer.add(spotButton, 1);
                    spotX += 35;
                }

                skillY += 38;
            }

            itemButtons = new JButton[kItemSlotCount];
            int[][] itemCoordinates = {
                    { 10, 137, 105, 23 },
                    { 120, 137, 105, 23 }
            };

            for (int i = 0; i < kItemSlotCount; i++) {
                final int itemIndex = i;
                JButton itemButton = createSpotButton();
                itemButton.setBounds(itemCoordinates[i][0], itemCoordinates[i][1], itemCoordinates[i][2],
                        itemCoordinates[i][3]);
                itemButton.addActionListener(e -> {
                    boardState.items[itemIndex] = randomItemForHero(boardState.hero);
                    refreshBoardText();
                });
                itemButtons[i] = itemButton;
                boardLayer.add(itemButton, 1);
            }

            add(boardLayer, BorderLayout.CENTER);
            refreshBoardText();
        }

        private JButton createSpotButton() {
            JButton button = new JButton();
            button.setMargin(new Insets(1, 2, 1, 2));
            button.setFont(new Font("Dialog", Font.BOLD, 10));
            button.setBackground(new Color(255, 248, 220));
            button.setFocusPainted(false);
            return button;
        }

        private void refreshBoardText() {
            lvlButton.setText("Lvl " + boardState.level);
            expButton.setText("EXP " + boardState.exp);
            goldButton.setText("Gold " + boardState.gold);
            hpButton.setText("HP " + boardState.hitPoints);
            armourButton.setText("AC " + boardState.armourClass);
            initiativeButton.setText("Init " + boardState.initiative);

            for (int i = 0; i < skillButtons.length; i++) {
                JButton button = skillButtons[i];
                SkillSlot slot = boardState.skills[i];

                if (slot.locked) {
                    button.setText("Locked");
                    button.setBackground(new Color(70, 70, 70));
                    button.setForeground(Color.WHITE);
                    for (JButton sb : skillSpotButtons[i])
                        sb.setVisible(false);
                } else if (slot.covered) {
                    button.setText("Covered");
                    button.setBackground(new Color(120, 120, 120));
                    button.setForeground(Color.WHITE);
                    for (JButton sb : skillSpotButtons[i])
                        sb.setVisible(false);
                } else {
                    button.setText(slot.name);
                    button.setBackground(new Color(255, 248, 220));
                    button.setForeground(Color.BLACK);
                    for (int j = 0; j < 4; j++) {
                        JButton sb = skillSpotButtons[i][j];
                        sb.setVisible(true);
                        String val = slot.spots[j];
                        sb.setText((val == null || val.isEmpty()) ? "" : val);
                    }
                }
            }

            for (int i = 0; i < itemButtons.length; i++) {
                itemButtons[i].setText(boardState.items[i]);
            }
        }
    }

    private static final class BoardState {
        private String handle;
        private String hero;
        private String sheetPath;

        private int level;
        private int exp;
        private int gold;
        private int hitPoints;
        private int armourClass;
        private int initiative;

        private SkillSlot[] skills;
        private String[] items;
        private String purchasedSignature;
    }

    private static final class SkillSlot {
        private String name;
        private boolean covered;
        private boolean locked;
        private final String[] spots = new String[4];
    }
}
