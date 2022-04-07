//github repo: https://github.com/davidkxing/Swen1
package MTCG.Server;

import MTCG.GameLogic.battleHandler;
import MTCG.GameLogic.cardHandler;
import MTCG.GameLogic.shopHandler;
import MTCG.GameLogic.userHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class server {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(10001);
        System.out.println("Listening on port 10001...");
        startServer(serverSocket);

    }

    public static void startServer(ServerSocket serverSocket) {
        try {
            while(true) {
                Socket socket = serverSocket.accept();
                InputStream inputStream = socket.getInputStream();

                //handles input stream and saves it char by char
                StringBuilder result = new StringBuilder();
                while(inputStream.available() > 0) {
                    result.append((char) inputStream.read());
                }

                //split string by spaces into String[]
                String request = result.toString();
                String[] requestSplit = request.split(" ");

                RequestHandler(requestSplit, socket, request);
                socket.close();
            }
        } catch(Exception e) {

            e.printStackTrace();
        }
    }


    public static void RequestHandler(String[] requestSplit, Socket serverSocket, String requestString) {
        String request = requestSplit[0];
        if(!request.isEmpty()) {
            String[] httpVersion = requestSplit[2].split("\\r?\\n");
            //send info of the request to requestInfo Class to save variables of the request
            requestInfo requestInfo = new requestInfo(request, httpVersion[0], requestSplit[1], requestString);

            //filters the incoming requests to right function
            try {
                String[] UriCase = requestInfo.URI.split("/");
                switch (UriCase[1]) {
                    case "users", "sessions", "stats", "score", "log" -> new userHandler(requestInfo, serverSocket);
                    case "packages", "transactions", "tradings" -> new shopHandler(requestInfo, serverSocket);
                    case "cards", "deck", "deck?format=plain" -> new cardHandler(requestInfo, serverSocket);
                    case "battles" -> new battleHandler(requestInfo, serverSocket);
                    default -> {
                        replyHandler replyHandler = new replyHandler(serverSocket);
                        replyHandler.generalErrorReply();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }
        }
    }
}
