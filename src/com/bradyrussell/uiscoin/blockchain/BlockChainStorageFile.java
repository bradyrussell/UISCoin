package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Override
    public boolean exists(byte[] Key, String Database) {
        return Files.exists(Path.of("blockchain/"+Database+"/"+ Util.Base64Encode(Key)));
    }

    @Override
    public List<byte[]> keys(String Database){
        try (Stream<Path> stream = Files.walk(Path.of("blockchain/"+Database+"/"), 1)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(Util::Base64Decode)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void MakeDir(String s){
        try {
            Files.createDirectory(Path.of(s));
        } catch (IOException ignored) {
        }
    }
}
