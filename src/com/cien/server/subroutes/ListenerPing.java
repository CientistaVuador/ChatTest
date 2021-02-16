/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.subroutes;

import com.cien.securesocket.ServerConnection;
import com.cien.server.ChatServer;
import com.cien.server.SubRoute;

/**
 *
 * @author Cien
 */
public class ListenerPing implements SubRoute {

    @Override
    public void onDataReceived(ServerConnection c, ChatServer server) throws Exception {
        
    }
    
}
