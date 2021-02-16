/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cien
 */
public class Group {

    private final IOManager io = IOManager.get();
    private final String name;
    protected final List<User> users = Collections.synchronizedList(new ArrayList<>());

    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean contains(User s) {
        return users.contains(s);
    }

    public boolean addUser(User s) {
        boolean result = s.addToGroup(this);
        if (result) {
            try {
                io.setGroupOf(s.getName(), name);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return result;
    }

    public boolean removeUser(User s) {
        boolean result = s.removeFromGroup(this);
        if (result) {
            io.removeGroupOf(s.getName(), name);

            if (users.isEmpty()) {
                io.deleteGroup(name);
                s.getServer().setGroupToNull(name);
            }
        }
        return result;
    }

    public User[] getUsers() {
        return users.toArray(new User[users.size()]);
    }

    public void sendMessageForAll(String user, String data, boolean image) {
        for (User u:getUsers()) {
            u.sendMessage(user, data, image, this.getName());
        }
    }

    public void sendUserListForAll() {
        User[] users = getUsers();
        String[] usersStatus = new String[users.length];
        for (int i = 0; i < usersStatus.length; i++) {
            usersStatus[i] = users[i].status();
        }
        for (User u : users) {
            u.sendUserList(usersStatus, this.getName());
        }
    }

}
