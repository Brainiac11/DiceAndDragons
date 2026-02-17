package src.players;

import java.util.List;

import src.armour.ArmourClass;

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
    private ArmourClass armour;
    private int currentLevel;
    private String name;
    // private List<Skill> skills;
    public int initiativeRanking;
}
