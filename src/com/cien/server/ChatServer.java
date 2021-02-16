/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server;

import com.cien.client.AddressConfig;
import com.cien.securesocket.Server;
import com.cien.securesocket.ServerConnection;
import com.cien.server.routes.Chat;
import com.cien.server.routes.ChatReceiver;
import com.cien.server.routes.UploadImageRoute;
import com.cien.server.routes.DownloadImageRoute;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Cien
 */
public class ChatServer extends Server {
    
    public static void main(String[] args) throws IOException {
        ChatServer server = new ChatServer(AddressConfig.getAddress());
        server.start();
    }

    private final IOManager io = IOManager.get();
    private final Map<String, Route> routes = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, User> users = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, User> sessionIds = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Group> groups = Collections.synchronizedMap(new HashMap<>());
    
    public ChatServer(InetSocketAddress address) throws IOException {
        super(address);
        configure();
    }
    
    private void configure() {
        
        routes.put(Protocols.CHAT, new Chat());
        routes.put(Protocols.CHAT_RECEIVER, new ChatReceiver());
        routes.put(Protocols.IMAGE_RECEIVER, new DownloadImageRoute());
        routes.put(Protocols.IMAGE, new UploadImageRoute());
        
    }
    
    public Group getGroup(String name) {
        Group p = groups.get(name);
        if (p == null) {
            if (io.groupExists(name)) {
                p = new Group(name);
                setGroup(p);
                
                String[] users = io.getUsersOf(name);
                for (String user:users) {
                    User e = getUser(user);
                    if (e == null) {
                        e = new User(name, this);
                        setUser(e);
                    }
                    e.addToGroup(p);
                }
            }
        }
        return p;
    }
    
    public void setGroupToNull(String name) {
        groups.put(name, null);
    }
    
    public void setGroup(Group p) {
        groups.put(p.getName(), p);
    }
    
    public User getUser(String name) {
        User user = users.get(name);
        if (user == null) {
            if (io.userExists(name)) {
                user = new User(name, this);
                setUser(user);
                
                String[] groups = io.getGroupsOf(name);
                for (String group:groups) {
                    getGroup(group);
                }
            }
        }
        return user;
    }
    
    public User getUserBySession(String session) {
        return sessionIds.get(session);
    }
    
    public void setUserBySession(String session, User user) {
        sessionIds.put(session, user);
    }
    
    public void setUser(User s) {
        users.put(s.getName(), s);
    }
    
    @Override
    public void onDataReceived(ServerConnection c) throws Exception {
        String protocol = c.getProtocol();
        Route rt = routes.get(protocol);
        if (rt != null) {
            rt.onDataReceived(c, this);
        }
    }
    
}
