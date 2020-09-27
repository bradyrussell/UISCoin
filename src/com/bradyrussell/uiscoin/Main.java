package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.address.Wallet;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageBase;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageFile;
import com.bradyrussell.uiscoin.transaction.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    public static void main(String[] args) {

        BlockChain.Initialize(BlockChainStorageFile.class);

        long timeStamp = Instant.now().getEpochSecond();

        UISCoinKeypair uisCoinKeypair = UISCoinKeypair.Create();

        byte[] RandomHash1 = new byte[64];
        byte[] RandomHash2 = new byte[64];
        byte[] RandomHash3 = new byte[64];
        byte[] RandomHash5 = new byte[64];
        byte[] RandomHash6 = new byte[64];

        ThreadLocalRandom.current().nextBytes(RandomHash1);
        ThreadLocalRandom.current().nextBytes(RandomHash2);
        ThreadLocalRandom.current().nextBytes(RandomHash3);
        ThreadLocalRandom.current().nextBytes(RandomHash5);
        ThreadLocalRandom.current().nextBytes(RandomHash6);

/*        TransactionBuilder tb = new TransactionBuilder();
        Transaction transaction = tb.setVersion(1).setLockTime(-1)
                .addInput(new TransactionInput(RandomHash1, 0))
                .addOutput(new TransactionOutput(Conversions.CoinsToSatoshis(.5), RandomHash2))
                .signTransaction(uisCoinKeypair).get();*/

        Block block = new Block(new BlockHeader(1,timeStamp,3, 0));

        block.addTransaction(new Transaction(1,timeStamp).addOutput(
                new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(50))
                        .setPayToPublicKeyHash(Base64.getDecoder().decode("UISxUisdl8E31ksaCZvw3RKR9biwgXPi/m6lUTyN4E9K0n2vI+Xc5QFVtWpPz9+8fr2DwE5T40qLVbEj7QFsEyve3YteiPg=")).get()));
       // block.addTransaction(transaction);

        block.Header.HashPreviousBlock = RandomHash2;
        block.Header.HashMerkleRoot = block.CalculateMerkleRoot();

        while(!Hash.validateHash(block.getHash(), block.Header.DifficultyTarget)) {
            block.Header.Nonce = ThreadLocalRandom.current().nextInt();
        }

        System.out.println(Base64.getEncoder().encodeToString(block.getHash()));

        BlockChain.get().putBlock(block);

        /*try {
            List<String> strings = Files.readAllLines(Path.of("search.txt"));
            for(String search:strings) {
                boolean searching = true;
                while (searching) {
                    UISCoinKeypair uisCoinKeypair = UISCoinKeypair.Create();
                    byte[] address = UISCoinAddress.fromPublicKey((ECPublicKey) uisCoinKeypair.Keys.getPublic());

                    //if(!UISCoinAddress.verifyAddressChecksum(address)) continue;;

                    String string = Base64.getEncoder().encodeToString(address);
                    // System.out.println(string);

                    if (string.substring(4).toLowerCase().startsWith(search.toLowerCase())) {
                        System.out.println(string);
                        searching = false;
                        Wallet.SaveKeypairToFileWithPassword(Path.of(search + ".uisw"), "VanityAddress1", uisCoinKeypair);
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/

    }
}
