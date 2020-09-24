package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static AtomicInteger threadCount = new AtomicInteger(0);

    public static boolean running = true;

    public static void main(String[] args) {

        ScriptBuilder sb1 = new ScriptBuilder(256);
        //sb1.fromText("PUSH 256 PUSH 123 PUSH 321 ADD ADD SHA512 PUSH 700 SHA512EQUAL VERIFY");
        sb1.fromText("push 1 push 2 push 3 push 4 flip shiftup return");
        System.out.println(Arrays.toString(sb1.get()));

        /*ScriptBuilder sb = new ScriptBuilder(256);
        sb
                .pushASCIIString("Hello, ")
                .pushASCIIString("world!")
                .op(ScriptOperator.APPEND)
                .op(ScriptOperator.SHA512)
                .pushASCIIString("Hello, world!")
                .op(ScriptOperator.SHA512EQUAL)
                .op(ScriptOperator.VERIFY);

        ;
        System.out.println(Arrays.toString(sb.get()));*/

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb1.get());

        while (scriptExecution.Step()){
            scriptExecution.dumpStack();
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

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
