package com.bradyrussell.uiscoin.blockchain.storage;

import com.bradyrussell.uiscoin.block.Block;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

public class BlockchainStorageSingleFile extends BlockchainStorageEphemeral {
    private final Path databasePath;

    public BlockchainStorageSingleFile(Path databasePath) {
        this.databasePath = databasePath;
    }

    @Override
    public boolean open() {
        if(Files.exists(databasePath)) {
            try {
                byte[] bytes = Files.readAllBytes(databasePath);

                ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
                int count = byteBuf.readInt();
                for (int i = 0; i < count; i++) {
                    int size = byteBuf.readInt();
                    byte[] data = new byte[size];
                    byteBuf.readBytes(data);
                    Block block = new Block();
                    block.setBinaryData(data);
                    putBlock(block);
                }

                System.out.println("Loaded blockchain at height "+getBlockHeight());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return super.open();
    }

    @Override
    public boolean close() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(blocksByHeight.size());

        for (Block block : blocksByHeight) {
            byte[] data = block.getBinaryData();
            buffer.writeInt(data.length);
            buffer.writeBytes(data);
        }

        try {
            Files.write(databasePath, buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return super.close();
    }
}
