/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.subroutes;

import com.cien.securesocket.ServerConnection;
import com.cien.server.ChatServer;
import com.cien.server.Group;
import com.cien.server.SubRoute;
import com.cien.server.User;

/**
 *
 * @author Cien
 */
public class SendMessage implements SubRoute {

    @Override
    public void onDataReceived(ServerConnection c, ChatServer server) throws Exception {
        
        String sessionId = c.getInput().readUTF();
        String group = c.getInput().readUTF();
        String text = c.getInput().readUTF();
        
        User user = server.getUserBySession(sessionId);
        if (user == null) {
            c.getOutput().writeInt(0);
            return;
        }
        
        Group g = server.getGroup(group);
        if (g == null) {
            c.getOutput().writeInt(1);
            return;
        }
        
        g.sendMessageForAll(user.getName(), text, false);
        
        c.getOutput().writeInt(2);
    }
    
}
