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
import ublu.util.Interpreter;
import ublu.util.Parser;
import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A socket server for access to an Ublu interpreter thread
 *
 * @author jwoehr
 */
public class Server extends Thread {

    private Socket socket;
    private Ublu ublu;

    /**
     * Get the socket the thread is hooked to
     *
     * @return the socket the thread is hooked to
     */
    protected final Socket getSocket() {
        return socket;
    }

    /**
     * Set the socket the thread is hooked to
     *
     * @param socket the socket the thread is hooked to
     */
    protected final void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Associated Ublu instance
     *
     * @return Associated Ublu instance
     */
    protected final Ublu getUblu() {
        return ublu;
    }

    /**
     * Associated Ublu instance
     *
     * @param ublu Associated Ublu instance
     */
    protected final void setUblu(Ublu ublu) {
        this.ublu = ublu;
    }

    /**
     * Associated logger
     *
     * @return Associated logger
     */
    protected Logger getLogger() {
        return getUblu().getLogger();
    }

    /**
     * Arity\0 ctor sets a thread name
     */
    protected Server() {
        super("Ublu Server");
    }

    /**
     * Ctor sets associated Ublu and socket
     *
     * @param ublu
     * @param socket
     */
    public Server(Ublu ublu, Socket socket) {
        this();
        setUblu(ublu);
        setSocket(socket);
    }

    /**
     * Read one command line for the interpreter
     *
     * @return
     * @throws IOException
     */
    protected String readCommandLine() throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                getSocket().getInputStream()));
        return br.readLine();
    }

    /**
     * interactive loop until bye
     *
     * @throws IOException
     */
    protected void serverInterpret() throws IOException {
        Interpreter i = new Interpreter(getUblu());
        i.setOutputStream(new PrintStream(getSocket().getOutputStream()));
        while (!i.isGoodBye()) {
            String s = readCommandLine();
            if (s != null) {
                i.setArgArray(new Parser(i, s).parseAnArgArray());
                i.loop();
            }
        }
    }

    @Override
    public void run() {
        try {
            serverInterpret();
            if (!getSocket().isClosed() && !getSocket().isInputShutdown()) {
                getSocket().getInputStream().close();
            }

            if (!getSocket().isClosed() && !getSocket().isOutputShutdown()) {
                getSocket().getOutputStream().close();
            }

            if (!getSocket().isClosed()) {
                getSocket().close();
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "I/O exception running server", e);
        }
    }
}
