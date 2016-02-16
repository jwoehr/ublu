/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ublu.server;

import ublu.Ublu;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Socket listener to serve up connections to new interpreter instance
 *
 * @author jwoehr
 */
public class Listener extends Thread {

    /**
     *
     */
    public static int DEFAULT_ACCEPT_TIMEOUT_MS = 4000;
    private int acceptTimeoutMS = DEFAULT_ACCEPT_TIMEOUT_MS;
    private int spawns = 0;

    /**
     * Get the timeout before we recycle our accept().
     * <p>We wait for a timeout to exit {@link listen()} and then close the
     * socket.</p>
     *
     * @return the timeout before we recycle our accept()
     */
    public int getAcceptTimeoutMS() {
        return acceptTimeoutMS;
    }

    /**
     * Set the timeout before we recycle our accept()
     * <p>We wait for a timeout to exit {@link listen()} and then close the
     * socket.</p>
     *
     * @param acceptTimeoutMS the timeout before we recycle our accept()
     */
    public void setAcceptTimeoutMS(int acceptTimeoutMS) {
        this.acceptTimeoutMS = acceptTimeoutMS;
    }

    /**
     * Get count of {@link Server} threads spawned.
     *
     * @return count of {@link Server} threads spawned
     */
    public int getSpawns() {
        return spawns;
    }

    /**
     * Set count of {@link Server} threads spawned.
     *
     * @param spawns count of {@link Server} threads spawned
     */
    protected void setSpawns(int spawns) {
        this.spawns = spawns;
    }

    /**
     * Bump the count of spawns by 1.
     */
    protected void incSpawns() {
        spawns++;
    }

    /**
     * Return a status message
     *
     * @return a status message
     */
    public String status() {
        StringBuilder sb = new StringBuilder();
        sb.append("Listener ");
        sb.append(this);
        if (isListening()) {
            sb.append(" is listening on port ").append(getPortnum()).append(".\n");
            sb.append("Total ").append(getSpawns()).append(" connections have been made.");
        } else {
            sb.append("is not active.");
        }
        return sb.toString();
    }
    private Ublu ublu;
    private int portnum;
    private ServerSocket serverSocket;
    private boolean listening;

    /**
     * Get associated instance.
     *
     * @return associated instance
     */
    protected Ublu getUblu() {
        return ublu;
    }

    /**
     * Set associated instance.
     *
     * @param ublu associated instance
     */
    protected void setUblu(Ublu ublu) {
        this.ublu = ublu;
    }

    /**
     * Get portnum we are/will listen on.
     *
     * @return portnum we are/will listen on
     */
    public int getPortnum() {
        return portnum;
    }

    /**
     * Set portnum we are/will listen on.
     *
     * @param portnum portnum we are/will listen on
     */
    protected void setPortnum(int portnum) {
        this.portnum = portnum;
    }

    /**
     * Get Socket instance
     *
     * @return Socket instance
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * Set Socket instance
     *
     * @param serverSocket Socket instance
     */
    protected void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * True if currently listening, false otherwise.
     *
     * @return True if currently listening, false otherwise
     */
    public boolean isListening() {
        return listening;
    }

    /**
     * Set flag that we are listening.
     *
     * @param listening True if currently listening, false otherwise
     */
    public final void setListening(boolean listening) {
        this.listening = listening;
    }

    /**
     * Create instance and set listening false/
     */
    protected Listener() {
        setListening(false);
    }

    /**
     * Create new instance with associated Ublu instance and choice of portnumber
     * recorded.
     *
     * @param ublu
     * @param portnum
     */
    public Listener(Ublu ublu, int portnum) {
        this();
        this.ublu = ublu;
        this.portnum = portnum;
    }

    /**
     * Get associated logger
     *
     * @return associated logger
     */
    protected Logger getLogger() {
        return getUblu().getLogger();
    }

    /**
     * Port listening loop, spawns {@link Server} threads which interpret
     * commands.
     */
    protected void listen() {
        setListening(true);
        try {
            setServerSocket(new ServerSocket(getPortnum()));
            getServerSocket().setSoTimeout(getAcceptTimeoutMS());
            try {
                while (listening) {
                    try {
                        new Server(getUblu(), getServerSocket().accept()).start();
                        incSpawns(); // If we get here without timeout exception there has been a spawn
                    } catch (SocketTimeoutException ex) {
                        // we don't care
                    }
                }
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Error spawning Server", ex);
            } finally {
                try {
                    getServerSocket().close();
                } catch (IOException ex) {
                    getLogger().log(Level.WARNING, "Error closing socket", ex);
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Listener could not listen on port: " + getPortnum() + ".", ex);

        }
    }

    @Override
    public void run() {
        listen();
    }
}