/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
    final static byte[] Prefix = {0x50, (byte) 0x84, (byte) 0x94, 0x21, 0x25, 0x08, 0x49, 0x42, 0x12};

    public static String getSHA512String(String Input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(Input.getBytes(Charset.defaultCharset()));
            return String.format("%0128x", new BigInteger(1, digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getSHA512Bytes(byte[] Input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(Input);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getSHA512String(byte[] Input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(Input);
            return String.format("%0128x", new BigInteger(1, digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getSHA512Bytes(String Input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(Input.getBytes(Charset.defaultCharset()));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static boolean validateHash(byte[] Hash, int Difficulty) {
        //final byte[] Prefix = {74, 97, 99, 107}; // Jack
        //final byte[] Prefix = {85, 73, 83}; // UIS
       // final byte[] Prefix = {0x50, (byte) 0x84, (byte) 0x9a}; // UISa in base64

        for (int i = 0; i < Difficulty; i++) {
            if (Hash[i] != Prefix[i % Prefix.length]) return false;
        }
        return true;
    }

    public static int getHashDifficulty(byte[] Hash) {
        //final byte[] Prefix = {74, 97, 99, 107}; // Jack
        //final byte[] Prefix = {85, 73, 83}; // UIS
        //final byte[] Prefix = {0x50, (byte) 0x84, (byte) 0x9a}; // UISa in base64

        int n = 0;

        for (int i = 0; i < Hash.length; i++) {
            if (Hash[i] != Prefix[i % Prefix.length]) return n;
            n++;
        }
        return n;
    }
}
