package MTCG.GameLogic;

import MTCG.Objects.battleCards;
import MTCG.Server.replyHandler;
import MTCG.Server.requestInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class battleHandler {

    public String player1Token = "";
    public String player2Token = "";
    public String player1Name;
    public String player2Name;
    public float damagePlayer1;
    public float damagePlayer2;
    public String message;

    public battleHandler(requestInfo requestInfo, Socket socket) throws ClassNotFoundException, SQLException, IOException {
        int numberOfPlayers = -1;
        replyHandler replyHandler = new replyHandler(socket);
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "passwort");

        //check number of cards of player
        int cards = 0;
        PreparedStatement cardsInDeck = con.prepareStatement("SELECT count(*) FROM stack WHERE deck = true AND player = ?");
        cardsInDeck.setString(1, requestInfo.authenticationToken(requestInfo.requestString));
        ResultSet numberOfCards = cardsInDeck.executeQuery();
        if(numberOfCards.next()){
            cards = numberOfCards.getInt(1);
        }

        //create game if no game exists in the database
        PreparedStatement gameInDB = con.prepareStatement("SELECT count(*) FROM battle WHERE id = 1");
        ResultSet numberDb = gameInDB.executeQuery();
        int game = 0;
        if(numberDb.next()){
            game = numberDb.getInt(1);
        }
        if(game == 0){
            PreparedStatement startBattle = con.prepareStatement("INSERT INTO battle (id, players, player1, player2) VALUES (1, 0, NULL, NULL)");
            startBattle.executeUpdate();
        }

        if(cards == 4) {
            PreparedStatement players = con.prepareStatement("SELECT players FROM battle");
            ResultSet number = players.executeQuery();
            if (number.next()) {
                numberOfPlayers = number.getInt(1);
            }

            //register first player if no one has registered to battle yet
            if (numberOfPlayers == 0) {
                PreparedStatement player1 = con.prepareStatement("UPDATE battle SET player1 = ?, players = 1 WHERE id = 1");
                player1.setString(1, requestInfo.authenticationToken(requestInfo.requestString));
                player1.executeUpdate();
                replyHandler.player1SignedUp();
            }

            //register second player and start battle
            if (numberOfPlayers == 1) {
                PreparedStatement player2 = con.prepareStatement("UPDATE battle SET player2 = ? WHERE id = 1");
                player2.setString(1, requestInfo.authenticationToken(requestInfo.requestString));
                player2.executeUpdate();
                player2Token = requestInfo.authenticationToken(requestInfo.requestString);

                PreparedStatement player1 = con.prepareStatement("SELECT player1 FROM battle");
                ResultSet player = player1.executeQuery();
                if (player.next()) {
                    player1Token = player.getString(1);
                }
                if (!player1Token.equals(player2Token)) {
                    startBattle(con);
                    replyHandler.getBattleLog(message);
                    createBattleLog();
                }else{
                    //the player tried to sign in again
                    replyHandler.samePlayer();
                }
            }
        }else{
            //the players do not have 4 cards in his deck
            replyHandler.notEnoughCards();
        }
    }

    public void startBattle(Connection con) throws SQLException {
        int result;
        //get name of player 1
        String[] tokenSplitPlayer1 = player1Token.split(" ");
        String[] tokenName1 = tokenSplitPlayer1[1].split("-");
        player1Name = tokenName1[0];

        //get name of player 2
        String[] tokenSplitPlayer2 = player2Token.split(" ");
        String[] tokenName2 = tokenSplitPlayer2[1].split("-");
        player2Name = tokenName2[0];

        ArrayList<battleCards> player1 = new ArrayList<>();
        ArrayList<battleCards> player2 = new ArrayList<>();

        //creating deck for player1
        PreparedStatement player1Card = con.prepareStatement("SELECT name, damage, element, type FROM stack WHERE player = ? AND deck = true");
        player1Card.setString(1,player1Token);
        ResultSet player1Cards = player1Card.executeQuery();
        while (player1Cards.next()) {
            player1.add(new battleCards(player1Cards.getString(1), player1Cards.getFloat(2), player1Cards.getString(3), player1Cards.getString(4)));
        }

        //creating deck for player2
        PreparedStatement player2Card = con.prepareStatement("SELECT name, damage, element, type FROM stack WHERE player = ? AND deck = true");
        player2Card.setString(1,player2Token);
        ResultSet player2Cards = player2Card.executeQuery();
        while (player2Cards.next()) {
            player2.add(new battleCards(player2Cards.getString(1), player2Cards.getFloat(2), player2Cards.getString(3), player2Cards.getString(4)));
        }
        //start battle
        result = battleLogic(player1, player2);
        setNewStats(result, player1Name, player2Name, con);
    }

    public int battleLogic(ArrayList<battleCards> player1, ArrayList<battleCards> player2) {
        int numberOfGames = 1;
        StringBuilder battle = new StringBuilder();
        //checks if 100 games have been played or one of the players has no cards left
        while (player1.size() != 0 && player2.size() != 0 && numberOfGames < 101) {
            boolean specialEvent = false;

            //random card chosen for player1 and player2
            Random rand1 = new Random();
            Random rand2 = new Random();
            int cardPlayer1 = rand1.nextInt(player1.size());
            int cardPlayer2 = rand2.nextInt(player2.size());

            //data of player1
            String Player1_cardName = player1.get(cardPlayer1).getName();
            String Player1_type = player1.get(cardPlayer1).getType();
            String Player1_element = player1.get(cardPlayer1).getElement();
            damagePlayer1 = player1.get(cardPlayer1).getDamage();

            //data of player2
            String Player2_cardName = player2.get(cardPlayer2).getName();
            String Player2_type = player2.get(cardPlayer2).getType();
            String Player2_element = player2.get(cardPlayer2).getElement();
            damagePlayer2 = player2.get(cardPlayer2).getDamage();

            //monster vs monster
            if (Player1_type.equals(Player2_type) && Player1_type.equals("monster")) {
                //goblin vs dragon
                if ((Player1_cardName.contains("Goblin") && Player2_cardName.contains("Dragon")) || (Player1_cardName.contains("Dragon") && Player2_cardName.contains("Goblin"))) {
                    if (Player1_cardName.contains("Dragon")) {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The goblin is too afraid to attack").append("\r\n");
                        battle.append("=> ").append(player1Name).append(" ").append(Player1_cardName).append(" defeats ").append(player2Name).append(" ").append(Player2_cardName).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The goblin is too afraid to attack").append("\r\n");
                        battle.append("=> ").append(player2Name).append(" ").append(Player2_cardName).append(" defeats ").append(player1Name).append(" ").append(Player1_cardName).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //wizard vs ork
                if ((Player1_cardName.contains("Wizard") && Player2_cardName.contains("Ork")) || (Player1_cardName.contains("Ork") && Player2_cardName.contains("Wizard"))) {
                    if (Player1_cardName.contains("Wizard")) {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The wizard controls the ork").append("\r\n");
                        battle.append("=> ").append(player1Name).append(" ").append(Player1_cardName).append(" defeats ").append(player2Name).append(" ").append(Player2_cardName).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The wizard controls the ork").append("\r\n");
                        battle.append("=> ").append(player2Name).append(" ").append(Player2_cardName).append(" defeats ").append(player1Name).append(" ").append(Player1_cardName).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //fireElf vs dragon
                if ((Player1_cardName.contains("Elf") && Player2_cardName.contains("Dragon")) || (Player1_cardName.contains("Dragon") && Player2_cardName.contains("Elf"))) {
                    if (Player1_cardName.contains("Elf")) {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The elf evaded the attack").append("\r\n");
                        battle.append("=> ").append(player1Name).append(" ").append(Player1_cardName).append(" defeats ").append(player2Name).append(" ").append(Player2_cardName).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The elf evaded the attack").append("\r\n");
                        battle.append("=> ").append(player2Name).append(" ").append(Player2_cardName).append(" defeats ").append(player1Name).append(" ").append(Player1_cardName).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //monster vs monster damage comparison
                if (!specialEvent) {
                    if (damagePlayer1 == damagePlayer2) {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("=> Draw").append("\r\n");
                    } else if (damagePlayer1 > damagePlayer2) {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("=> ").append(player1Name).append(" ").append(Player1_cardName).append(" defeats ").append(player2Name).append(" ").append(Player2_cardName).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("=> ").append(player2Name).append(" ").append(Player2_cardName).append(" defeats ").append(player1Name).append(" ").append(Player1_cardName).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                }
            }

            //spell vs spell
            if (Player1_type.equals(Player2_type) && Player1_type.equals("spell")) {
                printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                effectiveness(Player1_element, Player2_element, player1, player2, cardPlayer1, cardPlayer2);
                printSpellAndMixedResults(damagePlayer1, damagePlayer2, Player1_cardName, Player2_cardName, player1, player2, cardPlayer1, cardPlayer2, battle);
            }

            //spell vs monster
            if (!Player1_type.equals(Player2_type)) {
                //knight vs waterSpell
                if ((Player1_cardName.equals("Knight") && Player2_cardName.equals("WaterSpell")) || (Player1_cardName.equals("WaterSpell") && Player2_cardName.equals("Knight"))) {
                    if (Player1_cardName.equals("Knight")) {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The knight drowned").append("\r\n");
                        battle.append("-> (").append(player2Name).append(") ").append(Player2_cardName).append(" wins").append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    } else {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The knight drowned").append("\r\n");
                        battle.append("-> (").append(player1Name).append(") ").append(Player1_cardName).append(" wins").append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    }
                    specialEvent = true;
                }

                //kraken vs spell
                if ((Player1_cardName.equals("Kraken") && Player2_type.equals("spell")) || (Player1_type.equals("spell") && Player2_cardName.equals("Kraken"))) {
                    if (Player1_cardName.equals("Kraken")) {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The kraken is immune against spells").append("\r\n");
                        battle.append("-> (").append(player1Name).append(") ").append(Player1_cardName).append(" wins").append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        battle.append("The kraken is immune against spells").append("\r\n");
                        battle.append("-> (").append(player2Name).append(") ").append(Player2_cardName).append(" wins").append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //same element vs same element
                if(!specialEvent) {
                    if (Player1_element.equals(Player2_element)) {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        printSpellAndMixedResults(damagePlayer1, damagePlayer2, Player1_cardName, Player2_cardName, player1, player2, cardPlayer1, cardPlayer2, battle);
                    } else {
                        printBattle(Player1_cardName, Player2_cardName, damagePlayer1, damagePlayer2, battle);
                        effectiveness(Player1_element, Player2_element, player1, player2, cardPlayer1, cardPlayer2);
                        printSpellAndMixedResults(damagePlayer1, damagePlayer2, Player1_cardName, Player2_cardName, player1, player2, cardPlayer1, cardPlayer2, battle);
                    }
                }
            }
            //overview of current standings between the players
            battle.append("Number of cards Player 1: ").append(player1.size()).append("\r\n");
            battle.append("Number of cards Player 2: ").append(player2.size()).append("\r\n");
            battle.append("Number of round: ").append(numberOfGames).append("\r\n").append("\r\n");

            numberOfGames ++;
        }
        //requirement for a draw
        if(numberOfGames >= 100){
            battle.append("The game is a draw");
            message = battle.toString();
            return 0;
        }else {
            //if player 2 has 0 cards in his deck player wins and the other way around
            if (player2.size() == 0) {
                battle.append("Player 1 has won (").append(player1Name).append(")");
                message = battle.toString();
                return 1;
            } else {
                battle.append("Player 2 has won (").append(player2Name).append(")");
                message = battle.toString();
                return 2;
            }
        }
    }

    //prints out the damage and name of the card and who it belongs to
    public void printBattle(String Player1_cardName, String Player2_cardName, float damagePlayer1, float damagePlayer2, StringBuilder battle) {
        battle.append("Player 1 (").append(player1Name).append("): ").append(Player1_cardName).append(" (").append(damagePlayer1).append(" Damage) vs Player 2 (").append(player2Name).append("): ").append(Player2_cardName).append(" (").append(damagePlayer2).append(" Damage)").append("\r\n");
    }

    //if a spell and a monster or a spell and a spell fight against each other, then the reply string is different to a monster vs monster fight
    public void printSpellAndMixedResults(float damagePlayer1, float damagePlayer2, String Player1_cardName, String Player2_cardName, ArrayList<battleCards> player1, ArrayList<battleCards> player2, int cardPlayer1, int cardPlayer2, StringBuilder battle){
        if(damagePlayer1 == damagePlayer2){
            battle.append("=> Damage: ").append(Player1_cardName).append(": ").append(damagePlayer1).append(" vs ").append(Player2_cardName).append(": ").append(damagePlayer2).append("\r\n");
            battle.append("-> Draw").append("\r\n");
        }
        if(damagePlayer1 > damagePlayer2){
            battle.append("=> Damage: ").append(Player1_cardName).append(": ").append(damagePlayer1).append(" vs ").append(Player2_cardName).append(": ").append(damagePlayer2).append("\r\n");
            battle.append("-> (").append(player1Name).append(") ").append(Player1_cardName).append(" wins").append("\r\n");
            adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
        }if(damagePlayer1 < damagePlayer2){
            battle.append("=> Damage: ").append(Player1_cardName).append(": ").append(damagePlayer1).append(" vs ").append(Player2_cardName).append(": ").append(damagePlayer2).append("\r\n");
            battle.append("-> (").append(player2Name).append(") ").append(Player2_cardName).append(" wins").append("\r\n");
            adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
        }
    }

    //effectiveness of the mixed fights or spell fights is calculated by their element type water->fire, fire->normal, normal, water
    public void effectiveness(String Player1_element, String Player2_element, ArrayList<battleCards> player1, ArrayList<battleCards> player2, int cardPlayer1, int cardPlayer2){
        //fire vs water
        if ((Player1_element.equals("fire") && Player2_element.equals("water")) || (Player1_element.equals("water") && Player2_element.equals("fire"))) {
            //calculate damage
            if (Player1_element.equals("fire")) {
                damagePlayer1 = player1.get(cardPlayer1).getDamage()/2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()*2;
            }else{
                damagePlayer1 = player1.get(cardPlayer1).getDamage()*2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()/2;
            }
        }

        //normal vs fire
        if ((Player1_element.equals("fire") && Player2_element.equals("normal")) || (Player1_element.equals("normal") && Player2_element.equals("fire"))) {
            //calculate damage
            if (Player1_element.equals("normal")) {
                damagePlayer1 = player1.get(cardPlayer1).getDamage()/2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()*2;
            }else{
                damagePlayer1 = player1.get(cardPlayer1).getDamage()*2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()/2;
            }
        }

        //water vs normal
        if ((Player1_element.equals("normal") && Player2_element.equals("water")) || (Player1_element.equals("water") && Player2_element.equals("normal"))) {
            //calculate damage
            if (Player1_element.equals("water")) {
                damagePlayer1 = player1.get(cardPlayer1).getDamage()/2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()*2;
            }else{
                damagePlayer1 = player1.get(cardPlayer1).getDamage()*2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()/2;
            }
        }
    }

    //removes a card from the player that lost and adds it to the winners deck
    public void adjustDeck(ArrayList<battleCards> player1, ArrayList<battleCards> player2, int cardPlayer1, int cardPlayer2, int player){
        if(player == 1){
            player1.add(player2.get(cardPlayer2));
            player2.remove(cardPlayer2);
        }else if(player == 2){
            player2.add(player1.get(cardPlayer1));
            player1.remove(cardPlayer1);
        }
    }

    //updates the stats of players after the battle has ended (-3 elo for a loss and +5 elo for a win)
    public void setNewStats(int result, String player1Name, String player2Name, Connection con) throws SQLException {
        int Player1_elo = 0;
        int Player1_wins = 0;
        int Player1_draws = 0;
        int Player1_losses = 0;
        int Player1_games = 0;

        int Player2_elo = 0;
        int Player2_wins = 0;
        int Player2_draws = 0;
        int Player2_losses = 0;
        int Player2_games = 0;

        //get data from player1
        PreparedStatement player1 = con.prepareStatement("SELECT elo, wins, draws, losses, gamesplayed FROM users WHERE username = ?");
        player1.setString(1, player1Name);
        ResultSet statsUpdate1 = player1.executeQuery();
        if(statsUpdate1.next()){
            Player1_elo = statsUpdate1.getInt(1);
            Player1_wins = statsUpdate1.getInt(2);
            Player1_draws = statsUpdate1.getInt(3);
            Player1_losses = statsUpdate1.getInt(4);
            Player1_games = statsUpdate1.getInt(5);
        }

        //get data from player2
        PreparedStatement player2 = con.prepareStatement("SELECT elo, wins, draws, losses, gamesplayed FROM users WHERE username = ?");
        player2.setString(1, player2Name);
        ResultSet statsUpdate2 = player2.executeQuery();
        if(statsUpdate2.next()){
            Player2_elo = statsUpdate2.getInt(1);
            Player2_wins = statsUpdate2.getInt(2);
            Player2_draws = statsUpdate2.getInt(3);
            Player2_losses = statsUpdate2.getInt(4);
            Player2_games = statsUpdate2.getInt(5);
        }

        Player1_games += 1;
        Player2_games += 1;

        //draw
        if(result == 0){
            Player1_draws += 1;
            Player2_draws += 1;

            //update player1
            PreparedStatement Player1_draw = con.prepareStatement("UPDATE users SET draws = ?, gamesplayed = ? WHERE username = ?");
            Player1_draw.setInt(1, Player1_draws);
            Player1_draw.setInt(2, Player1_games);
            Player1_draw.setString(3, player1Name);
            Player1_draw.executeUpdate();

            //update player2
            PreparedStatement Player2_draw = con.prepareStatement("UPDATE users SET draws = ?, gamesplayed = ? WHERE username = ?");
            Player2_draw.setInt(1, Player2_draws);
            Player2_draw.setInt(2, Player2_games);
            Player2_draw.setString(3, player2Name);
            Player2_draw.executeUpdate();
        }
        //player1 won
        if(result == 1){
            Player1_elo += 3;
            Player2_elo -= 5;
            Player1_wins += 1;
            Player2_losses += 1;

            //update player1
            PreparedStatement Player1_win = con.prepareStatement("UPDATE users SET wins = ?, gamesplayed = ?, elo = ? WHERE username = ?");
            Player1_win.setInt(1, Player1_wins);
            Player1_win.setInt(2, Player1_games);
            Player1_win.setInt(3, Player1_elo);
            Player1_win.setString(4, player1Name);
            Player1_win.executeUpdate();

            //update player2
            PreparedStatement Player2_lose = con.prepareStatement("UPDATE users SET losses = ?, gamesplayed = ?, elo = ? WHERE username = ?");
            Player2_lose.setInt(1, Player2_losses);
            Player2_lose.setInt(2, Player2_games);
            Player2_lose.setInt(3, Player2_elo);
            Player2_lose.setString(4, player2Name);
            Player2_lose.executeUpdate();
        }
        //player2 won
        if(result == 2){
            Player2_elo += 3;
            Player1_elo -= 5;
            Player2_wins += 1;
            Player1_losses += 1;

            //update player1
            PreparedStatement Player1_lose = con.prepareStatement("UPDATE users SET losses = ?, gamesplayed = ?, elo = ? WHERE username = ?");
            Player1_lose.setInt(1, Player1_losses);
            Player1_lose.setInt(2, Player1_games);
            Player1_lose.setInt(3, Player1_elo);
            Player1_lose.setString(4, player1Name);
            Player1_lose.executeUpdate();

            //update player2
            PreparedStatement Player2_win = con.prepareStatement("UPDATE users SET wins = ?, gamesplayed = ?, elo = ? WHERE username = ?");
            Player2_win.setInt(1, Player2_wins);
            Player2_win.setInt(2, Player2_games);
            Player2_win.setInt(3, Player2_elo);
            Player2_win.setString(4, player2Name);
            Player2_win.executeUpdate();
        }
        PreparedStatement resetBattle = con.prepareStatement("UPDATE battle SET players = 0, player1 = NULL, player2 = NULL WHERE id = 1");
        resetBattle.executeUpdate();
    }

    //saves the battle log into a folder(unique feature)
    public void createBattleLog() throws IOException {
        File createLog = new File("battleLog/" + player1Name + " vs " + player2Name);

        if(!createLog.exists()){
            FileWriter writer = new FileWriter(createLog);
            writer.write(message);
            writer.close();
        }
    }
}
