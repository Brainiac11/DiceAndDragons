package src.token;

import java.util.List;

import src.dice.DiceEnum;
import src.skills.SkillEnum;

public abstract class Token {
    protected String name;
    protected int skillAffectLevel;
    protected List<DiceEnum> symbols;
    protected boolean disabled;

    public abstract void setSkillEnum(SkillEnum skill);

    public abstract SkillEnum getSkill();

    public abstract void setName(String name);

    public abstract String getName();

    public abstract int getSkillAffect();

    public void addSymbolCombo(DiceEnum diceValue) {
        symbols.add(diceValue);
    }

    public abstract void upgrade(int upgradeQuantity);
}