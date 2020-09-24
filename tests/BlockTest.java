import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.CoinbaseTransaction;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.fail;

public class BlockTest {
    @RepeatedTest(100)
    @DisplayName("Serialization / Deserialization")
    void TestBlockSerialization() {
        long timeStamp = Instant.now().getEpochSecond();
        Transaction testTransaction = new Transaction(0, timeStamp);

        byte[] RandomHash1 = new byte[64];
        byte[] RandomHash2 = new byte[64];
        byte[] RandomHash3 = new byte[64];

        byte[] RandomHash5 = new byte[64];
        byte[] RandomHash6 = new byte[64];
        byte[] RandomHash7 = new byte[64];

        ThreadLocalRandom.current().nextBytes(RandomHash1);
        ThreadLocalRandom.current().nextBytes(RandomHash2);
        ThreadLocalRandom.current().nextBytes(RandomHash3);

        ThreadLocalRandom.current().nextBytes(RandomHash5);

        testTransaction.addInput(new TransactionInput(RandomHash1,0,RandomHash2,0));
        testTransaction.addOutput(new TransactionOutput(Conversions.CoinsToSatoshis(1),RandomHash5));

        Block block = new Block(new BlockHeader(0xffffffff,timeStamp,2));
        block.Header.HashPreviousBlock = RandomHash3;

        block.addCoinbaseTransaction(new CoinbaseTransaction(RandomHash6,0,RandomHash7,0));
        block.addTransaction(testTransaction);

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