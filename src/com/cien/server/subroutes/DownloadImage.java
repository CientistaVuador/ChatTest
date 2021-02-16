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
import java.io.BufferedOutputStream;
import java.io.File;

/**
 *
 * @author Cien
 */
public class DownloadImage implements SubRoute {

    private final IOManager io = IOManager.get();
    
    @Override
    public void onDataReceived(ServerConnection c, ChatServer server) throws Exception {
        
        String name = c.getInput().readUTF();
        
        File file = io.getCacheFile(name);
        File writing = io.getCacheFile(name+".writing");
        
        if (!file.exists() && !writing.exists()) {
            c.getOutput().writeInt(0);
            return;
        }
        
        if (writing.exists()) {
            c.getOutput().writeInt(1);
            return;
        }
        
        c.getOutput().writeInt(2);
        c.getOutput().flush();
        
        c.getOutput().writeLong(file.length());
        io.writeCacheFileTo(name, c.getOutput());
    }
    
}
