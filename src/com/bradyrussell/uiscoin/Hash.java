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

    public static boolean validateHash(byte[] Hash){
        return Hash[0] == 74 && Hash[1] == 97 && Hash[2] == 99 && Hash[3] == 107;
    }
}
