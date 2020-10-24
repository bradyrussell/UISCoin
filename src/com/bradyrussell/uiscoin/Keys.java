package com.bradyrussell.uiscoin;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;

public class Keys {
    public static KeyPair makeKeyPair(byte[] Seed) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyGen.initialize(ecSpec, new SecureRandom(Seed));
        return keyGen.generateKeyPair();
    }

    public static SignedData SignData(KeyPair Keys, byte[] Message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature ecdsaSign = Signature.getInstance("SHA512withECDSA");
        ecdsaSign.initSign(Keys.getPrivate());
        ecdsaSign.update(Message);
        byte[] signature = ecdsaSign.sign();

        return new SignedData(Keys.getPublic().getEncoded(),signature,Message);
    }

    public static boolean VerifySignedData(SignedData Message) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        Signature ecdsaVerify = Signature.getInstance("SHA512withECDSA");

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Message.Pubkey));

        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(Message.Message);
        return ecdsaVerify.verify(Message.Signature);
    }

    public static KeyPair LoadKeys(byte[] Public, byte[] Private) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Public));
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Private));
        return new KeyPair(publicKey, privateKey);
    }

    public static ECPublicKey getPublicKeyFromPrivateKey(ECPrivateKey PrivateKey) throws NoSuchAlgorithmException {
            // todo cant get this working yet
        return null;
    }

    public static class SignedData {
        public byte[] Pubkey;
        public byte[] Signature;
        public byte[] Message;

        public SignedData(byte[] pubkey, byte[] signature, byte[] message) {
            Pubkey = pubkey;
            Signature = signature;
            Message = message;
        }
    }

    ///////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////
}
