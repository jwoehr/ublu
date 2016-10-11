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

import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.SysValHelper;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SystemValueList;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Retrieve system values and set them
 *
 * @author jwoehr
 */
public class CmdSysVal extends Command {

    {
        setNameAndDescription("sysval", "/0 [-as400 ~@as400] [-to datasink] [--,-sysval ~@sysval] [[-new,-instance alc|all|dattim|edt|libl|msg|net|sec|stg|sysctl] | [haskey ~@{ key }] | [-value ~@{ key }] | -set ~@{ key } ~@value] | [-systemvalue] | [-list] | [-map]] : retrieve a system value list");
    }

    /**
     * Operations we do
     */
    protected enum OPS {

        /**
         * Create instance
         */
        INSTANCE,
        /**
         * Key exists?
         */
        HASKEY,
        /**
         * Get the Value of a sysval by key
         */
        VAL,
        /**
         * Set sysval
         */
        SET,
        /**
         * Get the JTOpen System Value object by key
         */
        SYSVAL,
        /**
         * List of keys
         */
        KEYS,
        /**
         * Turn SysVal object into an ublu
         * <pre> list </pre>
         */
        LIST,
        /**
         * Get a Map of hashvals
         */
        MAP,
        /**
         * Nada
         */
        ERRNOP
    }

    /**
     * Return or set a system value on the host
     *
     * @param argArray arguments passed in
     * @return remnant of argument array
     */
    public ArgArray cmdSysVal(ArgArray argArray) {
        OPS op = OPS.ERRNOP;
        String groupname = null;
        String keyname = null;
        Tuple sysValTuple = null;
        Tuple valueTuple = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "--":
                case "-sysval":
                    sysValTuple = argArray.nextTupleOrPop();
                    break;
                case "-new":
                case "-instance":
                    op = OPS.INSTANCE;
                    groupname = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-keys":
                    op = OPS.KEYS;
                    break;
                case "-haskey":
                    op = OPS.HASKEY;
                    keyname = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-set":
                    op = OPS.SET;
                    keyname = argArray.nextMaybeQuotationTuplePopString();
                    valueTuple = argArray.nextTupleOrPop();
                    break;
                case "-value":
                    op = OPS.VAL;
                    keyname = argArray.nextMaybeTupleString();
                    break;
                case "-systemvalue":
                    op = OPS.SYSVAL;
                    break;
                case "-list":
                    op = OPS.LIST;
                    break;
                case "-map":
                    op = OPS.MAP;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            SysValHelper svh = null;
            if (sysValTuple != null) {
                Object o = sysValTuple.getValue();
                if (o instanceof SysValHelper) {
                    svh = SysValHelper.class.cast(o);
                }
            }
            switch (op) {
                case INSTANCE:
                    if (getAs400() == null) {
                        getLogger().log(Level.SEVERE, "No as400 instance provided to -instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        svh = new SysValHelper(getAs400());
                        Integer index = groupIndexFromName(groupname);
                        if (index == null) {
                            getLogger().log(Level.SEVERE, "Unknown group provided to -instance in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                svh.instanceSystemValues(index);
                                put(svh);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Error getting or putting for -instance in" + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case KEYS:
                    if (svh == null) {
                        getLogger().log(Level.SEVERE, "No -sysval @sysval provided to -keys in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(svh.keySet());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting for -keys in" + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case VAL:
                    if (svh == null) {
                        getLogger().log(Level.SEVERE, "No -sysval @sysval provided to -value in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(svh.getValue(keyname));
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting for -value in" + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case SYSVAL:
                    if (svh == null) {
                        getLogger().log(Level.SEVERE, "No -sysval @sysval provided to -systemvalue in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(svh.getSystemValue(keyname));
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting for -systemvalue in" + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case LIST:
                    if (svh == null) {
                        getLogger().log(Level.SEVERE, "No -sysval @sysval provided to -list in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(svh.getSystemValueList());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting for -list in" + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case MAP:
                    if (svh == null) {
                        getLogger().log(Level.SEVERE, "No -sysval @sysval provided to -map in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(svh.getSystemValueHashMap());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting for -map in" + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case HASKEY:
                    if (svh == null) {
                        getLogger().log(Level.SEVERE, "No -sysval @sysval provided to -haskey in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(svh.hasKey(keyname));
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting for -haskey in" + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case SET:
                    if (svh == null) {
                        getLogger().log(Level.SEVERE, "No -sysval @sysval provided to -set in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (valueTuple != null) { // never should be null but IDE complains
                        try {
                            svh.set(keyname, valueTuple.getValue());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting for -set in" + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No value tuple provided to -set in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unhandled operation {0} in {1}", new Object[]{op, getNameAndDescription()});
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private Integer groupIndexFromName(String groupname) {
        Integer i = null;
        switch (groupname) {
            case "alc":
                i = SystemValueList.GROUP_ALC;
                break;
            case "all":
                i = SystemValueList.GROUP_ALL;
                break;
            case "dattim":
                i = SystemValueList.GROUP_DATTIM;
                break;
            case "edt":
                i = SystemValueList.GROUP_EDT;
                break;
            case "libl":
                i = SystemValueList.GROUP_LIBL;
                break;
            case "msg":
                i = SystemValueList.GROUP_MSG;
                break;
            case "net":
                i = SystemValueList.GROUP_NET;
                break;
            case "sec":
                i = SystemValueList.GROUP_SEC;
                break;
            case "stg":
                i = SystemValueList.GROUP_STG;
                break;
            case "sysctl":
                i = SystemValueList.GROUP_SYSCTL;
                break;
        }
        return i;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdSysVal(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
