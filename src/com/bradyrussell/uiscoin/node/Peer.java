package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.MagicBytes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Peer {
    DatagramSocket ClientSocket;
    InetAddress Address;

    public Peer(InetAddress address) {
        Address = address;
        try {
            ClientSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void Send(byte[] Data){
        DatagramPacket packet = new DatagramPacket(Data, Data.length, Address, MagicBytes.NodeP2PPort.Value);
        try {
            ClientSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
