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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author Cien
 */
public class UploadImage implements SubRoute {

    private final IOManager io = IOManager.get();
    
    @Override
    public void onDataReceived(ServerConnection c, ChatServer server) throws Exception {
        
        String sessionId = c.getInput().readUTF();
        String groupName = c.getInput().readUTF();
        long size = c.getInput().readLong();
        
        User user = server.getUserBySession(sessionId);
        if (user == null) {
            c.getOutput().writeInt(0);
            return;
        }
        
        Group group = server.getGroup(groupName);
        if (group == null) {
            c.getOutput().writeInt(3);
            return;
        }
        
        if (size > 2000000) {
            c.getOutput().writeInt(2);
            return;
        }
        
        c.getOutput().writeInt(1);
        c.getOutput().flush();
        
        File theFile = io.getRandomCacheFile(".png");
        
        File theFileWriting = new File(theFile.getPath()+".writing");
        theFileWriting.createNewFile();
        
        group.sendMessageForAll(user.getName(), theFile.getName(), true);
        
        try {
            
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(theFile))) {
                for (long i = 0; i < size; i++) {
                    out.write(c.getInput().read());
                }
            }
            
        } catch (IOException ex) {
            theFile.delete();
        } finally {
            theFileWriting.delete();
        }
    }
    
}
