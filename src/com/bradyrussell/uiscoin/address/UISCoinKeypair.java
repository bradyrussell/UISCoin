package com.bradyrussell.uiscoin.address;

import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.block.Block;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class UISCoinKeypair implements IBinaryData {
    private static final Logger Log = Logger.getLogger(UISCoinKeypair.class.getName());

    public KeyPair Keys;
    public byte[] Seed; // todo this is a property of wallet not keypair

    public static UISCoinKeypair Create(){
        UISCoinKeypair keypair = new UISCoinKeypair();
        try {
            keypair.Seed =  new byte[32];
            ThreadLocalRandom.current().nextBytes(keypair.Seed);
            keypair.Keys = com.bradyrussell.uiscoin.Keys.makeKeyPair(keypair.Seed);
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
        buf.put(Seed);

        return buf.array();
    }

    @Override
    public int setBinaryData(byte[] Data) {
        ByteBuffer buf = ByteBuffer.wrap(Data);

        byte Version = buf.get();

        byte[] Public = new byte[88];
        byte[] Private = new byte[64];
        Seed = new byte[32];

        buf.get(Public);
        buf.get(Private);
        buf.get(Seed);

        try {
            Keys = com.bradyrussell.uiscoin.Keys.LoadKeys(Public,Private);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return buf.position();
    }

    @Override
    public int getSize() {
        return 4+88+64+32;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
