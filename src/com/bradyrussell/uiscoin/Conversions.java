package com.bradyrussell.uiscoin;

public class Conversions {
    public static long coinsToSatoshis(int Coins){
        return ((long)Coins)*100000000L;
    }

    public static long coinsToSatoshis(double Coins){
        return (long)(Coins*100000000.0);
    }

    public static double satoshisToCoins(long Satoshis){
        return ((double)Satoshis)/100000000.0;
    }
}
