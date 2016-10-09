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

import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics.ThingArrayList;
import ublu.util.Tuple;
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
        setNameAndDescription("objlist", "/0 [-as400 @as400] [-to datasink] [--,-objlist ~@objlist] [-lib libspec ] [-name objname] [-type objtype] [-new,-instance] [-list]  : retrieve a (filtered) object list");

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
        Tuple objListTuple = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    // /* Debug */ getLogger().log(Level.INFO, "my AS400 == {0}", getAs400());
                    break;
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "--":
                case "-objlist":
                    objListTuple = argArray.nextTupleOrPop();
                    break;
                case "-lib":
                    libspec = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-name":
                    objname = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-type":
                    objtype = argArray.nextMaybeQuotationTuplePopString();
                    break;
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
            ObjectList objList = null;
            switch (op) {
                case INSTANCE:
                    if (getAs400() == null) {
                        getLogger().log(Level.WARNING, "No as400 instance provided in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        objList = new ObjectList(getAs400(), libspec, objname, objtype);
                        try {
                            put(objList);
                        } catch (AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.INFO, "Exception putting objlist in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "SQL Exception putting objlist in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case LIST:
                    if (objListTuple != null) {
                        Object tupleValue = objListTuple.getValue();
                        if (tupleValue instanceof ObjectList) {
                            objList = ObjectList.class.cast(tupleValue);
                        } else {
                            getLogger().log(Level.WARNING, "Valued tuple which is not an ObjectList tuple provided to -objlist in {0}", getNameAndDescription());
                        }
                    }
                    if (objList == null) {
                        getLogger().log(Level.WARNING, "No objlist instance provided in {0}", getNameAndDescription());
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
                    getLogger().log(Level.WARNING, "Unknown operation in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
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
