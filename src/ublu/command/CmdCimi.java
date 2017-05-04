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
import java.util.logging.Logger;
import javax.cim.CIMDataType;
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
 * Common Information Model Instance (e.g. SNMP) support
 *
 * @author jax
 */
public class CmdCimi extends Command {

    {
        setNameAndDescription("cimi", "/0 [-to datasink] [--,-cimi @ciminstance] [-keys | -key ~@{keyname} | -path] : process CIM Instances");

    }

    /**
     * the operations we know
     */
    protected enum OPS {
        /**
         * Get array of keys
         */
        KEYS,
        /**
         * Get a named key
         */
        KEY,
        /**
         * Get path of instance
         */
        PATH
    }

    /**
     * Perform CIM operations
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray doCim(ArgArray argArray) {
        OPS op = OPS.KEYS;
        CIMInstance cIMInstance = null;
        String keyName = null;
//        URL url = null;
//        String pNamespace = null;
//        String pObjectName = null;
//        CIMObjectPath objectPath = null;
//        String pXmlSchemaName = null;
//        CIMPropertyArrayList pPropertyList = null;
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
                case "-cimi":
                    cIMInstance = argArray.nextTupleOrPop().value(CIMInstance.class);
                    break;
                case "-keys":
                    op = OPS.KEYS;
                    break;
                case "-key":
                    op = OPS.KEY;
                    keyName = argArray.nextMaybeQuotationTuplePopStringTrim();
//                case "-plist":
//                    break;
//                case "-prop": // This is wrong
//                    break;
//                case "-url":
//                    break;
//                case "-xmlschema":
//                    break;
                case "-path":
                    op = OPS.PATH;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            if (cIMInstance != null) {
                switch (op) {
                    case KEYS: {
                        try {
                            put(CimUbluHelper.getKeys(cIMInstance));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting keys in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                    case PATH: {
                        try {
                            put(cIMInstance.getObjectPath());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting object path in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    case KEY:
                        try {
                            put(CimUbluHelper.getKeyByName(CimUbluHelper.getKeys(cIMInstance), keyName));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting key by name in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Not supported yet.");
                }
            } else {
                getLogger().log(Level.SEVERE, "Null instance in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
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