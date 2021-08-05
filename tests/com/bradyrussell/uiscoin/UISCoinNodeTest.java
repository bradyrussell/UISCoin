/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;
import java.util.HashMap;

import com.bradyrussell.uiscoin.blockchain.BlockchainStorage;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.blockchain.storage.BlockchainStorageInMemory;
import com.bradyrussell.uiscoin.node.PeerAddress;
import com.bradyrussell.uiscoin.node.UISCoinNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UISCoinNodeTest {
/*    @Test
    @DisplayName("Node does not connect to self") // this doesnt actually appear to be trying to connect to itself,
    void TestNodeConnectToSelf() throws IOException, InterruptedException {
        Blockchain.initialize(new BlockchainStorageEphemeral());

        Node node = new Node();

        node.start();

        String ipJson = HTTP.request("https://api.ipify.org?format=json", "GET", null, null);
        String ip = ipJson.split(":")[1].replace("\"", "").replace("}", "");
        System.out.println(ip);

        node.connectToPeer(InetAddress.getByName(ip));

        Thread.sleep(5000);

        assertFalse(node.getPeers().size() > 1);
    }*/
    @Test
    @DisplayName("Transaction Output Identifier Test") //
    void TestTransactionOutputIdentifier() throws IOException, InterruptedException {
        HashMap<BlockchainStorage.TransactionOutputIdentifier, String> map = new HashMap<>();

        map.put(new BlockchainStorage.TransactionOutputIdentifier(Base64.getUrlDecoder().decode("VJ4HvWyMU3tyE1NdOkdftYthIFDKhGUNToDmaOSW6Ko099CLZ0E9pMyq2iq08X0wPRZgXgD2DX5R776dVNjbbg=="), 1), "aaaaaa");
        assertEquals("aaaaaa", map.get(new BlockchainStorage.TransactionOutputIdentifier(Base64.getUrlDecoder().decode("VJ4HvWyMU3tyE1NdOkdftYthIFDKhGUNToDmaOSW6Ko099CLZ0E9pMyq2iq08X0wPRZgXgD2DX5R776dVNjbbg=="), 1)));
    }

    @Test
    @DisplayName("Basic Node Test") //
    void TestNode() throws IOException, InterruptedException {
        Blockchain.initialize(new BlockchainStorageInMemory());
        UISCoinNode a = new UISCoinNode();
        UISCoinNode b = new UISCoinNode();

        a.start(54321);
        b.start(54322);
        
        a.connectToPeer(new PeerAddress(InetAddress.getLocalHost(), 54322));

        Thread.sleep(1000);

        a.stop();
        b.stop();
    }

}