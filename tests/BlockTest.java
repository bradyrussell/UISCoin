import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.fail;

public class BlockTest {
    @RepeatedTest(10)
    @DisplayName("Serialization / Deserialization")
    void TestBlockSerialization() {
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

        TransactionBuilder tb = new TransactionBuilder();
        Transaction transaction = tb.setVersion(1).setLockTime(-1)
                .addInput(new TransactionInput(RandomHash1, 0))
                .addOutput(new TransactionOutput(Conversions.CoinsToSatoshis(.5), RandomHash2))
                .signTransaction(uisCoinKeypair).get();

        Block block = new Block(new BlockHeader(1,timeStamp,1));

        block.addCoinbaseTransaction(new CoinbaseTransaction(1,1).addOutput(new TransactionOutput(Conversions.CoinsToSatoshis(1), RandomHash6)));
        block.addTransaction(transaction);

        block.Header.HashPreviousBlock = RandomHash2;
        block.Header.HashMerkleRoot = block.CalculateMerkleRoot();

        byte[] blockBinaryData = block.getBinaryData();

        Block deserializedBlock = new Block();
        deserializedBlock.setBinaryData(blockBinaryData);

        byte[] deserializedBlockBinaryData = deserializedBlock.getBinaryData();

        for (int i = 0, blockBinaryDataLength = blockBinaryData.length; i < blockBinaryDataLength; i++) {
            if(blockBinaryData[i] != deserializedBlockBinaryData[i]) fail("Byte mismatch at position "+i+"\n"+Arrays.toString(blockBinaryData)+"\n"+Arrays.toString(deserializedBlockBinaryData));
        }
    }
}