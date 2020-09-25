import com.bradyrussell.uiscoin.Encryption;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncryptionTest {
    @RepeatedTest(100)
    @DisplayName("Encryption")
    void TestEncryption() {
        byte[] RandomHash1 = new byte[64];
        byte[] RandomHash2 = new byte[64];

        ThreadLocalRandom.current().nextBytes(RandomHash1);
        ThreadLocalRandom.current().nextBytes(RandomHash2);

        byte[] encrypt = Encryption.Encrypt(RandomHash1, RandomHash2);

        byte[] decrypt = Encryption.Decrypt(encrypt, RandomHash2);

        assertTrue(Arrays.equals(RandomHash1, decrypt));
    }
}