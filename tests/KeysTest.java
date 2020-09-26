import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.address.Wallet;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
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

            byte[] LOCKSCRIPT = new ScriptBuilder(256).push(keyPair.getPublic().getEncoded()).op(ScriptOperator.VERIFYSIG).get();

            Keys.SignedData signedData = Keys.SignData(keyPair, LOCKSCRIPT);

            assertTrue(Keys.VerifySignedData(signedData));

            ScriptExecution unlockingScript = new ScriptExecution();
            unlockingScript.Initialize(new ScriptBuilder(256).push(signedData.Signature).get());

            while(unlockingScript.Step()){
                unlockingScript.dumpStack();
            }

            ScriptExecution lockingScript = new ScriptExecution();
            lockingScript.Initialize(LOCKSCRIPT, unlockingScript.Stack.elements());

            while(lockingScript.Step()){
                lockingScript.dumpStack();
            }

            assertFalse(lockingScript.bScriptFailed);

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