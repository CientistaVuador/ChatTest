/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Cien
 */
public class Util {

    public static void clear(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            b[i] = 0;
        }
    }

    public static byte[] generateSalt(int size) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        return bytes;
    }
    
    public static byte[] secureHash(byte[] data, byte[] salt) {
        try {
            
            byte[] mixed = Arrays.copyOf(data, data.length + salt.length);
            
            for (int i = data.length; i < salt.length; i++) {
                mixed[i] = salt[i - data.length];
            }
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(salt);
            
            clear(mixed);
            
            return result;
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static byte[] secureHash(byte[] data) throws IOException {
        byte[] salt = IOManager.get().getSalt();
        try {
            return secureHash(data, salt);
        } finally {
            clear(salt);
        }
    }

    private Util() {

    }
}
