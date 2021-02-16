/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.securesocket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author Cien
 */
public class CienOutputStream extends FilterOutputStream {

    public static SecretKey newSecretKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static final int BUFFER_SIZE = 16384;

    private final SecretKey key;

    protected final Object lock = new Object();
    protected final byte[] buffer = new byte[BUFFER_SIZE];

    protected volatile int index = 0;
    private final boolean useCompression;

    public CienOutputStream(OutputStream out, SecretKey key, boolean useCompression) {
        super(out);
        this.key = key;
        this.useCompression = useCompression;
    }
    
    public boolean usingCompression() {
        return useCompression;
    }

    public SecretKey getKey() {
        return key;
    }

    public OutputStream getOutput() {
        return out;
    }

    private void clear(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            b[i] = 0;
        }
    }

    private byte[] compressAndClear() throws IOException {
        byte[] copy = Arrays.copyOf(buffer, index); //copy

        index = 0; //clear index
        clear(buffer); //clear buffer
        
        if (!usingCompression()) {
            return copy;
        }
        
        ByteArrayOutputStream output = new ByteArrayOutputStream(copy.length) {
            @Override
            public synchronized void reset() {
                super.reset();
                clear(buf);
            }
        }; //to write

        GZIPOutputStream gzip = new GZIPOutputStream(output) {
            @Override
            public void finish() throws IOException {
                super.finish();
                clear(buf);
            }
        }; //gzip compression

        gzip.write(copy);

        gzip.finish(); //clear the gzip stream

        clear(copy); //clear the copy

        byte[] compressedData = output.toByteArray(); //get the array

        output.reset(); //clear the buffer

        return compressedData; //returns compressed data
    }

    private byte[] encryptDataAndClear(byte[] data) throws IOException {
        try {

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data);

            clear(data);

            return encrypted;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException ex) {
            throw new IOException(ex);
        }
    }

    private void writeArray(byte[] b) throws IOException {
        out.write(b);
    }

    private void writeInt(int i) throws IOException {
        byte[] array = ByteBuffer.allocate(4).putInt(i).array();
        writeArray(array);
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {

            if (index == 0) {
                out.flush();
                return;
            }

            byte[] encrypted = encryptDataAndClear(compressAndClear());

            writeInt(encrypted.length);
            writeArray(encrypted);

            clear(encrypted);

            out.flush();
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (index == buffer.length) {
            flush();
        }
        buffer[index] = (byte) b;
        index++;
        
    }

}
