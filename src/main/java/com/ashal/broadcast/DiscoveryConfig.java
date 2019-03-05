/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ashal.broadcast;

/**
 *
 * @author mohammad
 */
public class DiscoveryConfig {

    /**
     * Port used for broadcast and listening.
     */
    public static final int DISCOVERY_PORT = 8888;
    /**
     * String the client sends, to disambiguate packets on this port.
     */
    public static final String DISCOVERY_REQUEST = "Where-are-you-ashal?";
    /**
     * Prefix string that server sends along with his IP address.
     */
    public static final String DISCOVERY_REPLY = "ASHAL_SERVER_IP ";
}
