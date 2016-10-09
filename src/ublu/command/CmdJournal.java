/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
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
import ublu.util.JournalHelper;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Manipulate i Series OS journals
 *
 * @author jwoehr
 */
public class CmdJournal extends Command {

    {
        setNameAndDescription("jrnl",
                "/3? [-to datasink] [--,-jrnl ~@jrnl] [-as400 ~@as400 ] [-to @variable] [-new,-instance] [-lib ~@{libname}] [-journal ~@{journal}] [-get ~@###] [-size ~@intval (default 1024)]  : get journal entries");
    }

    /**
     * Our operations
     */
    protected enum OPS {

        /**
         * Get entries
         */
        GET,
        /**
         * Create Journal Helper instance
         */
        INSTANCE
    }

    /**
     * Fetcher of journal entries
     *
     * @param argArray
     * @return remnant of argArray
     */
    public ArgArray jrnl(ArgArray argArray) {
        String library = "";
        String journal = "";
        Tuple jrnlTuple = null;
        Integer numToGet = null;
        int averageEntrySize = 1024;
        OPS op = OPS.INSTANCE;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
//                case "-from":
//                    String srcName = argArray.next();
//                    setDataSrc(DataSink.fromSinkName(srcName));
//                    break;
                case "--":
                case "-jrnl":
                    jrnlTuple = argArray.nextTupleOrPop();
                    break;
                case "-get":
                    op = OPS.GET;
                    numToGet = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-new":
                case "-instance":
                    op = OPS.INSTANCE;
                    break;
                case "-lib":
                    library = argArray.nextMaybeQuotationTuplePopString().toUpperCase();
                    break;
                case "-journal":
                    journal = argArray.nextMaybeQuotationTuplePopString().toUpperCase();
                    break;
                case "-size":
                    averageEntrySize = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            JournalHelper journalHelper = null;
            if (jrnlTuple != null) {
                Object o = jrnlTuple.getValue();
                journalHelper = o instanceof JournalHelper ? JournalHelper.class.cast(o) : null;
            } else {
                AS400 as400 = null;
                try {
                    as400 = getAs400() != null ? getAs400() : as400FromArgs(argArray);
                } catch (PropertyVetoException ex) {
                    getLogger().log(Level.SEVERE, "No JournalHelper instance and no AS400 instance in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
                if (as400 != null) {
                    journalHelper = new JournalHelper(as400);
                }
            }
            if (journalHelper == null) {
                getLogger().log(Level.SEVERE, "Unable to instance JournalHelper in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                switch (op) {
                    case INSTANCE:
                        try {
                            put(journalHelper);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting JournalHelper instance and no AS400 instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case GET:
                        String retrievedEntries = null;
                        try {
                            retrievedEntries = journalHelper.fetchJournalEntries(getInterpreter(), library, journal, numToGet, averageEntrySize);
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Error fetching journal entries in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                if (retrievedEntries != null) {
                                    put(retrievedEntries);
                                } else {
                                    put(journalHelper.getLastCommandResult());
                                }
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Couldn't put journal entries in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    default:
                        getLogger().log(Level.SEVERE, "Unknown operation in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return jrnl(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
