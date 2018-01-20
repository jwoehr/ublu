/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 51, Golden CO 80402-0051 http://www.softwoehr.com
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

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import ublu.server.Listener;
import ublu.util.ArgArray;
import java.util.logging.Level;
import ublu.util.Tuple;

/**
 * Command to manage a thread server that interprets over tcpip
 *
 * @author jwoehr
 */
public class CmdServer extends Command {

    {
        setNameAndDescription("server",
                "/0  [-to datasink] [-- @listener]  [-port ~@{portnum}] [-usessl ] [-ssl @~t/f] [[ -block ~@{executionBlock} | $[execution block]$ ] | -getport | -start | -status | -stop ] : start, stop or monitor status of a thread server");
    }

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
        STATUS,
        /**
         * get port the listener is on
         */
        GETPORT
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

    private Tuple listenerTuple = null;

    /**
     * Carry out the server command to launch or stop or manage the tcpip thread
     * server
     *
     * @param argArray passed-in args
     * @return what's left of args
     */
    public ArgArray server(ArgArray argArray) {
        Listener listener = null;
        int port = DEFAULT_SERVER_PORT;
        int timeoutMs = Listener.DEFAULT_ACCEPT_TIMEOUT_MS;
        boolean useSSL = false;
        String executionBlock = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                    listener = argArray.nextTupleOrPop().value(Listener.class);
                    break;
                case "-block":
                    if (argArray.isNextTupleNameOrPop() || argArray.isNextQuotation()) {
                        executionBlock = argArray.nextMaybeQuotationTuplePopString();
                    } else {
                        executionBlock = argArray.nextUnlessNotBlock();
                    }
                    break;
                case "-getport":
                    setFunction(FUNCTIONS.GETPORT);
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
                    port = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-usessl":
                    useSSL = true;
                    break;
                case "-ssl":
                    useSSL = argArray.nextBooleanTupleOrPop();
                    break;
                case "-timeout":
                    timeoutMs = argArray.nextInt();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (getFunction()) {
                case GETPORT:
                    if (listener != null) {
                        try {
                            put(listener.getPortnum());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting Listener portnum in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No listener provided to getport in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case START:
                    if (executionBlock != null) {
                        listener = new Listener(getUblu(), port, executionBlock, getInterpreter(), useSSL);
                    } else {
                        listener = new Listener(getUblu(), port, getInterpreter(), useSSL);
                    }
                    listener.setAcceptTimeoutMS(timeoutMs);
                    listener.start();
                    try {
                        put(listener);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception putting Listener in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case STATUS:
                    getInterpreter().output((listener == null ? "No listener provided." : listener.status()) + "\n");
                    break;
                case STOP:
                    if (listener != null) {
                        listener.setListening(false);
                    } else {
                        getLogger().log(Level.SEVERE, "No listener provided to stop in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }
        return argArray;
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
