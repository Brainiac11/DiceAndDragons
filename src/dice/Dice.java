package src.dice;

public class Dice {
    private DiceEnum rolledValue;
    private static final DiceEnum[] diceTypes = {
            DiceEnum.SWORD,
            DiceEnum.CROSSBOWS,
            DiceEnum.DAGGGERS,
            DiceEnum.SHIELD,
            DiceEnum.DRAGON
    };

    public DiceEnum rollDice() {
        rolledValue = diceTypes[(int) Math.round(Math.random() * diceTypes.length)];
        return rolledValue;
    }

    public DiceEnum getRolledValue() {
        return rolledValue;
    }

    public DiceEnum[] getDiceTypes() {
        return diceTypes;
    }
}