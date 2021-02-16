/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server;

import com.cien.securesocket.ServerConnection;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Cien
 */
public class Route {
    
    private final Map<String, SubRoute> map = Collections.synchronizedMap(new HashMap<>());
    
    public Route() {
        
    }
    
    public void setSubRoute(String s, SubRoute e) {
        map.put(s, e);
    }
    
    public SubRoute getSubRoute(String s) {
        return map.get(s);
    }
    
    public void onDataReceived(ServerConnection e, ChatServer server) throws Exception {
        String f = e.getInput().readUTF();
        SubRoute sub = getSubRoute(f);
        if (sub != null) {
            sub.onDataReceived(e, server);
        }
    }
    
}
