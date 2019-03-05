/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ashal.broadcast;

import static com.ashal.broadcast.DiscoveryConfig.DISCOVERY_PORT;
import static com.ashal.broadcast.DiscoveryConfig.DISCOVERY_REPLY;
import static com.ashal.broadcast.DiscoveryConfig.DISCOVERY_REQUEST;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mohammad
 */
public class DiscoveryServer implements Runnable {

    // how much data to accept from a broadcast client.
    private static final int MAX_PACKET_SIZE = 2048;
    private static final Logger logger;
    private DatagramSocket socket;

    /**
     * Set an environment variable for logging format. This is for 1-line
     * messages.
     */
    static {
        // %1=datetime %2=methodname %3=loggername %4=level %5=message
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %3$s %4$-7s %5$s%n");
        logger = Logger.getLogger("DiscoveryServer");
    }

    public static void main(String[] args) {
        DiscoveryServer server = new DiscoveryServer();
        server.run();
    }

    @Override
    public void run() {
        // quit if we get this many consecutive receive errors.
        // reset the counter after successfully receiving a packet.
        final int max_errors = 5;
        int errorCount = 0;

        // this is weak - address could be null or wrong address
        final String MY_IP = NetworkUtil.getMyAddress().getHostAddress();
        logger.info("My IP Address " + MY_IP);
        // Keep a socket open to listen to all UDP trafic that is
        // destined for this port.
        try {
            // 0.0.0.0 means all IPv4 address on this machine.
            // Probably the best address to listen on.
            InetAddress addr = InetAddress.getByName("0.0.0.0");
//			InetAddress addr = NetworkUtil.getMyAddress();
            socket = new DatagramSocket(DISCOVERY_PORT, addr);
            // set flag to enable receipt of broadcast packets
            socket.setBroadcast(true);
        } catch (Exception ex) {
            String msg = "Could not create UDP socket on port " + DISCOVERY_PORT;
            logger.log(Level.SEVERE, msg);
            System.err.println(msg);  // delete this after testing (redundant)
            return;
        }

        System.out.printf("Server listening on port %d\n", DISCOVERY_PORT);

        while (true) {
            // Receive a packet
            byte[] recvBuf = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            try {
                // wait for a packet
                socket.receive(packet);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, ioe.getMessage(), ioe);
                // this is to avoid infinite loops when exception is raised.
                errorCount++;
                if (errorCount >= max_errors) {
                    return;
                }
                // try again
                continue;
            }

            // Packet received
            errorCount = 0;    // reset error counter 
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            logger.info(String.format("Packet received from %s:%d",
                    clientAddress.getHostAddress(), clientPort));

            logger.info("Received data: " + new String(packet.getData()));

            // See if the packet holds the correct signature string
            String message = new String(packet.getData()).trim();
            if (message.startsWith(DISCOVERY_REQUEST)) {
                String reply = DISCOVERY_REPLY+" " + MY_IP;
                byte[] sendData = reply.getBytes();

                // Send the response
                DatagramPacket sendPacket = new DatagramPacket(sendData,
                        sendData.length, clientAddress, clientPort);
                try {
                    socket.setBroadcast(true);

                    listAllBroadcastAddresses().stream().forEach(new Consumer<InetAddress>() {
                        @Override
                        public void accept(InetAddress arg0) {
                            try {
                                DatagramPacket sendPacketBroadcast = new DatagramPacket(sendData,
                                        sendData.length, arg0, clientPort);
                                
                                socket.send(sendPacketBroadcast);
                                
                                logger.info(String.format("Reply sent to %s:%d",
                                        sendPacketBroadcast.getAddress().getHostAddress(), sendPacketBroadcast.getPort()));
                            } catch (IOException ex) {
                                Logger.getLogger(DiscoveryServer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Logger.getLogger(DiscoveryServer.class.getName()).log(Level.SEVERE, null, ex);
                }
//                try {
//                    socket.send(sendPacket);
//                    logger.info(String.format("Reply sent to %s:%d",
//                            clientAddress.getHostAddress(), clientPort));
//                } catch (IOException ioe) {
//                    logger.log(Level.SEVERE, "IOException sending service reply", ioe);
//                }
            } else {
                logger.info(String.format("Packet from %s:%d not a discovery packet",
                        clientAddress.getHostAddress(), clientPort));
            }
        }
    }

    static List<InetAddress> listAllBroadcastAddresses() throws SocketException {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            networkInterface.getInterfaceAddresses().stream()
                    .map(a -> a.getBroadcast())
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
        }
        return broadcastList;
    }
}
