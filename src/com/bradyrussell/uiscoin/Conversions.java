package com.bradyrussell.uiscoin;

public class Conversions {
    public static long CoinsToSatoshis(int Coins){
        return ((long)Coins)*100000000L;
    }

    public static double SatoshisToCoins(long Satoshis){
        return ((double)Satoshis)/100000000.0;
    }
}
