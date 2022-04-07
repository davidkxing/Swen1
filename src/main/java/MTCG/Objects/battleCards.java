package MTCG.Objects;

public class battleCards {
    public String name;
    public float damage;
    public String element;
    public String type;

    public battleCards(String name, float damage, String element, String type) {
        this.name = name;
        this.damage = damage;
        this.element = element;
        this.type = type;
    }

    public String getName() {
        return name;
    }
    public float getDamage() {
        return damage;
    }
    public String getElement() {
        return element;
    }
    public String getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setDamage(int damage) {
        this.damage = damage;
    }
    public void setElement(String element) {
        this.element = element;
    }
    public void setType(String type) {
        this.type = type;
    }
}
