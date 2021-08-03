package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.Encryption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EncryptionTest {
    @RepeatedTest(100)
    @DisplayName("Encryption")
    void TestEncryption() {
        byte[] RandomHash1 = new byte[64];
        byte[] RandomHash2 = new byte[64];

        ThreadLocalRandom.current().nextBytes(RandomHash1);
        ThreadLocalRandom.current().nextBytes(RandomHash2);

        byte[] encrypt = new byte[0];
        try {
            encrypt = Encryption.encrypt(RandomHash1, RandomHash2);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            fail();
        }

        byte[] decrypt = new byte[0];
        try {
            decrypt = Encryption.decrypt(encrypt, RandomHash2);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            fail();
        }

        assertTrue(Arrays.equals(RandomHash1, decrypt));
    }
}