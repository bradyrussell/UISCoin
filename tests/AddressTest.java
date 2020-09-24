import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.CoinbaseTransaction;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class AddressTest {
    @RepeatedTest(1000)
    @DisplayName("Address Checksum")
    void TestAddressChecksum() {
        try {
            KeyPair keyPair = Keys.makeKeyPair();
            byte[] publicKey = Util.TrimByteArray(UISCoinAddress.fromPublicKey((ECPublicKey) keyPair.getPublic()));
            System.out.println(UISCoinAddress.verifyAddressChecksum(publicKey));

            System.out.println(Base64.getEncoder().encodeToString(publicKey));
            System.out.println(Util.TrimByteArray(publicKey).length);
            assertTrue(UISCoinAddress.verifyAddressChecksum(publicKey));

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }
}