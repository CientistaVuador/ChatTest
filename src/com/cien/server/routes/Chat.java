/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.routes;

import com.cien.server.Route;
import com.cien.server.subroutes.CreateGroup;
import com.cien.server.subroutes.EnterGroup;
import com.cien.server.subroutes.ExitGroup;
import com.cien.server.subroutes.Login;
import com.cien.server.subroutes.Register;
import com.cien.server.subroutes.SendMessage;

/**
 *
 * @author Cien
 */
public class Chat extends Route {

    public Chat() {
        configure();
    }
    
    private void configure() {
        
        setSubRoute("register", new Register());
        setSubRoute("login", new Login());
        setSubRoute("createGroup", new CreateGroup());
        setSubRoute("sendMessage", new SendMessage());
        setSubRoute("enterGroup", new EnterGroup());
        setSubRoute("exitGroup", new ExitGroup());
        
    }
    
}
