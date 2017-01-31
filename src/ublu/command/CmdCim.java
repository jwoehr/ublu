/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.cim.CIMObjectPath;
import javax.cim.CIMProperty;
import javax.security.auth.Subject;
import javax.wbem.WBEMException;
import javax.wbem.client.PasswordCredential;
import javax.wbem.client.UserPrincipal;
import ublu.util.ArgArray;
import ublu.util.CimUbluHelper;
import ublu.util.Generics.CIMObjectPathArrayList;

/**
 *
 * @author jax
 */
public class CmdCim extends Command {

    {
        setNameAndDescription("cim", "/0 [-to datasink] [--,-cim @ciminstance] [-keys ~@propertyKeyArray] [-namespace ~@{namespace}] [-objectname ~@{objectname}] [-url ~@{https://server:port}] [-xmlschema ~@{xmlschemaname}] [-new | -close | -path | -cred ~@{user} ~@{password} | -init ~@cimobjectpath | -ec  ~@cimobjectpath ~@deep_tf | -ei  ~@cimobjectpath] : CIM client");

    }

    /**
     * the operations we know
     */
    protected enum OPS {
        /**
         * Create the CIM Helper
         */
        INSTANCE,
        /**
         * Set path
         */
        PATH,
        /**
         * close client
         */
        CLOSE,
        /**
         * Set credentials
         */
        CRED,
        /**
         * Initialize the client
         */
        INIT,
        /**
         * Enumerate classes, instance names
         */
        EC, EI
    }

    public ArgArray doCim(ArgArray argArray) {
        OPS op = OPS.INSTANCE;
        URL url = null;
        String pNamespace = null;
        String pObjectName = null;
        CIMObjectPath objectPath = null;
        CIMProperty<?>[] pKeys = null;
        String pXmlSchemaName = null;
        CimUbluHelper cimUbluHelper = null;
        String user = null;
        String password = null;
        Boolean pDeep = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                case "-cim":
                    cimUbluHelper = argArray.nextTupleOrPop().value(CimUbluHelper.class);
                    break;
                case "-new":
                    op = OPS.INSTANCE;
                    break;
                case "-close":
                    op = OPS.CLOSE;
                    break;
                case "-namespace":
                    pNamespace = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-objectname":
                    pObjectName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-keys":
                    pKeys = argArray.nextTupleOrPop().value(CIMProperty[].class);
                    break;
                case "-url": {
                    try {
                        url = new URL(argArray.nextMaybeQuotationTuplePopStringTrim());
                    } catch (MalformedURLException ex) {
                        getLogger().log(Level.SEVERE, "Error parsing URL in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
                break;
                case "-xmlschema":
                    pXmlSchemaName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-path":
                    op = OPS.PATH;
                    break;
                case "-cred":
                    op = OPS.CRED;
                    user = argArray.nextMaybeQuotationTuplePopStringTrim();
                    password = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-init":
                    op = OPS.INIT;
                    objectPath = argArray.nextTupleOrPop().value(CIMObjectPath.class);
                    break;
                case "-ec":
                    op = OPS.EC;
                    objectPath = argArray.nextTupleOrPop().value(CIMObjectPath.class);
                    pDeep = argArray.nextBooleanTupleOrPop();
                    break;
                case "-ei":
                    op = OPS.EI;
                    objectPath = argArray.nextTupleOrPop().value(CIMObjectPath.class);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            switch (op) {
                case INSTANCE:
                    try {
                        cimUbluHelper = new CimUbluHelper();
                    } catch (WBEMException ex) {
                        getLogger().log(Level.SEVERE, "Error creating CimUbluHelper in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    if (getCommandResult() != COMMANDRESULT.FAILURE) {
                        try {
                            put(cimUbluHelper);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error putting CimUbluHelper in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case CLOSE:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, "Null instance in {0} for -close", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        cimUbluHelper.close();
                    }
                    break;
                case PATH:
                    CIMObjectPath cop = CimUbluHelper.newPath(url, pNamespace, pObjectName, pKeys, pXmlSchemaName);
                    try {
                        put(cop);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting CIMObjectPath in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CRED:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, "Null instance in {0} for -cred", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        Subject subject = cimUbluHelper.getSubject();
                        subject.getPrincipals().add(new UserPrincipal(user));
                        subject.getPrivateCredentials().add(new PasswordCredential(password));
                    }
                    break;
                case INIT:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, "Null instance in {0} for -init", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            cimUbluHelper.initialize(objectPath);
                        } catch (IllegalArgumentException | WBEMException ex) {
                            getLogger().log(Level.SEVERE, "Error initializing CimUbluHelper in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case EC:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, "Null instance in {0} for -ein", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        CIMObjectPathArrayList arrayList = null;
                        try {
                            arrayList = cimUbluHelper.enumerateClasses(objectPath, pDeep);
                        } catch (WBEMException ex) {
                            getLogger().log(Level.SEVERE, "Error getting instance names in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(arrayList);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Error putting instance names in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case EI:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, "Null instance in {0} for -ei", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        CIMObjectPathArrayList arrayList = null;
                        try {
                            arrayList = cimUbluHelper.enumerateInstanceNames(objectPath);
                        } catch (WBEMException ex) {
                            getLogger().log(Level.SEVERE, "Error getting instance names in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(arrayList);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Error putting instance names in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Not supported yet.");
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return doCim(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }

}
