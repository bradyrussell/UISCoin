package com.bradyrussell.uiscoin;

import java.math.BigInteger;
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

    public static SignedData signData(KeyPair Keys, byte[] Message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature ecdsaSign = Signature.getInstance("SHA512withECDSA");
        ecdsaSign.initSign(Keys.getPrivate());
        ecdsaSign.update(Message);
        byte[] signature = ecdsaSign.sign();

        return new SignedData(Keys.getPublic().getEncoded(), signature, Message);
    }

    public static boolean verifySignedData(SignedData Message) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        Signature ecdsaVerify = Signature.getInstance("SHA512withECDSA");

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Message.Pubkey));

        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(Message.Message);
        return ecdsaVerify.verify(Message.Signature);
    }

    public static KeyPair loadKeys(byte[] Public, byte[] Private) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Public));
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Private));
        return new KeyPair(publicKey, privateKey);
    }

    public static ECPublicKey getPublicKeyFromPrivateKey(ECPrivateKey PrivateKey) throws GeneralSecurityException {
        return getPublicKey(PrivateKey);
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

    //https://stackoverflow.com/questions/19673962/codes-to-generate-a-public-key-in-an-elliptic-curve-algorithm-using-a-given-priv
    ///////////////////////////////////////////////////////////////

    private static ECPoint doublePoint(final BigInteger p, final BigInteger a, final ECPoint R) {
        ECPoint result;
        if (R.equals(ECPoint.POINT_INFINITY)) {
            result = R;
        } else {
            BigInteger slope = (R.getAffineX().pow(2)).multiply(BigInteger.valueOf(3));
            slope = slope.add(a);
            slope = slope.multiply((R.getAffineY().multiply(BigInteger.TWO)).modInverse(p));
            final BigInteger Xout = slope.pow(2).subtract(R.getAffineX().multiply(BigInteger.TWO)).mod(p);
            final BigInteger Yout = (R.getAffineY().negate()).add(slope.multiply(R.getAffineX().subtract(Xout))).mod(p);
            result = new ECPoint(Xout, Yout);
        }
        return result;
    }

    private static ECPoint addPoint(final BigInteger p, final BigInteger a, final ECPoint r, final ECPoint g) {
        ECPoint result;
        if (r.equals(ECPoint.POINT_INFINITY)) {
            result = g;
        } else if (g.equals(ECPoint.POINT_INFINITY)) {
            result = r;
        } else if (r == g || r.equals(g)) {
            result = doublePoint(p, a, r);
        } else {
            final BigInteger gX = g.getAffineX();
            final BigInteger sY = g.getAffineY();
            final BigInteger rX = r.getAffineX();
            final BigInteger rY = r.getAffineY();
            final BigInteger slope = (rY.subtract(sY)).multiply(rX.subtract(gX).modInverse(p)).mod(p);
            final BigInteger Xout = (slope.modPow(BigInteger.TWO, p).subtract(rX)).subtract(gX).mod(p);
            BigInteger Yout = sY.negate().mod(p);
            Yout = Yout.add(slope.multiply(gX.subtract(Xout))).mod(p);
            result = new ECPoint(Xout, Yout);
        }
        return result;
    }

    private static ECPoint scalmult(final EllipticCurve curve, final ECPoint g, final BigInteger kin) {
        final ECField field = curve.getField();
        if (!(field instanceof ECFieldFp)) throw new UnsupportedOperationException(field.getClass().getCanonicalName());
        final BigInteger p = ((ECFieldFp) field).getP();
        final BigInteger a = curve.getA();
        ECPoint R = ECPoint.POINT_INFINITY;
        BigInteger k = kin.mod(p);
        final int length = k.bitLength();
        final byte[] binarray = new byte[length];
        for (int i = 0; i <= length - 1; i++) {
            binarray[i] = k.mod(BigInteger.TWO).byteValue();
            k = k.shiftRight(1);
        }
        for (int i = length - 1; i >= 0; i--) {
            R = doublePoint(p, a, R);
            if (binarray[i] == 1) R = addPoint(p, a, R, g);
        }
        return R;
    }

    private static ECPublicKey getPublicKey(final ECPrivateKey pk) throws GeneralSecurityException {
        final ECParameterSpec params = pk.getParams();
        final ECPoint w = scalmult(params.getCurve(), pk.getParams().getGenerator(), pk.getS());
        final KeyFactory kg = KeyFactory.getInstance("EC");
        return (ECPublicKey) kg.generatePublic(new ECPublicKeySpec(w, params));
    }
    ///////////////////////////////////////////////////////////////
}
