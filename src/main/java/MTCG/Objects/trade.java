package MTCG.Objects;

public class trade {
    public String tradeID;
    public String cardID;
    public String token;
    public String type;
    public int damage;
    public boolean traded;

    public trade(String tradeID, String cardID, String token, String type, int damage, boolean traded) {
        this.tradeID = tradeID;
        this.cardID = cardID;
        this.token = token;
        this.type = type;
        this.damage = damage;
        this.traded = traded;
    }

    public String getTradeId() {
        return tradeID;
    }
    public String getCardID() { return cardID; }
    public String getToken() { return token; }
    public String getType() {
        return type;
    }
    public int getDamage() {
        return damage;
    }
    public boolean isTraded() {
        return traded;
    }


    public void setTradeId(String tradeID) { this.tradeID = tradeID; }
    public void setCardID(String cardID) { this.cardID = cardID; }
    public void setToken(String token) { this.token = token; }
    public void setType(String type) {
        this.type = type;
    }
    public void setDamage(int damage) {
        this.damage = damage;
    }
    public void setTraded(boolean traded) {
        this.traded = traded;
    }
}
