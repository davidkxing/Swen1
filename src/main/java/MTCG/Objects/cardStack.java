package MTCG.Objects;

public class cardStack {
    public String id;
    public float damage;
    public String name;

    public cardStack(String id, float damage, String name) {
        this.id = id;
        this.damage = damage;
        this.name = name;
    }

    public String getId() {
        return id;
    }
    public float getDamage() {
        return damage;
    }
    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }
    public void setDamage(float damage) {
        this.damage = damage;
    }
    public void setName(String name) {
        this.name = name;
    }
}
