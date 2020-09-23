package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.CoinbaseTransaction;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static AtomicInteger threadCount = new AtomicInteger(0);

    public static boolean running = true;

    public static void main(String[] args) {

        long timeStamp = Instant.now().getEpochSecond();
        Transaction testTransaction = new Transaction(0, timeStamp);

        testTransaction.addInput(new TransactionInput(Hash.getSHA512Bytes("hello world"),0,Hash.getSHA512Bytes("hello world"),0));
        testTransaction.addOutput(new TransactionOutput(Conversions.CoinsToSatoshis(1),Hash.getSHA512Bytes("hello world")));

        Block block = new Block(new BlockHeader(0xffffffff,timeStamp,2));
        block.Header.HashPreviousBlock = Hash.getSHA512Bytes("hello world");
        block.Header.HashMerkleRoot = Hash.getSHA512Bytes("hello world");

        block.addCoinbaseTransaction(new CoinbaseTransaction(Hash.getSHA512Bytes("hello world"),0,Hash.getSHA512Bytes("hello world"),0));
        block.addTransaction(testTransaction);

        System.out.println(block.getSize());
        System.out.println(Hash.getSHA512String(block.getBinaryData()));
        System.out.println(Arrays.toString(block.getBinaryData()));

        int n = 0;

    while(!Hash.validateHash(block.getHash(),2)) {
        block.Header.Nonce = n++;
    }

        System.out.println("Found block: "+Hash.getSHA512String(block.getBinaryData()));
        System.out.println("Nonce: "+block.Header.Nonce);

        System.out.println("Hash Bytes: "+Arrays.toString(block.getHash()));

        System.out.println("Block Size: "+block.getSize());
        System.out.println();

        System.out.println("Block Data: "+Arrays.toString(block.getBinaryData()));

        byte[] blockBinaryData = block.getBinaryData();

        Block deserializedBlock = new Block();
        deserializedBlock.setBinaryData(blockBinaryData);
        System.out.println("Nonce: "+deserializedBlock.Header.Nonce);

        System.out.println("Hash Bytes: "+Arrays.toString(deserializedBlock.getHash()));

        System.out.println("Block Size: "+deserializedBlock.getSize());
        System.out.println();

        System.out.println("Block Data: "+Arrays.toString(deserializedBlock.getBinaryData()));

        /*try {
            KeyPair keyPair = Keys.makeKeyPair();

            System.out.println(keyPair.getPrivate().getEncoded().length);

            ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

            String privKeyString = String.format("%064x", privateKey.getS());

            System.out.println("Your private key is: ");
            System.out.println(privKeyString);
            System.out.println(Base64.getEncoder().encodeToString(privateKey.getS().toByteArray()));


            ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

            ECPoint publicW = publicKey.getW();
            String pubKeyXString = String.format("%064x", publicW.getAffineX());
            String pubKeyYString = String.format("%064x", publicW.getAffineY());

            String publicKeyString = "fa"+pubKeyXString+pubKeyYString;
            System.out.println(publicKeyString);

            System.out.println("Your public key is: ");
            System.out.println( Base64.getEncoder().encodeToString(Hash.getSHA512Bytes(publicKeyString)));




        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
*/

/*
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter origin: ");

        long NonceOrigin = scanner.nextLong();
        long NonceLimit = Long.MAX_VALUE;
        long NonceIndex = NonceOrigin;

        ArrayList<String> bases = new ArrayList<>();
        while(true) {

            int ThreadNum = threadCount.getAndAdd(1);

            long StartMS = ZonedDateTime.now().toInstant().toEpochMilli();

            // write your code here
            boolean bCont = true;
            int nTried = 0;

            while (bCont) {
                long time = ZonedDateTime.now().toInstant().toEpochMilli();
                if ((nTried) % 100000000 == 0 && (time - StartMS) > 1000) {
                    System.out.println("Thread " + ThreadNum);
                    System.out.println("Elapsed: " + (time - StartMS) + "\nTried: " + nTried);
                    System.out.println("Index: " + NonceIndex );
                    System.out.println("MH/s: " + (nTried / 1000000f) / ((time - StartMS) / 1000f));
                    System.out.println(bases);
                    System.out.println("\n");
                }

                String base = "" + NonceIndex++;

                byte[] sha512Bytes = new byte[4];

                try {
                    sha512Bytes = Hash.getSHA512Bytes(base);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                nTried++;
                if (Hash.validateHash(sha512Bytes, 3)) {
                    bCont = false;
                    System.out.println("Base found: " + base);
                    bases.add(base);

                    try {
                        System.out.println("Produces hash: " + Hash.getSHA512String(base));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    running = false;
                }
            }

            long StopMS = ZonedDateTime.now().toInstant().toEpochMilli();

            System.out.println("Took " + (StopMS - StartMS) + " ms.");


        }*/

    }
}
