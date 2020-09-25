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

    public static byte[] ConcatArray(byte[] A, byte[] B){
        byte[] C = new byte[A.length+B.length];
        for(int i = 0; i < A.length+B.length; i++){
            C[i] = (i < A.length) ? A[i] : B[i-A.length];
        }
        return C;
    }

}
