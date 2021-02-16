/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.client;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 *
 * @author Cien
 */
public class Message {

    private static final URL loadingImage = Message.class.getResource("loading.png");
    private static final URL notFoundImage = Message.class.getResource("notfound.png");
    
    public static String generateHTML(Message... m) {
        StringBuilder b = new StringBuilder();
        b.append("<html><head></head><body style=\"font-family: monospace;\">");
        for (Message e : m) {
            b.append(e.toHtmlTag());
        }
        b.append("</body></html>");
        return b.toString();
    }

    private final String user;
    private String data;
    private final boolean image;

    public Message(String user, String data, boolean image) {
        this.user = user;
        this.image = image;
        this.data = data;
    }

    public String getUser() {
        return user;
    }

    public boolean isImage() {
        return image;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
    
    public String toHtmlTag() {
        try {
            if (!image) {
                return "<p style=\"margin-top: 0;\">" + user + " >> " + data + "</p>";
            }
            boolean useLink = false;
            URL result;
            if (data.startsWith("request:")) {
                result = loadingImage;
            } else if (data.startsWith("notfound:")){
                result = notFoundImage;
            } else {
                File img = new File(data);
                if (!img.exists()) {
                    result = notFoundImage;
                } else {
                    result = img.toURI().toURL();
                    useLink = true;
                }
            }
            if (useLink) {
                return "<p style=\"margin-top: 0;\">" + user + " >> </p> <a href=\""+result.toString()+"\" ><img border=\"0\" src=\"" + result.toString() + "\" width=\"192\" height=\"128\"/></a>";
            } else {
                return "<p style=\"margin-top: 0;\">" + user + " >> </p> <img border=\"0\" src=\"" + result.toString() + "\" width=\"192\" height=\"128\"/>";
            }
        } catch (MalformedURLException ex) {
            return "error "+user+" "+data;
        }
    }

}
