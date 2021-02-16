/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 *
 * @author Cien
 */
public class AddressConfig {

    private static final String ip;
    private static final int port;

    static {
        String ipTemp = null;
        int portTemp = -1;
        try {
            try (InputStream in = AddressConfig.class.getResourceAsStream("address.txt")) {
                StringBuilder b = new StringBuilder(64);
                int r;
                while ((r = in.read()) != -1) {
                    char c = (char) (byte) r;
                    if (c == '\r') {
                        continue; //fuck off
                    }
                    if (c == '\n' && ipTemp == null) {
                        ipTemp = b.toString();
                        b.setLength(0);
                        continue;
                    }
                    if (c == '\n' && ipTemp != null) {
                        try {
                            portTemp = Integer.parseInt(b.toString());
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                        b.setLength(0);
                        break;
                    }
                    b.append((char) (byte) r);
                }
                if (b.length() != 0) {
                    if (ipTemp != null) {
                        try {
                            portTemp = Integer.parseInt(b.toString());
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        ipTemp = b.toString();
                    }
                }
            }
        } catch (IOException ex) {
            ipTemp = "localhost";
            portTemp = 5565;
        }

        if (ipTemp != null) {
            ip = ipTemp;
        } else {
            ip = "localhost";
        }
        if (portTemp != -1) {
            port = portTemp;
        } else {
            port = 5565;
        }
    }

    public static String getIp() {
        return ip;
    }

    public static int getPort() {
        return port;
    }

    public static InetSocketAddress getAddress() {
        return new InetSocketAddress(ip, port);
    }

    private AddressConfig() {

    }

}
