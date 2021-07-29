package com.bradyrussell.uiscoin.address;

import com.bradyrussell.uiscoin.Encryption;
import com.bradyrussell.uiscoin.Hash;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class Wallet {
    private static final Logger Log = Logger.getLogger(Wallet.class.getName());

    public static void saveKeypairToFileWithPassword(Path File, String Password, UISCoinKeypair Keypair) throws IOException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        Log.info("Saving keypair to encrypted file: "+File.toString()+"...");
        Files.write(File, Encryption.encrypt(Keypair.getBinaryData(), Hash.getSHA512Bytes(Password)));
        Log.info("Saved keypair to encrypted file: "+File.toString()+".");
    }

    public static UISCoinKeypair loadKeypairFromFileWithPassword(Path File, String Password) throws IOException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        Log.info("Loading keypair from encrypted file: "+File.toString()+"...");
        UISCoinKeypair coinKeypair = new UISCoinKeypair();
        coinKeypair.setBinaryData(Encryption.decrypt(Files.readAllBytes(File), Hash.getSHA512Bytes(Password)));
        Log.info("Loaded keypair from encrypted file: "+File.toString()+".");
        return coinKeypair;
    }

    public static void saveWalletToFileWithPassword(Path File, String Password, UISCoinWallet wallet) throws IOException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        Log.info("Saving wallet with "+wallet.Keypairs.size()+" keypairs to encrypted file: "+File.toString()+"...");
        Files.write(File, Encryption.encrypt(wallet.getBinaryData(), Hash.getSHA512Bytes(Password)));
        Log.info("Saved wallet with "+wallet.Keypairs.size()+" keypairs to encrypted file: "+File.toString()+".");
    }

    public static UISCoinWallet loadWalletFromFileWithPassword(Path File, String Password) throws IOException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        Log.info("Loading wallet from encrypted file: "+File.toString()+"...");
        UISCoinWallet wallet = new UISCoinWallet();
        wallet.setBinaryData(Encryption.decrypt(Files.readAllBytes(File), Hash.getSHA512Bytes(Password)));
        Log.info("Loaded wallet with "+wallet.Keypairs.size()+" keypairs from encrypted file: "+File.toString()+".");
        return wallet;
    }
}
