/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.securesocket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author Cien
 */
public class CienInputStream extends FilterInputStream {

    private final SecretKey key;
    private final boolean useCompression;
    
    protected final Object lock = new Object();

    protected byte[] buffer = new byte[0];
    protected int index = 0;
    

    public CienInputStream(InputStream in, SecretKey key, boolean useCompression) {
        super(in);
        this.key = key;
        this.useCompression = useCompression;
    }
    
    public boolean usingCompression() {
        return useCompression;
    }

    public SecretKey getKey() {
        return key;
    }

    public InputStream getInput() {
        return in;
    }

    private void clear(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            b[i] = 0;
        }
    }

    private byte[] readArray(int size) throws IOException {
        byte[] array = new byte[size];
        if (size == 0) {
            return array;
        }

        int firstByte = in.read();
        if (firstByte == -1) {
            return null;
        }
        array[0] = (byte) firstByte;

        for (int i = 1; i < array.length; i++) {
            array[i] = (byte) in.read();
        }
        
        return array;
    }

    private int readInt(byte[] array) {
        return ByteBuffer.wrap(array).getInt();
    }

    private byte[] decryptAndClearData(byte[] data) throws IOException {
        try {

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(data);

            clear(data);

            return decrypted;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException ex) {
            throw new IOException(ex);
        }
    }

    private byte[] decryptAndDecompressAndClearData(byte[] data) throws IOException {
        byte[] decrypted = decryptAndClearData(data);
        
        if (!usingCompression()) {
            return decrypted;
        }
        
        ByteArrayInputStream input = new ByteArrayInputStream(decrypted); //to read decrypted data

        ByteArrayOutputStream output = new ByteArrayOutputStream(data.length) { //to write decompressed data
            @Override
            public synchronized void reset() {
                super.reset();
                clear(buf);
            }
        };

        try (GZIPInputStream gzip = new GZIPInputStream(input) { //to decompress the decrypted data
            @Override
            public void close() throws IOException {
                super.close();
                clear(buf);
            }
        }) {
            
            int r;
            while ((r = gzip.read()) != -1) { //reading
                output.write(r);
            }
            
        }

        clear(decrypted); //clear decrypted data

        byte[] array = output.toByteArray(); //get the array

        output.reset(); //clear the output buffer

        return array;
    }

    private boolean readToBuffer() throws IOException {
        synchronized (lock) {            
            
            if (index != buffer.length) {
                return true;
            }
            
            byte[] sizeArray = readArray(4);

            if (sizeArray == null) {
                return false;
            }

            int size = readInt(sizeArray);
            
            byte[] data = decryptAndDecompressAndClearData(readArray(size));
            
            clear(buffer); //clear current buffer

            buffer = data;
            index = 0;
            
            return true;

        }
    }

    @Override
    public int read() throws IOException {
        if (index == buffer.length && !readToBuffer()) {
            return -1;
        }
        return Byte.toUnsignedInt(buffer[index++]);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = 0;
        for (int i = off; i < len; i++) {
            int r = read();
            if (r == -1) {
                return read;
            }
            b[i] = (byte) r;
            read++;
        }
        return len - off;
    }

    @Override
    public int available() throws IOException {
        return (buffer.length - index) + in.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
        clear(buffer);
    }

}
