import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.address.Wallet;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.CoinbaseTransaction;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class KeysTest {
    @RepeatedTest(100)
    @DisplayName("Data Signing / Verification")
    void TestDataSigning() {
        try {
            byte[] Randomseed = new byte[64];

            ThreadLocalRandom.current().nextBytes(Randomseed);

            KeyPair keyPair = Keys.makeKeyPair(Randomseed);

            byte[] RandomHash1 = new byte[64];

            ThreadLocalRandom.current().nextBytes(RandomHash1);

            Keys.SignedData signedData = Keys.SignData(keyPair, RandomHash1);
            assertTrue(Keys.VerifySignedData(signedData));

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | SignatureException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @RepeatedTest(100)
    @DisplayName("Keys Save / Load")
    void TestKeysSaveLoad() {
        try {
            byte[] Randomseed = new byte[64];

            ThreadLocalRandom.current().nextBytes(Randomseed);

            KeyPair keyPair = Keys.makeKeyPair(Randomseed);

            KeyPair otherKeyPair = Keys.LoadKeys(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());

            assertTrue(Arrays.equals(otherKeyPair.getPublic().getEncoded(), keyPair.getPublic().getEncoded()));
            assertTrue(Arrays.equals(otherKeyPair.getPrivate().getEncoded(), keyPair.getPrivate().getEncoded()));

            UISCoinKeypair uisCoinKeypair = UISCoinKeypair.Create();

            byte[] uisCoinKeypairBinaryData = uisCoinKeypair.getBinaryData();

            UISCoinKeypair newCoinKeypair = new UISCoinKeypair();
            newCoinKeypair.setBinaryData(uisCoinKeypairBinaryData);

            assertTrue(Arrays.equals(uisCoinKeypairBinaryData, newCoinKeypair.getBinaryData()));

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | SignatureException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @RepeatedTest(100)
    @DisplayName("Wallet Encrypted Keys Save / Load")
    void TestWalletKeysSaveLoad() {
        UISCoinKeypair uisCoinKeypair = UISCoinKeypair.Create();
        Wallet.SaveKeypairToFileWithPassword(Path.of("tests/test_wallet.uisw"),"boomer", uisCoinKeypair);
        UISCoinKeypair keypairFromFileWithPassword = Wallet.LoadKeypairFromFileWithPassword(Path.of("tests/test_wallet.uisw"), "boomer");

        assertNotNull(keypairFromFileWithPassword);
        assertTrue(Arrays.equals(uisCoinKeypair.getBinaryData(), keypairFromFileWithPassword.getBinaryData()));

    }
}