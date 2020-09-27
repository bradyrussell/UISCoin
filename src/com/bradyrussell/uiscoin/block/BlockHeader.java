package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;
import com.bradyrussell.uiscoin.IVerifiable;
import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.blockchain.BlockChain;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class BlockHeader implements IBinaryData, IVerifiable {
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
        return Hash.getSHA512Bytes(getBinaryData());
    }

    @Override
    public boolean Verify() {
        boolean valid = true;
        if(BlockHeight > 0) {
            BlockHeader previousBlockHeader = BlockChain.get().getBlockHeader(HashPreviousBlock);
            valid = (BlockHeight == previousBlockHeader.BlockHeight + 1); // we are previous Block Height + 1
            valid &= (DifficultyTarget >= CalculateDifficultyTarget(Time - previousBlockHeader.Time, previousBlockHeader.DifficultyTarget)); // we are using a proper difficulty
        }
        valid &= Time <= Instant.now().getEpochSecond(); // timestamp is not in the future

        return valid;
    }

    public static int CalculateDifficultyTarget(long TimeSinceLastBlock, int LastBlockDifficulty) {
        if(TimeSinceLastBlock < MagicNumbers.TargetSecondsPerBlock.Value) return Math.min(63, LastBlockDifficulty + 1);
        if(TimeSinceLastBlock > MagicNumbers.TargetSecondsPerBlock.Value) return Math.max(1, LastBlockDifficulty - 1);
        return LastBlockDifficulty;
    }
}
