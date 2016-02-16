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
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.HistoryLog;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * Examine OS400 history logs
 *
 * @author jwoehr
 */
public class CmdHistoryLog extends Command {

    {
        setNameAndDescription("histlog",
                "/0 [-to datasink] [--,-histlog @histlog] [-as400 ~@as400 ] [-instance] [-close] [-get] [-examine] [-jobs ~@listofjobs] [-severity 0-99] [-startdate yyyy/mm/dd] [-enddate yyyy/mm/dd] [-msgids ~@list]  [-msgidsinc omit|select] [-msgtypes ~@list] [-msgtypesinc omit|select] : get (filtered) server history log");
    }

    /**
     * Operations
     */
    protected enum OPERATIONS {

        /**
         * Close connection to log
         */
        CLOSE,
        /**
         * Instance log connection
         */
        INSTANCE,
        /**
         * Get messages
         */
        GET,
        /**
         * Number of hist log entries
         */
        LENGTH,
        /**
         * Examine the object properties, helps in debugging
         */
        EXAMINE
    }

    /**
     * The histlog command
     *
     * @param argArray
     * @return
     */
    public ArgArray histLog(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.INSTANCE;
        HistoryLog myHl = null;
        Tuple hlTuple = null;
        String startDateString = null;
        String endDateString = null;
        Tuple jobsTalTuple = null;
        Tuple msgIdsTalTuple = null;
        Tuple msgtypesTalTuple = null;
        Integer severity = 0;
        String msgidsindicatorstring = null;
        String msgtypesindicatorstring = null;
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
                case "-histlog":
                    hlTuple = argArray.nextTupleOrPop();
                    break;
                case "-close":
                    operation = OPERATIONS.CLOSE;
                    break;
                case "-get":
                    operation = OPERATIONS.GET;
                    break;
                case "length":
                    operation = OPERATIONS.LENGTH;
                    break;
                case "-startdate":
                    startDateString = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-enddate":
                    endDateString = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-jobs":
                    jobsTalTuple = argArray.nextTupleOrPop();
                    break;
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-msgids":
                    msgIdsTalTuple = argArray.nextTupleOrPop();
                    break;
                case "-msgidinc":
                    msgidsindicatorstring = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-msgtypes":
                    msgtypesTalTuple = argArray.nextTupleOrPop();
                    break;
                case "-msgtypesinc":
                    msgtypesindicatorstring = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-severity":
                    severity = argArray.nextIntMaybeTupleString();
                    break;
                case "-examine":
                    operation = OPERATIONS.EXAMINE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (hlTuple != null) {
                Object maybeHistoryLog = hlTuple.getValue();
                if (maybeHistoryLog instanceof HistoryLog) {
                    myHl = HistoryLog.class.cast(maybeHistoryLog);
                } else {
                    getLogger().log(Level.SEVERE, "Tuple provided to -histlist does not contain a HistoryList in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            switch (operation) {
                case CLOSE:
                    if (myHl == null) {
                        getLogger().log(Level.SEVERE, "No HistoryList instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            myHl.close();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error closing List in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    }
                case INSTANCE:
                    if (getAs400() != null) {
                        try {
                            myHl = formulateInstanceHistoryLog(getAs400(), jobsTalTupleToJobArray(jobsTalTuple), startDateString, endDateString, talTupleToStringArray(msgtypesTalTuple), msgtypesindicatorstring, talTupleToStringArray(msgIdsTalTuple), msgidsindicatorstring, severity);
                        } catch (ParseException ex) {
                            getLogger().log(Level.SEVERE, "Error parsing date in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (myHl != null) {
                            try {
                                put(myHl);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting List in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case GET:
                    if (myHl == null) {
                        getLogger().log(Level.SEVERE, "No HistoryList instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        ThingArrayList tal = null;
                        try {
                            myHl.load();
                            tal = new ThingArrayList(myHl.getMessages());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error getting HistoryList to List in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (tal != null) {
                            try {
                                put(tal);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting List in get from HistoryList in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case LENGTH:
                    if (myHl == null) {
                        getLogger().log(Level.SEVERE, "No HistoryList instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myHl.getLength());
                        } catch (SQLException | RequestNotSupportedException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting length in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                case EXAMINE:
                    if (myHl == null) {
                        getLogger().log(Level.SEVERE, "No HistoryList instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Object:\t").append(myHl).append('\n');
                            sb.append("Jobs:\t").append(myHl.getJobs()).append('\n');
                            sb.append("Start Date:\t").append(myHl.getStartingDate()).append('\n');
                            sb.append("End Date:\t").append(myHl.getEndingDate()).append('\n');
                            sb.append("Msg Ids:\t").append(myHl.getMessageIDs()).append('\n');
                            sb.append("Msg Ids Indicator:\t").append(myHl.getMessageIDsListIndicator()).append('\n');
                            sb.append("Msg Types:\t").append(myHl.getMessageTypes()).append('\n');
                            sb.append("Msg Types Indicator:\t").append(myHl.getMessageTypeListIndicator()).append('\n');
                            sb.append("Severity:\t").append(myHl.getMessageSeverity()).append('\n');
                            sb.append("Length:\t").append(myHl.getLength()).append('\n');
                            put(sb.toString());
                        } catch (SQLException | RequestNotSupportedException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting length in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unknown operation unhandled in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private HistoryLog formulateInstanceHistoryLog(AS400 as400,
            Job[] jobs,
            String startDateString,
            String endDateString,
            String[] messageTypes,
            String messageTypesIndicatorString,
            String[] messageIds,
            String messageIdsIndicatorString,
            int severity) throws ParseException {
        HistoryLog hl = null;
        int messageIdsIndicator = HistoryLog.SELECT;
        int messageTypesIndicator = HistoryLog.SELECT;
        Date startDate = null;
        Date endDate = null;
        if (startDateString != null) {
            startDate = DateFormat.getDateInstance().parse(startDateString);
        }
        if (endDateString != null) {
            endDate = DateFormat.getDateInstance().parse(endDateString);
        }
        if (messageIdsIndicatorString != null) {
            switch (messageIdsIndicatorString) {
                case "omit":
                    messageIdsIndicator = HistoryLog.OMIT;
                    break;
                case "select":
                    messageIdsIndicator = HistoryLog.SELECT;
            }
        }
        if (messageTypesIndicatorString != null) {
            switch (messageIdsIndicatorString) {
                case "omit":
                    messageTypesIndicator = HistoryLog.OMIT;
                    break;
                case "select":
                    messageTypesIndicator = HistoryLog.SELECT;
            }
        }
        hl = instanceHistoryLog(as400, jobs, startDate, endDate, messageTypes, messageTypesIndicator, messageIds, messageIdsIndicator, severity);
        return hl;
    }

    private HistoryLog instanceHistoryLog(AS400 as400,
            Job[] jobs,
            Date startDate,
            Date endDate,
            String[] messageTypes,
            int messageTypesIndicator,
            String[] messageIds,
            int messageIdsIndicator,
            int severity) {
        HistoryLog hl = new HistoryLog(as400);
        if (jobs != null) {
            hl.setJobs(jobs);
        }
        if (startDate != null) {
            hl.setStartingDate(startDate);
        }
        if (endDate != null) {
            hl.setEndingDate(endDate);
        }
        if (messageTypes != null) {
            hl.setMessageTypes(messageTypes);
            hl.setMessageTypeListIndicator(messageTypesIndicator);
        }
        if (messageIds != null) {
            hl.setMessageIDs(messageIds);
            hl.setMessageIDsListIndicator(messageIdsIndicator);
        }
        hl.setMessageSeverity(severity);
        return hl;
    }

    private Job[] jobsTalTupleToJobArray(Tuple t) {
        Job[] jobs = null;


        if (t != null) {
            ThingArrayList list = ThingArrayList.class
                    .cast(t.getValue());
            Iterator it = list.iterator();
            jobs = new Job[list.size()];
            int i = 0;

            while (it.hasNext()) {
                jobs[i++] = Job.class.cast(it.next());
            }
        }
        return jobs;
    }

    private String[] talTupleToStringArray(Tuple t) {
        String[] strings = null;


        if (t != null) {
            ThingArrayList list = ThingArrayList.class
                    .cast(t.getValue());
            Iterator it = list.iterator();
            strings = new String[list.size()];
            int i = 0;

            while (it.hasNext()) {
                strings[i++] = String.class.cast(it.next());
            }
        }
        return strings;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return histLog(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
