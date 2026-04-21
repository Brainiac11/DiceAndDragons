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
import src.item.ItemEnum;
import src.item.VillageMarketplaceCatalog;
import src.networking.LobbyState;
import src.networking.PlayerInfo;
import src.players.DragonCatalog;
import src.players.Heroes;

public class GameScreen extends JPanel {
    private static final int kBoardWidth = 320;
    private static final int kBoardHeight = 460;
    private static final int kItemSlotCount = 2;
    private static final int kSkillSlotCount = 6;
    private static final int kStartingLevel = 1;
    private static final int kDefaultXp = 0;
    private static final String kBoardBakupPath = "imgs/blank_sheet.png";

    private static final Map<String, String> kHeroToSheetPaths = createHeroToSheetPathMap();
    private static final Map<String, String[]> kHeroToSkills = createHeroToSkillsMap();
    private static final Map<String, HeroStats> kHeroToStats = createHeroToStatsMap();

    private JPanel boardsPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private Consumer<String> chatSender;
    private JLabel dragonInfoLabel;
    private JLabel marketInfoLabel;
    private JLabel teamGoldInfoLabel;

    private Map<String, BoardState> boardStateByPlayer;
    private Map<String, ImageIcon> boardIconCache;

    public GameScreen() {
        this(null, null, null);
    }

    // bs'd all the values for ts
    public GameScreen(LobbyState lobbyState, String localHandle, Consumer<String> chatSender) {
        this.chatSender = chatSender;
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

        JPanel encounterInfoPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        encounterInfoPanel.setBorder(BorderFactory.createTitledBorder("Encounter"));
        dragonInfoLabel = new JLabel("Dragon: None");
        marketInfoLabel = new JLabel("Village: None");
        teamGoldInfoLabel = new JLabel("Team Gold: 0");
        encounterInfoPanel.add(dragonInfoLabel);
        encounterInfoPanel.add(marketInfoLabel);
        encounterInfoPanel.add(teamGoldInfoLabel);
        sidePanel.add(encounterInfoPanel, BorderLayout.NORTH);

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
        refreshEncounterInfo(lobbyState);
        refreshChat(lobbyState);
    }

    public void updateFromLobbyState(LobbyState lobbyState) {
        rebuildBoards(lobbyState, null);
        refreshEncounterInfo(lobbyState);
        refreshChat(lobbyState);
    }

    private void refreshEncounterInfo(LobbyState lobbyState) {
        int teamGold = lobbyState == null ? 0 : Math.max(0, lobbyState.teamGold);
        teamGoldInfoLabel.setText("Team Gold: " + teamGold);

        if (lobbyState == null) {
            dragonInfoLabel.setText("Dragon: None");
            marketInfoLabel.setText("Village: None");
            return;
        }

        DragonCatalog.DragonProfile dragon = DragonCatalog.findBySelection(lobbyState.selectedDragon);
        if (dragon == null) {
            dragonInfoLabel.setText("Dragon: None");
            marketInfoLabel.setText("Village: None");
            return;
        }

        dragonInfoLabel.setText("Dragon: " + dragon.getDisplayName());
        VillageMarketplaceCatalog.Marketplace market = VillageMarketplaceCatalog.forDragon(dragon.getId());
        if (market == null) {
            marketInfoLabel.setText("Village: Unknown");
            return;
        }

        marketInfoLabel.setText("Village: " + market.getVillageName());
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
        String selectedDragon = lobbyState == null ? null : lobbyState.selectedDragon;
        int teamGold = lobbyState == null ? 0 : Math.max(0, lobbyState.teamGold);

        ArrayList<PlayerInfo> players = new ArrayList<>();
        if (lobbyState != null && lobbyState.players != null) {
            players = lobbyState.players;
        }

        LinkedHashMap<String, BoardState> nextBoardStateMap = new LinkedHashMap<>();

        if (players.isEmpty()) {
            PlayerInfo fallbackPlayer = new PlayerInfo(
                    localHandle == null || localHandle.isBlank() ? "Player" : localHandle);
            fallbackPlayer.hero = Heroes.WARRIOR;
            BoardState state = buildBoardState(fallbackPlayer, 0, selectedDragon, teamGold);
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
                    boardState = buildBoardState(copy, index, selectedDragon, teamGold);
                } else {
                    boardState = existing;
                    applyBoardBaseValues(boardState, hero, teamGold);
                    updateBoardFromPurchases(boardState, selectedDragon, player.purchasedItems);
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

    private BoardState buildBoardState(PlayerInfo player, int index, String selectedDragon, int teamGold) {
        String handle = cleanHandle(player.handle, index);
        String hero = cleanHero(player.hero);

        BoardState state = new BoardState();
        state.handle = handle;
        state.hero = hero;
        state.sheetPath = resolveSheetPath(hero);
        applyBoardBaseValues(state, hero, teamGold);

        String[] heroSkills = getSkillsForHero(hero);
        state.skills = new SkillSlot[kSkillSlotCount];

        for (int i = 0; i < kSkillSlotCount; i++) {
            SkillSlot slot = new SkillSlot();
            if (i < heroSkills.length) {
                slot.name = heroSkills[i];
                slot.locked = false;
                slot.covered = false;
            } else {
                slot.name = "Locked";
                slot.locked = true;
                slot.covered = true;
            }
            state.skills[i] = slot;
        }

        state.baseSkillNames = new String[kSkillSlotCount];
        state.baseSkillCovered = new boolean[kSkillSlotCount];
        state.baseSkillLocked = new boolean[kSkillSlotCount];
        for (int i = 0; i < kSkillSlotCount; i++) {
            SkillSlot slot = state.skills[i];
            state.baseSkillNames[i] = slot.name;
            state.baseSkillCovered[i] = slot.covered;
            state.baseSkillLocked[i] = slot.locked;
        }

        updateBoardFromPurchases(state, selectedDragon, player.purchasedItems);
        return state;
    }

    private void applyBoardBaseValues(BoardState state, String hero, int teamGold) {
        HeroStats stats = getHeroStats(hero);
        state.level = kStartingLevel;
        state.exp = kDefaultXp;
        state.gold = teamGold;
        state.hitPoints = stats.hitPoints;
        state.armourClass = stats.armourClass;
        state.initiative = stats.initiative;
    }

    private void updateBoardFromPurchases(BoardState boardState, String selectedDragon,
            ArrayList<String> purchasedItems) {
        String purchasedSignature = buildPurchasedSignature(selectedDragon, purchasedItems);
        if (purchasedSignature.equals(boardState.purchasedSignature)) {
            return;
        }

        boardState.purchasedSignature = purchasedSignature;
        resetSkillSlotsToBase(boardState);
        boardState.items = new String[kItemSlotCount];

        ArrayList<String> boughtItems = new ArrayList<>();
        ArrayList<String> boughtSkills = new ArrayList<>();

        if (purchasedItems != null) {
            for (String item : purchasedItems) {
                if (item == null) {
                    continue;
                }
                String clean = item.trim();
                if (clean.isEmpty()) {
                    continue;
                }

                VillageMarketplaceCatalog.MarketplaceItem marketEntry = VillageMarketplaceCatalog.findItem(
                        selectedDragon, clean);
                if (marketEntry != null && marketEntry.getType() == ItemEnum.SKILL) {
                    boughtSkills.add(clean);
                } else {
                    boughtItems.add(clean);
                }
            }
        }

        int slotIndex = 0;
        for (String item : boughtItems) {
            if (slotIndex >= kItemSlotCount) {
                break;
            }
            boardState.items[slotIndex] = item;
            slotIndex++;
        }

        while (slotIndex < kItemSlotCount) {
            boardState.items[slotIndex] = "Empty";
            slotIndex++;
        }

        for (int i = 0; i < boughtSkills.size() && i < kSkillSlotCount; i++) {
            SkillSlot slot = boardState.skills[i];
            slot.name = boughtSkills.get(i);
            slot.locked = false;
            slot.covered = false;
            for (int j = 0; j < slot.spots.length; j++) {
                slot.spots[j] = "";
            }
        }
    }

    private void resetSkillSlotsToBase(BoardState boardState) {
        if (boardState.baseSkillNames == null || boardState.skills == null) {
            return;
        }

        for (int i = 0; i < kSkillSlotCount; i++) {
            SkillSlot slot = boardState.skills[i];
            slot.name = boardState.baseSkillNames[i];
            slot.covered = boardState.baseSkillCovered[i];
            slot.locked = boardState.baseSkillLocked[i];
            for (int j = 0; j < slot.spots.length; j++) {
                slot.spots[j] = "";
            }
        }
    }

    private String buildPurchasedSignature(String selectedDragon, ArrayList<String> purchasedItems) {
        StringBuilder sb = new StringBuilder();
        if (selectedDragon != null) {
            String cleanDragon = selectedDragon.trim();
            if (!cleanDragon.isEmpty()) {
                sb.append(cleanDragon);
            }
        }
        sb.append("|");

        if (purchasedItems == null || purchasedItems.isEmpty()) {
            return sb.toString();
        }

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
            return new String[0];
        }
        return skills;
    }

    private HeroStats getHeroStats(String hero) {
        HeroStats stats = kHeroToStats.get(hero);
        if (stats == null) {
            return new HeroStats(0, 0, 0);
        }
        return stats;
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

    private static Map<String, HeroStats> createHeroToStatsMap() {
        LinkedHashMap<String, HeroStats> map = new LinkedHashMap<>();

        map.put(Heroes.WARRIOR, new HeroStats(60, 3, 2));
        map.put(Heroes.WIZARD, new HeroStats(42, 1, 4));
        map.put(Heroes.CLERIC, new HeroStats(50, 2, 3));
        map.put(Heroes.RANGER, new HeroStats(48, 2, 4));
        map.put(Heroes.ROGUE, new HeroStats(44, 1, 5));

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
            boardLayer.add(backgroundLabel, -1);

            lvlButton = createSpotButton();
            lvlButton.setBounds(230, 48, 60, 22);

            expButton = createSpotButton();
            expButton.setBounds(230, 83, 60, 22);

            goldButton = createSpotButton();
            goldButton.setBounds(230, 118, 60, 22);

            hpButton = createSpotButton();
            hpButton.setBounds(160, 80, 45, 30);

            armourButton = createSpotButton();
            armourButton.setBounds(73, 80, 45, 30);

            initiativeButton = createSpotButton();
            initiativeButton.setBounds(20, 20, 50, 20);

            boardLayer.add(lvlButton, 0);
            boardLayer.add(expButton, 0);
            boardLayer.add(goldButton, 0);
            boardLayer.add(hpButton, 0);
            boardLayer.add(armourButton, 0);
            boardLayer.add(initiativeButton, 0);

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
                        return;
                    }
                    slot.covered = !slot.covered;
                    refreshBoardText();
                });
                skillButtons[i] = skillButton;
                boardLayer.add(skillButton, 0);

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
                    boardLayer.add(spotButton, 0);
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
                JButton itemButton = createSpotButton();
                itemButton.setBounds(itemCoordinates[i][0], itemCoordinates[i][1], itemCoordinates[i][2],
                        itemCoordinates[i][3]);
                itemButtons[i] = itemButton;
                boardLayer.add(itemButton, 0);
            }

            add(boardLayer, BorderLayout.CENTER);
            refreshBoardText();
        }

        private JButton createSpotButton() {
            JButton button = new JButton();
            button.setMargin(new Insets(1, 2, 1, 2));
            button.setFont(new Font("Dialog", Font.BOLD, 10));
            button.setBackground(new Color(255, 248, 220));
            button.setFocusPainted(true);
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
        private String[] baseSkillNames;
        private boolean[] baseSkillCovered;
        private boolean[] baseSkillLocked;
        private String[] items;
        private String purchasedSignature;
    }

    private static final class SkillSlot {
        private String name;
        private boolean covered;
        private boolean locked;
        private final String[] spots = new String[4];
    }

    private static final class HeroStats {
        private final int hitPoints;
        private final int armourClass;
        private final int initiative;

        private HeroStats(int hitPoints, int armourClass, int initiative) {
            this.hitPoints = hitPoints;
            this.armourClass = armourClass;
            this.initiative = initiative;
        }
    }
}
