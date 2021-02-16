/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.securesocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 *
 * @author Cien
 */
public class Client {
    
    private final String protocol;
    private final InetSocketAddress address;
    private final Object lock = new Object();
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream output = null;
    private SecretKey key = null;
    private volatile boolean working = false;
    private final Object workingLock = new Object();
    private final Map<String, Object> map = Collections.synchronizedMap(new HashMap<>());
    private final boolean useCompression;
    
    public Client(String protocol, InetSocketAddress address, boolean useCompression) {
        this.protocol = protocol;
        this.address = address;
        this.useCompression = useCompression;
    }
    
    public boolean usingCompression() {
        return useCompression;
    }
    
    public Object get(String s) {
        return map.get(s);
    }
    
    public void set(String s, Object obj) {
        map.put(s, obj);
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public InetSocketAddress getAddress() {
        return address;
    }
    
    public DataInputStream getInput() throws IOException {
        if (input == null) {
            throw new IOException("Not connected");
        }
        return input;
    }
    
    public SecretKey getKey() {
        return key;
    }
    
    public DataOutputStream getOutput() throws IOException {
        if (output == null) {
            throw new IOException("Not connected");
        }
        return output;
    }
    
    public Socket getSocket() throws IOException {
        if (socket == null) {
            throw new IOException("Not connected");
        }
        return socket;
    }
    
    public void connect() throws IOException {
        synchronized (lock) {
            if (socket != null && !socket.isClosed()) {
                close();
            }
            
            try {
                
                KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(256);
                key = generator.generateKey();
                
                socket = new Socket(address.getAddress(), address.getPort());
                socket.setReceiveBufferSize(Short.MAX_VALUE * 2);
                socket.setSendBufferSize(Short.MAX_VALUE * 2);
                socket.setSoTimeout(5000);
                
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                
                int publicKeySize = in.readInt();
                byte[] publicKeyBytes = new byte[publicKeySize];
                for (int i = 0; i < publicKeySize; i++) {
                    publicKeyBytes[i] = (byte) in.read();
                }
                PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
                
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] encryptedSecretKey = cipher.doFinal(this.key.getEncoded());
                
                out.writeInt(encryptedSecretKey.length);
                out.write(encryptedSecretKey);
                out.flush();
                
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, this.key);
                byte[] encryptedProtocol = cipher.doFinal(this.protocol.getBytes(StandardCharsets.UTF_8));
                
                out.writeInt(encryptedProtocol.length);
                out.write(encryptedProtocol);
                
                out.writeBoolean(usingCompression());
                
                out.flush();
                
                input = new DataInputStream(new CienInputStream(socket.getInputStream(), this.key, usingCompression()));
                output = new DataOutputStream(new CienOutputStream(socket.getOutputStream(), this.key, usingCompression()));
                
                setWorking(false);
                
            } catch (GeneralSecurityException e) {
                throw new IOException(e);
            }
        }
    }
    
    public void close() {
        synchronized (lock) {
            
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ex) {
                }
            }
            
            key = null;
            input = null;
            output = null;
            socket = null;
            working = false;
        }
    }
    
    public boolean isWorking() {
        synchronized (workingLock) {
            return working;
        }
    }
    
    public void setWorking(boolean working) {
        synchronized (workingLock) {
            this.working = working;
        }
    }
    
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
    
}
