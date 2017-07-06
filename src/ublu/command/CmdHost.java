/*
 * Copyright (c) 2015, Jack J. Woehr jax@well.com po box 51 golden co 80402-0051
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

import ublu.util.ArgArray;
import ublu.smapi.Host;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to create an instance of a z/VM SMAPI host.
 *
 * @author jax
 */
public class CmdHost extends Command {

    static {
        setNameAndDescription("host",
                "/3 [-to @var] [-new,-instance] [-port ~@{portnum}] [-ssl ~@tf] [-usessl] ~@{hostname} ~@{user} ~@{password} : instance a smapi host, default port 44444");
    }

    /**
     * Operations we know
     */
    protected enum OPERATIONS {

        /**
         * Create instance
         */
        INSTANCE
    }

    /**
     * Create a Host instance to be kept in a Tuple and perform operations
     * thereupon.
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray cmdHost(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.INSTANCE; // the default
        Host host = null;
        Tuple hostTuple = null;
        String hostname = null;
        String username = null;
        String password = null;
        Integer port = 44444;
        boolean usessl = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-new":
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-port":
                    port = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-ssl":
                    usessl = argArray.nextTupleOrPop().getValue().equals(true);
                    break;
                case "-usessl":
                    usessl = true;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (hostTuple == null) {
                hostname = argArray.nextMaybeQuotationTuplePopString();
                username = argArray.nextMaybeQuotationTuplePopString();
                password = argArray.nextMaybeQuotationTuplePopString();
            } else {
                host = getHostFromTuple(hostTuple);
                if (host == null) {
                    getLogger().log(Level.SEVERE, "Null host object provided to {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (getCommandResult() != COMMANDRESULT.FAILURE) {
                switch (operation) {
                    case INSTANCE:
                        if (host != null) {
                            getLogger().log(Level.INFO, "Host object {0} already instanced.", host);
                        } else {
                            host = new Host(hostname, port, username, password, usessl);
                        }
                        try {
                            put(host);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting the Host object " + host + " to the destination datasink in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                }
            }
        }
        return argArray;
    }

    private Host getHostFromTuple(Tuple t) {
        Host h = null;
        Object o = t.getValue();

        if (o instanceof Host) {
            h = Host.class
                    .cast(o);
        }
        return h;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdHost(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }

}
