package MTCG.Server;

public class requestInfo {
    public String requestString;
    public String request;
    public String HttpType;
    public String URI;
    public String msg;

    //class to save important variables to use later
    public requestInfo(String request, String HttpType, String URI, String requestString)
    {
        this.request = request;
        this.HttpType = HttpType;
        this.URI = URI;
        this.requestString = requestString;
        System.out.println(requestString);
        if(request.equals("POST") || request.equals("PUT")) {
            messageHandler(requestString);
        }
    }
    public String getRequest() {
        return request;
    }
    public String getHttpType() {
        return HttpType;
    }
    public String getMessage() {
        return msg;
    }
    public String getURI() {
        return URI;
    }

    //saves the message of a request to use it later
    public void messageHandler(String requestString) {
        String[] lines = requestString.split("\\r?\\n");
        if (!URI.equals("/battles") && !URI.equals("/transactions/packages")) {
            int i = 0;
            while (!(lines[i].length() == 0)) {
                i++;
            }
            msg = lines[i + 1];
        }
    }

    //gets the token if it is in the request
    public String authenticationToken(String requestString) {
        String[] lines = requestString.split("\\r?\\n");
        int i = 0;
        if (requestString.contains("Authorization:")) {
            while (!(lines[i].startsWith("Authorization:"))) {
                i++;
            }
            String[] realToken = lines[i].split(": ");
            return realToken[1];
        }
        return null;
    }
}
