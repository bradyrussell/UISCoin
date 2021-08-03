package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockBuilder;
import com.bradyrussell.uiscoin.blockchain.exception.InvalidBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.transaction.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class BlockTest {
    @RepeatedTest(10)
    @DisplayName("Serialization / Deserialization")
    void TestBlockSerialization() throws NoSuchTransactionException, NoSuchBlockException, InvalidBlockException {
        long timeStamp = Instant.now().getEpochSecond();

        UISCoinKeypair uisCoinKeypair = UISCoinKeypair.create();

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

        TransactionBuilder tb = new TransactionBuilder();
        Transaction transaction = tb.setVersion(1).setLockTime(-1)
                .addInput(new TransactionInputBuilder().setInputTransaction(RandomHash2,0).setUnlockingScript(Hash.getSHA512Bytes("aaa")).get())
                .addOutput(new TransactionOutput(Conversions.coinsToSatoshis(.5), RandomHash2))
                .get();

        Block block = new BlockBuilder().setBlockHeight(1).addCoinbasePayToPublicKeyHash(RandomHash1).setHashPreviousBlock(RandomHash2).get();

        block.addTransaction(transaction);

        block.Header.HashPreviousBlock = RandomHash2;
        block.Header.HashMerkleRoot = block.calculateMerkleRoot();

        byte[] blockBinaryData = block.getBinaryData();

        Block deserializedBlock = new Block();
        deserializedBlock.setBinaryData(blockBinaryData);

        byte[] deserializedBlockBinaryData = deserializedBlock.getBinaryData();

        for (int i = 0, blockBinaryDataLength = blockBinaryData.length; i < blockBinaryDataLength; i++) {
            if(blockBinaryData[i] != deserializedBlockBinaryData[i]) fail("Byte mismatch at position "+i+"\n"+Arrays.toString(blockBinaryData)+"\n"+Arrays.toString(deserializedBlockBinaryData));
        }

    }

    @RepeatedTest(100)
    @DisplayName("Hash GetDifficulty")
    void TestHashGetDifficulty() {

        byte[] hash = BytesUtil.base64Decode("UISa1foAACU7v1siyHul3StuvEJx5rebFgMkP_Sgiz60NoOSV00SP-yD_MO7JoHr7mmixKvj28QECDeaXQfzIQ==");
        assertEquals(Hash.validateHash(hash,3),Hash.getHashDifficulty(hash)==3);

    }

/*    @RepeatedTest(1)
    @DisplayName("Verification")
    void TestBlockVerification() {
        BlockChain.get().

        UISCoinKeypair address1 = UISCoinKeypair.Create();
        byte[] addressBytes = UISCoinAddress.fromPublicKey((ECPublicKey) address1.Keys.getPublic());

        BlockBuilder blockBuilder = new BlockBuilder().setVersion(1).setTimestamp(Instant.now().getEpochSecond()).setDifficultyTarget(2).setBlockHeight(0)
                .setHashPreviousBlock(Hash.getSHA512Bytes("Hello world from UISCoin."))
                .addCoinbasePayToPublicKeyHash(UISCoinAddress.decodeAddress(addressBytes).PublicKeyHash)
                .CalculateMerkleRoot();

        while(!Hash.validateHash(blockBuilder.get().getHash(), blockBuilder.get().Header.DifficultyTarget)) {
            blockBuilder.setNonce(ThreadLocalRandom.current().nextInt());
        }

        Block finishedBlock = blockBuilder.get();
        System.out.println(Base64.getEncoder().encodeToString(finishedBlock.getHash()));

        finishedBlock.DebugVerify();

        assertTrue(finishedBlock.Verify());
        //BlockChain.get().putBlock(finishedBlock);

    }*/
}