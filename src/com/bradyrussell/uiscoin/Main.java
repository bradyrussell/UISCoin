package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.address.Wallet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            List<String> strings = Files.readAllLines(Path.of("search.txt"));
            for(String search:strings) {
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
                        Wallet.SaveKeypairToFileWithPassword(Path.of(search + ".uisw"), "VanityAddress1", uisCoinKeypair);
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
