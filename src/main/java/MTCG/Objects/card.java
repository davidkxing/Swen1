package MTCG.Objects;

public class card {
    public String Id;
    public float Damage;
    public String Name;

    public card() {
    }

    public String getId() {
        return Id;
    }
    public float getDamage() {
        return Damage;
    }
    public String getName() {
        return Name;
    }

    public void setId(String id) {
        Id = id;
    }
    public void setDamage(float damage) {
        Damage = damage;
    }
    public void setName(String name) {
        Name = name;
    }
}
