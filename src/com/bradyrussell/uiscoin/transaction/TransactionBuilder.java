package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;

import java.security.interfaces.ECPublicKey;
import java.util.*;
import java.util.logging.Logger;

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
        ArrayList<byte[]> outputsToAddress = BlockChain.get().matchUtxoForP2phkAddress(UISCoinAddress.decodeAddress(UISCoinAddress.fromPublicKey((ECPublicKey) UnlockingKeypair.Keys.getPublic())).HashData);

        System.out.println("Found " + outputsToAddress.size() + " unspent outputs to your address.");

        for (byte[] toAddress : outputsToAddress) {
            try {
                byte[] TsxnHash = new byte[64];
                byte[] IndexBytes = new byte[4];

                System.arraycopy(toAddress, 0, TsxnHash, 0, 64);
                System.arraycopy(toAddress, 64, IndexBytes, 0, 4);

                TransactionOutput output = BlockChain.get().getTransactionOutput(TsxnHash, BytesUtil.byteArrayToNumber32(IndexBytes));
                transaction.addInput(
                        new TransactionInputBuilder()
                                .setUnlockPayToPublicKeyHash(UnlockingKeypair, output)
                                .setInputTransaction(TsxnHash, BytesUtil.byteArrayToNumber32(IndexBytes))
                                .get());

            } catch (NoSuchTransactionException | NoSuchBlockException e) {
                e.printStackTrace();
            }
        }

        return this;
    }

    public static class UTXO {
        byte[] hash;
        int index;
        long value;
        UISCoinKeypair unlockingKeypair;

        public UTXO(byte[] hash, int index, long value, UISCoinKeypair unlockingKeypair) {
            this.hash = hash;
            this.index = index;
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
                ArrayList<byte[]> outputsToAddress = BlockChain.get().matchUtxoForP2phkAddress(UISCoinAddress.decodeAddress(UISCoinAddress.fromPublicKey((ECPublicKey) keypair.Keys.getPublic())).HashData);
                for (byte[] toAddress : outputsToAddress) {
                    byte[] TsxnHash = new byte[64];
                    byte[] IndexBytes = new byte[4];

                    System.arraycopy(toAddress, 0, TsxnHash, 0, 64);
                    System.arraycopy(toAddress, 64, IndexBytes, 0, 4);

                    long amount;
                    try {
                        amount = BlockChain.get().getUnspentTransactionOutput(TsxnHash, BytesUtil.byteArrayToNumber32(IndexBytes)).Amount;
                    } catch (NoSuchTransactionException e) {
                        e.printStackTrace();
                        continue;
                    }
                    total += amount;
                    utxos.put(amount, new UTXO(TsxnHash, BytesUtil.byteArrayToNumber32(IndexBytes), amount, keypair));
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
                TransactionOutput output = BlockChain.get().getTransactionOutput(utxo.getValue().hash, utxo.getValue().index);
                currentTotal += utxo.getKey();
                transaction.addInput(
                        new TransactionInputBuilder()
                                .setUnlockPayToPublicKeyHash(utxo.getValue().unlockingKeypair, output)
                                .setInputTransaction(utxo.getValue().hash, utxo.getValue().index)
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
        ArrayList<byte[]> outputsToAddress = BlockChain.get().matchUtxoForP2phkAddress(UISCoinAddress.decodeAddress(UISCoinAddress.fromPublicKey((ECPublicKey) UnlockingKeypair.Keys.getPublic())).HashData);

        System.out.println("Found " + outputsToAddress.size() + " unspent outputs to your address.");

        // todo could this be optimized with subset nearest to sum?

        long totalBalance = 0;

        // sort by ascending amount
        TreeMap<Long, byte[]> utxosByAmount = new TreeMap<>();
        for (byte[] toAddress : outputsToAddress) {
            byte[] TsxnHash = new byte[64];
            byte[] IndexBytes = new byte[4];

            System.arraycopy(toAddress, 0, TsxnHash, 0, 64);
            System.arraycopy(toAddress, 64, IndexBytes, 0, 4);

            long amount;
            try {
                amount = BlockChain.get().getUnspentTransactionOutput(TsxnHash, BytesUtil.byteArrayToNumber32(IndexBytes)).Amount;
            } catch (NoSuchTransactionException e) {
                e.printStackTrace();
                continue;
            }
            totalBalance += amount;
            utxosByAmount.put(amount, toAddress);
        }

        if (totalBalance < AmountIncludingEstimatedFee) {
            Log.warning("Insufficient funds to add inputs for " + AmountIncludingEstimatedFee + " sats!");
            return this;
        }

        long currentTotal = 0;

        for (Map.Entry<Long, byte[]> utxo : utxosByAmount.entrySet()) {
            try {
                byte[] TsxnHash = new byte[64];
                byte[] IndexBytes = new byte[4];

                System.arraycopy(utxo.getValue(), 0, TsxnHash, 0, 64);
                System.arraycopy(utxo.getValue(), 64, IndexBytes, 0, 4);

                TransactionOutput output = BlockChain.get().getTransactionOutput(TsxnHash, BytesUtil.byteArrayToNumber32(IndexBytes));
                currentTotal += utxo.getKey();
                transaction.addInput(
                        new TransactionInputBuilder()
                                .setUnlockPayToPublicKeyHash(UnlockingKeypair, output)
                                .setInputTransaction(TsxnHash, BytesUtil.byteArrayToNumber32(IndexBytes))
                                .get());

            } catch (NoSuchTransactionException | NoSuchBlockException e) {
                e.printStackTrace();
            }

            if (currentTotal >= AmountIncludingEstimatedFee) {
                Log.info("Successfully added inputs from P2PKH outputs!");
                break;
            }
        }

        return this;
    }

    public Transaction get() {
        return transaction;
    }
}
