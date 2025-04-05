/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 82, Beulah CO 81023-0082 http://www.softwoehr.com
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
import ublu.util.Generics.StringArrayList;

/**
 * Command to create an AS400 instance to be kept in a Tuple.
 *
 * @author jwoehr
 */
public class CmdAS400 extends Command {

    {
        setNameAndDescription("as400",
                "/4? [-to @var] [--,-as400,-from ~@var] [-usessl] [-ssl ~@tf] [-nodefault] [-new,-instance | -alive | -alivesvc ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -connectsvc ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -connectedsvc ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -connected | -disconnect | -disconnectsvc ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -ping sysname ~@{[ALL|CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -local | -validate | -qsvcport ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} | -svcport ~@{[CENTRAL|COMMAND|DATABASE|DATAQUEUE|FILE|PRINT|RECORDACCESS|SIGNON]} ~@portnum | -setaspgrp -@{aspgrp} ~@{curlib} ~@{liblist} | -svcportdefault | -proxy ~@{server[:portnum]} | -sockets ~@tf | -netsockets ~@tf | -vrm ] ~@{system} ~@{user} ~@{password} [~@{additionalAuthenticationFactor}] : instance, connect to, query connection, or disconnect from an as400 system");
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
         * test if running local on an i server
         */
        LOCAL,
        /**
         * Ping server or services
         */
        PING,
        /**
         * Query service port
         */
        QSVCPORT,
        /**
         * Set ASP Group
         */
        SETASPGRP,
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
         * Force use of sockets when running locally
         */
        USESOCKETS,
        /**
         * Force use of Internet domain sockets only when running locally
         */
        USENETSOCKETS,
        /**
         * Validate login
         */
        VALIDATE,
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
        String aspGroup = "";
        String curLib = "";
        String libList = "";
        boolean defaultServicePorts = true;
        boolean useSSL = false;
        boolean useSockets = false;
        boolean useNetSockets = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                case "-as400":
                case "-from":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-new":
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
                case "-local":
                    operation = OPERATIONS.LOCAL;
                    break;
                case "-ping":
                    operation = OPERATIONS.PING;
                    systemName = argArray.nextMaybeQuotationTuplePopString();
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-setaspgrp":
                    operation = OPERATIONS.SETASPGRP;
                    aspGroup = argArray.nextMaybeQuotationTuplePopString();
                    curLib = argArray.nextMaybeQuotationTuplePopString();
                    libList = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-svcport":
                    operation = OPERATIONS.SVCPORT;
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    servicePort = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-svcportdefault":
                    operation = OPERATIONS.SVCPORTDEFAULT;
                    break;
                case "-nodefault":
                    defaultServicePorts = false;
                    break;
                case "-proxy":
                    operation = OPERATIONS.PROXY;
                    proxyServer = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-qsvcport":
                    operation = OPERATIONS.QSVCPORT;
                    serviceName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-ssl":
                    useSSL = argArray.nextTupleOrPop().getValue().equals(true);
                    break;
                case "-sockets":
                    operation = OPERATIONS.USESOCKETS;
                    useSockets = argArray.nextTupleOrPop().getValue().equals(true);
                    break;
                case "-netsockets":
                    operation = OPERATIONS.USENETSOCKETS;
                    useNetSockets = argArray.nextTupleOrPop().getValue().equals(true);
                    break;
                case "-usessl":
                    useSSL = true;
                    break;
                case "-validate":
                    operation = OPERATIONS.VALIDATE;
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
                        getLogger().log(Level.INFO, locMsg("AS400_object") + " {0} already instanced.", getAs400());
                        try {
                            put(getAs400());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    locMsg("Encountered_an_exception") + " " + locMsg("putting_the_AS400_object") + " " + getAs400() + " " + locMsg("to_the_destination_datasink") + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        try {
                            setAs400(instanceAS400(argArray, useSSL));
                        } catch (PropertyVetoException | AS400SecurityException | IOException ex) {
                            getLogger().log(Level.SEVERE,
                                    locMsg("Encountered_an_exception") + " " + locMsg("instancing_the_AS400_object") + inNameAndDescription(), ex);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                if (defaultServicePorts) // defaults to true
                                // However, this is undesirable if application wants to set service ports
                                // or use native access on IBM i.
                                {
                                    getAs400().setServicePortsToDefault();
                                }
                                put(getAs400());
                            } catch (RequestNotSupportedException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE,
                                        locMsg("Encountered_an_exception") + " " + locMsg("putting_the_AS400_object") + " " + getAs400() + " " + locMsg("to_the_destination_datasink") + inNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case ALIVE:
                    if (getAs400() != null) {
                        try {
                            put(getAs400().isConnectionAlive());
                        } catch (RequestNotSupportedException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE,
                                    locMsg("Encountered_an_exception") + " " + locMsg("putting") + " the connection alive state of " + locMsg("AS400_object") + " " + getAs400() + " " + locMsg("to_the_destination_datasink") + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of {0} {1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
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
                                getLogger().log(Level.SEVERE, locMsg("Encountered_an_exception") + " " + "querying connection to service " + serviceName + inNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "{0} {1} provided to query live connection to service {2}", new Object[]{locMsg("No"), locMsg("AS400_object"), inNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to query live connection to service {0}", inNameAndDescription());
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
                                getLogger().log(Level.SEVERE, "Exception connecting to service " + inNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "{0} {1} provided to connect service {2}", new Object[]{locMsg("No"), locMsg("AS400_object"), inNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to connect service {0}", inNameAndDescription());
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
                                getLogger().log(Level.SEVERE, "Exception querying connection to service " + serviceName + inNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "{0} {1} provided to query connection to service {2}", new Object[]{locMsg("No"), locMsg("AS400_object"), inNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to  query connection to service {0}", inNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CONNECTED:
                    if (getAs400() != null) {
                        try {
                            put(getAs400().isConnected());
                        } catch (RequestNotSupportedException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting the connected state of " + locMsg("AS400_object") + getAs400() + " " + locMsg("to_the_destination_datasink") + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of{0}{1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DISCONNECT:
                    if (getAs400() != null) {
                        getAs400().disconnectAllServices();
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of {0}{1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DISCONNECTSVC:
                    serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    if (serviceInteger != null) {
                        if (getAs400() != null) {
                            getAs400().disconnectService(serviceInteger);
                        } else {
                            getLogger().log(Level.SEVERE, "{0} {1} provided to disconnect service {2}", new Object[]{locMsg("No"), locMsg("AS400_object"), inNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to disconnect service {0}", inNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case LOCAL:
                    if (getAs400() != null) {
                        try {
                            put(getAs400().isLocal());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn''t put result of -local {0}", inNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of {0} for -local {1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
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
                            getLogger().log(Level.SEVERE, "Couldn''t put result of AS400 ping {0}", inNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to ping service {0}", inNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case QSVCPORT:
                    serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    if (serviceInteger != null) {
                        if (getAs400() != null) {
                            try {
                                put(getAs400().getServicePort(serviceInteger));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception putting service port number", ex);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No instance of {0} to set service port {1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to service port {0}", inNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SETASPGRP:
                    if (getAs400() != null) {
                        StringArrayList libListArray = new StringArrayList(libList);
                        try {
                            getAs400().setIASPGroup(aspGroup, curLib, libListArray.toStringArray());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | PropertyVetoException ex) {
                            getLogger().log(Level.SEVERE, "Error setting ASP group / current library / library list " + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of {0} to set ASP group / current library / library list {1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SVCPORT:
                    serviceInteger = AS400Factory.serviceNameToInteger(serviceName);
                    if (serviceInteger != null) {
                        if (getAs400() != null) {
                            getAs400().setServicePort(serviceInteger, servicePort);
                        } else {
                            getLogger().log(Level.SEVERE, "No instance of {0} to set service port {1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Unknown service name provided to service port {0}", inNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SVCPORTDEFAULT:
                    if (getAs400() != null) {
                        getAs400().setServicePortsToDefault();
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of {0} to set service ports to default {1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case PROXY:
                    if (getAs400() != null) {
                        try {
                            getAs400().setProxyServer(proxyServer);
                        } catch (PropertyVetoException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't set proxy server " + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "{0} {1} provided to proxy {2}", new Object[]{locMsg("No"), locMsg("AS400_object"), inNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case USESOCKETS:
                    if (getAs400() != null) {
                        getAs400().setMustUseSockets(useSockets);
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of {0} for -sockets {1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case USENETSOCKETS:
                    if (getAs400() != null) {
                        getAs400().setMustUseNetSockets(useNetSockets);
                    } else {
                        getLogger().log(Level.SEVERE, "No instance of {0} for -netsockets {1}", new Object[]{locMsg("AS400_object"), inNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case VALIDATE:
                    if (getAs400() != null) {
                        try {
                            put(getAs400().validateSignon());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't validate login in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "{0} {1} provided to validate login {2}", new Object[]{locMsg("No"), locMsg("AS400_object"), inNameAndDescription()});
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
                        getLogger().log(Level.SEVERE, "{0} {1} provided to VRM {2}", new Object[]{locMsg("No"), locMsg("AS400_object"), inNameAndDescription()});
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
     * @param useSSL true if SSL should be used
     * @return the AS400 instance or null
     * @throws PropertyVetoException
     * @throws com.ibm.as400.access.AS400SecurityException
     * @throws java.io.IOException
     */
    public AS400 instanceAS400(ArgArray args, boolean useSSL) throws PropertyVetoException, AS400SecurityException, IOException {
        AS400 as400 = null;
        if (args.size() < 3) {
            logArgArrayTooShortError(args);
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            as400 = useSSL
                    ? as400FromArgs(args, AS400Factory.SIGNON_SECURITY_TYPE.SSL)
                    : as400FromArgs(args);
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
