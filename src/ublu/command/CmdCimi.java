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
import java.sql.SQLException;
import java.util.logging.Level;
import javax.cim.CIMInstance;
import ublu.util.ArgArray;
import ublu.util.CimUbluHelper;

/**
 * Common Information Model Instance (e.g. SNMP) support
 *
 * @author jax
 */
public class CmdCimi extends Command {

    {
        setNameAndDescription("cimi", "/0 [-to datasink] [--,-cimi @ciminstance] [-keys | -key ~@{keyname} | -properties | -propint ~@{intindex} | -propname ~@{name} | -path] : manipulate CIM Instances");

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
        PATH,
        /**
         * get property by int index
         */
        PROPINT,
        /**
         * get property by string name
         */
        PROP,
        /**
         * get prop array
         */
        PROPS

    }

    /**
     * Perform CIM operations
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray doCimi(ArgArray argArray) {
        OPS op = OPS.KEYS;
        CIMInstance cIMInstance = null;
        String keyName = null;
        String propName = null;
        Integer propInt = null;
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
                    break;
                case "-path":
                    op = OPS.PATH;
                    break;
                case "-properties":
                    break;
                case "-propname":
                    op = OPS.PROP;
                    propName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-propint":
                    op = OPS.PROPINT;
                    propInt = argArray.nextIntMaybeQuotationTuplePopString();
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
                    case KEYS:
                        try {
                            put(CimUbluHelper.getKeys(cIMInstance));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting keys in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case KEY:
                        try {
                            put(CimUbluHelper.getKeyByName(cIMInstance, keyName));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting key by name in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case PATH:
                        try {
                            put(cIMInstance.getObjectPath());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting object path in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case PROP:
                        try {
                            put(CimUbluHelper.getPropByName(cIMInstance, propName));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting prop by name in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case PROPINT:
                        try {
                            put(CimUbluHelper.getPropByInt(cIMInstance, propInt));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting prop by int in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case PROPS:
                        try {
                            put(CimUbluHelper.getProps(cIMInstance));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting props in " + getNameAndDescription(), ex);
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
        return doCimi(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
