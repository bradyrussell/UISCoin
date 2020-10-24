package com.bradyrussell.uiscoin.address;

import com.bradyrussell.uiscoin.Encryption;
import com.bradyrussell.uiscoin.Hash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class Wallet {
    private static final Logger Log = Logger.getLogger(Wallet.class.getName());

    public static void SaveKeypairToFileWithPassword(Path File, String Password, UISCoinKeypair Keypair) throws IOException {
        Log.info("Saving keypair to encrypted file: "+File.toString()+"...");
        Files.write(File, Encryption.Encrypt(Keypair.getBinaryData(), Hash.getSHA512Bytes(Password)));
        Log.info("Saved keypair to encrypted file: "+File.toString()+".");
    }

    public static UISCoinKeypair LoadKeypairFromFileWithPassword(Path File, String Password) throws IOException {
        Log.info("Loading keypair from encrypted file: "+File.toString()+"...");
        UISCoinKeypair coinKeypair = new UISCoinKeypair();
        coinKeypair.setBinaryData(Encryption.Decrypt(Files.readAllBytes(File), Hash.getSHA512Bytes(Password)));
        Log.info("Loaded keypair from encrypted file: "+File.toString()+".");
        return coinKeypair;
    }

    public static void SaveWalletToFileWithPassword(Path File, String Password, UISCoinWallet wallet) throws IOException {
        Log.info("Saving wallet with "+wallet.Keypairs.size()+" keypairs to encrypted file: "+File.toString()+"...");
        Files.write(File, Encryption.Encrypt(wallet.getBinaryData(), Hash.getSHA512Bytes(Password)));
        Log.info("Saved wallet with "+wallet.Keypairs.size()+" keypairs to encrypted file: "+File.toString()+".");
    }

    public static UISCoinWallet LoadWalletFromFileWithPassword(Path File, String Password) throws IOException {
        Log.info("Loading wallet from encrypted file: "+File.toString()+"...");
        UISCoinWallet wallet = new UISCoinWallet();
        wallet.setBinaryData(Encryption.Decrypt(Files.readAllBytes(File), Hash.getSHA512Bytes(Password)));
        Log.info("Loaded wallet with "+wallet.Keypairs.size()+" keypairs from encrypted file: "+File.toString()+".");
        return wallet;
    }
}
