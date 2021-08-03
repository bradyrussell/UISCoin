/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.address;

import java.security.interfaces.ECPublicKey;
import java.util.logging.Logger;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.MagicBytes;

public class UISCoinAddress {
    private static final Logger Log = Logger.getLogger(UISCoinAddress.class.getName());

    public static byte[] fromPublicKey(ECPublicKey PubKey){
        byte[] header = {MagicBytes.AddressHeader.Value, MagicBytes.AddressHeader2.Value, MagicBytes.AddressVersion.Value};
        byte[] pubkeyHash = Hash.getSHA512Bytes(PubKey.getEncoded());
        byte[] checksum = getChecksumFromHashData(pubkeyHash);

        return BytesUtil.concatArray(header, BytesUtil.concatArray(pubkeyHash, checksum));
    }

    public static byte[] fromScriptHash(byte[] ScriptHash){
        byte[] header = {MagicBytes.AddressHeader.Value, MagicBytes.AddressHeader2.Value, MagicBytes.AddressVersionP2SH.Value};
        byte[] checksum = getChecksumFromHashData(ScriptHash);
        return BytesUtil.concatArray(header, BytesUtil.concatArray(ScriptHash, checksum));
    }

    public static DecodedAddress decodeAddress(byte[] Address){
        DecodedAddress decodedAddress = new DecodedAddress();
        byte[] header = new byte[3];
        decodedAddress.Checksum = new byte[4];
        decodedAddress.HashData = new byte[Address.length-7];

        System.arraycopy(Address, 0, header, 0, 3);
        System.arraycopy(Address, Address.length-4, decodedAddress.Checksum, 0, 4);
        System.arraycopy(Address, 3, decodedAddress.HashData, 0, Address.length-7);

        decodedAddress.Type = header[2];

        return decodedAddress;
    }

    public static byte[] getChecksumFromHashData(byte[] PublicKeyHash) {
        byte[] checksum = new byte[4];
        System.arraycopy(Hash.getSHA512Bytes(PublicKeyHash), 0, checksum, 0, 4);
        return checksum;
    }

    public static boolean verifyAddressChecksum(byte[] Address){
        DecodedAddress decodedAddress = decodeAddress(Address);

        byte[] properChecksum = getChecksumFromHashData(decodedAddress.HashData);

        for (int i = 0; i < properChecksum.length; i++) {
            if(properChecksum[i] != decodedAddress.Checksum[i]) return false;
        }
        return true;
    }

    public static class DecodedAddress {
        public byte Type;
        public byte[] HashData;
        public byte[] Checksum;
    }
}
