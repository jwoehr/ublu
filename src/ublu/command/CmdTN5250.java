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
import ublu.util.Generics.StringArrayList;
import ublu.util.Sess5250;
import ublu.util.TN5250Helper;
import ublu.util.TN5250Helper.My5250Thread;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to create and manage tn5250 instances and data streams.
 *
 * @author jwoehr
 */
public class CmdTN5250 extends Command {

    {
        setNameAndDescription("tn5250",
                "/0 [-tn5250 @tn5250] [-to datasink] [[-new,-instance] | [-my5250] | [-run] | [-session] | [-sessionlist]]  [-args ~@${ arg string }$] ~@system : instance a programmable or interactive tn5250j");
    }

    enum OPERATIONS {

        My5250, INSTANCE, NOOP, RUN, SESSION, SESSIONLIST
    }

    /**
     * Create and manage tn5250 instances and data streams.
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray tn5250(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.My5250; // the default
        String tn5250HelperTupleName = null;
        Tuple tn5250HelperTuple = null;
        StringArrayList tn5250args = new StringArrayList();
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "-from":
                    setDataSrc(DataSink.fromSinkName(argArray.next()));
                    break;
                case "--":
                case "-tn5250":
                    tn5250HelperTupleName = argArray.next();
                    tn5250HelperTuple = getTuple(tn5250HelperTupleName);
                    break;
                case "-args":
                    tn5250args = new StringArrayList(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-new":
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-my5250":
                    operation = OPERATIONS.My5250;
                    break;
                case "-run":
                    operation = OPERATIONS.RUN;
                    break;
                case "-session":
                    operation = OPERATIONS.SESSION;
                    break;
                case "-sessionlist":
                    operation = OPERATIONS.SESSIONLIST;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            TN5250Helper tn5250Helper;
            switch (operation) {
                case My5250:
                    String my5250ThreadName = java.util.Calendar.getInstance().getTime().toString() + "";
                    My5250Thread mt = new My5250Thread(my5250ThreadName, tn5250args.toStringArray());
                    mt.setDaemon(true);
                    mt.start();
                    break;

                case INSTANCE:
                    tn5250Helper = instanceTN5250HelperFromArgArray(argArray);
                    if (tn5250Helper != null) {
                        try {
                            if (tn5250args.size() > 0) {
                                tn5250Helper.setArgs(tn5250args);
                            }
                            put(tn5250Helper);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not put tn5250 instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Could not instance tn5250 in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case RUN:
                    tn5250Helper = instanceTN5250HelperFromTuple(tn5250HelperTupleName, tn5250HelperTuple);
                    if (tn5250Helper == null) {
                        getLogger().log(Level.SEVERE, "Could not instance tn5250 for run in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        if (tn5250args.size() > 0) {
                            tn5250Helper.setArgs(tn5250args);
                        }
                        tn5250Helper.start();
                    }
                    break;

                case SESSION:
                    tn5250Helper = instanceTN5250HelperFromTuple(tn5250HelperTupleName, tn5250HelperTuple);
                    if (tn5250Helper == null) {
                        getLogger().log(Level.SEVERE, "Could not instance tn5250 for sessionlist in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(new Sess5250(tn5250Helper.getSessionList().get(0)));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not put tn5250 session in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case SESSIONLIST:
                    tn5250Helper = instanceTN5250HelperFromTuple(tn5250HelperTupleName, tn5250HelperTuple);
                    if (tn5250Helper == null) {
                        getLogger().log(Level.SEVERE, "Could not instance tn5250 for sessionlist in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(tn5250Helper.getSessionList());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not put tn5250 sessionlist in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
            }
        }
        return argArray;
    }

    private TN5250Helper instanceTN5250HelperFromArgArray(ArgArray argArray) {
        TN5250Helper tnh;
        String system = argArray.nextMaybeQuotationTuplePopString();
        tnh = new TN5250Helper(system);
        return tnh;
    }

    private TN5250Helper instanceTN5250HelperFromTuple(String tupleName, Tuple t) {
        TN5250Helper tnh = null;
        Object o = t.getValue();
        if (o instanceof TN5250Helper) {
            tnh = TN5250Helper.class.cast(o);
        } else {
            getLogger().log(Level.SEVERE, "Tuple {0} does not represent a tn5250", tupleName);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        return tnh;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return tn5250(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
