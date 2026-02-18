package src.skills;

import java.util.List;

import src.dice.DiceEnum;

public abstract class Skill {
    private SkillEnum skillType;
    private String name;
    private int skillAffectLevel;
    private int upgradeLevel;
    private List<DiceEnum> symbol;
    private boolean disabled;

    public SkillEnum getSkillType() {
        return skillType;
    }

    public String getName() {
        return name;
    }

    public int getSkillAffectLevel() {
        return skillAffectLevel;
    }

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public List<DiceEnum> getSymbol() {
        return symbol;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setSkillAffectLevel(int skillAffectLevel) {
        this.skillAffectLevel = skillAffectLevel;
    }

    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

}
