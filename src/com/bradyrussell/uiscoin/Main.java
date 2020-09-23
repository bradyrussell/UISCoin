package com.bradyrussell.uiscoin;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.InvalidParameterSpecException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static AtomicInteger threadCount = new AtomicInteger(0);

    public static boolean running = true;

    public static void main(String[] args) {
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
                if (Hash.validateHash(sha512Bytes, 4)) {
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


        }

    }
}
