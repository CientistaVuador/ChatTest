/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.securesocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Cien
 */
public abstract class Server {

    private final ServerSocket server;
    private final ExecutorService service = Executors.newFixedThreadPool(32);
    private final List<ServerConnection> connections = Collections.synchronizedList(new ArrayList<>());
    private final KeyPair keys;
    private boolean started = false;

    public Server(InetSocketAddress address) throws IOException {
        this.server = new ServerSocket(address.getPort(), 0, address.getAddress());
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            keys = keyGen.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public KeyPair getKeys() {
        return keys;
    }

    public ServerSocket getServer() {
        return server;
    }

    public void start() {
        if (!started) {
            started = true;
            service.submit(this::runServer);
            service.submit(this::runListener);
        }
    }

    private void runServer() {
        while (!server.isClosed()) {

            try {

                Socket s = server.accept();

                s.setSoTimeout(5000);
                s.setReceiveBufferSize(Short.MAX_VALUE * 2);
                s.setSendBufferSize(Short.MAX_VALUE * 2);

                new Thread(() -> {
                    runNegotiator(s);
                }).start();

            } catch (IOException ex) {
            }

        }
    }

    private void runNegotiator(Socket s) {
        try {

            DataInputStream input = new DataInputStream(s.getInputStream());
            DataOutputStream output = new DataOutputStream(s.getOutputStream());

            byte[] publicKeyBytes = keys.getPublic().getEncoded();
            output.writeInt(publicKeyBytes.length);
            output.write(publicKeyBytes);
            output.flush();

            int secretKeySize = input.readInt();
            byte[] secretKeyBytes = new byte[secretKeySize];
            for (int i = 0; i < secretKeySize; i++) {
                secretKeyBytes[i] = (byte) input.read();
            }

            try {
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
                SecretKey secret = new SecretKeySpec(cipher.doFinal(secretKeyBytes), "AES");

                int encryptedProtocolSize = input.readInt();
                byte[] encryptedProtocol = new byte[encryptedProtocolSize];
                for (int i = 0; i < encryptedProtocolSize; i++) {
                    encryptedProtocol[i] = (byte) input.read();
                }
                
                boolean useCompression = input.readBoolean();
                
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, secret);

                String protocol = new String(cipher.doFinal(encryptedProtocol), StandardCharsets.UTF_8);

                ServerConnection conn = new ServerConnection(s, protocol, secret, useCompression);

                connections.add(conn);
            } catch (GeneralSecurityException e) {
                throw new IOException(e);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                s.close();
            } catch (IOException ex1) {
            }
        }
    }

    private void runListener() {
        while (!server.isClosed()) {
            ServerConnection[] conns = connections.toArray(new ServerConnection[connections.size()]);
            boolean noData = true;
            for (int i = 0; i < conns.length; i++) {
                ServerConnection conn = conns[i];
                try {
                    if (conn.getSocket().getInputStream().available() != 0 && !conn.isWorking()) {
                        noData = false;
                        conn.setWorking(true);
                        service.submit(() -> {
                            try {
                                onDataReceived(conn);
                                conn.getOutput().flush();
                                conn.setWorking(false);
                            } catch (Exception ex) {
                                try {
                                    conn.getSocket().close();
                                } catch (IOException ex1) {}
                                ex.printStackTrace();
                                connections.remove(conn);
                            }
                        });
                    }
                    
                } catch (IOException ex) {
                    try {
                        conn.getSocket().close();
                    } catch (IOException ex1) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    ex.printStackTrace();
                    connections.remove(conn);
                }
                
            }
            if (noData) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public abstract void onDataReceived(ServerConnection c) throws Exception;

    public void shutdown() {
        try {
            server.close();
        } catch (IOException ex) {
        }
        service.shutdown();
    }
    
    public ServerConnection[] getConnections() {
        return connections.toArray(new ServerConnection[connections.size()]);
    }

}
