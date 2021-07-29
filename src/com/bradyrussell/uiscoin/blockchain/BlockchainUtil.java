package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.script.ScriptMatcher;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;

import java.util.ArrayList;
import java.util.Arrays;

public class BlockchainUtil {
    public static ArrayList<BlockchainStorage.TransactionOutputIdentifier> matchUtxoForP2phkAddress(byte[] PublicKeyHash) {
        ArrayList<BlockchainStorage.TransactionOutputIdentifier> utxo = new ArrayList<>();

        ScriptMatcher matcherP2PKH = ScriptMatcher.getMatcherP2PKH();

        for (BlockchainStorage.TransactionOutputIdentifier unspentTransactionOutput : Blockchain.get().getUnspentTransactionOutputs()) {
            try {
                TransactionOutput output = Blockchain.get().getTransactionOutput(unspentTransactionOutput.transactionHash, unspentTransactionOutput.index);
                if(matcherP2PKH.match(output.LockingScript) && Arrays.equals(matcherP2PKH.getPushData(0),PublicKeyHash)) {
                    utxo.add(unspentTransactionOutput);
                }
            } catch (NoSuchTransactionException | NoSuchBlockException e) {
                e.printStackTrace();
            }
        }

        return utxo;
    }


    public static long getBalanceForP2phkAddress(byte[] PublicKeyHash) {
        long total = 0;

        ScriptMatcher matcherP2PKH = ScriptMatcher.getMatcherP2PKH();

        for (BlockchainStorage.TransactionOutputIdentifier unspentTransactionOutput : Blockchain.get().getUnspentTransactionOutputs()) {
            try {
                TransactionOutput output = Blockchain.get().getTransactionOutput(unspentTransactionOutput.transactionHash, unspentTransactionOutput.index);
                if(matcherP2PKH.match(output.LockingScript) && Arrays.equals(matcherP2PKH.getPushData(0),PublicKeyHash)) {
                    total += output.Amount;
                }
            } catch (NoSuchTransactionException | NoSuchBlockException e) {
                e.printStackTrace();
            }
        }

        return total;
    }
}
