package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        UISCoinKeypair coinKeypair = UISCoinKeypair.Create();
        try {
            Keys.SignedData signedData = Keys.SignData(coinKeypair.Keys, Hash.getSHA512Bytes("What is the message??"));

            byte[] script = new ScriptBuilder(256).push(signedData.Pubkey).push(signedData.Signature).fromText("flip VERIFYSIG return").get();

            Util.printBytesReadable(script);
            System.out.println(script.length);

            ScriptExecution scriptExecution = new ScriptExecution();

            scriptExecution.Initialize(script);
            while(scriptExecution.Step()){
                scriptExecution.dumpStackReadable();
            }

            System.out.println("Script returned "+!scriptExecution.bScriptFailed);

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }




       /* Scanner scanner = new Scanner(System.in);
        System.out.println("Enter desired address: ");
        String search = scanner.nextLine();

        boolean searching = true;
        while(searching){
            UISCoinKeypair uisCoinKeypair = UISCoinKeypair.Create();
            byte[] address = UISCoinAddress.fromPublicKey((ECPublicKey) uisCoinKeypair.Keys.getPublic());
            String string = Base64.getEncoder().encodeToString(address);
           // System.out.println(string);

            if(string.substring(4).toLowerCase().startsWith(search.toLowerCase())) {
                System.out.println(string);
                searching = false;
                try {
                    Files.write(Path.of(search+".uisa"), uisCoinKeypair.getBinaryData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
*/
    }
}
