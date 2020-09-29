package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.address.Wallet;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageFile;
import com.bradyrussell.uiscoin.node.BlockRequest;
import com.bradyrussell.uiscoin.node.MemPool;
import com.bradyrussell.uiscoin.node.Node;
import com.bradyrussell.uiscoin.transaction.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class Main {

    public static void main(String[] args) {
        BlockChain.Initialize(BlockChainStorageFile.class);

        Node node = new Node(1);

        //node.Start();

        try {
            node.ConnectToPeer(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        node.RequestBlockFromPeers(new BlockRequest(Util.Base64Decode("UIRTCXb5LIKUQMJuU5dM18OoNdlHztGJMRv0KUM3FbzhxHk9_rJyphibpcTT40NfjmE4GN5AZrGDQo1X2c8mJg==")));


        while (!node.peerClients.isEmpty());

        node.Stop();
       /* String myTransactionInputHash = "YdfeDWmoO9Xklr_T3dSfdrCuHZBohDlw9gS7Z4RutuDg6ASjmaGbZmfIIcNIV2nZsfYR_NrqzcuA5Y0D9ScbgQ==";

        BlockChain.Initialize(BlockChainStorageFile.class);
        MemPool memPool = new MemPool();
        //byte[] bytes = BlockChain.get().get(Hash.getSHA512Bytes("mempool"), "mempool");
        //if(bytes != null){
         //   memPool.setBinaryData(bytes);
       // }

        System.out.println("Loaded mempool with "+memPool.pendingTransactions.size()+" pending transactions.");
        for(Transaction pendingTransaction:memPool.pendingTransactions){
            //System.out.println("Pending Transaction: "+Util.Base64Encode(pendingTransaction.getBinaryData()));
            for(TransactionInput transactionInput: pendingTransaction.Inputs){
                System.out.println(Util.Base64Encode(transactionInput.UnlockingScript));
            }
*//*            for(TransactionOutput transactionOutput: pendingTransaction.Outputs){
                System.out.println("Output: "+Util.Base64Encode(transactionOutput.getHash()));
            }*//*
        }

        UISCoinKeypair address1 = Wallet.LoadKeypairFromFileWithPassword(Path.of("C:\\Users\\Admin\\Desktop\\MyRealUISCoinWallet\\kushJr.uisw"), "VanityAddress1");
        UISCoinKeypair address2 = Wallet.LoadKeypairFromFileWithPassword(Path.of("C:\\Users\\Admin\\Desktop\\MyRealUISCoinWallet\\coin.uisw"), "VanityAddress1");

        assert address1 != null;
        assert address2 != null;

        TransactionOutput outputToSpend = BlockChain.get().getUnspentTransactionOutput(Util.Base64Decode(myTransactionInputHash),0);

        assert outputToSpend != null;

        TransactionInput transactionInput = new TransactionInputBuilder().setInputTransaction(Util.Base64Decode(myTransactionInputHash), 0).setUnlockPayToPublicKeyHash(address1, outputToSpend).get();
        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(.25)).setPayToPublicKeyHash(UISCoinAddress.decodeAddress(UISCoinAddress.fromPublicKey((ECPublicKey) address2.Keys.getPublic())).PublicKeyHash).get();

        Transaction transaction = new TransactionBuilder().setVersion(1).addInput(transactionInput).addOutput(transactionOutput).addChangeOutput(UISCoinAddress.decodeAddress(UISCoinAddress.fromPublicKey((ECPublicKey) address1.Keys.getPublic())).PublicKeyHash, 1024).get();

        System.out.println(Util.Base64Encode(transaction.getHash()));
        transaction.DebugVerify();
        System.out.println(transaction.Verify());

        BlockChain.get().put(Hash.getSHA512Bytes("mempool"), memPool.getBinaryData(), "mempool");*/
/*
        Block block = BlockChain.get().getBlock(Util.Base64Decode("UISXXJXK9KEfRp1bc6oiCOjAS_Ks_KBvfUoGrKDl_Kaw0bl4a_Ufh8mTiISr7qEkc_NQ2rlbvH1K8l1AeM2QFg=="));

        System.out.println(Util.Base64Encode(block.getHash()));
        for(Transaction transaction: block.Transactions){
            System.out.println(Util.Base64Encode(transaction.getHash()));
        }
*/


/*        UISCoinKeypair address1 = Wallet.LoadKeypairFromFileWithPassword(Path.of("C:\\Users\\Admin\\Desktop\\MyRealUISCoinWallet\\kushJr.uisw"), "VanityAddress1");
        assert address1 != null;
        byte[] addressBytes = UISCoinAddress.fromPublicKey((ECPublicKey) address1.Keys.getPublic());

        BlockBuilder blockBuilder = new BlockBuilder().setVersion(1).setTimestamp(Instant.now().getEpochSecond()).setDifficultyTarget(2)
                .setHashPreviousBlock(Hash.getSHA512Bytes("Hello world from UISCoin."))
                .addCoinbase(new TransactionBuilder().setVersion(1).setLockTime(0).addOutput(new TransactionOutputBuilder().setPayToPublicKeyHash(addressBytes).setAmount(Conversions.CoinsToSatoshis(1)).get()).get())
                .CalculateMerkleRoot();

        while(!Hash.validateHash(blockBuilder.get().getHash(), blockBuilder.get().Header.DifficultyTarget)) {
            blockBuilder.setNonce(ThreadLocalRandom.current().nextInt());
        }

        Block finishedBlock = blockBuilder.get();
        System.out.println(Base64.getEncoder().encodeToString(finishedBlock.getHash()));

        BlockChain.get().putBlock(finishedBlock);*/




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
