/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cien
 */
public class IOManager {

    private static final IOManager INSTANCE = new IOManager();

    public static IOManager get() {
        return INSTANCE;
    }

    private final String passFileName = "&pass.bin";
    private final File serverFolder = new File("server");
    private final File usersFolder = new File(serverFolder, "users");
    private final File groupsFolder = new File(serverFolder, "groups");
    private final File cacheFolder = new File(serverFolder, "cache");
    private final File saltFile = new File(serverFolder, "salt.bin");

    private IOManager() {
        if (cacheFolder.exists()) {
            for (File g : cacheFolder.listFiles()) {
                if (g.isFile()) {
                    g.delete();
                }
            }
        }
    }

    public String encode(String s) {
        try {
            return "." + URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String decode(String s) {
        try {
            return URLDecoder.decode(s.substring(1), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean userExists(String user) {
        return new File(usersFolder, encode(user)).exists();
    }

    public File getUserFolder(String user) {
        return new File(usersFolder, encode(user));
    }

    public boolean groupExists(String group) {
        return new File(groupsFolder, encode(group)).exists();
    }

    public File getGroupFolder(String group) {
        return new File(groupsFolder, encode(group));
    }

    public boolean isOnGroup(String user, String group) {
        File fileAtGroup = new File(getGroupFolder(group), encode(user));
        File fileAtUser = new File(getUserFolder(user), encode(group));

        return fileAtGroup.exists() && fileAtUser.exists();
    }

    public void registerGroup(String group, byte[] pass) throws IOException {
        File folder = getGroupFolder(group);
        folder.mkdirs();

        File passFile = new File(folder, passFileName);
        try (FileOutputStream out = new FileOutputStream(passFile)) {
            out.write(pass);
        }
    }

    public void setGroupOf(String user, String group) throws IOException {
        File fileAtGroup = new File(getGroupFolder(group), encode(user));
        File fileAtUser = new File(getUserFolder(user), encode(group));

        fileAtGroup.createNewFile();
        fileAtUser.createNewFile();
    }

    public void removeGroupOf(String user, String group) {
        File fileAtGroup = new File(getGroupFolder(group), encode(user));
        File fileAtUser = new File(getUserFolder(user), encode(group));

        fileAtGroup.delete();
        fileAtUser.delete();
    }

    public String[] getUsersOf(String group) {
        File groupFolder = getGroupFolder(group);

        List<String> groups = new ArrayList<>();
        for (File f : groupFolder.listFiles()) {
            if (f.getName().equals(passFileName)) {
                continue;
            }

            groups.add(decode(f.getName()));
        }

        return groups.toArray(new String[groups.size()]);
    }

    public String[] getGroupsOf(String user) {
        File userFolder = getUserFolder(user);

        List<String> groups = new ArrayList<>();
        for (File f : userFolder.listFiles()) {
            if (f.getName().equals(passFileName)) {
                continue;
            }

            groups.add(decode(f.getName()));
        }

        return groups.toArray(new String[groups.size()]);
    }

    public byte[] getGroupPassword(String group) throws IOException {
        File pass = new File(getGroupFolder(group), passFileName);
        byte[] password = new byte[(int) pass.length()];
        try (FileInputStream in = new FileInputStream(pass)) {
            in.read(password);
        }
        return password;
    }

    public byte[] getUserPassword(String user) throws IOException {
        File f = getUserFolder(user);
        File userFile = new File(f, passFileName);
        byte[] bytes = new byte[(int) userFile.length()];
        try (FileInputStream in = new FileInputStream(userFile)) {
            in.read(bytes);
        }
        return bytes;
    }

    public File getRandomCacheFile(String sufix) {
        File g = new File(cacheFolder, UUID.randomUUID().toString() + sufix);
        g.getParentFile().mkdirs();
        return g;
    }

    public File getCacheFile(String name) {
        return new File(cacheFolder, name);
    }

    public void registerUser(String user, byte[] password) throws IOException {
        File folder = getUserFolder(user);
        folder.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(folder, passFileName))) {
            out.write(password);
        }
    }

    public boolean deleteGroup(String group) {
        File folder = getGroupFolder(group);
        if (!folder.exists()) {
            return false;
        }
        for (File file : folder.listFiles()) {
            file.delete();
        }
        return folder.delete();
    }

    public byte[] getSalt() throws IOException {
        if (!saltFile.exists()) {
            saltFile.getParentFile().mkdirs();
            
            byte[] generated = Util.generateSalt(256);
            
            try (FileOutputStream out = new FileOutputStream(saltFile)) {
                out.write(generated);
            }
            
            return generated;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            @Override
            public synchronized void reset() {
                super.reset();

                Util.clear(buf);
            }
        };
        try (FileInputStream input = new FileInputStream(saltFile)) {
            int r;
            while ((r = input.read()) != -1) {
                out.write(r);
            }
        }
        byte[] result = out.toByteArray();
        out.reset();
        return result;
    }

    public void writeCacheFileTo(String name, OutputStream out) throws IOException {
        File file = getCacheFile(name);
        if (!file.exists()) {
            return;
        }

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            int r;
            while ((r = in.read()) != -1) {
                out.write(r);
            }
        }

    }

}
