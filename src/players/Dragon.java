package src.players;

public class Dragon {
    private int health;
    private double attackDamage;
    private int expPts;
    private int goldReward;

    public Dragon(int health, double attackDamage, int expPts, int goldReward){
        this.health = health;
        this.attackDamage = attackDamage;
        this.expPts = expPts;
        this.goldReward = goldReward;
    }

    public void takeDamage(double damage){

    }

    public boolean isAlive(){
        return false;
    }
    public double getHealth(){
        return health;
    }

    public int counterAttack(){
        return 0;
    }
}
