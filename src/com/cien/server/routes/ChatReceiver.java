/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.routes;

import com.cien.server.Route;
import com.cien.server.subroutes.CreateGroup;
import com.cien.server.subroutes.ListenerLogin;
import com.cien.server.subroutes.ListenerPing;
import com.cien.server.subroutes.ListenerShutdown;
import com.cien.server.subroutes.Login;
import com.cien.server.subroutes.Register;

/**
 *
 * @author Cien
 */
public class ChatReceiver extends Route {
    
    public ChatReceiver() {
        configure();
    }
    
    private void configure() {
        
        setSubRoute("login", new ListenerLogin());
        setSubRoute("ping", new ListenerPing());
        setSubRoute("shutdown", new ListenerShutdown());
        
    }
}
