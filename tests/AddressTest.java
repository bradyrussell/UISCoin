import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressTest {
    @RepeatedTest(1000)
    @DisplayName("Address Checksum")
    void TestAddressChecksum() {
        try {
            byte[] Randomseed = new byte[64];

            ThreadLocalRandom.current().nextBytes(Randomseed);

            KeyPair keyPair = Keys.makeKeyPair(Randomseed);

            byte[] publicKey = UISCoinAddress.fromPublicKey((ECPublicKey) keyPair.getPublic());

            System.out.println(UISCoinAddress.verifyAddressChecksum(publicKey));

            System.out.println(Base64.getEncoder().encodeToString(publicKey));
            System.out.println(publicKey.length);

            assertTrue(UISCoinAddress.verifyAddressChecksum(publicKey));

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }
}