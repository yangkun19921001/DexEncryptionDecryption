package com.example.proxy_core;
//Ref : https://blog.csdn.net/fyq201749/article/details/81504251

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptPKCS7Core {
    private String ALGO = "AES";
    private String ALGO_MODE = "AES/CBC/NoPadding";
    private String akey = "00001111222233334444555566667777";

    public static final byte[] ivBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public static void main(String[] args) throws Exception {
        EncryptPKCS7Core aes = new EncryptPKCS7Core();

        String testData = "testJJJOOOSSSHHHUUUAAAtestJJJOOOSSSHHHUUUAAAtest";
        String rstData = pkcs7padding(testData);
        byte[] passwordEnc = aes.encrypt(rstData);
        byte[] passwordDec = aes.decrypt(passwordEnc);
        System.out.println("passwordEnc:" + passwordEnc);
        System.out.println("passwordDec:" + new String(passwordDec));
    }

    public byte[] encrypt(String Data) throws Exception {
        return encrypt(Data.getBytes());
    }

    public byte[] encrypt(byte[] dataBytes) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance(ALGO_MODE);
            int blockSize = cipher.getBlockSize();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }
            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
            SecretKeySpec keyspec = new SecretKeySpec(akey.getBytes("utf-8"), ALGO);
            IvParameterSpec ivspec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(plaintext);
            return Base64.encode(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decrypt(byte[] encryptedData) throws Exception {
        try {
            byte[] encrypted1 = Base64.decode(encryptedData, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance(ALGO_MODE);
            SecretKeySpec keyspec = new SecretKeySpec(akey.getBytes("utf-8"), ALGO);
            IvParameterSpec ivspec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            return cipher.doFinal(encrypted1);
//            byte[] original = cipher.doFinal(encrypted1);
//            String originalString = new String(original);
//            return originalString.trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] toByteArray(String hexString) {
        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() >> 1];
        int index = 0;
        for (int i = 0; i < hexString.length(); i++) {
            if (index > hexString.length() - 1)
                return byteArray;
            byte highDit = (byte) (Character.digit(hexString.charAt(index), 16) & 0xFF);
            byte lowDit = (byte) (Character.digit(hexString.charAt(index + 1), 16) & 0xFF);
            byteArray[i] = (byte) (highDit << 4 | lowDit);
            index += 2;
        }
        System.out.println(byteArray.length);
        return byteArray;
    }


    public static String pkcs7padding(String data) {
        int bs = 16;
        int padding = bs - (data.length() % bs);
        StringBuilder padding_text = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            padding_text.append((char) padding);
        }
        return data + padding_text;
    }
}

