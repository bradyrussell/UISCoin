package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.MagicNumbers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Instant;
import java.util.*;

public class Node {
    DatagramSocket ServerSocket;
    HashMap<InetAddress, Peer> Peers = new HashMap<>();
    int Version;

    public Node(int Version) {
        this.Version = Version;
        try {
            ServerSocket = new DatagramSocket(MagicNumbers.NodeP2PPort.Value);
            ServerSocket.setSoTimeout(1);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void CheckForTimeouts(){
        List<InetAddress> deadPeers = new ArrayList<>();

        Peers.forEach((k,v)-> {
            if(v.LastSeen < (Instant.now().getEpochSecond() - MagicNumbers.NodeP2PTimeout.Value)) {
                deadPeers.add(v.Address);
            }
            else if(v.LastSeen < (Instant.now().getEpochSecond() - MagicNumbers.NodeP2PTimeout.Value/2)) {
                v.Send(new PeerPacketBuilder(8).putPing(1).get());
            }
        });

        for(InetAddress addr: deadPeers){
            Peers.remove(addr);
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
            Peers.get(packet.getAddress()).Seen();
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

    public Set<InetAddress> getPeers(){
        return Peers.keySet();
    }

    public void Stop(){
        ServerSocket.disconnect();
        ServerSocket.close();
    }
}
