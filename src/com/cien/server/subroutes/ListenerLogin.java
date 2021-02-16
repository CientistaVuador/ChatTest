/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.subroutes;

import com.cien.securesocket.ServerConnection;
import com.cien.server.ChatServer;
import com.cien.server.SubRoute;
import com.cien.server.User;

/**
 *
 * @author Cien
 */
public class ListenerLogin implements SubRoute {

    @Override
    public void onDataReceived(ServerConnection c, ChatServer server) throws Exception {

        String sessionId = c.getInput().readUTF();

        User user = server.getUserBySession(sessionId);

        if (user == null) {
            c.getOutput().writeInt(1);
            return;
        }

        c.getOutput().writeInt(0);
        
        user.addListener(c);
    }

}
