package com.bradyrussell.uiscoin.address;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.Util;

import java.nio.ByteBuffer;
import java.security.interfaces.ECPublicKey;

public class UISCoinAddress {
    public static byte[] fromPublicKey(ECPublicKey PubKey){
        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.put(MagicBytes.AddressHeader.Value);
        buf.put(MagicBytes.AddressType.Value);
        buf.put(MagicBytes.AddressVersion.Value);
        buf.put(PubKey.getW().getAffineX().toByteArray());
        buf.put(PubKey.getW().getAffineY().toByteArray());

        byte[] prehash = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, prehash, 0, buf.position());

        //System.out.println("Pre hash: ");
        //Util.printBytesReadable(prehash);



        byte[] checksum = new byte[4];
        System.arraycopy(Hash.getSHA512Bytes(prehash), 0, checksum, 0, 4);
       // FillNullBytes(checksum);

        //System.out.println("checksum");
        //Util.printBytesReadable(checksum);

        buf.put(checksum);
       // System.out.println("final address");
        byte[] output = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, output, 0, buf.position());
        //Util.printBytesReadable(output);
        return output;
    }

    private static void FillNullBytes(byte[] Array){ // if the checksum contains null bytes we cant trim it
        for(int i = 0; i < Array.length; i++){
            if(Array[i] == 0x00) Array[i] = (byte) 0xA1;
        }
    }

    public static boolean verifyAddressChecksum(byte[] Address){
        ByteBuffer buf = ByteBuffer.wrap(Address);

        byte[] data = new byte[Address.length-4];
        buf.get(data);

        System.out.println("pre hash data");
        Util.printBytesReadable(data);

        byte[] properChecksum = new byte[4];
        System.arraycopy(Hash.getSHA512Bytes(data), 0, properChecksum, 0, 4);
      //  FillNullBytes(properChecksum);

        System.out.println("proper checksum");
        Util.printBytesReadable(properChecksum);

        byte[] inputChecksum = new byte[4];
        buf.get(inputChecksum);

        System.out.println("input checksum");
        Util.printBytesReadable(inputChecksum);

        for (int i = 0; i < properChecksum.length; i++) {
            if(properChecksum[i] != inputChecksum[i]) return false;
        }
        return true;
    }
}
