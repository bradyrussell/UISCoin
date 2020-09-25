package com.bradyrussell.uiscoin;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class Encryption {
    public static byte[] Encrypt(byte[] Data, byte[] Key){
        try {
            byte[] keyBytes = Arrays.copyOf(Hash.getSHA512Bytes(Key), 16);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return cipher.doFinal(Data);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] Decrypt(byte[] Data, byte[] Key){
        try {
            byte[] keyBytes = Arrays.copyOf(Hash.getSHA512Bytes(Key), 16);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return cipher.doFinal(Data);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
