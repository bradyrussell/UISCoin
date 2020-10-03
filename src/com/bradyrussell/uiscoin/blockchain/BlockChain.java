package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class BlockChain {
    private static final Logger Log = Logger.getLogger(BlockChain.class.getName());

    public static BlockChainStorageBase Storage = null;

    public static <T extends BlockChainStorageBase> void Initialize(Class<T> StorageClass) {
        try {
            Storage = StorageClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static BlockChainStorageBase get() {
        if (Storage == null) throw new IllegalStateException("BlockChainStorage singleton has not been initialized!");
        return Storage;
    }

    public static boolean Verify(int StartBlockHeight) {
        List<Block> blockChain = get().getBlockChainFromHeight(StartBlockHeight);
        for (Block b : blockChain) {
            if (!b.Verify()) {
                b.DebugVerify();
                System.out.println("Block " + Util.Base64Encode(b.Header.getHash()) + " at height " + b.Header.BlockHeight + " has failed verification!");
                return false;
            }
        }
        return true;
    }

    public static void BuildUTXOSet(int StartBlockHeight) {
        List<Block> blockChain = get().getBlockChainFromHeight(StartBlockHeight);

        ArrayList<byte[]> TransactionOutputs = new ArrayList<>();

        for (Block b : blockChain) {
            for (Transaction transaction : b.Transactions) {
                for (TransactionInput input : transaction.Inputs) {
                    final byte[] concatArray = Util.ConcatArray(input.InputHash, Util.NumberToByteArray(input.IndexNumber));
                    TransactionOutputs.removeIf(bytes -> Arrays.equals(bytes,concatArray)); // this has been spent, remove it
                }
                for (int i = 0; i < transaction.Outputs.size(); i++) {
                    TransactionOutputs.add(Util.ConcatArray(transaction.getHash(), Util.NumberToByteArray(i)));
                }
            }
        }

        for (byte[] transactionOutput : TransactionOutputs) {
            byte[] TsxnHash = new byte[64];
            byte[] Index = new byte[4];

            System.arraycopy(transactionOutput, 0, TsxnHash, 0, 64);
            System.arraycopy(transactionOutput, 64, Index, 0, 4);

            //System.out.println("Saving UTXO " + Util.Base64Encode(transactionOutput));
            Storage.putUnspentTransactionOutput(TsxnHash, Util.ByteArrayToNumber(Index), Storage.getTransactionOutput(TsxnHash, Util.ByteArrayToNumber(Index)));
        }
    }
}
