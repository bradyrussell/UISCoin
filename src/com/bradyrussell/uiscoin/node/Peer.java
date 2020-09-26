package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.MagicNumbers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Instant;

public class Peer {
    DatagramSocket ClientSocket;
    InetAddress Address;
    long LastSeen;

    public Peer(InetAddress address) {
        Address = address;
        Seen();
        try {
            ClientSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void Seen(){
        LastSeen = Instant.now().getEpochSecond();
    }

    public void Send(byte[] Data){
        DatagramPacket packet = new DatagramPacket(Data, Data.length, Address, MagicNumbers.NodeP2PPort.Value);
        try {
            ClientSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
