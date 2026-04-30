package src.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import src.dice.DiceEnum;
import src.item.ItemEnum;
import src.item.VillageMarketplaceCatalog;
import src.networking.GameMessage;
import src.networking.LobbyState;
import src.networking.PlayerInfo;
import src.players.DragonCatalog;
import src.players.Heroes;

public class GameScreen extends JPanel {
    private static final int kBoardWidth = 320;
    private static final int kBoardHeight = 460;
    private static final int kItemSlotCount = 2;
    private static final int kSkillSlotCount = 8;
    private static final int kSkillSymbolCount = 5;
    private static final int kDiceCount = 5;
    private static final int kStartingLevel = 1;
    private static final int kDefaultXp = 0;
    private static final String kBoardBakupPath = "imgs/blank_sheet.png";
    private static final String kSkillSymbolMatch = "=";
    private static final String kSkillSymbolDifferent = "!=";
    private static final String kSkillSymbolSword = "S";
    private static final String kSkillSymbolMagic = "M";
    private static final String kSkillSymbolCrossbow = "C";
    private static final String kSkillSymbolDagger = "D";
    private static final String kSkillSymbolHammer = "H";

    private static final Map<String, String> kHeroToSheetPaths = createHeroToSheetPathMap();
    private static final Map<String, String[]> kHeroToSkills = createHeroToSkillsMap();
    private static final Map<String, String[]> kSkillToRequiredSymbols = createSkillToRequiredSymbolsMap();
    private static final Map<String, HeroStats> kHeroToStats = createHeroToStatsMap();

    private static final Map<DiceEnum, String> kDiceImagePaths = createDiceImagePathMap();
    private final Map<DiceEnum, ImageIcon> diceImageCache = new HashMap<>();

    private JPanel boardsPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private Consumer<String> chatSender;
    private Consumer<GameMessage> gameActionSender;
    private JLabel dragonInfoLabel;
    private JLabel marketInfoLabel;
    private JLabel teamGoldInfoLabel;

    private JPanel dicePanel;
    private JButton[] diceButtons;
    private JButton rollButton;
    private JButton clearSelectionButton;
    private JButton endTurnButton;

    private String localHandle;
    private String primaryHandle;
    private boolean canControlDice;
    private boolean[] localDiceSelection;
    private int activeDieIndex;
    private DiceEnum[] dicePool;
    private int[] diceSkillIndex;
    private int[] diceSymbolIndex;
    private boolean[] usedSkills;
    private PlayerBoardPanel primaryBoardPanel;

    private Map<String, BoardState> boardStateByPlayer;
    private Map<String, ImageIcon> boardIconCache;
    private final Map<String, ImageIcon> symbolIconCache;

    public GameScreen() {
        this(null, null, null, null);
    }

    public GameScreen(LobbyState lobbyState, String localHandle, Consumer<String> chatSender) {
        this(lobbyState, localHandle, chatSender, null);
    }

    public GameScreen(LobbyState lobbyState, String localHandle, Consumer<String> chatSender,
            Consumer<GameMessage> gameActionSender) {
        this.chatSender = chatSender;
        this.gameActionSender = gameActionSender;
        this.localHandle = localHandle;
        this.boardStateByPlayer = new LinkedHashMap<>();
        this.boardIconCache = new HashMap<>();
        this.symbolIconCache = new HashMap<>();
        this.localDiceSelection = new boolean[kDiceCount];
        this.activeDieIndex = -1;
        initializeLocalDiceState();

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        boardsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        JScrollPane boardScrollPane = new JScrollPane(boardsPanel);
        boardScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel centerWrapper = new JPanel(new BorderLayout(6, 6));
        JPanel dicePanelInstance = buildDicePanel();
        centerWrapper.add(dicePanelInstance, BorderLayout.NORTH);
        centerWrapper.add(boardScrollPane, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);
        dicePanel = dicePanelInstance;

        JPanel sidePanel = new JPanel(new BorderLayout(8, 8));
        sidePanel.setPreferredSize(new Dimension(360, 10));

        JPanel encounterInfoPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        encounterInfoPanel.setBorder(BorderFactory.createTitledBorder("Encounter"));
        dragonInfoLabel = new JLabel("Dragon: None");
        marketInfoLabel = new JLabel("Village: None");
        teamGoldInfoLabel = new JLabel("Team Gold: 0");
        endTurnButton = new JButton("End Turn");
        encounterInfoPanel.add(dragonInfoLabel);
        encounterInfoPanel.add(marketInfoLabel);
        encounterInfoPanel.add(teamGoldInfoLabel);
        encounterInfoPanel.add(endTurnButton);
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

        dicePanel = null;

        add(sidePanel, BorderLayout.EAST);

        rebuildBoards(lobbyState, localHandle);
        refreshEncounterInfo(lobbyState);
        refreshChat(lobbyState);
    }

    public void updateFromLobbyState(LobbyState lobbyState) {
        rebuildBoards(lobbyState, localHandle);
        refreshEncounterInfo(lobbyState);
        refreshChat(lobbyState);
    }

    private void initializeLocalDiceState() {
        dicePool = new DiceEnum[kDiceCount];
        for (int i = 0; i < kDiceCount; i++) {
            dicePool[i] = DiceEnum.SWORD;
        }
        diceSkillIndex = new int[kDiceCount];
        diceSymbolIndex = new int[kDiceCount];
        usedSkills = new boolean[kSkillSlotCount];
        for (int i = 0; i < kDiceCount; i++) {
            diceSkillIndex[i] = -1;
            diceSymbolIndex[i] = -1;
        }
    }

    private JPanel buildDicePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Dice"));

        JPanel diceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        diceButtons = new JButton[kDiceCount];
        for (int i = 0; i < kDiceCount; i++) {
            final int dieIndex = i;
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(46, 40));
            button.setFont(new Font("Dialog", Font.BOLD, 12));
            button.setOpaque(true);
            button.setIcon(createDiceIcon(dicePool == null ? null : dicePool[i]));
            button.setText("");
            button.addActionListener(e -> handleDiceButtonClick(dieIndex));
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && canControlDice && !isDiePlaced(dieIndex)
                            && gameActionSender != null) {
                        System.out.println("DIe Index; " + dieIndex);
                        gameActionSender.accept(new GameMessage(GameMessage.DICE_ROLL, new int[] { dieIndex }));
                    }
                }
            });
            diceButtons[i] = button;
            diceRow.add(button);
        }

        rollButton = new JButton("Roll Selected");
        rollButton.addActionListener(e -> rollSelectedDice());

        clearSelectionButton = new JButton("Clear Selection");
        clearSelectionButton.addActionListener(e -> clearDiceSelection());

        JPanel diceControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        diceControls.add(rollButton);
        diceControls.add(clearSelectionButton);

        panel.add(diceRow, BorderLayout.CENTER);
        panel.add(diceControls, BorderLayout.SOUTH);
        return panel;
    }

    private void updateDiceStateFromLobby(LobbyState lobbyState) {
        DiceEnum[] incomingPool = lobbyState == null ? null : lobbyState.dicePool;
        int[] incomingSkillIndex = lobbyState == null ? null : lobbyState.diceSkillIndex;
        int[] incomingSymbolIndex = lobbyState == null ? null : lobbyState.diceSymbolIndex;
        boolean[] incomingUsedSkills = lobbyState == null ? null : lobbyState.usedSkills;

        dicePool = normalizeDicePool(incomingPool);
        diceSkillIndex = normalizeIntArray(incomingSkillIndex, kDiceCount, -1);
        diceSymbolIndex = normalizeIntArray(incomingSymbolIndex, kDiceCount, -1);
        usedSkills = normalizeBooleanArray(incomingUsedSkills, kSkillSlotCount);
        syncLocalDiceSelection();
    }

    private DiceEnum[] normalizeDicePool(DiceEnum[] pool) {
        DiceEnum[] normalized = new DiceEnum[kDiceCount];
        if (pool != null) {
            for (int i = 0; i < Math.min(pool.length, kDiceCount); i++) {
                normalized[i] = isEligibleDieFace(pool[i]) ? pool[i] : DiceEnum.SWORD;
            }
        }
        for (int i = 0; i < kDiceCount; i++) {
            if (normalized[i] == null) {
                normalized[i] = DiceEnum.SWORD;
            }
        }
        return normalized;
    }

    private int[] normalizeIntArray(int[] values, int size, int defaultValue) {
        int[] normalized = new int[size];
        for (int i = 0; i < size; i++) {
            normalized[i] = defaultValue;
        }
        if (values != null) {
            System.arraycopy(values, 0, normalized, 0, Math.min(values.length, size));
        }
        return normalized;
    }

    private boolean[] normalizeBooleanArray(boolean[] values, int size) {
        boolean[] normalized = new boolean[size];
        if (values != null) {
            System.arraycopy(values, 0, normalized, 0, Math.min(values.length, size));
        }
        return normalized;
    }

    private void syncLocalDiceSelection() {
        if (localDiceSelection == null || localDiceSelection.length != kDiceCount) {
            localDiceSelection = new boolean[kDiceCount];
        }
        int nextActive = -1;
        for (int i = 0; i < kDiceCount; i++) {
            if (localDiceSelection[i] && !isDiePlaced(i)) {
                nextActive = i;
            } else {
                localDiceSelection[i] = false;
            }
        }
        activeDieIndex = nextActive;
    }

    private void refreshDicePanel() {
        if (diceButtons == null) {
            return;
        }

        if (!canControlDice && localDiceSelection != null) {
            for (int i = 0; i < localDiceSelection.length; i++) {
                localDiceSelection[i] = false;
            }
            activeDieIndex = -1;
        }

        for (int i = 0; i < diceButtons.length; i++) {
            JButton button = diceButtons[i];
            DiceEnum value = dicePool == null ? null : dicePool[i];
            boolean placed = isDiePlaced(i);
            button.setIcon(createDiceIcon(value));
            button.setEnabled(canControlDice && !placed);
            if (localDiceSelection != null && localDiceSelection[i]) {
                button.setBackground(new Color(210, 230, 255));
            } else if (placed) {
                button.setBackground(new Color(200, 220, 200));
            } else {
                button.setBackground(new Color(245, 245, 245));
            }
        }

        if (rollButton != null) {
            rollButton.setEnabled(canControlDice);
        }
        if (clearSelectionButton != null) {
            clearSelectionButton.setEnabled(canControlDice);
        }
    }

    private ImageIcon createDiceIcon(DiceEnum value) {
        if (value == null) {
            value = DiceEnum.SWORD;
        }
        int w = 36;
        int h = 36;
        ImageIcon cached = diceImageCache.get(value);
        if (cached != null) {
            return cached;
        }

        ImageIcon imgIcon = getDiceImageIcon(value, w, h);
        if (imgIcon != null) {
            diceImageCache.put(value, imgIcon);
            return imgIcon;
        }

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(250, 250, 250));
            g.fillRoundRect(0, 0, w, h, 8, 8);
            g.setColor(new Color(160, 160, 160));
            g.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
            BufferedImage face = loadScaledFaceImage(value, 22, 22);
            if (face != null) {
                int x = (w - face.getWidth()) / 2;
                int y = (h - face.getHeight()) / 2;
                g.drawImage(face, x, y, null);
            }
        } finally {
            g.dispose();
        }
        ImageIcon fallback = new ImageIcon(img);
        diceImageCache.put(value, fallback);
        return fallback;
    }

    private ImageIcon getDiceImageIcon(DiceEnum value, int w, int h) {
        if (value == null)
            return null;
        String path = kDiceImagePaths.get(value);
        if (path == null)
            return null;
        File f = new File(path);
        if (!f.exists())
            return null;

        BufferedImage tmp = loadScaledFaceImage(value, w - 8, h - 8);
        if (tmp == null)
            return null;

        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(new Color(250, 250, 250));
            g.fillRoundRect(0, 0, w, h, 8, 8);
            g.setColor(new Color(160, 160, 160));
            g.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            int x = (w - tmp.getWidth()) / 2;
            int y = (h - tmp.getHeight()) / 2;
            g.drawImage(tmp, x, y, null);
        } finally {
            g.dispose();
        }

        return new ImageIcon(canvas);
    }

    private BufferedImage loadScaledFaceImage(DiceEnum value, int maxWidth, int maxHeight) {
        if (value == null) {
            return null;
        }
        String path = kDiceImagePaths.get(value);
        if (path == null) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        try {
            BufferedImage source = ImageIO.read(file);
            if (source == null) {
                return null;
            }

            double scale = Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight());
            if (scale <= 0) {
                scale = 1.0;
            }
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            try {
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
            } finally {
                g.dispose();
            }
            return scaled;
        } catch (IOException ex) {
            return null;
        }
    }

    private ImageIcon getSymbolIcon(String symbol) {
        if (symbol == null || symbol.isBlank() || kSkillSymbolMatch.equals(symbol)
                || kSkillSymbolDifferent.equals(symbol)) {
            return null;
        }

        ImageIcon cached = symbolIconCache.get(symbol);
        if (cached != null) {
            return cached;
        }

        DiceEnum face = getFaceForSymbol(symbol);
        if (face == null) {
            return null;
        }

        BufferedImage scaled = loadScaledFaceImage(face, 16, 16);
        if (scaled == null) {
            return null;
        }

        ImageIcon icon = new ImageIcon(scaled);
        symbolIconCache.put(symbol, icon);
        return icon;
    }

    private DiceEnum getFaceForSymbol(String symbol) {
        if (kSkillSymbolSword.equals(symbol)) {
            return DiceEnum.SWORD;
        }
        if (kSkillSymbolMagic.equals(symbol)) {
            return DiceEnum.MAGIC;
        }
        if (kSkillSymbolCrossbow.equals(symbol)) {
            return DiceEnum.CROSSBOWS;
        }
        if (kSkillSymbolDagger.equals(symbol)) {
            return DiceEnum.DAGGGERS;
        }
        if (kSkillSymbolHammer.equals(symbol)) {
            return DiceEnum.SHIELD;
        }
        return null;
    }

    private void handleDiceButtonClick(int dieIndex) {
        if (!canControlDice || dieIndex < 0 || dieIndex >= kDiceCount) {
            return;
        }
        if (isDiePlaced(dieIndex)) {
            return;
        }

        localDiceSelection[dieIndex] = !localDiceSelection[dieIndex];
        if (localDiceSelection[dieIndex]) {
            activeDieIndex = dieIndex;
        } else if (activeDieIndex == dieIndex) {
            activeDieIndex = -1;
        }
        refreshDicePanel();
    }

    private void clearDiceSelection() {
        for (int i = 0; i < localDiceSelection.length; i++) {
            localDiceSelection[i] = false;
        }
        activeDieIndex = -1;
        refreshDicePanel();
    }

    // allah
    private void rollSelectedDice() {
        if (!canControlDice || gameActionSender == null) {
            return;
        }

        // if ( gameActionSender == null) {
        // return;
        // }

        int selectedCount = 0;
        for (int i = 0; i < kDiceCount; i++) {
            if (localDiceSelection[i] && !isDiePlaced(i)) {
                selectedCount++;
            }
        }

        // i think this shoud fix it

        if (selectedCount == 0) {
            selectedCount = kDiceCount; // i think having this might let me roll all them at once
            for (int i = 0; i<localDiceSelection.length; i++){
                localDiceSelection[i] = true;
            }

        }

        int[] indices = null;
        if (selectedCount > 0) {
            indices = new int[selectedCount];
            int index = 0;
            for (int i = 0; i < kDiceCount; i++) {
                if (localDiceSelection[i] && (!isDiePlaced(i) || selectedCount == kDiceCount)) {
                    indices[index] = i;
                    index++;
                }
            }
        }

        gameActionSender.accept(new GameMessage(GameMessage.DICE_ROLL, indices));
        clearDiceSelection();
    }

    private void handleSkillSpotClick(BoardState boardState, int skillIndex, int symbolIndex) {
        if (!canControlDice || gameActionSender == null || boardState == null || !boardState.isPrimary) {
            return;
        }
        if (boardState.skills == null || skillIndex < 0 || skillIndex >= boardState.skills.length) {
            return;
        }

        SkillSlot slot = boardState.skills[skillIndex];
        if (slot.locked) {
            return;
        }

        int existingDie = findDieIndexAt(skillIndex, symbolIndex);
        if (existingDie != -1) {
            gameActionSender.accept(new GameMessage(GameMessage.DICE_REMOVE, existingDie, -1, -1));
            diceSkillIndex[existingDie] = -1;
            diceSymbolIndex[existingDie] = -1;
            clearDiceSelection();
            if (primaryBoardPanel != null) {
                primaryBoardPanel.refreshBoardText();
            }
            return;
        }

        if (activeDieIndex < 0 || activeDieIndex >= kDiceCount) {
            return;
        }
        if (isDiePlaced(activeDieIndex)) {
            return;
        }
        DiceEnum dieValue = dicePool == null ? null : dicePool[activeDieIndex];
        if (!canPlaceDie(boardState, skillIndex, symbolIndex, dieValue)) {
            return;
        }

        gameActionSender.accept(new GameMessage(GameMessage.DICE_PLACE, activeDieIndex, skillIndex, symbolIndex));
        diceSkillIndex[activeDieIndex] = skillIndex;
        diceSymbolIndex[activeDieIndex] = symbolIndex;
        clearDiceSelection();
        if (primaryBoardPanel != null) {
            primaryBoardPanel.refreshBoardText();
        }
    }

    private void toggleSkillUsed(BoardState boardState, int skillIndex) {
        if (!canControlDice || gameActionSender == null || boardState == null || !boardState.isPrimary) {
            return;
        }
        if (usedSkills == null || skillIndex < 0 || skillIndex >= usedSkills.length) {
            return;
        }
        if(boardState.skills[skillIndex].requiredSymbols == boardState)

        boolean next = !usedSkills[skillIndex];
        gameActionSender.accept(new GameMessage(GameMessage.SKILL_USED, skillIndex, next));
        setSkillUsedLocal(boardState, skillIndex, next);
    }

    private void setSkillUsedLocal(BoardState boardState, int skillIndex, boolean used) {
        if (usedSkills == null || skillIndex < 0 || skillIndex >= usedSkills.length) {
            return;
        }
        usedSkills[skillIndex] = used;
        if (boardState != null && boardState.skills != null && skillIndex < boardState.skills.length) {
            SkillSlot slot = boardState.skills[skillIndex];
            if (slot != null && !slot.locked) {
                slot.covered = used;
            }
        }
        if (primaryBoardPanel != null) {
            primaryBoardPanel.refreshBoardText();
        }
    }

    private void applyUsedSkillsToBoard(BoardState state) {
        if (state == null || state.skills == null || usedSkills == null) {
            return;
        }
        for (int i = 0; i < state.skills.length && i < usedSkills.length; i++) {
            SkillSlot slot = state.skills[i];
            if (slot != null && !slot.locked) {
                slot.covered = usedSkills[i];
            }
        }
    }

    private boolean isDiePlaced(int dieIndex) {
        if (diceSkillIndex == null || diceSymbolIndex == null) {
            return false;
        }
        if (dieIndex < 0 || dieIndex >= diceSkillIndex.length) {
            return false;
        }
        return diceSkillIndex[dieIndex] >= 0 && diceSymbolIndex[dieIndex] >= 0;
    }

    private int findDieIndexAt(int skillIndex, int symbolIndex) {
        if (diceSkillIndex == null || diceSymbolIndex == null) {
            return -1;
        }
        for (int i = 0; i < kDiceCount; i++) {
            if (diceSkillIndex[i] == skillIndex && diceSymbolIndex[i] == symbolIndex) {
                return i;
            }
        }
        return -1;
    }

    private DiceEnum getPlacedDie(int skillIndex, int symbolIndex) {
        int dieIndex = findDieIndexAt(skillIndex, symbolIndex);
        if (dieIndex == -1 || dicePool == null || dieIndex >= dicePool.length) {
            return null;
        }
        return dicePool[dieIndex];
    }

    private boolean canSkillActivate(int skillIndex, String skillName){
        for(int i = 0; i<kSkillSymbolCount; i++){
            if(!getPlacedDie(skillIndex, i).name().equals(kSkillToRequiredSymbols.get(skillName)[i])){
                return false;
            }
        }
        return true;
    }

    private String getDieLabel(DiceEnum value) {
        if (value == null) {
            return kSkillSymbolSword;
        }
        if (value == DiceEnum.SWORD) {
            return kSkillSymbolSword;
        }
        if (value == DiceEnum.MAGIC) {
            return kSkillSymbolMagic;
        }
        if (value == DiceEnum.CROSSBOWS) {
            return kSkillSymbolCrossbow;
        }
        if (value == DiceEnum.DAGGGERS) {
            return kSkillSymbolDagger;
        }
        if (value == DiceEnum.SHIELD) {
            return kSkillSymbolHammer;
        }
        if (value == DiceEnum.DRAGON) {
            return "DR";
        }
        return "?";
    }

    private String getDieSkillSymbol(DiceEnum value) {
        if (value == null) {
            return null;
        }
        if (value == DiceEnum.SWORD) {
            return kSkillSymbolSword;
        }
        if (value == DiceEnum.MAGIC) {
            return kSkillSymbolMagic;
        }
        if (value == DiceEnum.CROSSBOWS) {
            return kSkillSymbolCrossbow;
        }
        if (value == DiceEnum.DAGGGERS) {
            return kSkillSymbolDagger;
        }
        if (value == DiceEnum.SHIELD) {
            return kSkillSymbolHammer;
        }
        return null;
    }

    private boolean canPlaceDie(BoardState boardState, int skillIndex, int symbolIndex, DiceEnum dieValue) {
        if (boardState == null || boardState.skills == null) {
            return false;
        }
        if (skillIndex < 0 || skillIndex >= boardState.skills.length) {
            return false;
        }
        SkillSlot slot = boardState.skills[skillIndex];
        if (slot == null || slot.locked) {
            return false;
        }
        if (symbolIndex < 0 || symbolIndex >= slot.requiredSymbols.length) {
            return false;
        }

        String required = slot.requiredSymbols[symbolIndex];
        if (required == null || required.isEmpty()) {
            return false;
        }

        String dieSymbol = getDieSkillSymbol(dieValue);
        if (dieSymbol == null) {
            return false;
        }

        if (required.equals(kSkillSymbolMatch)) {
            for (int i = 0; i < slot.requiredSymbols.length; i++) {
                if (!kSkillSymbolMatch.equals(slot.requiredSymbols[i])) {
                    continue;
                }
                DiceEnum placed = getPlacedDie(skillIndex, i);
                if (placed == null) {
                    continue;
                }
                String placedSymbol = getDieSkillSymbol(placed);
                if (placedSymbol != null && !placedSymbol.equals(dieSymbol)) {
                    return false;
                }
            }
            return true;
        }

        if (required.equals(kSkillSymbolDifferent)) {
            for (int i = 0; i < slot.requiredSymbols.length; i++) {
                if (!kSkillSymbolDifferent.equals(slot.requiredSymbols[i])) {
                    continue;
                }
                DiceEnum placed = getPlacedDie(skillIndex, i);
                if (placed == null) {
                    continue;
                }
                String placedSymbol = getDieSkillSymbol(placed);
                if (placedSymbol != null && placedSymbol.equals(dieSymbol)) {
                    return false;
                }
            }
            return true;
        }

        return required.equals(dieSymbol);
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
        if (localHandle != null) {
            this.localHandle = localHandle;
        }
        updateDiceStateFromLobby(lobbyState);
        String selectedDragon = lobbyState == null ? null : lobbyState.selectedDragon;
        int teamGold = lobbyState == null ? 0 : Math.max(0, lobbyState.teamGold);

        ArrayList<PlayerInfo> players = new ArrayList<>();
        if (lobbyState != null && lobbyState.players != null) {
            players = lobbyState.players;
        }

        LinkedHashMap<String, BoardState> nextBoardStateMap = new LinkedHashMap<>();
        primaryBoardPanel = null;
        primaryHandle = null;
        if (!players.isEmpty()) {
            PlayerInfo primaryPlayer = players.get(0);
            primaryHandle = cleanHandle(primaryPlayer == null ? null : primaryPlayer.handle, 0);
        } else if (localHandle != null && !localHandle.isBlank()) {
            primaryHandle = cleanHandle(localHandle, 0);
        }
        canControlDice = localHandle != null
                && primaryHandle != null
                && !localHandle.isBlank()
                && localHandle.equalsIgnoreCase(primaryHandle)
                && gameActionSender != null;

        if (players.isEmpty()) {
            PlayerInfo fallbackPlayer = new PlayerInfo(
                    localHandle == null || localHandle.isBlank() ? "Player" : localHandle);
            fallbackPlayer.hero = Heroes.WARRIOR;
            BoardState state = buildBoardState(fallbackPlayer, 0, selectedDragon, teamGold);
            state.isPrimary = true;
            applyUsedSkillsToBoard(state);
            nextBoardStateMap.put(state.handle, state);
            PlayerBoardPanel panel = new PlayerBoardPanel(state);
            primaryBoardPanel = panel;
            boardsPanel.add(panel);
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

                boardState.isPrimary = index == 0;
                if (boardState.isPrimary) {
                    applyUsedSkillsToBoard(boardState);
                }

                nextBoardStateMap.put(handle, boardState);
                PlayerBoardPanel panel = new PlayerBoardPanel(boardState);
                if (boardState.isPrimary) {
                    primaryBoardPanel = panel;
                }
                boardsPanel.add(panel);
                index++;
            }
        }

        boardStateByPlayer.clear();
        boardStateByPlayer.putAll(nextBoardStateMap);

        boardsPanel.revalidate();
        boardsPanel.repaint();
        refreshDicePanel();
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
                setSkillNameAndRequirements(slot, heroSkills[i]);
                slot.locked = false;
                slot.covered = false;
            } else {
                setSkillNameAndRequirements(slot, "Locked");
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

        int boughtSkillIndex = 0;
        for (int slotIndexToFill = 0; slotIndexToFill < kSkillSlotCount && boughtSkillIndex < boughtSkills
                .size(); slotIndexToFill++) {
            if (boardState.baseSkillLocked == null || !boardState.baseSkillLocked[slotIndexToFill]) {
                continue;
            }

            SkillSlot slot = boardState.skills[slotIndexToFill];
            setSkillNameAndRequirements(slot, boughtSkills.get(boughtSkillIndex));
            slot.locked = false;
            slot.covered = false;
            boughtSkillIndex++;
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
            setSkillNameAndRequirements(slot, slot.name);
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

    private void setSkillNameAndRequirements(SkillSlot slot, String skillName) {
        slot.name = skillName == null ? "" : skillName;
        String[] requiredSymbols = kSkillToRequiredSymbols.get(toSkillKey(skillName));
        for (int i = 0; i < kSkillSymbolCount; i++) {
            if (requiredSymbols != null && i < requiredSymbols.length && requiredSymbols[i] != null) {
                slot.requiredSymbols[i] = requiredSymbols[i];
            } else {
                slot.requiredSymbols[i] = "";
            }
        }
    }

    private String getSymbolDescription(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        if (symbol.equals(kSkillSymbolSword)) {
            return "Warrior symbol (Sword)";
        }
        if (symbol.equals(kSkillSymbolMagic)) {
            return "Wizard symbol (Magic)";
        }
        if (symbol.equals(kSkillSymbolCrossbow)) {
            return "Ranger symbol (Crossbow)";
        }
        if (symbol.equals(kSkillSymbolDagger)) {
            return "Rogue symbol (Daggers)";
        }
        if (symbol.equals(kSkillSymbolHammer)) {
            return "Shield symbol";
        }
        if (symbol.equals(kSkillSymbolMatch)) {
            return "Must match the other '=' symbols";
        }
        if (symbol.equals(kSkillSymbolDifferent)) {
            return "Must differ from the other '!=' symbols";
        }
        return symbol;
    }

    private static String toSkillKey(String rawSkillName) {
        if (rawSkillName == null) {
            return "";
        }
        return rawSkillName.trim().toUpperCase().replace('-', ' ');
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

    private static Map<String, String[]> createSkillToRequiredSymbolsMap() {
        LinkedHashMap<String, String[]> map = new LinkedHashMap<>();

        putSkillRequirements(map, "Strike", kSkillSymbolMagic, kSkillSymbolSword, kSkillSymbolDagger);
        putSkillRequirements(map, "Wild Strike", kSkillSymbolCrossbow, kSkillSymbolSword, kSkillSymbolDagger);
        putSkillRequirements(map, "Holy Strike", kSkillSymbolHammer, kSkillSymbolSword, kSkillSymbolMagic);
        putSkillRequirements(map, "Critical Hit", kSkillSymbolMagic, kSkillSymbolSword, kSkillSymbolDagger,
                kSkillSymbolCrossbow, kSkillSymbolHammer);

        putSkillRequirements(map, "Slash", kSkillSymbolSword, kSkillSymbolSword);
        putSkillRequirements(map, "Smashing Blow", kSkillSymbolSword, kSkillSymbolSword, kSkillSymbolSword);
        putSkillRequirements(map, "Savage Attack", kSkillSymbolSword, kSkillSymbolSword, kSkillSymbolSword,
                kSkillSymbolSword);
        putSkillRequirements(map, "Parry", kSkillSymbolSword, kSkillSymbolSword, kSkillSymbolDifferent,
                kSkillSymbolDifferent, kSkillSymbolDifferent);

        putSkillRequirements(map, "Magic Bolt", kSkillSymbolMagic, kSkillSymbolMagic);
        putSkillRequirements(map, "Fireball", kSkillSymbolMagic, kSkillSymbolMagic, kSkillSymbolMagic);
        putSkillRequirements(map, "Lightning Storm", kSkillSymbolMagic, kSkillSymbolMagic, kSkillSymbolCrossbow,
                kSkillSymbolCrossbow);
        putSkillRequirements(map, "Shield", kSkillSymbolMagic, kSkillSymbolMagic, kSkillSymbolHammer,
                kSkillSymbolHammer);
        putSkillRequirements(map, "Drain Life", kSkillSymbolMagic, kSkillSymbolMagic, kSkillSymbolMagic,
                kSkillSymbolMagic);
        putSkillRequirements(map, "Genie", kSkillSymbolMagic, kSkillSymbolMagic, kSkillSymbolDifferent,
                kSkillSymbolDifferent, kSkillSymbolDifferent);

        putSkillRequirements(map, "Blessing", kSkillSymbolHammer);
        putSkillRequirements(map, "Smite", kSkillSymbolHammer, kSkillSymbolHammer);
        putSkillRequirements(map, "Healing Hands", kSkillSymbolHammer, kSkillSymbolHammer, kSkillSymbolHammer);
        putSkillRequirements(map, "Holy Storm", kSkillSymbolHammer, kSkillSymbolHammer, kSkillSymbolHammer,
                kSkillSymbolMatch, kSkillSymbolMatch);
        putSkillRequirements(map, "Holy Water", kSkillSymbolHammer, kSkillSymbolSword, kSkillSymbolMagic);
        putSkillRequirements(map, "Heal", kSkillSymbolHammer, kSkillSymbolHammer);
        putSkillRequirements(map, "Healing Wave", kSkillSymbolHammer, kSkillSymbolHammer, kSkillSymbolMatch,
                kSkillSymbolMatch, kSkillSymbolMatch);

        putSkillRequirements(map, "Accurate Shot", kSkillSymbolCrossbow, kSkillSymbolCrossbow);
        putSkillRequirements(map, "Dual Shot", kSkillSymbolCrossbow, kSkillSymbolCrossbow, kSkillSymbolCrossbow);
        putSkillRequirements(map, "Crossfire", kSkillSymbolCrossbow, kSkillSymbolCrossbow, kSkillSymbolCrossbow,
                kSkillSymbolCrossbow);
        putSkillRequirements(map, "Pin Down", kSkillSymbolCrossbow, kSkillSymbolCrossbow, kSkillSymbolDagger,
                kSkillSymbolDagger);
        putSkillRequirements(map, "Bestial Pounce", kSkillSymbolCrossbow, kSkillSymbolCrossbow,
                kSkillSymbolDifferent, kSkillSymbolDifferent, kSkillSymbolDifferent);
        putSkillRequirements(map, "Throwing Axe", kSkillSymbolSword, kSkillSymbolSword, kSkillSymbolCrossbow,
                kSkillSymbolCrossbow);

        putSkillRequirements(map, "Stab", kSkillSymbolDagger, kSkillSymbolDagger);
        putSkillRequirements(map, "Flanking Strike", kSkillSymbolDagger, kSkillSymbolDagger, kSkillSymbolDagger);
        putSkillRequirements(map, "Flanking Blow", kSkillSymbolDagger, kSkillSymbolDagger, kSkillSymbolDagger);
        putSkillRequirements(map, "Sneak Attack", kSkillSymbolDagger, kSkillSymbolDagger, kSkillSymbolSword,
                kSkillSymbolSword);
        putSkillRequirements(map, "Sudden Death", kSkillSymbolDagger, kSkillSymbolDagger, kSkillSymbolDagger,
                kSkillSymbolMatch, kSkillSymbolMatch);
        putSkillRequirements(map, "Deflect", kSkillSymbolDagger, kSkillSymbolDagger, kSkillSymbolDifferent,
                kSkillSymbolDifferent, kSkillSymbolDifferent);
        putSkillRequirements(map, "Defensive Stance", kSkillSymbolDagger, kSkillSymbolDagger,
                kSkillSymbolDifferent, kSkillSymbolDifferent, kSkillSymbolDifferent);
        putSkillRequirements(map, "Poison Tip", kSkillSymbolMagic, kSkillSymbolSword, kSkillSymbolDagger,
                kSkillSymbolCrossbow, kSkillSymbolHammer);

        putSkillRequirements(map, "Jab", kSkillSymbolMatch, kSkillSymbolMatch, kSkillSymbolMatch);
        putSkillRequirements(map, "Treat Wounds", kSkillSymbolMatch, kSkillSymbolMatch, kSkillSymbolMatch);

        return map;
    }

    private static void putSkillRequirements(Map<String, String[]> map, String skillName, String... symbols) {
        String[] required = new String[kSkillSymbolCount];
        for (int i = 0; i < kSkillSymbolCount; i++) {
            required[i] = "";
        }

        if (symbols != null) {
            for (int i = 0; i < symbols.length && i < kSkillSymbolCount; i++) {
                required[i] = symbols[i];
            }
        }

        map.put(toSkillKey(skillName), required);
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

    private static Map<DiceEnum, String> createDiceImagePathMap() {
        Map<DiceEnum, String> map = new HashMap<>();
        map.put(DiceEnum.SWORD, "imgs/sword.png");
        map.put(DiceEnum.CROSSBOWS, "imgs/crossbow.png");
        map.put(DiceEnum.DAGGGERS, "imgs/dagger.png");
        map.put(DiceEnum.DRAGON, "imgs/dragon.png");
        map.put(DiceEnum.SHIELD, "imgs/magic.png");
        return map;
    }

    private boolean isEligibleDieFace(DiceEnum value) {
        return value == DiceEnum.SWORD
                || value == DiceEnum.CROSSBOWS
                || value == DiceEnum.DAGGGERS
                || value == DiceEnum.SHIELD
                || value == DiceEnum.DRAGON;
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



        private boolean isTurn;

        public boolean isTurn() {
            return isTurn;
        }

        public void setTurn(boolean turn) {
            isTurn = turn;
        }

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
            skillSpotButtons = new JButton[kSkillSlotCount][kSkillSymbolCount];
            int skillY = 175;
            for (int i = 0; i < kSkillSlotCount; i++) {
                final int skillIndex = i;
                JButton skillButton = createSpotButton();
                skillButton.setBounds(15, skillY, 115 - 28, 22);
                skillButton.addActionListener(e -> {
                    SkillSlot slot = boardState.skills[skillIndex];
                    if (slot.locked) {
                        return;
                    }
                    toggleSkillUsed(boardState, skillIndex);
                });
                skillButtons[i] = skillButton;
                boardLayer.add(skillButton, 0);

                int spotX = 135 - 29;
                for (int j = 0; j < kSkillSymbolCount; j++) {
                    JButton spotButton = createSpotButton();
                    spotButton.setBounds(spotX, skillY, 24, 22);
                    final int symbolIndex = j;
                    spotButton.addActionListener(e -> handleSkillSpotClick(boardState, skillIndex, symbolIndex));
                    skillSpotButtons[i][j] = spotButton;
                    boardLayer.add(spotButton, 0);
                    spotX += 31;
                }

                skillY += 35;
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
            button.setBackground(new Color(255, 250, 190));
            button.setOpaque(true);
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
                    button.setBackground(new Color(70, 67, 70));
                    button.setForeground(Color.WHITE);
                    for (JButton sb : skillSpotButtons[i])
                        sb.setVisible(false);
                } else {
                    if (slot.covered) {
                        button.setText(slot.name + " (Used)");
                        button.setForeground(new Color(90, 90, 90));
                    } else {
                        button.setText(slot.name);
                        button.setForeground(Color.BLACK);
                    }
                    button.setBackground(new Color(248, 242, 137));

                    for (int j = 0; j < kSkillSymbolCount; j++) {
                        JButton sb = skillSpotButtons[i][j];
                        sb.setVisible(true);
                        String val = slot.requiredSymbols[j];
                        DiceEnum placedDie = boardState.isPrimary ? getPlacedDie(i, j) : null;
                        if (placedDie != null) {
                            sb.setIcon(createDiceIcon(placedDie));
                            sb.setText("");
                            sb.setBackground(new Color(210, 235, 210));
                        } else if (val != null && !val.isEmpty() && !kSkillSymbolMatch.equals(val)
                                && !kSkillSymbolDifferent.equals(val)) {
                            sb.setIcon(getSymbolIcon(val));
                            sb.setText("");
                            sb.setBackground(new Color(255, 250, 190));
                        } else {
                            sb.setIcon(null);
                            sb.setText((val == null || val.isEmpty()) ? "" : val);
                            sb.setBackground(new Color(255, 250, 190));
                        }
                        String tooltip = getSymbolDescription(val);
                        if (placedDie != null) {
                            String dieText = getDieLabel(placedDie);
                            tooltip = tooltip == null ? "Placed: " + dieText : "Placed: " + dieText + " / " + tooltip;
                        }
                        sb.setToolTipText(tooltip);
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
        private boolean isPrimary;

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
        private final String[] requiredSymbols = new String[kSkillSymbolCount];
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
