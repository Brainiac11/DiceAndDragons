package src.item;

public abstract class Item {
    private ItemEnum type;
    private String name;

    public ItemEnum getType(){
        return type;
    }

    public String getName(){
        return name;
    }
}
