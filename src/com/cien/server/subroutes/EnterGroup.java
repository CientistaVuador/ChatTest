/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.subroutes;

import com.cien.securesocket.ServerConnection;
import com.cien.server.ChatServer;
import com.cien.server.Group;
import com.cien.server.IOManager;
import com.cien.server.SubRoute;
import com.cien.server.User;
import com.cien.server.Util;
import java.util.Arrays;

/**
 *
 * @author Cien
 */
public class EnterGroup implements SubRoute {

    @Override
    public void onDataReceived(ServerConnection c, ChatServer server) throws Exception {

        String sessionId = c.getInput().readUTF();
        String name = c.getInput().readUTF();
        byte[] passwordUnsafe = new byte[c.getInput().readShort()];
        c.getInput().readFully(passwordUnsafe);
        byte[] password = Util.secureHash(passwordUnsafe);
        Util.clear(passwordUnsafe);

        User user = server.getUserBySession(sessionId);
        if (user == null) {
            c.getOutput().writeInt(0);
            Util.clear(password);
            return;
        }

        Group group = server.getGroup(name);
        if (group == null) {
            c.getOutput().writeInt(1);
            Util.clear(password);
            return;
        }

        if (group.contains(user)) {
            c.getOutput().writeInt(2);
            Util.clear(password);
            return;
        }

        byte[] passwordCheck = IOManager.get().getGroupPassword(name);
        try {
            if (!Arrays.equals(password, passwordCheck)) {
                c.getOutput().writeInt(1);
                return;
            }
        } finally {
            Util.clear(password);
            Util.clear(passwordCheck);
        }

        group.addUser(user);
        
        Util.clear(password);

        c.getOutput().writeInt(3);
    }

}
