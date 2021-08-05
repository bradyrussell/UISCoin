/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.address;

import java.nio.ByteBuffer;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;
import com.bradyrussell.uiscoin.MagicBytes;

public class UISCoinWallet implements IBinaryData {
    private static final Logger Log = Logger.getLogger(UISCoinWallet.class.getName());

    public final ArrayList<UISCoinKeypair> Keypairs = new ArrayList<>();

    public UISCoinKeypair generateNewKey(){
        UISCoinKeypair create = UISCoinKeypair.create();
        Keypairs.add(create);
        return create;
    }

    public List<byte[]> getAddresses(){
        ArrayList<byte[]> decodedAddresses = new ArrayList<>();

        for (UISCoinKeypair keypair : Keypairs) {
            byte[] bytes = UISCoinAddress.fromPublicKey((ECPublicKey) keypair.Keys.getPublic());
            decodedAddresses.add(bytes);
        }

        return decodedAddresses;
    }

    public List<UISCoinAddress.DecodedAddress> getDecodedAddresses(){
        ArrayList<UISCoinAddress.DecodedAddress> decodedAddresses = new ArrayList<>();

        for (UISCoinKeypair keypair : Keypairs) {
            byte[] bytes = UISCoinAddress.fromPublicKey((ECPublicKey) keypair.Keys.getPublic());
            decodedAddresses.add(UISCoinAddress.decodeAddress(bytes));
        }

        return decodedAddresses;
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.put(MagicBytes.AddressVersion.Value);
        buf.putInt(Keypairs.size());

        for (UISCoinKeypair keypair : Keypairs) {
            buf.putInt(keypair.getSize());
            buf.put(keypair.getBinaryData());
        }

        return buf.array();
    }

    @Override
    public int setBinaryData(byte[] Data) {
        ByteBuffer buf = ByteBuffer.wrap(Data);
        byte MagicValue = buf.get();
        int NumPairs = buf.getInt();

        for(int i = 0; i<NumPairs;i++){
            int KeyPairSize = buf.getInt();
            byte[] KeyPair = new byte[KeyPairSize];
            buf.get(KeyPair);

            UISCoinKeypair uck = new UISCoinKeypair();
            uck.setBinaryData(KeyPair);
            Keypairs.add(uck);
        }

        return buf.position();
    }

    @Override
    public int getSize() {
        int n = 0;
        for (UISCoinKeypair keypair : Keypairs) {
            n+= 4 + keypair.getSize();
        }
        return n+5;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
