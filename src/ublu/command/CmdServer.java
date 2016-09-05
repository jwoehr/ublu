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
package ublu.command;

import ublu.Ublu;
import ublu.server.Listener;
import ublu.util.ArgArray;
import java.util.logging.Level;

/**
 * Command to manage a thread server that interprets over tcpip
 *
 * @author jwoehr
 */
public class CmdServer extends Command {

    {
        setNameAndDescription("server",
                "/0  [ -block $[execution block]$ ] -start | -status | -stop [-port portnum] : start, stop or monitor status of a thread server that accepts command lines");    }

    /**
     * Functions the server command knows
     */
    protected static enum FUNCTIONS {

        /**
         * Start the server
         */
        START,
        /**
         * Stop the server
         */
        STOP,
        /**
         * Report status
         */
        STATUS
    }
    private FUNCTIONS function;

    private FUNCTIONS getFunction() {
        return function;
    }

    private void setFunction(FUNCTIONS function) {
        this.function = function;
    }
    /**
     * By default we're on port 43860
     */
    public final static int DEFAULT_SERVER_PORT = 0xab54;

    /**
     * Arity-0 ctor
     */
    public CmdServer() {
    }

    /**
     * Carry out the server command to launch or stop or manage the tcpip thread
     * server
     *
     * @param args passed-in args
     * @return what's left of args
     */
    public ArgArray server(ArgArray args) {
        int port = DEFAULT_SERVER_PORT;
        int timeoutMs = Listener.DEFAULT_ACCEPT_TIMEOUT_MS;
        String executionBlock = null;
        while (args.hasDashCommand()) {
            String dashCommand = args.parseDashCommand();
            switch (dashCommand) {
                case "-block":
                    executionBlock = args.nextUnlessNotBlock();
                    break;
                case "-start":
                    setFunction(FUNCTIONS.START);
                    break;
                case "-stop":
                    setFunction(FUNCTIONS.STOP);
                    break;
                case "-status":
                    setFunction(FUNCTIONS.STATUS);
                    break;
                case "-port":
                    port = args.nextInt();
                    break;
                case "-timeout":
                    timeoutMs = args.nextInt();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Listener l = Ublu.getSingletonListener();
            switch (getFunction()) {
                case START:
                    if (l != null) {
                        getLogger().log(Level.WARNING, "Server is already running");
                    } else {
                        if (executionBlock != null) {
                            getUblu().newListener(port, executionBlock);
                        } else {
                            getUblu().newListener(port);
                        }
                        Ublu.getSingletonListener().setAcceptTimeoutMS(timeoutMs);
                        Ublu.getSingletonListener().start();
                    }
                    break;
                case STATUS:
                    getInterpreter().output((l == null ? "No server is active." : l.status()) + "\n");
                    break;
                case STOP:
                    Ublu.stopListener();
                    break;
            }
        }
        return args;
    }

    @Override
    public void reinit() {
        super.reinit();
        setFunction(FUNCTIONS.STATUS);
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return server(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
