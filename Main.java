import src.skills.SkillEnum;

public class Main {
    public static void main(String[] args) {
        for (SkillEnum dice : SkillEnum.values()) {
            System.out.println(dice);
        }
    }
}
