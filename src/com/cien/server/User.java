/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server;

import com.cien.securesocket.ServerConnection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Cien
 */
public class User {

    private final String name;
    private final List<String> sessionIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Group> groups = Collections.synchronizedList(new ArrayList<>());
    private final List<ServerConnection> listeners = Collections.synchronizedList(new ArrayList<>());
    private final ChatServer server;

    public User(String name, ChatServer server) {
        this.name = name;
        this.server = server;
    }

    public String getName() {
        return name;
    }

    public ChatServer getServer() {
        return server;
    }

    public Group[] getGroups() {
        return groups.toArray(new Group[groups.size()]);
    }

    private void sendUserListForAllGroups() {
        for (Group p : getGroups()) {
            p.sendUserListForAll();
        }
    }

    private void sendGroupList() {
        String[] groups = Arrays
                .asList(getGroups())
                .stream()
                .map(Group::getName)
                .toArray(String[]::new);

        sendGroupList(groups);
    }

    public boolean addToGroup(Group p) {
        if (p.contains(this)) {
            return false;
        }
        p.users.add(this);
        groups.add(p);

        sendGroupList();
        sendUserListForAllGroups();

        return true;
    }

    public boolean removeFromGroup(Group p) {
        if (!p.contains(this)) {
            return false;
        }
        groups.remove(p);
        p.users.remove(this);

        sendGroupList();
        sendUserListForAllGroups();

        return true;
    }

    public String login() {
        String id = UUID.randomUUID().toString();
        sessionIds.add(id);
        server.setUserBySession(id, this);
        return id;
    }

    public boolean isSessionIDValid(String s) {
        return sessionIds.contains(s);
    }

    public ServerConnection[] getListeners() {
        return listeners.toArray(new ServerConnection[listeners.size()]);
    }

    public void addListener(ServerConnection c) {
        listeners.add(c);

        if (c.getProtocol().equals(Protocols.CHAT_RECEIVER)) {

            sendGroupList();
            sendUserListForAllGroups();

        }

    }

    public boolean removeListener(ServerConnection c) {
        boolean result = listeners.remove(c);

        if (c.getProtocol().equals(Protocols.CHAT_RECEIVER)) {

            sendUserListForAllGroups();

        }

        return result;
    }

    public boolean isOnline() {
        return !listeners.isEmpty();
    }

    public String status() {
        if (isOnline()) {
            return getName();
        } else {
            return getName() + " (Off)";
        }
    }

    public void sendGroupList(String[] groups) {
        for (ServerConnection c : getListeners()) {
            try {

                if (c.getProtocol().equals(Protocols.CHAT_RECEIVER)) {

                    synchronized (c) {

                        c.getOutput().writeUTF("updateGroups");
                        c.getOutput().writeInt(groups.length);
                        for (String group : groups) {
                            c.getOutput().writeUTF(group);
                        }
                        c.getOutput().flush();

                    }

                }

            } catch (IOException ex) {
                removeListener(c);
            }
        }
    }

    public void sendUserList(String[] users, String group) {
        for (ServerConnection c : getListeners()) {
            try {

                if (c.getProtocol().equals(Protocols.CHAT_RECEIVER)) {

                    synchronized (c) {

                        c.getOutput().writeUTF("updateUsers");
                        c.getOutput().writeUTF(group);
                        c.getOutput().writeInt(users.length);
                        for (String user : users) {
                            c.getOutput().writeUTF(user);
                        }
                        c.getOutput().flush();

                    }

                }

            } catch (IOException ex) {
                removeListener(c);
            }
        }
    }

    public void sendMessage(String user, String data, boolean image, String group) {
        for (ServerConnection c : getListeners()) {
            try {

                if (c.getProtocol().equals(Protocols.CHAT_RECEIVER)) {

                    synchronized (c) {

                        c.getOutput().writeUTF("messageReceived");
                        c.getOutput().writeBoolean(image);
                        c.getOutput().writeUTF(group);
                        c.getOutput().writeUTF(user);
                        c.getOutput().writeUTF(data);
                        c.getOutput().flush();

                    }

                }

            } catch (IOException ex) {
                removeListener(c);
            }
        }
    }

}
