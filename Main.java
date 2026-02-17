import src.dice.DiceEnum;
public class Main {
    public static void main(String[] args) {
        for (DiceEnum dice : DiceEnum.values()) {
            System.out.println(dice);
        }
    }
}
