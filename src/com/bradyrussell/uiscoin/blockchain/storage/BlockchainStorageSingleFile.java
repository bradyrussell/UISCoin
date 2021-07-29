package com.bradyrussell.uiscoin.blockchain.storage;

import com.bradyrussell.uiscoin.block.Block;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

public class BlockchainStorageSingleFile extends BlockchainStorageEphemeral {
    private final String databasePath;

    public BlockchainStorageSingleFile(String databasePath) {
        this.databasePath = databasePath;
    }

    @Override
    public boolean open() {
        if(Files.exists(Path.of(databasePath))) {
            try(FileInputStream fileInputStream = new FileInputStream(databasePath); ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                blocksByHeight.addAll((ArrayList<Block>) objectInputStream.readObject());
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                e.printStackTrace();
            }
        }

        for (Block block : blocksByHeight) {
            putBlock(block);
        }

        return super.open();
    }

    @Override
    public boolean close() {
        try(FileOutputStream fileOutputStream = new FileOutputStream(databasePath); ObjectOutputStream objectInputStream = new ObjectOutputStream(fileOutputStream)) {
            objectInputStream.writeObject(blocksByHeight);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.close();
    }
}
