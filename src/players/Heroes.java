package src.players;

import src.armour.ArmourClass;
import src.item.Item;

public class Heroes {
    public static final String WARRIOR = "Warrior";
    public static final String WIZARD = "Wizard";
    public static final String CLERIC = "Cleric";
    public static final String RANGER = "Ranger";
    public static final String ROGUE = "Rogue";

    private int hitPoints;

    public int getHitPoints() {
        return hitPoints;
    }

    public ArmourClass getArmour() {
        return armour;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public String getName() {
        return name;
    }

    public int getInitiativeRanking() {
        return initiativeRanking;
    }

    public Item getItem1() {
        return item1;
    }

    public Item getItem2() {
        return item2;
    }

    public boolean isAlive() {
        return hitPoints > 0;
    }

    public void ressurect() {

    }

    private ArmourClass armour;
    private int currentLevel;
    private String name;
    // private List<Skill> skills;
    public int initiativeRanking;
    public Item item1;
    public Item item2;

}
