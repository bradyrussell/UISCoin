package com.bradyrussell.uiscoin;

public interface IBinaryData {
    byte[] getBinaryData();
    void setBinaryData(byte[] Data);
    int getSize();
    byte[] getHash();
}
