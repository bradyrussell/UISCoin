import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.node.Node;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.transaction.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class NodeTest {
    @RepeatedTest(100)
    @DisplayName("Node Peer Communication")
    void TestNode() throws UnknownHostException {


        Node node= new Node(1);

        node.AddPeer(InetAddress.getLocalHost());

        byte[] RandomHash1 = new byte[64];
        byte[] RandomHash2 = new byte[64];

        ThreadLocalRandom.current().nextBytes(RandomHash1);

        node.SendAll(RandomHash1);

        byte[] buffer = new byte[64];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        if(node.Receive(packet)) {
            RandomHash2 = packet.getData();
        }

        node.Stop();

        assertTrue(Arrays.equals(RandomHash1,RandomHash2));
    }


    @RepeatedTest(100)
    @DisplayName("Node Packets Communication")
    void TestNodePackets() throws UnknownHostException {
        long timeStamp = Instant.now().getEpochSecond();

        UISCoinKeypair uisCoinKeypair = UISCoinKeypair.Create();

        byte[] RandomHash1 = new byte[64];
        byte[] RandomHash2 = new byte[64];
        byte[] RandomHash3 = new byte[64];
        byte[] RandomHash5 = new byte[64];
        byte[] RandomHash6 = new byte[64];

        ThreadLocalRandom.current().nextBytes(RandomHash1);
        ThreadLocalRandom.current().nextBytes(RandomHash2);
        ThreadLocalRandom.current().nextBytes(RandomHash3);
        ThreadLocalRandom.current().nextBytes(RandomHash5);
        ThreadLocalRandom.current().nextBytes(RandomHash6);

        TransactionBuilder tb = new TransactionBuilder();
        Transaction transaction = tb.setVersion(1).setLockTime(-1)
                .addInput(new TransactionInput(RandomHash1, 0))
                .addOutput(new TransactionOutput(Conversions.CoinsToSatoshis(.5), RandomHash2))
                .signTransaction(uisCoinKeypair).get();

        Block block = new Block(new BlockHeader(1,timeStamp,1, 0));

        block.addCoinbaseTransaction(new Transaction(1,1).addOutput(new TransactionOutput(Conversions.CoinsToSatoshis(1), RandomHash6)));
        block.addTransaction(transaction);

        block.Header.HashPreviousBlock = RandomHash2;
        block.Header.HashMerkleRoot = block.CalculateMerkleRoot();


        Node node= new Node(1);

        node.AddPeer(InetAddress.getLocalHost());

        byte[] dataToSend = new PeerPacketBuilder(2048).putBlock(block).get();
        node.SendAll(dataToSend);

        byte[] buffer = new byte[64];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        if(node.Receive(packet)) {
            byte[] packetData = packet.getData();
            assertEquals(packetData[0], PeerPacketType.BLOCK.Header);

            Util.printBytesReadable(dataToSend);
            Util.printBytesReadable(packetData);
            assertTrue(Arrays.equals(dataToSend, packetData));

            ByteBuffer byteBuffer = ByteBuffer.wrap(packetData);

            byte Header = byteBuffer.get();
            int BlockLen = byteBuffer.getInt();

            byte[] BlockData = new byte[BlockLen];
            byteBuffer.get(BlockData);

            assertTrue(Arrays.equals(BlockData, block.getBinaryData()));
        } else {
            System.out.println("Nothing to receive!");
        }

        node.Stop();

       // assertTrue(Arrays.equals(RandomHash1,RandomHash2));
    }
}