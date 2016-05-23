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

import ublu.AS400Factory;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JPing;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to create an AS400 instance to be kept in a Tuple.
 *
 * @author jwoehr
 */
public class CmdAS400 extends Command {

    {
        setNameAndDescription("as400",
                "/3? [-to @var] [--,-as400,-from @var] [-instance | -alive | -alivesvc ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -connectsvc ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -connectedsvc ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -connected | -disconnect | -disconnectsvc ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -ping sysname ~@{[ALL|CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -svcport ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} ~@portnum | -svcportdefault | -proxy ~@{server[:portnum]} | -vrm ] ~@{system} ~@{user} ~@{password} : instance, connect to, query connection, or disconnect from an as400 system");
    }

    /**
     * Operations we know
     */
    protected enum OPERATIONS {

        /**
         * Create instance
         */
        INSTANCE,
        /**
         * Are any connections alive?
         */
        ALIVE,
        /**
         * Is the connection to the specified service alive?
         */
        ALIVESVC,
        /**
         * Connect to service
         */
        CONNECTSVC,
        /**
         * Is connected to specific service?
         */
        CONNECTEDSVC,
        /**
         * Is connected at all?
         */
        CONNECTED,
        /**
         * Disconnect
         */
        DISCONNECT,
        /**
         * Disconnect from specific service
         */
        DISCONNECTSVC,
        /**
         * Ping server or services
         */
        PING,
        /**
         * Indicate server port for service
         */
        SVCPORT,
        /**
         * Set ports to default
         */
        SVCPORTDEFAULT,
        /**
         * Indicate JTOpen Proxy server
         */
        PROXY,
        /**
         * Get version/revision/mod of server
         */
        VRM
    }

    /**
     * Create an AS400 instance to be kept in a Tuple and perform control
     * operations thereupon.
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray as400(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.INSTANCE; // the default
        String systemName = "";
        String serviceName = "";
        String proxyServer = "";
        int servicePort = -1;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "--":
                case "-as400":
                case "-from":
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-alive":
                    operation = OPERATIONS.ALIVE;
                    break;
                case "-alivesvc":
                    operation = OPERATIONS.CONNECTEDSVC;
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-connectsvc":
                    operation = OPERATIONS.CONNECTSVC;
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-connectedsvc":
                    operation = OPERATIONS.CONNECTEDSVC;
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-connected":
                    operation = OPERATIONS.CONNECTED;
                    break;
                case "-disconnect":
                    operation = OPERATIONS.DISCONNECT;
                    break;
                case "-disconnectsvc":
                    operation = OPERATIONS.DISCONNECTSVC;
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-ping":
                    operation = OPERATIONS.PING;
                    systemName = argArray.nextMaybeQuotationTuplePopString();
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-svcport":
                    operation = OPERATIONS.SVCPORT;
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    servicePort = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-svcportdefault":
                    operation = OPERATIONS.SVCPORTDEFAULT;
                    break;
                case "-proxy":
                    operation = OPERATIONS.PROXY;
                    proxyServer = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-vrm":
                    operation = OPERATIONS.VRM;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Integer serviceInteger;
            switch (operation) {
                case INSTANCE:
                    if (getAs400() != null) {
                        getLogger().log(Level.INFO, "AS400 object {0} already instanced.", getAs400());
                        try {
                            put(getAs400());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting the AS400 object " + getAs400() + " to the destination datasink in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        try {
                            setAs400(instanceAS400(argArray));
                            put(getAs400());
                        } catch (PropertyVetoException | RequestNotSupportedException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting the AS400 object " + getAs400() + " to the destination datasink in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case ALIVE:
                    if (getAs400() != null) {
                        try {
                            put(getAs400().isConnectionAlive());
                        } catch (RequestNotSupportedException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting the connection alive state of AS400 object " + getAs400() + " to the destination datasink in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of AS400 object in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case ALIVESVC:
                    serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    if (serviceInteger != null) {
                        if (getAs400() != null) {
                            try {
                                put(getAs400().isConnectionAlive(serviceInteger));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception querying connection to service " + serviceName + " in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No AS400 object provided to query live connection to service in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to  query live connection to service in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CONNECTSVC:
                    serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    if (serviceInteger != null) {
                        if (getAs400() != null) {
                            try {
                                getAs400().connectService(serviceInteger);
                            } catch (AS400SecurityException | IOException ex) {
                                getLogger().log(Level.SEVERE, "Exception connecting to service in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No AS400 object provided to connect service in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to connect service in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CONNECTEDSVC:
                    serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    if (serviceInteger != null) {
                        if (getAs400() != null) {
                            try {
                                put(getAs400().isConnected(serviceInteger));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception querying connection to service " + serviceName + " in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No AS400 object provided to query connection to service in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to  query connection to service in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CONNECTED:
                    if (getAs400() != null) {
                        try {
                            put(getAs400().isConnected());
                        } catch (RequestNotSupportedException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting the connected state of AS400 object " + getAs400() + " to the destination datasink in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of AS400 object in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DISCONNECT:
                    if (getAs400() != null) {
                        getAs400().disconnectAllServices();
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of AS400 object in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DISCONNECTSVC:
                    serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    if (serviceInteger != null) {
                        if (getAs400() != null) {
                            getAs400().disconnectService(serviceInteger);
                        } else {
                            getLogger().log(Level.SEVERE, "No AS400 object provided to disconnect service in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to disconnect service in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case PING:
                    if (serviceName.toUpperCase().equals("ALL")) {
                        serviceInteger = AS400JPing.ALL_SERVICES;
                    } else {
                        serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    }
                    if (serviceInteger != null) {
                        AS400JPing jping = new AS400JPing(systemName, serviceInteger);
                        try {
                            put(jping.ping());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put result of AS400 ping in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to ping service in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SVCPORT:
                    serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    if (serviceInteger != null) {
                        if (getAs400() != null) {
                            getAs400().setServicePort(serviceInteger, servicePort);
                        } else {
                            getLogger().log(Level.SEVERE, "No instance of AS400 object to set service port in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to service port in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SVCPORTDEFAULT:
                    if (getAs400() != null) {
                        getAs400().setServicePortsToDefault();
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of AS400 object to set service ports to default in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case PROXY:
                    if (getAs400() != null) {
                        try {
                            getAs400().setProxyServer(proxyServer);
                        } catch (PropertyVetoException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't set proxy server " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400 object provided to proxy in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }

                    break;
                case VRM:
                    if (getAs400() != null) {
                        try {
                            put(Integer.toHexString((getAs400().getVRM())));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put result of AS400 VRM in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400 object provided to VRM in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }
        return argArray;
    }

    /**
     * Create the AS400 instance after checking that we have enough args.
     *
     * @param args the argument array
     * @return the AS400 instance or null
     * @throws PropertyVetoException
     */
    public AS400 instanceAS400(ArgArray args) throws PropertyVetoException {
        AS400 as400 = null;
        if (args.size() < 3) {
            logArgArrayTooShortError(args);
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            as400 = as400FromArgs(args);
        }
        return as400;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return as400(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
