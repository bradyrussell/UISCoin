package com.bradyrussell.uiscoin;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
    public static String getSHA512String(String Input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        digest.update(Input.getBytes(Charset.defaultCharset()));
        return String.format("%0128x", new BigInteger(1, digest.digest()));
    }

    public static byte[] getSHA512Bytes(byte[] Input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        digest.update(Input);
        return digest.digest();
    }

    public static String getSHA512String(byte[] Input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        digest.update(Input);
        return String.format("%0128x", new BigInteger(1, digest.digest()));
    }

    public static byte[] getSHA512Bytes(String Input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        digest.update(Input.getBytes(Charset.defaultCharset()));
        return digest.digest();
    }

    public static boolean validateHash(byte[] Hash, int Difficulty){
        //final byte[] Prefix = {74, 97, 99, 107}; // Jack
        //final byte[] Prefix = {85, 73, 83}; // UIS
        final byte[] Prefix = {0x55, 0x49, 0x53}; // UIS

        for(int i = 0; i < Difficulty; i++){
            if(Hash[i] != Prefix[i % Prefix.length]) return false;
        }
        return true;
    }
}
