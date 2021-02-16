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
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.crypto.SecretKey;

/**
 *
 * @author Cien
 */
public class ServerConnection {
    
    private final Socket socket;
    private final SecretKey key;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final String protocol;
    private final Map<String, Object> map = Collections.synchronizedMap(new HashMap<>());
    private final Object workingLock = new Object();
    private final Queue<byte[]> queueSend = new ConcurrentLinkedQueue<>();
    private volatile boolean working = false;
    private final boolean usingCompression;
    
    public ServerConnection(Socket socket, String protocol, SecretKey key, boolean usingCompression) throws IOException {
        this.socket = socket;
        this.key = key;
        this.usingCompression = usingCompression;
        this.input = new DataInputStream(new CienInputStream(socket.getInputStream(), key, usingCompression));
        this.output = new DataOutputStream(new CienOutputStream(socket.getOutputStream(), key, usingCompression));
        this.protocol = protocol;
    }

    public boolean usingCompression() {
        return usingCompression;
    }
    
    public String getProtocol() {
        return protocol;
    }

    public DataInputStream getInput() {
        return input;
    }

    public SecretKey getKey() {
        return key;
    }

    public DataOutputStream getOutput() {
        return output;
    }

    public Socket getSocket() {
        return socket;
    }

    public void set(String name, Object obj) {
        map.put(name, obj);
    }
    
    public Object get(String name) {
        return map.get(name);
    }
    
    public void queueData(byte[] b) throws IOException {
        synchronized (workingLock) {
            if (working) {
                queueSend.add(b);
            } else {
                output.write(b);
                output.flush();
            }
        }
    }

    public void setWorking(boolean working) throws IOException {
        synchronized (workingLock) {
            if (!working) {
                byte[] b;
                while ((b = queueSend.poll()) != null) {
                    output.write(b);
                    output.flush();
                }
            }
            this.working = working;
        }
    }

    public boolean isWorking() {
        synchronized (workingLock) {
            return working;
        }
    }
    
    
    
}
