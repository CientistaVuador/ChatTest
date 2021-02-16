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

/**
 *
 * @author Cien
 */
public class CreateGroup implements SubRoute {

    private final IOManager io = IOManager.get();

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

        try {
            if (io.groupExists(name)) {
                c.getOutput().writeInt(1);
                return;
            }
            io.registerGroup(name, password);
        } finally {
            Util.clear(password);
        }

        Group p = server.getGroup(name);
        p.addUser(user);
        
        c.getOutput().writeInt(2);
        
        System.out.println(user.getName()+" Created group "+name);
        
    }

}
