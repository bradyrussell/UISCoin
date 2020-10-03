package com.bradyrussell.uiscoin.address;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.Util;

import java.security.interfaces.ECPublicKey;
import java.util.logging.Logger;

public class UISCoinAddress {
    private static final Logger Log = Logger.getLogger(UISCoinAddress.class.getName());

    public static byte[] fromPublicKey(ECPublicKey PubKey){
        byte[] header = {MagicBytes.AddressHeader.Value, MagicBytes.AddressType.Value, MagicBytes.AddressVersion.Value};

        byte[] pubkeyHash = Hash.getSHA512Bytes(PubKey.getEncoded());

        byte[] checksum = getChecksumFromPublicKeyHash(pubkeyHash);

        return Util.ConcatArray(header,Util.ConcatArray(pubkeyHash, checksum));
    }

    public static DecodedAddress decodeAddress(byte[] Address){
        DecodedAddress decodedAddress = new DecodedAddress();
        byte[] header = new byte[3];
        decodedAddress.Checksum = new byte[4];
        decodedAddress.PublicKeyHash = new byte[Address.length-7];

        System.arraycopy(Address, 0, header, 0, 3);
        System.arraycopy(Address, Address.length-4, decodedAddress.Checksum, 0, 4);
        System.arraycopy(Address, 3, decodedAddress.PublicKeyHash, 0, Address.length-7);

        decodedAddress.Type = header[1];
        decodedAddress.Version = header[2];

        return decodedAddress;
    }

    public static byte[] getChecksumFromPublicKeyHash(byte[] PublicKeyHash) {
        byte[] checksum = new byte[4];
        System.arraycopy(Hash.getSHA512Bytes(PublicKeyHash), 0, checksum, 0, 4);
        return checksum;
    }

    public static boolean verifyAddressChecksum(byte[] Address){
        DecodedAddress decodedAddress = decodeAddress(Address);

        byte[] properChecksum = getChecksumFromPublicKeyHash(decodedAddress.PublicKeyHash);

        for (int i = 0; i < properChecksum.length; i++) {
            if(properChecksum[i] != decodedAddress.Checksum[i]) return false;
        }
        return true;
    }

    public static class DecodedAddress {
        public byte Type;
        public byte Version;
        public byte[] PublicKeyHash;
        public byte[] Checksum;
    }
}
