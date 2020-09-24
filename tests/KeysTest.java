import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.CoinbaseTransaction;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.fail;

public class KeysTest {
    @RepeatedTest(100)
    @DisplayName("KeyGeneration")
    void TestKeyGeneration() {
        try {
            KeyPair keyPair = Keys.makeKeyPair();

            ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
            ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

            String PrivateKeyString = String.format("%064x", privateKey.getS());
            byte[] PrivateKeyBytes = privateKey.getS().toByteArray();

            System.out.println("PrivateKey: "+PrivateKeyString);
            System.out.println("PrivateKey: "+ Arrays.toString(PrivateKeyBytes));
            System.out.println(PrivateKeyBytes.length);

            String PublicKeyString = ""+ MagicBytes.AddressHeader.Value + String.format("%064x", publicKey.getW().getAffineX()) + String.format("%064x", publicKey.getW().getAffineY());

            ByteBuffer PublicKeyBuffer = ByteBuffer.allocate(128);
            PublicKeyBuffer.put(publicKey.getW().getAffineX().toByteArray());
            PublicKeyBuffer.put(publicKey.getW().getAffineY().toByteArray());

            byte[] PublicKeyBytes = PublicKeyBuffer.array();

            System.out.println("PublicKey: "+PublicKeyString);
            System.out.println("PublicKey: "+ Arrays.toString(PublicKeyBytes));
            System.out.println(PublicKeyBytes.length);

            System.out.println(Hash.getSHA512String(PublicKeyBytes));

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }
}