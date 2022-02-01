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

import ublu.util.ArgArray;
import ublu.util.Generics.ThingArrayList;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ObjectList;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * Get a list of objects on the system
 *
 * @author jwoehr
 */
public class CmdObjList extends Command {

    {
        setNameAndDescription("objlist", "/0 [-as400 ~@as400] [-to datasink] [--,-objlist ~@objlist] [-lib libspec ] [-name objname] [-type objtype] [-asp ~@{ALL|ALLAVL|CURASPGRP|SYSBAS}] [-new,-instance] [-list]  : retrieve a (filtered) object list");

    }

    /**
     * the operations we know
     */
    protected enum OPS {

        /**
         * Create the object list
         */
        INSTANCE,
        /**
         * Fetch and render as a Generics.ThingArrayList
         */
        LIST
    }

    /**
     * Arity-0 ctor
     */
    public CmdObjList() {
    }

    /**
     * retrieve a (filtered) list of OS400 Objects on the system
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray objlist(ArgArray argArray) {
        OPS op = OPS.INSTANCE;
        String libspec = "*ALL";
        String objname = "*ALL";
        String objtype = "*ALL";
        String aspDevName = ObjectList.ASP_NAME_ALL;
        ObjectList objList = null;
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
                case "-objlist":
                    objList = argArray.nextTupleOrPop().value(ObjectList.class);
                    break;
                case "-lib":
                    libspec = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-name":
                    objname = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-type":
                    objtype = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-asp":
                    aspDevName = aspDeviceName(argArray.nextMaybeQuotationTuplePopStringTrim());
                case "-list":
                    op = OPS.LIST;
                    break;
                case "-new":
                case "-instance":
                    op = OPS.INSTANCE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (op) {
                case INSTANCE:
                    if (getAs400() == null) {
                        getLogger().log(Level.SEVERE, "No as400 instance provided in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        if (aspDevName == null) {
                            getLogger().log(Level.SEVERE, "Unknown ASP selector provided in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            objList = new ObjectList(getAs400(), libspec, objname, objtype, aspDevName);
                            try {
                                put(objList);
                            } catch (AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception putting objlist in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } catch (SQLException ex) {
                                getLogger().log(Level.SEVERE, "SQL Exception putting objlist in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case LIST:
                    if (objList == null) {
                        getLogger().log(Level.SEVERE, "No objlist instance provided in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            Enumeration e = objList.getObjects();
                            ThingArrayList tal = new ThingArrayList(e);
                            put(tal);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Exception getting or putting list from objlist enumeration in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unknown operation in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private String aspDeviceName(String s) {
        String result = null;
        switch (s.toUpperCase()) {
            case "ALL": // ASP_NAME_ALL - The ASPs in the thread's library name space.
                result = ObjectList.ASP_NAME_ALL;
                break;
            case "ALLAVL":// ASP_NAME_ALLAVL - All available ASPs.
                result = ObjectList.ASP_NAME_ALLAVL;
                break;
            case "CURASPGRP": // ASP_NAME_CURASPGRP - The ASPs in the current thread's ASP group.
                result = ObjectList.ASP_NAME_CURASPGRP;
                break;
            case "SYSBAS": // ASP_NAME_SYSBAS - The system ASP (ASP 1) and defined basic user ASPs (ASPs 2-32).
                result = ObjectList.ASP_NAME_SYSBAS;
                break;
            default:
                getLogger().log(Level.SEVERE, "Unknown ASP selector {0} provided in {1}", new Object[]{s, getNameAndDescription()});
        }
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return objlist(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
