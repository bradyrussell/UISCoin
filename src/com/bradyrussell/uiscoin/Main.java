package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Scanner;

public class Main {



    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
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

    }
}
