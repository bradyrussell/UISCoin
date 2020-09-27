package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlockChainStorageFile extends BlockChainStorageBase {
    @Override
    public byte[] get(byte[] Key, String Database) {
        if(!Files.exists(Path.of("blockchain/"+Database+"/"+ Util.Base64Encode(Key)))) return null;
        try {
            return Files.readAllBytes(Path.of("blockchain/"+Database+"/"+Util.Base64Encode(Key)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void put(byte[] Key, byte[] Value, String Database) {
        try {
            MakeDir("blockchain/");
            MakeDir("blockchain/"+Database+"/");
            Files.write(Path.of("blockchain/"+Database+"/"+Util.Base64Encode(Key)),Value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(byte[] Key, String Database) {
        if(!Files.exists(Path.of("blockchain/"+Database+"/"+ Util.Base64Encode(Key)))) return;
        try {
            Files.delete(Path.of("blockchain/"+Database+"/"+ Util.Base64Encode(Key)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void MakeDir(String s){
        try {
            Files.createDirectory(Path.of(s));
        } catch (IOException ignored) {
        }
    }
}
