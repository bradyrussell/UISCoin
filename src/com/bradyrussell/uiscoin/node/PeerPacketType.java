package com.bradyrussell.uiscoin.node;

public enum PeerPacketType {
    DISCONNECT(0x00),
    GREETING(0x01), // includes version
    HANDSHAKE(0x02), // includes version
    PING(0x03),
    PEER(0x04), // includes a peer's address (will be the same version)
    TRANSACTION(0x05), // this is a transaction broadcast
    BLOCK(0x06), // block broadcast

    REQUEST(0x07), // this indicates a request for a block

;

    public final byte Header;

    PeerPacketType(int header) {
        Header = (byte) header;
    }

    public static PeerPacketType getByHeader(byte Header){
        for(PeerPacketType type:PeerPacketType.values()){
            if(type.Header == Header) return type;
        }
        return null;
    }
}
