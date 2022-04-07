package MTCG.GameLogic;

import MTCG.Objects.cardStack;
import MTCG.Server.replyHandler;
import MTCG.Server.requestInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.Socket;
import java.sql.*;

public class cardHandler {
    public String URI;
    public String request;
    public boolean isRequestHandle;
    public int specialCaseInt;

    //takes care of all the incoming requests and directs them to the right response
    public cardHandler(requestInfo requestInfo, Socket socket) throws SQLException, ClassNotFoundException, JsonProcessingException {
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "passwort");
        this.URI = requestInfo.URI;
        this.request = requestInfo.request;
        replyHandler replyHandler = new replyHandler(socket);
        if(URI.equals("/cards")){
            String msg = getCards(requestInfo, con);
            if(isRequestHandle){
                replyHandler.getCards(msg);
            }else{
                replyHandler.userWrongToken();
            }
        }if(URI.startsWith("/deck")) {
            if (URI.equals("/deck")) {
                if (request.equals("GET")) {
                    //3rd parameter 0 stands for a reply that should be in json format
                    String msg = getDeck(requestInfo, con, 0);
                    if (isRequestHandle) {
                        replyHandler.getDeck(msg, false);
                    } else {
                        replyHandler.userWrongToken();
                    }
                }
                if (request.equals("PUT")) {
                    isRequestHandle = setDeck(requestInfo, con);
                    if (isRequestHandle) {
                        replyHandler.deckCreated();
                    } else if (specialCaseInt == 1) {
                        //player does not exist of token is wrong
                        replyHandler.cardTokenError();
                    } else if (specialCaseInt == 2) {
                        //not enough cards
                        replyHandler.notEnoughCards();
                    } else {
                        replyHandler.generalErrorReply();
                    }
                }
            }else{
                //3rd parameter 1 stands for a reply that should be in plain text format
                String msg = getDeck(requestInfo, con, 1);
                if(isRequestHandle){
                    replyHandler.getDeck(msg, true);
                }else{
                    replyHandler.userWrongToken();
                }
            }
        }
    }

    public cardHandler(){
    }

    //shows all the cards that the player currently owns
    public String getCards(requestInfo requestInfo, Connection con) throws SQLException, JsonProcessingException {
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        //checks if a token has been sent with the request
        if(token != null){
            StringBuilder msg = new StringBuilder();
            PreparedStatement cards = con.prepareStatement("SELECT id, name, damage FROM stack WHERE player = ?");
            cards.setString(1, token);
            ResultSet cardStack = cards.executeQuery();
            ObjectMapper mapper = new ObjectMapper();
            msg.append("[");
            //creates the json objects of all cards
            while(cardStack.next()){
                cardStack card = new cardStack(cardStack.getString(1), cardStack.getFloat(3), cardStack.getString(2));
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(card);
                msg.append(json);
                if(!cardStack.isLast()){
                    msg.append(",");
                }
            }
            msg.append("]");
            isRequestHandle = true;
            return msg.toString();
        }
        isRequestHandle = false;
        return null;
    }

    //shows the deck of the player that is currently selected
    public String getDeck(requestInfo requestInfo, Connection con, int textFormat) throws SQLException, JsonProcessingException {
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        StringBuilder plainText = new StringBuilder();
        //checks if a token has been sent with the request
        if(token != null){
            StringBuilder msg = new StringBuilder();
            PreparedStatement cards = con.prepareStatement("SELECT id, name, damage FROM stack WHERE player = ? AND deck = true");
            cards.setString(1, token);
            ResultSet cardStack = cards.executeQuery();
            ObjectMapper mapper = new ObjectMapper();
            //creates a plain text and json response
            msg.append("[");
            while(cardStack.next()){
                if(textFormat == 1){
                    plainText.append("ID: ").append(cardStack.getString(1)).append(" Name: ").append(cardStack.getString(2)).append(" Damage: ").append(cardStack.getFloat(3)).append("\r\n");
                }
                cardStack card = new cardStack(cardStack.getString(1), cardStack.getFloat(3), cardStack.getString(2));
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(card);
                msg.append(json);
                if (!cardStack.isLast()) {
                    msg.append(",");
                }
            }
            msg.append("]");
            isRequestHandle = true;
            //if plain text is requested return the plain text string else return the json string
            if(textFormat == 1){
                return plainText.toString();
            }
            return msg.toString();
        }
        isRequestHandle = false;
        return null;
    }

    public boolean setDeck(requestInfo requestInfo, Connection con) throws JsonProcessingException, SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestInfo.msg);
        int numberOfCards = jsonNode.size();

        //checks if 4 cards have been sent to set in the deck
        if (numberOfCards == 4) {
            for (int i = 0; i < 4; i++) {
                PreparedStatement checkCardToken = con.prepareStatement("SELECT count(*) AS number FROM stack WHERE id = ? AND player = ?");
                checkCardToken.setString(1, jsonNode.get(i).asText());
                checkCardToken.setString(2, requestInfo.authenticationToken(requestInfo.requestString));
                ResultSet cardHolder = checkCardToken.executeQuery();
                if (cardHolder.next()) {
                    int number = cardHolder.getInt(1);
                    //if player does not exist, reply with false
                    if (number == 0) {
                        specialCaseInt = 1;
                        return false;
                    }
                }
            }

            //take all cards that have been set in the deck and set them from deck=true to deck=false
            for (int i = 0; i < 4; i++) {
                PreparedStatement searchForDeck = con.prepareStatement("SELECT id FROM stack WHERE deck = true AND player = ?");
                searchForDeck.setString(1, requestInfo.authenticationToken(requestInfo.requestString));
                ResultSet cardID = searchForDeck.executeQuery();
                String id;
                if (cardID.next()) {
                    id = cardID.getString(1);
                    PreparedStatement updateDeck = con.prepareStatement("UPDATE stack SET deck = false WHERE id = ?");
                    updateDeck.setString(1, id);
                    updateDeck.executeUpdate();
                }
            }

            //checks if the card belongs to the player and sets the card to deck=true
            for (int i = 0; i < 4; i++) {
                String id = jsonNode.get(i).toString();
                id = id.replaceAll("^\"|\"$", "");  //stackoverflow solution
                PreparedStatement cardExists = con.prepareStatement("SELECT count(*) FROM stack WHERE id = ? AND player = ?");
                cardExists.setString(1, id);
                cardExists.setString(2, requestInfo.authenticationToken(requestInfo.requestString));
                ResultSet cardCount = cardExists.executeQuery();
                int cardCountInt = 0;
                if (cardCount.next()) {
                    cardCountInt = cardCount.getInt(1);
                }
                if (cardCountInt == 1) {
                    PreparedStatement updateDeck = con.prepareStatement("UPDATE stack SET deck = true WHERE id = ?");
                    updateDeck.setString(1, id);
                    updateDeck.executeUpdate();
                }
            }
            return true;
        }
        specialCaseInt = 2;
        return false;
    }
}