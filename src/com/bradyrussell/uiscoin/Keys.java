package com.bradyrussell.uiscoin;

import java.nio.charset.Charset;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Base64;

public class Keys {
    public static KeyPair makeKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyGen.initialize(ecSpec, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    public static SignedMessage SignMessage(KeyPair Keys, String Message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature ecdsaSign = Signature.getInstance("SHA512withECDSA");
        ecdsaSign.initSign(Keys.getPrivate());
        ecdsaSign.update(Message.getBytes(Charset.defaultCharset()));
        byte[] signature = ecdsaSign.sign();
        String pub = Base64.getEncoder().encodeToString(Keys.getPublic().getEncoded());
        String sig = Base64.getEncoder().encodeToString(signature);
        System.out.println(pub);
        System.out.println(sig);
        return new SignedMessage(pub, sig, Message);
    }

    public static boolean VerifyMessage(SignedMessage Message) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        Signature ecdsaVerify = Signature.getInstance("SHA512withECDSA");

        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(Message.Pubkey));

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(Message.Message.getBytes(Charset.defaultCharset()));
        return ecdsaVerify.verify(Base64.getDecoder().decode(Message.Signature));
    }

    public static class SignedMessage{
        String Pubkey;
        String Signature;
        String Message;

        public SignedMessage(String pubkey, String signature, String message) {
            Pubkey = pubkey;
            Signature = signature;
            Message = message;
        }
    }

}
