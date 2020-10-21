package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;
import com.bradyrussell.uiscoin.IVerifiable;
import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Logger;

public class BlockHeader implements IBinaryData, IVerifiable {
    private static final Logger Log = Logger.getLogger(BlockHeader.class.getName());

    public int Version; // 4
    public byte[] HashPreviousBlock; // 64
    public byte[] HashMerkleRoot; // concat and hash all transactions hashes // 64
    public long Time; // 8
    public int DifficultyTarget; //https://en.bitcoinwiki.org/wiki/Difficulty_in_Mining#:~:text=Difficulty%20is%20a%20value%20used,a%20lower%20limit%20for%20shares. // 4
    public int Nonce; // 4
    public int BlockHeight; // 4

    public BlockHeader() {
    }

    public BlockHeader(int version, long time, int difficultyTarget, int blockHeight) {
        Version = version;
        Time = time;
        DifficultyTarget = difficultyTarget;
        Nonce = 0;
        BlockHeight = blockHeight;
    }

    public BlockHeader(int version, long time, int difficultyTarget, byte[] hashPreviousBlock, byte[] hashMerkleRoot) {
        Version = version;
        HashPreviousBlock = hashPreviousBlock;
        HashMerkleRoot = hashMerkleRoot;
        Time = time;
        DifficultyTarget = difficultyTarget;
        Nonce = 0;
    }

    public BlockHeader(int version, long time, int difficultyTarget, int nonce, int blockHeight) {
        Version = version;
        Time = time;
        DifficultyTarget = difficultyTarget;
        Nonce = nonce;
        BlockHeight = blockHeight;
    }

    public BlockHeader(int version, long time, int difficultyTarget, int nonce, int blockHeight, byte[] hashPreviousBlock, byte[] hashMerkleRoot) {
        Version = version;
        HashPreviousBlock = hashPreviousBlock;
        HashMerkleRoot = hashMerkleRoot;
        Time = time;
        DifficultyTarget = difficultyTarget;
        Nonce = nonce;
        BlockHeight = blockHeight;
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.putInt(Version);
        buffer.put(HashPreviousBlock);
        buffer.put(HashMerkleRoot);
        buffer.putLong(Time);
        buffer.putInt(DifficultyTarget);
        buffer.putInt(Nonce);
        buffer.putInt(BlockHeight);
        return buffer.array();
    }

    @Override
    public int setBinaryData(byte[] Data) {
        HashMerkleRoot = new byte[64];
        HashPreviousBlock = new byte[64];

        ByteBuffer buffer = ByteBuffer.wrap(Data);
        Version = buffer.getInt();
        buffer.get(HashPreviousBlock, 0, 64);
        buffer.get(HashMerkleRoot, 0, 64);
        Time = buffer.getLong();
        DifficultyTarget = buffer.getInt();
        Nonce = buffer.getInt();
        BlockHeight = buffer.getInt();
        return buffer.position();
    }

    @Override
    public int getSize() {
        return 4+64+64+8+4+4+4;
    }

    @Override
    public byte[] getHash() {
        if(HashMerkleRoot == null || Arrays.equals(HashMerkleRoot, new byte[64])) {
            Log.severe("Calculated a block header hash with an empty Merkle Root!");
            return null;
        }
        return Hash.getSHA512Bytes(getBinaryData());
    }

    @Override
    public boolean Verify() {
        if(BlockHeight > 0) {
            BlockHeader previousBlockHeader = null;
            try {
                previousBlockHeader = BlockChain.get().getBlockHeader(HashPreviousBlock);
            } catch (NoSuchBlockException e) {
                e.printStackTrace();
                return false;
            }

            if(BlockHeight != previousBlockHeader.BlockHeight + 1) {
                assert (BlockHeight == previousBlockHeader.BlockHeight + 1);
                return false; // we are previous Block Height + 1
            }
            if(DifficultyTarget < CalculateDifficultyTarget(Time - previousBlockHeader.Time, previousBlockHeader.DifficultyTarget)){
                assert (DifficultyTarget >= CalculateDifficultyTarget(Time - previousBlockHeader.Time, previousBlockHeader.DifficultyTarget));
                return false;
            }

        }

        boolean timeValid = (Time - 30) <= Instant.now().getEpochSecond();

        if(!timeValid){
            Log.severe("Error: Block time is in the future! Please check the system time is correct.");
            assert timeValid;
            return false;
        }

        return true;
    }

    public static int CalculateDifficultyTarget(long TimeSinceLastBlock, int LastBlockDifficulty) {
        if(TimeSinceLastBlock < MagicNumbers.TargetSecondsPerBlock.Value) return Math.min(63, LastBlockDifficulty + 1);
        if(TimeSinceLastBlock > MagicNumbers.TargetSecondsPerBlock.Value) return Math.max(3, LastBlockDifficulty - 1);
        return LastBlockDifficulty;
    }
}
