package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.MagicBytes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Node {
    DatagramSocket ServerSocket;
    HashMap<InetAddress, Peer> Peers = new HashMap<>();
    int Version;

    public Node(int Version) {
        this.Version = Version;
        try {
            ServerSocket = new DatagramSocket(MagicBytes.NodeP2PPort.Value);
            ServerSocket.setSoTimeout(10);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void SendAll(byte[] Data) {
        Peers.forEach((k,v)-> {
            v.Send(Data);
        });
    }

    public boolean Receive(DatagramPacket packet){
        try {
            ServerSocket.receive(packet);
            AddPeer(packet.getAddress());
        } catch (IOException e) {
           // e.printStackTrace();
        }
        return packet.getPort() > 0;
    }

    public void AddPeer(InetAddress Address){
        Peers.put(Address, new Peer(Address));
    }

    public int getNumberPeers(){
        return Peers.size();
    }

    public void Stop(){
        ServerSocket.disconnect();
        ServerSocket.close();
    }
}
