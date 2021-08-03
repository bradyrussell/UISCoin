/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

public interface IBinaryData {
    byte[] getBinaryData();
    int setBinaryData(byte[] Data);
    int getSize();
    byte[] getHash();
}
