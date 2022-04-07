package MTCG.GameLogic;

import MTCG.Objects.scoreBoard;
import MTCG.Server.replyHandler;
import MTCG.Server.requestInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;


public class userHandler {
    public String URI;
    public String request;
    public boolean requestHandeled;
    public int specialCaseInt;

    //takes care of all the incoming requests and directs them to the right response
    public userHandler(requestInfo requestInfo, Socket socket) throws JsonProcessingException, SQLException, ClassNotFoundException, FileNotFoundException {
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "passwort");
        this.URI = requestInfo.URI;
        this.request = requestInfo.request;
        replyHandler replyHandler = new replyHandler(socket);
        if(URI.startsWith("/users")) {
            if (request.equals("POST")) {
                requestHandeled = createUser(requestInfo, con);
                if (requestHandeled) {
                    replyHandler.userCreated();
                } else {
                    replyHandler.userAlreadyExists();
                }
            }
            if(request.equals("GET")){
                ResultSet resultSet = getUserData(requestInfo, con);
                if(requestHandeled){
                    replyHandler.userData(resultSet);
                }else{
                    replyHandler.userWrongToken();
                }
            }
            if(request.equals("PUT")){
                requestHandeled = setUserData(requestInfo, con);
                if(requestHandeled){
                    replyHandler.setUserInfo();
                }else{
                    replyHandler.userWrongToken();
                }
            }
        }
        if(URI.equals("/sessions")){
            requestHandeled = loginUser(requestInfo, con);
            if(requestHandeled){
                replyHandler.userLoggedIn();
            }else{
                replyHandler.userFailedLogin();
            }
        }
        if(URI.equals("/stats")){
            ResultSet stats = getStats(requestInfo, con);
            if(requestHandeled){
                replyHandler.getStats(stats);
            }else{
                replyHandler.userWrongToken();
            }
        }
        if(URI.equals("/score")){
            String msg = getScore(requestInfo, con);
            if(requestHandeled){
                replyHandler.getScore(msg);
            }else{
                replyHandler.userWrongToken();
            }
        }
        if(URI.equals("/log")){
            String msg = getBattleLogs(requestInfo);
            if(msg != null){
                replyHandler.getLogs(msg);
            }else if(specialCaseInt == 1){
                //user has no battles under his belt
                replyHandler.noLogEntries();
            }else if(specialCaseInt == 2){
                //there was no token in the request
                replyHandler.noToken();
            }
        }
    }

    public userHandler(){
    }

    //creates the user
    public boolean createUser(requestInfo requestInfo, Connection con) throws JsonProcessingException, SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestInfo.msg);
        String username = jsonNode.get("Username").asText();
        String password = jsonNode.get("Password").asText();
        PreparedStatement count = con.prepareStatement("SELECT count(*) AS total FROM users WHERE username = ?");
        count.setString(1, username);
        ResultSet resultSet = count.executeQuery();
        if (resultSet.next()) {
            int rows = resultSet.getInt(1);
            //checks if user already exists if not creates a new user
            if (rows == 0) {
                PreparedStatement pst = con.prepareStatement("INSERT INTO users(username, password) VALUES(?,?) ");
                pst.setString(1, username);
                pst.setString(2, password);
                pst.executeUpdate();
                return true;
            }
        }
        return false;
    }

    //logs in the user
    public boolean loginUser(requestInfo requestInfo, Connection con) throws JsonProcessingException, SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestInfo.msg);
        String username = jsonNode.get("Username").asText();
        String password = jsonNode.get("Password").asText();
        PreparedStatement data = con.prepareStatement("SELECT * FROM users WHERE username = ?");
        data.setString(1, username);
        ResultSet resultSet = data.executeQuery();
        if (resultSet.next()) {
            //if password in database and password from json string equal log player in
            return password.equals(resultSet.getString(2));
        } else {
            return false;
        }
    }

    //get all data that the user has set in his profile
    public ResultSet getUserData(requestInfo requestInfo, Connection con) throws SQLException {
        String[] user = requestInfo.URI.split("/");
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        //checks if the name of the token and the name of the user are the same
        if(user[2].equals(tokenName[0])){
            PreparedStatement userBio = con.prepareStatement("SELECT name, bio, image FROM users WHERE username = ?");
            userBio.setString(1,user[2]);
            ResultSet userInfo = userBio.executeQuery();
            if(userInfo.next()){
                requestHandeled = true;
                return userInfo;
            }
        }
        requestHandeled = false;
        return null;
    }

    //put data that the users provides into his profile
    public boolean setUserData(requestInfo requestInfo, Connection con) throws SQLException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String[] user = requestInfo.URI.split("/");
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        //checks if the name of the token and the name of the user are the same and sets the provided data into their profile
        if(user[2].equals(tokenName[0])){
            JsonNode jsonNode = mapper.readTree(requestInfo.msg);
            String name = jsonNode.get("Name").asText();
            String bio = jsonNode.get("Bio").asText();
            String image = jsonNode.get("Image").asText();
            PreparedStatement updateUser = con.prepareStatement("UPDATE users SET name = ?, bio = ?, image= ? WHERE username = ?");
            updateUser.setString(1, name);
            updateUser.setString(2, bio);
            updateUser.setString(3, image);
            updateUser.setString(4,user[2]);
            updateUser.executeUpdate();
            return true;
        }
        return false;
    }

    //reply with the stats of the player (elo, wins, losses, draws and games played)
    public ResultSet getStats(requestInfo requestInfo, Connection con) throws SQLException {
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        PreparedStatement stats = con.prepareStatement("SELECT elo, wins, losses, draws, gamesplayed FROM users WHERE username = ?");
        stats.setString(1,tokenName[0]);
        ResultSet resultStats = stats.executeQuery();
        if(resultStats.next()){
            requestHandeled = true;
            return resultStats;
        }
        requestHandeled = false;
        return null;
    }

    //shows the current scoreboard of all players which are registered
    public String getScore(requestInfo requestInfo, Connection con) throws SQLException, JsonProcessingException {
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        PreparedStatement userExists = con.prepareStatement("SELECT count(*) FROM users WHERE username = ?");
        userExists.setString(1,tokenName[0]);
        ResultSet playerExists = userExists.executeQuery();
        if(playerExists.next()){
            int rows = playerExists.getInt(1);
            //checks if player that sent the request exists
            if(rows == 1){
                StringBuilder msg = new StringBuilder();
                PreparedStatement score = con.prepareStatement("SELECT username, elo FROM users ORDER BY elo DESC");
                ResultSet scoreboard = score.executeQuery();
                ObjectMapper mapper = new ObjectMapper();
                msg.append("[");
                while(scoreboard.next()){
                    scoreBoard scoreboardClass = new scoreBoard(scoreboard.getString(1), scoreboard.getInt(2));
                    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(scoreboardClass);
                    msg.append(json);
                    if(!scoreboard.isLast()) {
                        msg.append(",");
                    }
                }
                msg.append("]");
                requestHandeled = true;
                return msg.toString();
            }
        }
        requestHandeled = false;
        return null;
    }

    //sends back all their previous battles that have been taken place
    public String getBattleLogs(requestInfo requestInfo) throws FileNotFoundException {
        String token = requestInfo.authenticationToken(requestInfo.requestString);
        //checks if a token has been sent in the request
        if (token != null) {
            String[] tokenSplit = token.split(" ");
            String[] tokenName = tokenSplit[1].split("-");

            File getFiles = new File("battleLog/");
            String[] pathNames = getFiles.list();
            ArrayList<String> userLogs = new ArrayList<>();
            StringBuilder logs = new StringBuilder();
            int numberOfEntries;

            //assert pathNames != null;
            if(pathNames == null){
                return null;
            }
            for (String name : pathNames) {
                if (name.contains(tokenName[0])) {
                    userLogs.add(name);
                }
            }
            numberOfEntries = userLogs.size();
            if (numberOfEntries == 0) {
                specialCaseInt = 1;
                return null;
            }
            for (String log : userLogs) {
                logs.append(log).append("\r\n");
                File file = new File("battleLog/" + log);
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    logs.append(scanner.nextLine()).append("\r\n");
                }
                logs.append("\r\n");
                scanner.close();
            }
            return logs.toString();
        }else{
            specialCaseInt = 2;
            return null;
        }
    }

}
