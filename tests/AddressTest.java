import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.Util;
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

            byte[] address = UISCoinAddress.fromPublicKey((ECPublicKey) keyPair.getPublic());

            System.out.println(UISCoinAddress.verifyAddressChecksum(address));

            System.out.println(Base64.getEncoder().encodeToString(address));
            System.out.println(address.length);

            UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(address);
            System.out.println("Address Type: "+decodedAddress.Type);
            System.out.print("Address PubKeyHash: ");
            Util.printBytesReadable(decodedAddress.HashData);
            System.out.print("Address Checksum: ");
            Util.printBytesReadable(decodedAddress.Checksum);

            assertTrue(UISCoinAddress.verifyAddressChecksum(address));

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    @RepeatedTest(1000)
    @DisplayName("Script Hash Address Checksum")
    void TestSHAddressChecksum() {
        byte[] Randomseed = new byte[64];

        ThreadLocalRandom.current().nextBytes(Randomseed);

        byte[] address = UISCoinAddress.fromScriptHash(Randomseed);

        System.out.println(UISCoinAddress.verifyAddressChecksum(address));

        System.out.println(Base64.getEncoder().encodeToString(address));
        System.out.println(address.length);

        UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(address);
        System.out.println("Address Type: "+decodedAddress.Type);
        System.out.print("Address HashData: ");
        Util.printBytesReadable(decodedAddress.HashData);
        System.out.print("Address Checksum: ");
        Util.printBytesReadable(decodedAddress.Checksum);

        assertTrue(UISCoinAddress.verifyAddressChecksum(address));

    }
}