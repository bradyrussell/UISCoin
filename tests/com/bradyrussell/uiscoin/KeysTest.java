/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.address.UISCoinWallet;
import com.bradyrussell.uiscoin.address.Wallet;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;
import com.bradyrussell.uiscoin.script.exception.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

public class KeysTest {
    @RepeatedTest(100)
    @DisplayName("Data Signing / Verification")
    void TestDataSigning() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException {
        try {
            byte[] Randomseed = new byte[64];

            ThreadLocalRandom.current().nextBytes(Randomseed);

            KeyPair keyPair = Keys.makeKeyPair(Randomseed);

            byte[] LOCKSCRIPT = new ScriptBuilder(256).push(keyPair.getPublic().getEncoded()).op(ScriptOperator.VERIFYSIG).get();

            Keys.SignedData signedData = Keys.signData(keyPair, LOCKSCRIPT);

            assertTrue(Keys.verifySignedData(signedData));

            ScriptExecution unlockingScript = new ScriptExecution();
            unlockingScript.initialize(new ScriptBuilder(256).push(signedData.Signature).get());

            while(unlockingScript.step()){
                unlockingScript.dumpStack();
            }

            ScriptExecution lockingScript = new ScriptExecution();
            lockingScript.initialize(LOCKSCRIPT, unlockingScript.Stack.elements());
            lockingScript.setSignatureVerificationMessage(LOCKSCRIPT);

            while(lockingScript.step()){
                lockingScript.dumpStack();
            }

            assertFalse(lockingScript.bScriptFailed);

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | SignatureException | InvalidKeySpecException | ScriptUnsupportedOperationException | ScriptFailedException e) {
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

            KeyPair otherKeyPair = Keys.loadKeys(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());

            assertTrue(Arrays.equals(otherKeyPair.getPublic().getEncoded(), keyPair.getPublic().getEncoded()));
            assertTrue(Arrays.equals(otherKeyPair.getPrivate().getEncoded(), keyPair.getPrivate().getEncoded()));

            UISCoinKeypair uisCoinKeypair = UISCoinKeypair.create();

            byte[] uisCoinKeypairBinaryData = uisCoinKeypair.getBinaryData();

            UISCoinKeypair newCoinKeypair = new UISCoinKeypair();
            newCoinKeypair.setBinaryData(uisCoinKeypairBinaryData);

            assertTrue(Arrays.equals(uisCoinKeypairBinaryData, newCoinKeypair.getBinaryData()));

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | SignatureException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @RepeatedTest(100)
    @DisplayName("Keys Generate Pubkey from Privkey")
    void TestKeysPubFromPriv() {
        try {
            byte[] Randomseed = new byte[64];

            ThreadLocalRandom.current().nextBytes(Randomseed);

            KeyPair keyPair = Keys.makeKeyPair(Randomseed);

            ECPublicKey publicKey = Keys.getPublicKeyFromPrivateKey((ECPrivateKey) keyPair.getPrivate());

            assertTrue(Arrays.equals(keyPair.getPublic().getEncoded(), publicKey.getEncoded()));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    @RepeatedTest(100)
    @DisplayName("Wallet Encrypted Keys Save / Load")
    void TestWalletKeysSaveLoad() {
        UISCoinKeypair uisCoinKeypair = UISCoinKeypair.create();

        String tempPass = "password"+ThreadLocalRandom.current().nextInt();

        try {
            Wallet.saveKeypairToFileWithPassword(Path.of("tests/test_wallet.uisw"),tempPass, uisCoinKeypair);
        } catch (IOException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            fail();
        }
        UISCoinKeypair keypairFromFileWithPassword = null;
        try {
            keypairFromFileWithPassword = Wallet.loadKeypairFromFileWithPassword(Path.of("tests/test_wallet.uisw"), tempPass);
        } catch (IOException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(keypairFromFileWithPassword);
        assertTrue(Arrays.equals(uisCoinKeypair.getBinaryData(), keypairFromFileWithPassword.getBinaryData()));

    }

    @RepeatedTest(100)
    @DisplayName("MultiKeyWallet Encrypted Keys Save / Load")
    void TestWalletMultiKeysSaveLoad() {
        UISCoinWallet wallet = new UISCoinWallet();

        String tempPass = "password"+ThreadLocalRandom.current().nextInt();

        for(int i = 0; i < 10; i++){
            wallet.generateNewKey();
        }

        assertTrue(wallet.Keypairs.size() > 0);

        try {
            Wallet.saveWalletToFileWithPassword(Path.of("tests/test_wallet.uiscoin"),tempPass,wallet);
        } catch (IOException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            fail();
        }
        UISCoinWallet wallet1 = null;
        try {
            wallet1 = Wallet.loadWalletFromFileWithPassword(Path.of("tests/test_wallet.uiscoin"), tempPass);
        } catch (IOException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(wallet1);
        assertTrue(Arrays.equals(wallet.getBinaryData(),wallet1.getBinaryData()));
        assertEquals(wallet1.Keypairs.size(), wallet.Keypairs.size());
    }
}