package src.players;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DragonCatalog {
    private static final Map<String, DragonProfile> kDragonIdMap = createDragonMap();
    private static final Map<String, DragonProfile> kDragonToDisplayNameMap = createDisplayNameMap(kDragonIdMap);

    private DragonCatalog() {
    }

    public static DragonProfile findById(String dragonId) {
        if (dragonId == null) {
            return null;
        }
        return kDragonIdMap.get(dragonId.trim());
    }

    public static DragonProfile findBySelection(String rawSelection) {
        if (rawSelection == null) {
            return null;
        }
        String cleanSelection = rawSelection.trim();
        if (cleanSelection.isEmpty()) {
            return null;
        }

        DragonProfile byId = kDragonIdMap.get(cleanSelection);
        if (byId != null) {
            return byId;
        }

        return kDragonToDisplayNameMap.get(cleanSelection);
    }

    public static boolean isSelectable(String rawSelection) {
        return findBySelection(rawSelection) != null;
    }

    public static String[] getSelectableDisplayNames() {
        String[] displayNames = new String[kDragonIdMap.size()];
        int index = 0;
        for (DragonProfile profile : kDragonIdMap.values()) {
            displayNames[index] = profile.getDisplayName();
            index++;
        }
        return displayNames;
    }

    public static String toDisplayName(String dragonIdOrSelection) {
        DragonProfile profile = findBySelection(dragonIdOrSelection);
        if (profile == null) {
            return null;
        }
        return profile.getDisplayName();
    }

    private static Map<String, DragonProfile> createDragonMap() {
        Map<String, DragonProfile> map = new LinkedHashMap<>();

        put(map, new DragonProfile(
                "YOUNG_RED_DRAGON",
            "Young Red Dragon",
            new int[] { 5, 7, 10 }));

        put(map, new DragonProfile(
                "PALE_DRAGON",
            "Pale Dragon",
            new int[] { 6, 9, 12 }));

        put(map, new DragonProfile(
                "YOUNG_BLACK_DRAGON",
            "Young Black Dragon",
            new int[] { 6, 10, 13 }));

        put(map, new DragonProfile(
                "GREEN_DRAGON",
            "Green Dragon",
            new int[] { 6, 10, 13 }));

        put(map, new DragonProfile(
                "RED_DRAGON",
            "Red Dragon",
            new int[] { 7, 11, 15 }));

        put(map, new DragonProfile(
                "BLUE_DRAGON",
            "Blue Dragon",
            new int[] { 6, 10, 13 }));

        put(map, new DragonProfile(
                "UNDEAD_DRAGON",
            "Undead Dragon",
            new int[] { 7, 11, 14 }));

        put(map, new DragonProfile(
                "BLACK_DRAGON",
            "Black Dragon",
            new int[] { 7, 13, 18 }));

        return map;
    }

    private static Map<String, DragonProfile> createDisplayNameMap(Map<String, DragonProfile> dragonsById) {
        Map<String, DragonProfile> map = new LinkedHashMap<>();
        for (DragonProfile profile : dragonsById.values()) {
            map.put(profile.getDisplayName(), profile);
        }
        return map;
    }

    private static void put(Map<String, DragonProfile> map, DragonProfile profile) {
        map.put(profile.getId(), profile);
    }

    public static final class DragonProfile {
        private final String id;
        private final String displayName;
        private final int[] counterAttackDamage;

        public DragonProfile(String id, String displayName) {
            this(id, displayName, null);
        }

        public DragonProfile(String id, String displayName, int[] counterAttackDamage) {
            this.id = id;
            this.displayName = displayName;
            this.counterAttackDamage = counterAttackDamage == null ? new int[0] : counterAttackDamage.clone();
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getCounterAttackDamage(int dragonSymbols) {
            if (dragonSymbols <= 0 || counterAttackDamage.length == 0) {
                return 0;
            }
            int index = Math.min(dragonSymbols, counterAttackDamage.length) - 1;
            if (index < 0 || index >= counterAttackDamage.length) {
                return 0;
            }
            return counterAttackDamage[index];
        }
    }
}
