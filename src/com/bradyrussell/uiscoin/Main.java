package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;
import com.bradyrussell.uiscoin.transaction.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        UISCoinKeypair coinKeypair = UISCoinKeypair.Create();
        UISCoinKeypair coinKeypairRecipient = UISCoinKeypair.Create();
/*

        TransactionBuilder tb = new TransactionBuilder();
        Transaction transaction = tb.setVersion(1).setLockTime(-1)
                .addInput(new TransactionInput(Hash.getSHA512Bytes("Nothing"), 0, 0))
                .addOutput(new TransactionOutput(Conversions.CoinsToSatoshis(1), UISCoinAddress.fromPublicKey((ECPublicKey) coinKeypairRecipient.Keys.getPublic())))
                .signTransaction(coinKeypair).get();

        byte[] transactionBinaryData = transaction.getBinaryData();

        Util.printBytesReadable(transactionBinaryData);
        System.out.println("Transaction is " + transactionBinaryData.length + " bytes.");



*/


/*
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter desired address: ");
        String search = scanner.nextLine();


        boolean searching = true;
        while (searching) {
            UISCoinKeypair uisCoinKeypair = UISCoinKeypair.Create();
            byte[] address = UISCoinAddress.fromPublicKey((ECPublicKey) uisCoinKeypair.Keys.getPublic());

            //if(!UISCoinAddress.verifyAddressChecksum(address)) continue;;

            String string = Base64.getEncoder().encodeToString(address);
            // System.out.println(string);

            if (string.substring(4).toLowerCase().startsWith(search.toLowerCase())) {
                System.out.println(string);
                searching = false;
                try {
                    Files.write(Path.of(search + ".uisa"), uisCoinKeypair.getBinaryData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
*/
    }
}
