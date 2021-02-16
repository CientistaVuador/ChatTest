/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server;

import com.cien.securesocket.ServerConnection;

/**
 *
 * @author Cien
 */
public interface SubRoute {
    public void onDataReceived(ServerConnection c, ChatServer server) throws Exception;
}
