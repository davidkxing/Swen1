package MTCG.GameLogic;

import MTCG.Server.requestInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

class userHandlerTest {
    Connection con;

    @BeforeEach
    public void createDatabase() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mtcg", "postgres", "passwort");
    }


    @Test
    void createUserFailTest() throws SQLException, JsonProcessingException {
        requestInfo requestInfoUser = new requestInfo("POST", "HTTP/1.1", "/users", """
                POST /users HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Content-Length: 44\r
                \r
                {"Username":"kienboec", "Password":"daniel"}""");
        userHandler userHandler = new userHandler();
        boolean user = userHandler.createUser(requestInfoUser, con);
        Assertions.assertFalse(user);
    }

    @Test
    void loginUserSuccessTest() throws SQLException, JsonProcessingException {
        requestInfo requestInfoUser = new requestInfo("POST", "HTTP/1.1", "/sessions", """
                POST /sessions HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Content-Length: 44\r
                \r
                {"Username":"kienboec", "Password":"daniel"}""");
        userHandler userHandler = new userHandler();
        boolean user = userHandler.loginUser(requestInfoUser, con);
        Assertions.assertTrue(user);
    }

    @Test
    void loginUserFailTest() throws SQLException, JsonProcessingException {
        requestInfo requestInfoUser = new requestInfo("POST", "HTTP/1.1", "/sessions", """
                POST /sessions HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Content-Length: 44\r
                \r
                {"Username":"kienboec", "Password":"daiel"}""");
        userHandler userHandler = new userHandler();
        boolean user = userHandler.loginUser(requestInfoUser, con);
        Assertions.assertFalse(user);
    }

    @Test
    void getUserDataFailTest() throws SQLException {
        requestInfo requestInfoUser = new requestInfo("GET", "HTTP/1.1", "/users/kienboec", """
                GET /users/kienboec HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        userHandler userHandler = new userHandler();
        ResultSet resultSet = userHandler.getUserData(requestInfoUser, con);
        Assertions.assertNull(resultSet);
    }

    @Test
    void setUserDataFailTest() throws SQLException, JsonProcessingException {
        requestInfo requestInfoUser = new requestInfo("PUT", "HTTP/1.1", "/users/kienboec", """
              PUT /users/kienboec HTTP/1.1\r
              Host: localhost:10001\r
              User-Agent: curl/7.71.1\r
              Accept: */*\r
              Content-Type: application/json\r
              Authorization: Basic dummy-mtcgToken\r
              Content-Length: 61\r
              \r
              {"Name": "Kienboeck",  "Bio": "me playin...", "Image": ":-)"}\r""");
        userHandler userHandler = new userHandler();
        boolean bool = userHandler.setUserData(requestInfoUser, con);
        Assertions.assertFalse(bool);
    }

    @Test
    void getStatsFailTest() throws SQLException {
        requestInfo requestInfoUser = new requestInfo("GET", "HTTP/1.1", "/stats", """
                GET /stats HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        userHandler userHandler = new userHandler();
        ResultSet resultSet = userHandler.getStats(requestInfoUser, con);
        Assertions.assertNull(resultSet);
    }

    @Test
    void getScoreFailTest() throws SQLException, JsonProcessingException {
        requestInfo requestInfoUser = new requestInfo("GET", "HTTP/1.1", "/score", """
                GET /score HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        userHandler userHandler = new userHandler();
        String string = userHandler.getScore(requestInfoUser, con);
        Assertions.assertNull(string);
    }

    @Test
    void getBattleLogsFailTestEmptyReply() throws FileNotFoundException {
        requestInfo requestInfoUser = new requestInfo("GET", "HTTP/1.1", "/logs", """
                GET /logs HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        userHandler userHandler = new userHandler();
        String string = userHandler.getBattleLogs(requestInfoUser);
        Assertions.assertNull(string);
    }

    @Test
    void getBattleLogsFailNoToken() throws FileNotFoundException {
        requestInfo requestInfoUser = new requestInfo("GET", "HTTP/1.1", "/logs", """
                GET /logs HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r""");
        userHandler userHandler = new userHandler();
        String string = userHandler.getBattleLogs(requestInfoUser);
        Assertions.assertNull(string);
    }
}