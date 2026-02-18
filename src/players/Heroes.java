package src.players;

import java.util.List;

import src.armour.ArmourClass;
import src.item.Item;

public class Heroes {
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
