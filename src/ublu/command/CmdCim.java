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
import javax.cim.CIMInstance;
import javax.cim.CIMObjectPath;
import javax.cim.CIMProperty;
import javax.security.auth.Subject;
import javax.wbem.WBEMException;
import javax.wbem.client.PasswordCredential;
import javax.wbem.client.UserPrincipal;
import ublu.util.ArgArray;
import ublu.util.CimUbluHelper;
import ublu.util.Generics.CIMObjectPathArrayList;
import ublu.util.Generics.CIMPropertyArrayList;
import ublu.util.Generics.StringArrayList;

/**
 * Common Information Model Client (e.g. SNMP) support
 *
 * @author jax
 */
public class CmdCim extends Command {

    static {
        setNameAndDescription("cim", "/0 [-to datasink] [--,-cim @cimclient] [-keys ~@propertyKeyArray] [-namespace ~@{namespace}] [-objectname ~@{objectname}] [-plist ~@arraylist] [-url ~@{https://server:port}] [-xmlschema ~@{xmlschemaname}] [-new | -close | -path | -cred ~@{user} ~@{password} | -init ~@cimobjectpath | -ec  ~@cimobjectpath ~@deep_tf | -ei  ~@cimobjectpath | -trace ~@tf] : CIM client");

    }

    /**
     * the operations we know
     */
    protected enum OPS {
        /**
         * Create the CIM Helper
         */
        NEW,
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
         * Enumerate instance names
         */
        EC,
        /**
         * Enumerate instance names
         */
        EI,
        /**
         * Get instance
         */
        GI,
        /**
         * Trace
         */
        TRACE
    }

    /**
     * Perform CIM operations
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray doCim(ArgArray argArray) {
        OPS op = OPS.NEW;
        URL url = null;
        String pNamespace = null;
        String pObjectName = null;
        CIMObjectPath objectPath = null;
        String pXmlSchemaName = null;
        CimUbluHelper cimUbluHelper = null;
        String user = null;
        String password = null;
        CIMPropertyArrayList pPropertyList = null;
        StringArrayList stringPropertyList = null;
        Boolean pDeep = false;
        Boolean pLocalOnly = false;
        Boolean pIncludeClassOrigin = false;
        Boolean traceTF = false;
        while (argArray.hasDashCommand() && getCommandResult() != COMMANDRESULT.FAILURE) {
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
                    op = OPS.NEW;
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
                    pPropertyList = new CIMPropertyArrayList(argArray.nextTupleOrPop().value(CIMProperty[].class));
                    break;
                case "-key":
                    if (pPropertyList == null) {
                        pPropertyList = new CIMPropertyArrayList();
                    }
                    pPropertyList.add(argArray.nextTupleOrPop().value(CIMProperty.class));
                    break;
                case "-plist":
                    pPropertyList = argArray.nextTupleOrPop().value(CIMPropertyArrayList.class);
                    break;
                case "-prop": // This is wrong
                    if (stringPropertyList == null) {
                        stringPropertyList = new StringArrayList();
                    }
                    stringPropertyList.add(argArray.nextMaybeQuotationTuplePopStringTrim());
                    break;
                case "-url":
                    try {
                        url = new URL(argArray.nextMaybeQuotationTuplePopStringTrim());
                    } catch (MalformedURLException ex) {
                        getLogger().log(Level.SEVERE, "Error parsing URL in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
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
                case "-gi":
                    op = OPS.GI;
                    objectPath = argArray.nextTupleOrPop().value(CIMObjectPath.class);
                    pLocalOnly = argArray.nextBooleanTupleOrPop();
                    pIncludeClassOrigin = argArray.nextBooleanTupleOrPop();
                    break;
                case "-trace":
                    op = OPS.TRACE;
                    traceTF = argArray.nextBooleanTupleOrPop();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            CIMObjectPathArrayList arrayList = null;
            CIMInstance instance = null;
            switch (op) {
                case NEW:
                    try {
                        cimUbluHelper = new CimUbluHelper();
                    } catch (WBEMException ex) {
                        getLogger().log(Level.SEVERE, locMsg("Error_creating") + " CimUbluHelper in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    if (getCommandResult() != COMMANDRESULT.FAILURE) {
                        try {
                            put(cimUbluHelper);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, locMsg("Error_putting") + " CimUbluHelper in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case CLOSE:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, locMsg("Null_client") + " for -close in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        cimUbluHelper.close();
                    }
                    break;
                case PATH:
                    objectPath = CimUbluHelper.newPath(url, pNamespace, pObjectName, pPropertyList, pXmlSchemaName);
                    try {
                        put(objectPath);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, locMsg("Error_putting") + " CIMObjectPath in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CRED:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, locMsg("Null_client") + " for -cred  in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        Subject subject = cimUbluHelper.getSubject();
                        subject.getPrincipals().add(new UserPrincipal(user));
                        subject.getPrivateCredentials().add(new PasswordCredential(password));
                    }
                    break;
                case INIT:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, locMsg("Null_client") + " for -init in {0}", getNameAndDescription());
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
                        getLogger().log(Level.SEVERE, locMsg("Null_client") + " for -ec in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            arrayList = cimUbluHelper.enumerateClasses(objectPath, pDeep);
                        } catch (WBEMException ex) {
                            getLogger().log(Level.SEVERE, locMsg("Error_getting") + " instance names in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(arrayList);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, locMsg("Error_putting") + " instance names in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case EI:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, locMsg("Null_client") + " for -ei in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            arrayList = cimUbluHelper.enumerateInstanceNames(objectPath);
                        } catch (WBEMException ex) {
                            getLogger().log(Level.SEVERE, locMsg("Error_getting") + " CIM instance names in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(arrayList);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, locMsg("Error_putting") + " CIM instance names in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case GI:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, locMsg("Null_client") + " for -gi  in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            instance = cimUbluHelper.getInstance(objectPath, pLocalOnly, pIncludeClassOrigin, stringPropertyList);
                        } catch (WBEMException ex) {
                            getLogger().log(Level.SEVERE, locMsg("Error_getting") + " CIM instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(instance);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, locMsg("Error_putting") + " CIM instance in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case TRACE:
                    if (cimUbluHelper == null) {
                        getLogger().log(Level.SEVERE, locMsg("Null_client") + " for -trace in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        cimUbluHelper.trace(traceTF);
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
