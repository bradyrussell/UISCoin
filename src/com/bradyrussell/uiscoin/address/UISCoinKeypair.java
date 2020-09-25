package com.bradyrussell.uiscoin.address;

import com.bradyrussell.uiscoin.*;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class UISCoinKeypair implements IBinaryData {
    public KeyPair Keys;

    public static UISCoinKeypair Create(){
        UISCoinKeypair keypair = new UISCoinKeypair();
        try {
            keypair.Keys = com.bradyrussell.uiscoin.Keys.makeKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return keypair;
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.put(MagicBytes.AddressVersion.Value);
        byte[] pub = Keys.getPublic().getEncoded();
        buf.put(pub);
        byte[] priv = Keys.getPrivate().getEncoded();
        buf.put(priv);

        return buf.array();
    }

    @Override
    public void setBinaryData(byte[] Data) {
        ByteBuffer buf = ByteBuffer.wrap(Data);

        byte Version = buf.get();

        byte[] Public = new byte[88];
        byte[] Private = new byte[64];

        buf.get(Public);
        buf.get(Private);

        try {
            Keys = com.bradyrussell.uiscoin.Keys.LoadKeys(Public,Private);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int getSize() {
        return 4+88+64;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
