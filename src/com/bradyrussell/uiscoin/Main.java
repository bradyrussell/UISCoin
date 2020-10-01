package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageFile;
import com.bradyrussell.uiscoin.node.BlockRequest;
import com.bradyrussell.uiscoin.node.Node;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) {
        BlockChain.Initialize(BlockChainStorageFile.class);

        Node node = new Node(1);

        //node.Start();

        try {
            node.ConnectToPeer(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        node.RequestBlockFromPeers(new BlockRequest(Util.Base64Decode("UIRTCXb5LIKUQMJuU5dM18OoNdlHztGJMRv0KUM3FbzhxHk9_rJyphibpcTT40NfjmE4GN5AZrGDQo1X2c8mJg==")));
        UISCoinKeypair a;

        while (!node.peerClients.isEmpty());

        node.Stop();
    }
}
