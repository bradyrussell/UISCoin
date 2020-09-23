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
        for(int i = 0; i < Difficulty; i++){
            byte KeyByte;

            switch (i % 4){
                case 0 -> KeyByte = 74;
                case 1 -> KeyByte = 97;
                case 2 -> KeyByte = 99;
                case 3 -> KeyByte = 107;
                default -> {
                    return false;
                }
            }

            if(Hash[i] != KeyByte) return false;
        }
        return true;
    }
}
