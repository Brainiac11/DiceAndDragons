package src.dice;

public class Dice {
    private DiceEnum rolledValue;
    private DiceEnum[] diceTypes = DiceEnum.values();

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