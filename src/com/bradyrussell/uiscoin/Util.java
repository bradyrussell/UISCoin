package com.bradyrussell.uiscoin;

public class Util {
    public static void printBytesReadable(byte[] bytes) {
        System.out.print("[");
        for(byte b: bytes){

            if(b >= 32 && b <= 126) {
                System.out.print((char)b);
            } else {
                System.out.print("0x");
                System.out.printf("%02X", b);
            }
            System.out.print(" ");
        }
        System.out.println("]");
    }

    public static byte[] TrimByteArray(byte[] array){
        int LastNonNullByteIndex = 0;
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            byte b = array[i];
            if (b != 0) LastNonNullByteIndex = i;
        }

        byte[] trimmed = new byte[LastNonNullByteIndex+1];

        System.arraycopy(array,0,trimmed, 0, LastNonNullByteIndex+1);

        return trimmed;
    }


}
