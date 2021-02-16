/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.routes;

import com.cien.server.Route;
import com.cien.server.subroutes.DownloadImage;

/**
 *
 * @author Cien
 */
public class DownloadImageRoute extends Route {
    
    public DownloadImageRoute() {
        configure();
    }
    
    private void configure() {
        
        setSubRoute("downloadImage", new DownloadImage());
        
    }
    
}
