package com.avrix.api.client;

import java.net.InetSocketAddress;

import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;

/**
 * A set of tools for managing the client.
 */
public class ClientUtils {

    /**
     * Returns the current client.
     *
     * @return The current GameClient.
     */
    public static GameClient getClient() {
        return GameClient.instance;
    }

    /**
     * Returns the current client's UDP engine.
     *
     * @return The current client's UDP engine.
     */
    public static UdpConnection getUdpConnection() {
        return GameClient.connection;
    }

    /**
     * Returns the IP address of the connected server.
     *
     * @return The IP address of the connected server.
     */
    public static String getConnectedServerIP() {
        String serverIP = getUdpConnection().getInetSocketAddress().getHostString();
        return serverIP;
    }

    /**
     * Returns the port of the connected server.
     *
     * @return The port of the connected server.
     */
    public static int getConnectedServerPort() {
        int port = getUdpConnection().getInetSocketAddress().getPort();
        return port;
    }

    /**
     * Returns the InetSocketAddress of the client.
     *
     * @return The InetSocketAddress of the client.
     */
    public static InetSocketAddress getClientInetSocketAddress() {
        InetSocketAddress address= getUdpConnection().getInetSocketAddress();
        return address;
    }

    /**
     * Returns the Average Ping of the client.
     *
     * @return The average ping of the client.
     */
    public static int getAveragePing() {
        return getUdpConnection().getAveragePing();
    }

    /**
     * Returns the Last Ping of the client.
     *
     * @return The last ping of the client.
     */
    public static int getLastPing() {
        return getUdpConnection().getLastPing();
    }

    /**
     * Checks if the client is fully connected.
     *
     * @return True if the client is fully connected, false otherwise.
     */
    public static boolean isFullyConnected() {
        return getUdpConnection().isFullyConnected();
    }

    /**
     * Returns the Connection Type of the client.
     *
     * @return The Connection Type of the client.
     */
    public static int getConnectionType() {
        return getUdpConnection().getPeer().GetConnectionType(getUdpConnection().getConnectedGUID());
    }
}
