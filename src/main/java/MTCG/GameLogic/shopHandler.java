package MTCG.GameLogic;

import MTCG.Objects.card;
import MTCG.Objects.trade;
import MTCG.Server.replyHandler;
import MTCG.Server.requestInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.Socket;
import java.sql.*;
import java.util.List;
import java.util.Locale;

public class shopHandler {
    public String URI;
    public String request;
    public boolean isRequestHandle;
    public int specialCaseInt;

    //takes care of incoming requests and directs them to the right response
    public shopHandler(requestInfo requestInfo, Socket socket) throws SQLException, ClassNotFoundException, JsonProcessingException {
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "passwort");
        this.URI = requestInfo.URI;
        this.request = requestInfo.request;
        replyHandler replyHandler = new replyHandler(socket);
        if (URI.equals("/packages")) {
            isRequestHandle = createPackage(requestInfo, con);
            replyHandler.packageCreated();
        }
        if (URI.startsWith("/transactions/packages")) {
            isRequestHandle = buyPackage(requestInfo, con);
            if (isRequestHandle) {
                replyHandler.boughtPackage();
            } else if (specialCaseInt == 1) {
                //user has no money
                replyHandler.userNoMoney();
            } else if (specialCaseInt == 2) {
                //there are no more cards available
                replyHandler.noMoreCards();
            }else{
                replyHandler.generalErrorReply();
            }
        }
        if(URI.equals("/tradings")){
            if(request.equals("POST")){
                isRequestHandle = createTrade(requestInfo, con);
                replyHandler.tradeCreated();
            }
            if(request.equals("GET")){
                String msg = getTrades(requestInfo, con);
                if(isRequestHandle) {
                    replyHandler.getTrades(msg);
                }else{
                    //token was incorrect
                    replyHandler.userWrongToken();
                }
            }
        }
        if(URI.startsWith("/tradings/")) {
            if (request.equals("POST")) {
                isRequestHandle = trade(requestInfo, con);
                if (isRequestHandle) {
                    replyHandler.traded();
                } else if (specialCaseInt == 1) {
                    //card does not meet requirements
                    replyHandler.requirementsError();
                } else if (specialCaseInt == 2) {
                    //card does not exist
                    replyHandler.nonExistingCard();
                } else if (specialCaseInt == 3) {
                    //user tries to trade with himself
                    replyHandler.tradeWithYourself();
                } else if (specialCaseInt == 4) {
                    //trade does not exist
                    replyHandler.tradeNonExistent();
                }else{
                    replyHandler.generalErrorReply();
                }
            }
            if(request.equals("DELETE")){
                isRequestHandle = deleteTrade(requestInfo, con);
                replyHandler.tradeDeleted();
            }
        }
    }

    public shopHandler(){}

    //creates a package with the provided cards in the pack database
    public boolean createPackage(requestInfo requestInfo, Connection con) throws SQLException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        List<card> listCard = mapper.readValue(requestInfo.msg, new TypeReference<List<card>>() {
        });
        for (card card : listCard) {
            PreparedStatement pst = con.prepareStatement("INSERT INTO pack(id, name, damage) VALUES(?,?,?) ");
            pst.setString(1, card.getId());
            pst.setString(2, card.getName());
            pst.setFloat(3, card.getDamage());
            pst.executeUpdate();
        }
        return true;
    }

    //player tries to buy a package
    public boolean buyPackage(requestInfo requestInfo, Connection con) throws SQLException {
        int index = 1;
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        String[] tokenSplit = token.split(" ");
        String[] user = tokenSplit[1].split("-");

        int currentMoney = 0;
        PreparedStatement packAvailable = con.prepareStatement("SELECT count(*)  FROM pack");
        ResultSet pack = packAvailable.executeQuery();
        if (pack.next()) {
            int numberOfCards = pack.getInt(1);
            //checks if the pack database has a pack left
            if (numberOfCards != 0) {
                PreparedStatement money = con.prepareStatement("SELECT money FROM users WHERE username = ?");
                money.setString(1, user[0]);
                ResultSet moneySet = money.executeQuery();
                if (moneySet.next()) {
                    currentMoney = moneySet.getInt(1);
                }
                //checks if the player has any money left to buy a pack or the number of cards in pack is not 5
                if (currentMoney > 0 && numberOfCards >= 4) {
                    currentMoney -= 5;
                    PreparedStatement updateMoney = con.prepareStatement("UPDATE users SET money = ? WHERE username = ?");
                    updateMoney.setInt(1, currentMoney);
                    updateMoney.setString(2, user[0]);
                    updateMoney.executeUpdate();

                    PreparedStatement number = con.prepareStatement("SELECT MIN(number) FROM pack");
                    ResultSet resultNumber = number.executeQuery();
                    if (resultNumber.next()) {
                        index = resultNumber.getInt(1);
                    }
                    for (int i = 1; i <= 5; i++) {
                        PreparedStatement data = con.prepareStatement("SELECT id, name, damage FROM pack WHERE number = ?");
                        data.setInt(1, index);
                        ResultSet resultSet = data.executeQuery();
                        index++;
                        if (resultSet.next()) {
                            String type = null;
                            String element = null;
                            String card = resultSet.getString(2);
                            String[] cardAttributes = card.split("(?<!^)(?=[A-Z])");    //stackoverflow
                            int attributeLength = cardAttributes.length;
                            if (attributeLength == 1) {
                                element = "normal";
                                type = "monster";
                            }
                            if (attributeLength == 2) {
                                element = cardAttributes[0].toLowerCase(Locale.ROOT);
                                if (cardAttributes[1].equals("Spell")) {
                                    type = "spell";
                                    if (cardAttributes[0].equals("Regular")) {
                                        element = "normal";
                                    }
                                } else {
                                    type = "monster";
                                }
                            }
                            PreparedStatement insert = con.prepareStatement("INSERT INTO stack(id, name, damage, player, element, type) VALUES (?,?,?,?,?,?)");
                            insert.setString(1, resultSet.getString(1));
                            insert.setString(2, resultSet.getString(2));
                            insert.setFloat(3, resultSet.getFloat(3));
                            insert.setString(4, token);
                            insert.setString(5, element);
                            insert.setString(6, type);
                            insert.executeUpdate();

                            PreparedStatement delete = con.prepareStatement("DELETE FROM pack WHERE id = ?");
                            delete.setString(1, resultSet.getString(1));
                            delete.executeUpdate();
                            resultSet.next();
                        }
                    }
                    return true;
                }
                //no more money
                specialCaseInt = 1;
                return false;
            }
            //no more cards
            specialCaseInt = 2;
            return false;
        }
        return false;
    }

    //creates a trade for the player with the requirements that the player set
    public boolean createTrade(requestInfo requestInfo, Connection con) throws SQLException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestInfo.msg);
        String tradeId = jsonNode.get("Id").asText();
        String cardId = jsonNode.get("CardToTrade").asText();
        String type = jsonNode.get("Type").asText();
        int minimumDamage = jsonNode.get("MinimumDamage").asInt();
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        PreparedStatement insertShop = con.prepareStatement("INSERT INTO shop(tradeid, cardid, token, typ, damage) VALUES (?,?,?,?,?)");
        insertShop.setString(1, tradeId);
        insertShop.setString(2, cardId);
        insertShop.setString(3, token);
        insertShop.setString(4, type);
        insertShop.setInt(5, minimumDamage);
        insertShop.executeUpdate();
        return true;
    }

    //player tries to trade
    public boolean trade(requestInfo requestInfo, Connection con) throws SQLException {
        String[] tradeID = requestInfo.URI.split("/");
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        PreparedStatement getTokenFromTrade = con.prepareStatement("SELECT token, cardid, typ, damage FROM shop WHERE tradeid = ? AND traded = false");
        getTokenFromTrade.setString(1,tradeID[2]);
        ResultSet dbToken = getTokenFromTrade.executeQuery();
        String dataBaseToken;
        String tradeCardId;
        String type;
        int minDamage;
        if(dbToken.next()) {
            dataBaseToken = dbToken.getString(1);
            tradeCardId = dbToken.getString(2);
            type = dbToken.getString(3);
            minDamage = dbToken.getInt(4);
            //checks if the player does not trade with himself
            if (!token.equals(dataBaseToken)) {
                String cardId = requestInfo.msg.replaceAll("^\"|\"$", "");
                float tradeDamage;
                String tradeType;
                PreparedStatement checkRequirements = con.prepareStatement("SELECT type, damage FROM stack WHERE player = ? AND id = ?");
                checkRequirements.setString(1, token);
                checkRequirements.setString(2, cardId);
                ResultSet requirements = checkRequirements.executeQuery();
                if (requirements.next()) {
                    tradeType = requirements.getString(1);
                    tradeDamage = requirements.getFloat(2);
                    //checks if wanted card type and minimum damage requirements are met
                    if (tradeType.equals(type) && tradeDamage >= minDamage) {
                        PreparedStatement tradeCard = con.prepareStatement("UPDATE shop SET traded = true WHERE tradeid = ?");
                        tradeCard.setString(1, tradeID[2]);
                        tradeCard.executeUpdate();


                        PreparedStatement idSwap1 = con.prepareStatement("UPDATE stack SET player = ? WHERE id = ?");
                        idSwap1.setString(1, token);
                        idSwap1.setString(2, tradeCardId);
                        idSwap1.executeUpdate();

                        PreparedStatement idSwap2 = con.prepareStatement("UPDATE stack SET player = ? WHERE id = ?");
                        idSwap2.setString(1, dataBaseToken);
                        idSwap2.setString(2, cardId);
                        idSwap2.executeUpdate();
                        return true;
                    }
                    //card does not have the right type or damage
                    specialCaseInt = 1;
                    return false;
                }
                //card that tries to be traded does not exist
                specialCaseInt = 2;
                return false;
            }
            //user tries to trade with himself
            specialCaseInt = 3;
            return false;
        }
        //trade does not exist
        specialCaseInt = 4;
        return false;
    }

    //get all the trades that are existing
    public String getTrades(requestInfo requestInfo, Connection con) throws SQLException, JsonProcessingException {
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        PreparedStatement userExists = con.prepareStatement("SELECT count(*) FROM users WHERE username = ?");
        userExists.setString(1,tokenName[0]);
        ResultSet playerExists = userExists.executeQuery();
        if(playerExists.next()){
            int rows = playerExists.getInt(1);
            //checks if player exists
            if(rows == 1){
                StringBuilder msg = new StringBuilder();
                PreparedStatement trade = con.prepareStatement("SELECT * FROM shop");
                ResultSet trades = trade.executeQuery();
                ObjectMapper mapper = new ObjectMapper();
                msg.append("[");
                while(trades.next()){
                    trade tradeClass = new trade(trades.getString(1), trades.getString(2), trades.getString(3), trades.getString(4),trades.getInt(5), trades.getBoolean(6));
                    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tradeClass);
                    msg.append(json);
                    if(!trades.isLast()){
                        msg.append(",");
                    }
                }
                msg.append("]");
                isRequestHandle = true;
                return msg.toString();
            }
        }
        isRequestHandle = false;
        return null;
    }

    //deletes the trade of the trader
    public boolean deleteTrade(requestInfo requestInfo, Connection con) throws SQLException {
        String[] tradeId = requestInfo.URI.split("/");
        String token = requestInfo.authenticationToken(requestInfo.requestString);

        PreparedStatement delete = con.prepareStatement("DELETE FROM shop WHERE token = ? AND tradeid = ?");
        delete.setString(1, token);
        delete.setString(2,tradeId[2]);
        delete.executeUpdate();
        return true;
    }
}
