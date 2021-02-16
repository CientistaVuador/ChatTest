/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.subroutes;

import com.cien.securesocket.ServerConnection;
import com.cien.server.ChatServer;
import com.cien.server.IOManager;
import com.cien.server.SubRoute;
import com.cien.server.User;
import com.cien.server.Util;

/**
 *
 * @author Cien
 */
public class Register implements SubRoute {

    private final IOManager io = IOManager.get();
    
    @Override
    public void onDataReceived(ServerConnection c, ChatServer server) throws Exception {
        
        String user = c.getInput().readUTF();
        byte[] passwordUnsafe = new byte[c.getInput().readShort()];
        c.getInput().readFully(passwordUnsafe);
        byte[] password = Util.secureHash(passwordUnsafe);
        Util.clear(passwordUnsafe);
        
        if (io.userExists(user)) {
            c.getOutput().writeInt(1);
            Util.clear(password);
            return;
        }
        
        io.registerUser(user, password);
        
        Util.clear(password);
        
        c.getOutput().writeInt(0);
        
        User u = new User(user, server);
        server.setUser(u);
        
        String id = u.login();
        c.getOutput().writeUTF(id);
        
        System.out.println(u.getName()+" Logged out");
    }
    
}
