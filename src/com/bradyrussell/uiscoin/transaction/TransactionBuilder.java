/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.transaction;

import java.security.interfaces.ECPublicKey;
import java.util.*;
import java.util.logging.Logger;

import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.blockchain.BlockchainStorage;
import com.bradyrussell.uiscoin.blockchain.BlockchainUtil;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;

public class TransactionBuilder {
    private static final Logger Log = Logger.getLogger(TransactionBuilder.class.getName());
    Transaction transaction = new Transaction();

    public TransactionBuilder setVersion(int Version) {
        transaction.Version = Version;
        return this;
    }

    public TransactionBuilder setLockTime(long LockTime) {
        transaction.TimeStamp = LockTime;
        return this;
    }

    public TransactionBuilder addInput(TransactionInput transactionInput) {
        transaction.addInput(transactionInput);
        return this;
    }

    public TransactionBuilder addOutput(TransactionOutput transactionOutput) {
        transaction.addOutput(transactionOutput);
        Log.info("Added output to transaction for " + transactionOutput.Amount + " satoshis.");
        return this;
    }

    public TransactionBuilder addChangeOutputToPublicKeyHash(byte[] PublicKeyHash, long FeeToLeave) throws NoSuchTransactionException, NoSuchBlockException {
        long Amount = (transaction.getInputTotal() - transaction.getOutputTotal()) - FeeToLeave;

        if (Amount < 0) {
            Log.warning("Insufficient inputs for this transaction! Input: " + transaction.getInputTotal() + " Output: " + transaction.getOutputTotal() + " Fee: " + FeeToLeave);
        }
        if (Amount == 0) {
            Log.info("There is no extra change for this transaction.");
            return this;
        }
        assert Amount > 0;

        transaction.addOutput(new TransactionOutputBuilder().setPayToPublicKeyHash(PublicKeyHash).setAmount(Amount).get());
        Log.info("Added change output to transaction for " + Amount + " satoshis.");
        return this;
    }

    public TransactionBuilder addInputsFromAllP2pkhUtxo(UISCoinKeypair UnlockingKeypair) {
        ArrayList<BlockchainStorage.TransactionOutputIdentifier> outputsToAddress = BlockchainUtil.matchUtxoForP2phkAddress(UISCoinAddress.decodeAddress(UISCoinAddress.fromPublicKey((ECPublicKey) UnlockingKeypair.Keys.getPublic())).HashData);

        System.out.println("Found " + outputsToAddress.size() + " unspent outputs to your address.");

        for (BlockchainStorage.TransactionOutputIdentifier toAddress : outputsToAddress) {
            try {
                TransactionOutput output = Blockchain.get().getTransactionOutput(toAddress.transactionHash, toAddress.index);
                transaction.addInput(
                        new TransactionInputBuilder()
                                .setUnlockPayToPublicKeyHash(UnlockingKeypair, output)
                                .setInputTransaction(toAddress.transactionHash, toAddress.index)
                                .get());

            } catch (NoSuchTransactionException | NoSuchBlockException e) {
                e.printStackTrace();
            }
        }

        return this;
    }

    public static class UTXO {
        BlockchainStorage.TransactionOutputIdentifier id;
        long value;
        UISCoinKeypair unlockingKeypair;

        public UTXO(byte[] hash, int index, long value, UISCoinKeypair unlockingKeypair) {
            this.id = new BlockchainStorage.TransactionOutputIdentifier(hash, index);
            this.value = value;
            this.unlockingKeypair = unlockingKeypair;
        }

        public UTXO(BlockchainStorage.TransactionOutputIdentifier id, long value, UISCoinKeypair unlockingKeypair) {
            this.id = id;
            this.value = value;
            this.unlockingKeypair = unlockingKeypair;
        }
    }

    public TransactionBuilder addInputsFromMultipleKeypairsP2pkh(List<UISCoinKeypair> keypairs, long amountIncludingFees) {
        long total = 0;
        // sort by ascending amount
        TreeMap<Long, UTXO> utxos = new TreeMap<>();

        for (UISCoinKeypair keypair : keypairs) {
            try {
                ArrayList<BlockchainStorage.TransactionOutputIdentifier> outputsToAddress = BlockchainUtil.matchUtxoForP2phkAddress(UISCoinAddress.decodeAddress(UISCoinAddress.fromPublicKey((ECPublicKey) keypair.Keys.getPublic())).HashData);
                for (BlockchainStorage.TransactionOutputIdentifier toAddress : outputsToAddress) {
                    long amount;
                    try {
                        if(Blockchain.get().isTransactionOutputSpent(toAddress.transactionHash, toAddress.index)) continue;
                        amount = Blockchain.get().getTransactionOutput(toAddress.transactionHash, toAddress.index).Amount;
                    } catch (NoSuchTransactionException e) {
                        e.printStackTrace();
                        continue;
                    }
                    total += amount;
                    utxos.put(amount, new UTXO(toAddress, amount, keypair));
                }
            } catch (Exception e) {
                e.printStackTrace();
                //continue without failing
            }
        }

        if (total < amountIncludingFees) {
            Log.warning("Insufficient funds to add inputs for " + amountIncludingFees + " sats!");
            return this;
        }

        long currentTotal = 0;

        for (Map.Entry<Long, UTXO> utxo : utxos.entrySet()) {
            try {
                TransactionOutput output = Blockchain.get().getTransactionOutput(utxo.getValue().id.transactionHash, utxo.getValue().id.index);
                currentTotal += utxo.getKey();
                transaction.addInput(
                        new TransactionInputBuilder()
                                .setUnlockPayToPublicKeyHash(utxo.getValue().unlockingKeypair, output)
                                .setInputTransaction(utxo.getValue().id.transactionHash, utxo.getValue().id.index)
                                .get());

            } catch (NoSuchTransactionException | NoSuchBlockException e) {
                e.printStackTrace();
            }

            if (currentTotal >= amountIncludingFees) {
                Log.info("Successfully added inputs from P2PKH outputs!");
                break;
            }
        }

        return this;
    }

    public TransactionBuilder addInputsFromP2pkhUtxo(UISCoinKeypair UnlockingKeypair, long AmountIncludingEstimatedFee) {
        return addInputsFromMultipleKeypairsP2pkh(Collections.singletonList(UnlockingKeypair), AmountIncludingEstimatedFee);
    }

    public Transaction get() {
        return transaction;
    }
}
