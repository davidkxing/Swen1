package MTCG.Objects;

public class scoreBoard {
    public String username;
    public int elo;

    public scoreBoard(String username, int elo){
        this.username = username;
        this.elo = elo;
    }

    public String getUsername() {
        return username;
    }
    public int getElo() {
        return elo;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setElo(int elo) {
        this.elo = elo;
    }
}
