package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

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

    public static boolean Verify(int StartBlockHeight){
        List<Block> blockChain = get().getBlockChainFromHeight(StartBlockHeight);
        for(Block b:blockChain){
            if(!b.Verify()){
                b.DebugVerify();
                System.out.println("Block "+ Util.Base64Encode(b.getHash())+" at height "+b.Header.BlockHeight+" has failed verification!");
                return false;
            }
        }
        return true;
    }
}
