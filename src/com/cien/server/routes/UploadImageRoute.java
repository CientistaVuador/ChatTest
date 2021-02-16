/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.server.routes;

import com.cien.server.Route;
import com.cien.server.subroutes.UploadImage;

/**
 *
 * @author Cien
 */
public class UploadImageRoute extends Route {

    public UploadImageRoute() {
        configure();
    }
    
    private void configure() {
        
        setSubRoute("uploadImage", new UploadImage());
        
    }
    
}
