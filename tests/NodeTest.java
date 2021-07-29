import com.bradyrussell.uiscoin.HTTP;
import com.bradyrussell.uiscoin.node.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class NodeTest {
    @Test
    @DisplayName("Node does not connect to self")
    void TestNodeConnectToSelf() throws IOException, InterruptedException {
        Node node = new Node();

        node.start();

        String ipJson = HTTP.request("https://api.ipify.org?format=json", "GET", null, null);
        String ip = ipJson.split(":")[1].replace("\"", "").replace("}", "");
        System.out.println(ip);

        node.connectToPeer(InetAddress.getByName(ip));

        Thread.sleep(5000);

        assertFalse(node.getPeers().size() > 1);
    }

}