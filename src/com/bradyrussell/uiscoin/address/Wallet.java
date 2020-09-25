package com.bradyrussell.uiscoin.address;

import com.bradyrussell.uiscoin.Encryption;
import com.bradyrussell.uiscoin.Hash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Wallet {
    public static void SaveKeypairToFileWithPassword(Path File, String Password, UISCoinKeypair Keypair){
        try {
            Files.write(File, Encryption.Encrypt(Keypair.getBinaryData(), Hash.getSHA512Bytes(Password)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static UISCoinKeypair LoadKeypairFromFileWithPassword(Path File, String Password){
        try {
            UISCoinKeypair coinKeypair = new UISCoinKeypair();
            coinKeypair.setBinaryData(Encryption.Decrypt(Files.readAllBytes(File), Hash.getSHA512Bytes(Password)));
            return coinKeypair;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
