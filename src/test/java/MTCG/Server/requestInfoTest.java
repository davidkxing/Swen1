package MTCG.Server;

import MTCG.Server.requestInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class requestInfoTest {

    @Test public void testConstructor(){
        requestInfo requestInfoPOST = new requestInfo("POST", "HTTP/1.1", "/users", "POST /users HTTP/1.1\r\nHost: localhost:10001\r\nUser-Agent: insomnia/2020.4.2\r\nAccept: */*\r\nContent-Length: 5\r\n\r\nTest2\r\n");
        Assertions.assertEquals("POST", requestInfoPOST.getRequest());
        Assertions.assertEquals("HTTP/1.1", requestInfoPOST.getHttpType());
        Assertions.assertEquals("/users", requestInfoPOST.getURI());
        Assertions.assertEquals("Test2", requestInfoPOST.getMessage());

        requestInfo requestInfoGET = new requestInfo("GET", "HTTP/1.1", "/log", "POST /log HTTP/1.1\r\nHost: localhost:10001\r\nUser-Agent: insomnia/2020.4.2\r\nAccept: */*\r\nContent-Length: 5\r\n\r\nTest2\r\n");
        Assertions.assertNull(requestInfoGET.getMessage());
    }
}