package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;

import java.nio.ByteBuffer;

public class BlockHeader implements IBinaryData {
    public int Version; // 4
    public byte[] HashPreviousBlock; // 64
    public byte[] HashMerkleRoot; // concat and hash all transactions hashes // 64
    public long Time; // 8
    public int DifficultyTarget; //https://en.bitcoinwiki.org/wiki/Difficulty_in_Mining#:~:text=Difficulty%20is%20a%20value%20used,a%20lower%20limit%20for%20shares. // 4
    public int Nonce; // 4

    public BlockHeader() {
    }

    public BlockHeader(int version, long time, int difficultyTarget) {
        Version = version;
        Time = time;
        DifficultyTarget = difficultyTarget;
        Nonce = 0;
    }

    public BlockHeader(int version, long time, int difficultyTarget, byte[] hashPreviousBlock, byte[] hashMerkleRoot) {
        Version = version;
        HashPreviousBlock = hashPreviousBlock;
        HashMerkleRoot = hashMerkleRoot;
        Time = time;
        DifficultyTarget = difficultyTarget;
        Nonce = 0;
    }

    public BlockHeader(int version, long time, int difficultyTarget, int nonce) {
        Version = version;
        Time = time;
        DifficultyTarget = difficultyTarget;
        Nonce = nonce;
    }

    public BlockHeader(int version, long time, int difficultyTarget, int nonce, byte[] hashPreviousBlock, byte[] hashMerkleRoot) {
        Version = version;
        HashPreviousBlock = hashPreviousBlock;
        HashMerkleRoot = hashMerkleRoot;
        Time = time;
        DifficultyTarget = difficultyTarget;
        Nonce = nonce;
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
        return buffer.array();
    }

    @Override
    public void setBinaryData(byte[] Data) {
        HashMerkleRoot = new byte[64];
        HashPreviousBlock = new byte[64];

        ByteBuffer buffer = ByteBuffer.wrap(Data);
        Version = buffer.getInt();
        buffer.get(HashPreviousBlock, 0, 64);
        buffer.get(HashMerkleRoot, 0, 64);
        Time = buffer.getLong();
        DifficultyTarget = buffer.getInt();
        Nonce = buffer.getInt();
    }

    @Override
    public int getSize() {
        return 4+64+64+8+4+4;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
