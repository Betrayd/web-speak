package net.betrayd.webspeak.impl;

import net.betrayd.webspeak.WebSpeakServer;

/**
 * A "backend type" that a webspeak server can use. This will handle
 * implementing Jetty and manage player connections.
 */
public interface ServerBackend {
    /**
     * Get the webspeak server this backend belongs to.
     * @return WebSpeak server.
     */
    public WebSpeakServer getServer();

    /**
     * Start the server backend on a specified port, and block until it has started.
     * @param port Port to start on.
     * @throws Exception If something goes wrong during startup.
     */
    public void start(int port) throws Exception;

    /**
     * Stop the server, and block until it has fully stopped.
     * @throws Exception If something goes wrong during shutdown.
     */
    public void stop() throws Exception;

    public boolean isRunning();

    /**
     * Get the port this server was started on.
     * @return The port, or <code>-1</code> if it hasn't been started yet.
     */
    public int getPort();
}
