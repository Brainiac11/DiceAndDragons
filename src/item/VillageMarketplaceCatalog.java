package src.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import src.players.DragonCatalog;

public final class VillageMarketplaceCatalog {
    private static final Map<String, ItemDefinition> kItemDescriptions = createItemDefinitions();
    private static final Map<String, Marketplace> kMarketDragonIdMap = createMarketByDragonId();

    public static Marketplace forDragon(String dragonSelection) {
        DragonCatalog.DragonProfile dragon = DragonCatalog.findBySelection(dragonSelection);
        if (dragon == null) {
            return null;
        }
        return kMarketDragonIdMap.get(dragon.getId());
    }

    public static MarketplaceItem findItem(String dragonSelection, String itemName) {
        Marketplace market = forDragon(dragonSelection);
        if (market == null || itemName == null) {
            return null;
        }

        String cleanItem = itemName.trim();
        if (cleanItem.isEmpty()) {
            return null;
        }

        for (MarketplaceItem item : market.getItems()) {
            if (item.getName().equals(cleanItem)) {
                return item;
            }
        }
        return null;
    }

    private static Map<String, ItemDefinition> createItemDefinitions() {
        Map<String, ItemDefinition> map = new LinkedHashMap<>();

        putItem(map, "Small Healing Potion", ItemEnum.INSTANT, "Heals +4 HP", 1);
        putItem(map, "Healing Potion", ItemEnum.INSTANT, "Heals +7 HP", 2);
        putItem(map, "Great Healing Potion", ItemEnum.INSTANT, "Heals +9 HP", 3);

        putItem(map, "Haste Potion", ItemEnum.INSTANT, "Re-roll up to 2 Dragon Dice", 1);
        putItem(map, "Great Haste Potion", ItemEnum.INSTANT, "Re-roll up to 3 Dragon Dice", 2);
        putItem(map, "Scroll of Knowledge", ItemEnum.INSTANT, "Re-use a Skill", 1);

        putItem(map, "Stealth Potion", ItemEnum.INSTANT, "Add a Daggers symbol", 1);
        putItem(map, "Strength Potion", ItemEnum.INSTANT, "Add a Sword symbol", 1);
        putItem(map, "Holy Water", ItemEnum.INSTANT, "Add a Hammer symbol", 1);
        putItem(map, "Vision Potion", ItemEnum.INSTANT, "Add a Crossbow symbol", 1);
        putItem(map, "Mana Potion", ItemEnum.INSTANT, "Add a Magic symbol", 1);

        putItem(map, "Steel Shield", ItemEnum.DURABLE, "+1 AC", 2);
        putItem(map, "Magic Shield", ItemEnum.DURABLE, "+2 AC", 4);
        putItem(map, "Magic Bracelet", ItemEnum.DURABLE, "+2 AC", 4);

        putItem(map, "Magic Sword", ItemEnum.DURABLE, "Add a Sword symbol each turn", 5);
        putItem(map, "Pinpoint Crossbow", ItemEnum.DURABLE, "Add a Crossbow symbol each turn", 5);
        putItem(map, "Blessed Hammer", ItemEnum.DURABLE, "Add a Hammer symbol each turn", 5);
        putItem(map, "Stealth Cloak", ItemEnum.DURABLE, "Add a Daggers symbol each turn", 5);
        putItem(map, "Magic Staff", ItemEnum.DURABLE, "Add a Magic symbol each turn", 5);

        putItem(map, "Gauntlets of Power", ItemEnum.DURABLE,
                "+1 HP extra damage when activating a Strike/Skill", 4);
        putItem(map, "Staff of Healing", ItemEnum.DURABLE,
                "+1 HP extra healing when activating a Healing Skill", 4);

        return map;
    }

    private static Map<String, Marketplace> createMarketByDragonId() {
        Map<String, Marketplace> map = new LinkedHashMap<>();

        map.put("YOUNG_RED_DRAGON", market("Bearwood Marketplace",
                "Small Healing Potion",
                "Scroll of Knowledge",
                "Haste Potion",
                "Holy Water",
                "Mana Potion"));

        map.put("PALE_DRAGON", market("Bearwood Marketplace",
                "Small Healing Potion",
                "Healing Potion",
                "Scroll of Knowledge",
                "Haste Potion",
                "Vision Potion",
                "Holy Water",
                "Mana Potion",
                "Stealth Potion",
                "Steel Shield"));

        map.put("YOUNG_BLACK_DRAGON", market("Angelos Marketplace",
                "Small Healing Potion",
                "Healing Potion",
                "Holy Water",
                "Stealth Potion",
                "Steel Shield",
                "Magic Sword",
                "Pinpoint Crossbow"));

        map.put("GREEN_DRAGON", market("Raindrop Keep Marketplace",
                "Healing Potion",
                "Great Healing Potion",
                "Scroll of Knowledge",
                "Strength Potion",
                "Great Haste Potion",
                "Blessed Hammer",
                "Gauntlets of Power",
                "Magic Staff"));

        map.put("RED_DRAGON", market("Deepridge Burrow Marketplace",
                "Healing Potion",
                "Great Healing Potion",
                "Scroll of Knowledge",
                "Strength Potion",
                "Great Haste Potion",
                "Blessed Hammer",
                "Gauntlets of Power",
                "Magic Staff"));

        map.put("BLUE_DRAGON", market("Bearwood Marketplace",
                "Healing Potion",
                "Great Healing Potion",
                "Scroll of Knowledge",
                "Holy Water",
                "Great Haste Potion",
                "Magic Sword"));

        map.put("UNDEAD_DRAGON", market("Kemora Marketplace",
                "Healing Potion",
                "Great Healing Potion",
                "Scroll of Knowledge",
                "Stealth Potion",
                "Great Haste Potion",
                "Vision Potion",
                "Gauntlets of Power",
                "Staff of Healing"));

        map.put("BLACK_DRAGON", market("Jovryk Marketplace",
                "Healing Potion",
                "Great Healing Potion",
                "Scroll of Knowledge",
                "Stealth Potion",
                "Great Haste Potion",
                "Vision Potion",
                "Gauntlets of Power",
                "Staff of Healing"));

        return map;
    }

    private static Marketplace market(String villageName, String... itemNames) {
        ArrayList<MarketplaceItem> items = new ArrayList<>();
        for (String itemName : itemNames) {
            ItemDefinition definition = kItemDescriptions.get(itemName);
            if (definition == null) {
                throw new IllegalStateException("Unknown marketplace item: " + itemName);
            }
            items.add(new MarketplaceItem(definition.name, definition.type, definition.effect, definition.cost));
        }
        return new Marketplace(villageName, items);
    }

    private static void putItem(Map<String, ItemDefinition> map, String name, ItemEnum type, String effect, int cost) {
        map.put(name, new ItemDefinition(name, type, effect, cost));
    }

    private static final class ItemDefinition {
        private final String name;
        private final ItemEnum type;
        private final String effect;
        private final int cost;

        private ItemDefinition(String name, ItemEnum type, String effect, int cost) {
            this.name = name;
            this.type = type;
            this.effect = effect;
            this.cost = cost;
        }
    }

    public static final class Marketplace {
        private final String villageName;
        private final ArrayList<MarketplaceItem> items;

        private Marketplace(String villageName, ArrayList<MarketplaceItem> items) {
            this.villageName = villageName;
            this.items = new ArrayList<>(items);
        }

        public String getVillageName() {
            return villageName;
        }

        public ArrayList<MarketplaceItem> getItems() {
            return new ArrayList<>(items);
        }
    }

    public static final class MarketplaceItem {
        private final String name;
        private final ItemEnum type;
        private final String effect;
        private final int cost;

        private MarketplaceItem(String name, ItemEnum type, String effect, int cost) {
            this.name = name;
            this.type = type;
            this.effect = effect;
            this.cost = cost;
        }

        public String getName() {
            return name;
        }

        public ItemEnum getType() {
            return type;
        }

        public String getEffect() {
            return effect;
        }

        public int getCost() {
            return cost;
        }
    }
}