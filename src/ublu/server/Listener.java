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
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import ublu.util.Generics;
import ublu.util.Generics.StringArrayList;
import ublu.util.Generics.ThingArrayList;
import ublu.util.Interpreter;

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
    private Interpreter parentInterpreter;
    private boolean useSSL;

    /**
     *
     * @return true if SSL
     */
    public boolean isUseSSL() {
        return useSSL;
    }

    /**
     *
     * @param useSSL true if SSL
     */
    public final void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    /**
     *
     * @return default accept timeout for class
     */
    public static int getDEFAULT_ACCEPT_TIMEOUT_MS() {
        return DEFAULT_ACCEPT_TIMEOUT_MS;
    }

    /**
     *
     * @param DEFAULT_ACCEPT_TIMEOUT_MS default accept timeout for class
     */
    public static void setDEFAULT_ACCEPT_TIMEOUT_MS(int DEFAULT_ACCEPT_TIMEOUT_MS) {
        Listener.DEFAULT_ACCEPT_TIMEOUT_MS = DEFAULT_ACCEPT_TIMEOUT_MS;
    }

    /**
     *
     * @return
     */
    public Interpreter getParentInterpreter() {
        return parentInterpreter;
    }

    /**
     *
     * @param parentInterpreter
     */
    public void setParentInterpreter(Interpreter parentInterpreter) {
        this.parentInterpreter = parentInterpreter;
    }

    /**
     * Get the timeout before we recycle our accept().
     * <p>
     * We wait for a timeout to exit {@link #listen()} and then close the
     * socket.</p>
     *
     * @return the timeout before we recycle our accept()
     */
    public int getAcceptTimeoutMS() {
        return acceptTimeoutMS;
    }

    /**
     * Set the timeout before we recycle our accept()
     * <p>
     * We wait for a timeout to exit {@link #listen()} and then close the
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

    private String executionBlock;

    /**
     * Get the value of executionBlock
     *
     * @return the value of executionBlock
     */
    public String getExecutionBlock() {
        return executionBlock;
    }

    /**
     * Set the value of executionBlock
     *
     * @param executionBlock new value of executionBlock
     */
    public final void setExecutionBlock(String executionBlock) {
        this.executionBlock = executionBlock;
    }

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
     * Get Socket instance as SSL
     *
     * @return Socket instance
     */
    public SSLServerSocket getSSLServerSocket() {
        return SSLServerSocket.class.cast(serverSocket);
    }

    private StringArrayList getEnabledCipherSuites() {
        return new StringArrayList(getSSLServerSocket().getEnabledCipherSuites());
    }

    private void setEnabledCipherSuites(ThingArrayList tal) {
        getSSLServerSocket().setEnabledCipherSuites(tal.toStringArray());
    }

    private StringArrayList getEnabledProtocols() {
        return new StringArrayList(getSSLServerSocket().getEnabledProtocols());
    }

    private void setEnabledProtocols(ThingArrayList tal) {
        getSSLServerSocket().setEnabledProtocols(tal.toStringArray());
    }

    /**
     * Returns the SSLParameters in effect for newly accepted connections.
     *
     * @return the SSLParameters in effect for newly accepted connections.
     */
    public SSLParameters getSSLParameters() {
        return getSSLServerSocket().getSSLParameters();
    }

    /**
     * Applies SSLParameters to newly accepted connections.
     *
     * @param sslp the SSLParameters
     */
    public void setSSLParameters(SSLParameters sslp) {
        getSSLServerSocket().setSSLParameters(sslp);
    }

    /**
     * Returns the names of the cipher suites which could be enabled for use on
     * an SSL connection.
     *
     * @return the names of the cipher suites which could be enabled for use on
     * an SSL connection.
     */
    public StringArrayList getSupportedCipherSuites() {
        return new StringArrayList(getSSLServerSocket().getSupportedCipherSuites());
    }

    /**
     * Returns the names of the protocols which could be enabled for use.
     *
     * @return the names of the protocols which could be enabled for use.
     */
    public StringArrayList getSupportedProtocols() {
        return new StringArrayList(getSSLServerSocket().getSupportedProtocols());
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
        setUseSSL(false);
        setListening(false);
    }

    /**
     * Create new instance with associated Ublu instance and choice of
     * portnumber recorded.
     *
     * @param ublu application controller
     * @param portnum port to listen on
     */
    public Listener(Ublu ublu, int portnum) {
        this();
        this.ublu = ublu;
        this.portnum = portnum;
    }

    /**
     * Create new instance with associated Ublu instance and choice of
     * portnumber recorded and Interpreter to spawn server's interpreter from
     *
     * @param ublu application controller
     * @param portnum port to listen on
     * @param parentInterpreter Interpreter to spawn server's interpreter from
     */
    public Listener(Ublu ublu, int portnum, Interpreter parentInterpreter) {
        this();
        this.ublu = ublu;
        this.portnum = portnum;
        this.parentInterpreter = parentInterpreter;
    }

    /**
     * Create new instance with associated Ublu instance and choice of
     * portnumber recorded and Interpreter to spawn server's interpreter from,
     * possibly using SSL.
     *
     * @param ublu application controller
     * @param portnum port to listen on
     * @param parentInterpreter Interpreter to spawn server's interpreter from
     * @param useSSL true if SSL desired
     */
    public Listener(Ublu ublu, int portnum, Interpreter parentInterpreter, boolean useSSL) {
        this();
        setUseSSL(useSSL);
        this.ublu = ublu;
        this.portnum = portnum;
        this.parentInterpreter = parentInterpreter;
    }

    /**
     * Create new instance with associated Ublu instance and choice of
     * portnumber recorded.
     *
     * @param ublu application controller
     * @param portnum port to listen on
     * @param executionBlock block for server thread to execute
     */
    public Listener(Ublu ublu, int portnum, String executionBlock) {
        this();
        this.ublu = ublu;
        this.portnum = portnum;
        this.executionBlock = executionBlock;
    }

    /**
     * Create new instance with associated Ublu instance and choice of
     * portnumber recorded and Interpreter to spawn server's interpreter from
     *
     * @param ublu application controller
     * @param portnum port to listen on
     * @param executionBlock block for server thread to execute
     * @param parentInterpreter Interpreter to spawn server's interpreter from
     */
    public Listener(Ublu ublu, int portnum, String executionBlock, Interpreter parentInterpreter) {
        this();
        this.ublu = ublu;
        this.portnum = portnum;
        this.executionBlock = executionBlock;
        this.parentInterpreter = parentInterpreter;
    }

    /**
     * Create new instance with associated Ublu instance and choice of
     * portnumber recorded and Interpreter to spawn server's interpreter from
     * possibly using SSL.
     *
     * @param ublu application controller
     * @param portnum port to listen on
     * @param executionBlock block for server thread to execute
     * @param parentInterpreter Interpreter to spawn server's interpreter from
     * @param useSSL true if SSL socket desired
     */
    public Listener(Ublu ublu, int portnum, String executionBlock, Interpreter parentInterpreter, boolean useSSL) {
        this();
        setUseSSL(useSSL);
        this.ublu = ublu;
        this.portnum = portnum;
        this.executionBlock = executionBlock;
        this.parentInterpreter = parentInterpreter;
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
            setServerSocket(isUseSSL()
                    ? SSLServerSocketFactory.getDefault().createServerSocket(getPortnum())
                    : new ServerSocket(getPortnum())
            );
            getServerSocket().setSoTimeout(getAcceptTimeoutMS());
            try {
                while (listening) {
                    try {
                        Server s;
                        if (getExecutionBlock() != null) {
                            s = new Server(getUblu(), getServerSocket().accept(), getExecutionBlock(), parentInterpreter);
                        } else {
                            s = new Server(getUblu(), getServerSocket().accept(), parentInterpreter);
                        }
                        s.start();
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
