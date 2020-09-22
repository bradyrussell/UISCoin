package com.bradyrussell.uiscoin;

import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static AtomicInteger threadCount = new AtomicInteger(0);

    public static boolean running = true;

    public static void main(String[] args) {

        ArrayList<String> bases = new ArrayList<>();
        while(true) {

            int ThreadNum = threadCount.getAndAdd(1);

            long StartMS = ZonedDateTime.now().toInstant().toEpochMilli();

            // write your code here
            boolean bCont = true;
            int nTried = 0;
            while (bCont) {
                if ((nTried) % 1000000 == 0 && (ZonedDateTime.now().toInstant().toEpochMilli() - StartMS) > 1000) {
                    System.out.println("Thread " + ThreadNum);
                    System.out.println("Elapsed: " + (ZonedDateTime.now().toInstant().toEpochMilli() - StartMS) + "\nTried: " + nTried);
                    System.out.println("MH/s: " + (nTried / 1000000f) / ((ZonedDateTime.now().toInstant().toEpochMilli() - StartMS) / 1000f));
                    System.out.println("\n");
                }

                String base = "" + ThreadLocalRandom.current().nextLong();

                byte[] sha512Bytes = new byte[4];

                try {
                    sha512Bytes = Hash.getSHA512Bytes(base);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                nTried++;
                if (Hash.validateHash(sha512Bytes)) {
                    bCont = false;
                    System.out.println("Base found: " + base);
                    bases.add(base);
                    System.out.println(bases);
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
