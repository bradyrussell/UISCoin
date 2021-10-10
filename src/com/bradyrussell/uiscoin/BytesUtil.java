/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.*;

import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;

public class BytesUtil {
    @Deprecated
    public static void printBytesReadable(byte[] bytes) {
        System.out.print("[");
        for (byte b : bytes) {

            if (b >= 32 && b <= 126) {
                System.out.print((char) b);
            } else {
                System.out.print("0x");
                System.out.printf("%02X", b);
            }
            System.out.print(" ");
        }
        System.out.println("]");
    }

    @Deprecated
    public static void printBytesHex(byte[] bytes) {
        System.out.print("[");
        for (byte b : bytes) {
            System.out.print("0x");
            System.out.printf("%02X", b);
            System.out.print(" ");
        }
        System.out.println("]");
    }

    @Deprecated
    public static void printBytesHexDump(byte[] bytes) {
        System.out.print("[");
        System.out.print("0x");
        for (byte b : bytes) {
            System.out.printf("%02X", b);
        }
        System.out.println("]");
    }

    public static byte[] concatArray(byte[] A, byte[] B) {
        byte[] C = new byte[A.length + B.length];
        for (int i = 0; i < A.length + B.length; i++) {
            C[i] = (i < A.length) ? A[i] : B[i - A.length];
        }
        return C;
    }

    public static int byteArrayToNumber32(byte[] Bytes) {
        return ((int) Bytes[0] & 0xFF) << 24
                | ((int) Bytes[1] & 0xFF) << 16
                | ((int) Bytes[2] & 0xFF) << 8
                | ((int) Bytes[3] & 0xFF);
    }

    public static byte[] numberToByteArray32(int Number) {
        return new byte[]{
                (byte) (Number >> 24),
                (byte) (Number >> 16),
                (byte) (Number >> 8),
                (byte) Number
        };
    }

    //https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
    public static long byteArrayToNumber64(byte[] Bytes) {
        return ((long) Bytes[0] << 56)
                | ((long) Bytes[1] & 0xFF) << 48
                | ((long) Bytes[2] & 0xFF) << 40
                | ((long) Bytes[3] & 0xFF) << 32
                | ((long) Bytes[4] & 0xFF) << 24
                | ((long) Bytes[5] & 0xFF) << 16
                | ((long) Bytes[6] & 0xFF) << 8
                | ((long) Bytes[7] & 0xFF);
    }

    public static byte[] numberToByteArray64(long Number) {
        return new byte[]{
                (byte) (Number >> 56),
                (byte) (Number >> 48),
                (byte) (Number >> 40),
                (byte) (Number >> 32),
                (byte) (Number >> 24),
                (byte) (Number >> 16),
                (byte) (Number >> 8),
                (byte) Number
        };
    }

    // zlib seems to produce smaller output in all my tests vs gzip
    public static byte[] zipBytes(byte[] uncompressedData) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(uncompressedData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(uncompressedData.length);

        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }

    public static byte[] unzipBytes(byte[] compressedData) throws DataFormatException, IOException {
        if (compressedData[0] == 120 && compressedData[1] == -100) { // zlib header
            Inflater inflater = new Inflater();
            inflater.setInput(compressedData);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length);
            byte[] buffer = new byte[1024];
            while (!inflater.finished() && inflater.getBytesRead() < MagicNumbers.MaximumUnzipLength.Value) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            return outputStream.toByteArray();
        } else { // for backwards compatibility
            byte[] result = new byte[]{};
            try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 GZIPInputStream gzipIS = new GZIPInputStream(bis)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzipIS.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                result = bos.toByteArray();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            return result;
        }
    }

    //https://www.baeldung.com/java-convert-float-to-byte-array
    public static byte[] floatToByteArray(float Number) {
        int intBits = Float.floatToIntBits(Number);
        return new byte[]{
                (byte) (intBits >> 24), (byte) (intBits >> 16), (byte) (intBits >> 8), (byte) (intBits)};
    }

    public static float byteArrayToFloat(byte[] Bytes) {
        int intBits =
                Bytes[0] << 24 | (Bytes[1] & 0xFF) << 16 | (Bytes[2] & 0xFF) << 8 | (Bytes[3] & 0xFF);
        return Float.intBitsToFloat(intBits);
    }

    public static String base64Encode(byte[] Data) {
        return Base64.getUrlEncoder().encodeToString(Data);
    }

    public static byte[] base64Decode(String Base64String) {
        return Base64.getUrlDecoder().decode(Base64String);
    }

    public static boolean doTransactionsContainTXO(byte[] TransactionHash, int Index, List<
            Transaction> Transactions) {
        for (Transaction transaction : Transactions) {
            for (TransactionInput input : transaction.Inputs) {
                if (Arrays.equals(input.InputHash, TransactionHash) && input.IndexNumber == Index) return true;
            }
        }
        return false;
    }

    public static String getConstantSalt() {
        return "_UISCoin1.0_salted_password";
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static byte[] getBytesFromHexString(String Hex) {
        int len = Hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(Hex.charAt(i), 16) << 4)
                    + Character.digit(Hex.charAt(i + 1), 16));
        }
        return data;
    }
}
