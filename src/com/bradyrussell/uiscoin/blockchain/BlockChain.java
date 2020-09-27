package com.bradyrussell.uiscoin.blockchain;

import java.lang.reflect.InvocationTargetException;

public class BlockChain {
    public static BlockChainStorageBase Storage = null;

    public static <T extends BlockChainStorageBase> void Initialize(Class<T> StorageClass) {
        try {
            Storage = StorageClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static BlockChainStorageBase get(){
        if(Storage == null) throw new IllegalStateException("BlockChainStorage singleton has not been initialized!");
        return Storage;
    }
}
