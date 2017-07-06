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
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to manipulate jobs
 *
 * @author jwoehr
 */
public class CmdJob extends Command {

    static {
        setNameAndDescription("job", "/6? [-as400 ~@as400] [--,-job ~@job] [-to datasink] [-refresh] [-end ~@{delaytime} (-1 for \"controlled\") | -get ~@{property([name|number|system|user|description|type])} | -getsys | -hold ~@tf_holdspooledfiles | -info | -new,-instance | -noop | -query ~@{property([user|curlibname|number|subsystem|status|activejobstatus|user|description|type|auxioreq|breakmsghandling|cachechanges|callstack|ccsid|completionstatus|countryid|cpuused|curlib|date|defaultwait|endseverity|funcname|functype|inqmsgreply|internaljobident|jobactivedate|jobdate|jobenddate|jobentersysdate|joblog|msgqfullaction|msgqmaxsize|jobqueuedate|statusinjobq|switches|outqpriority|poolident|prtdevname|purge|q|qpriority|routingdata|runpriority|scheddate|timeslice|workidunit])} | -release | -spec] ~@{jobName} ~@{userName} ~@{jobNumber} ~@{system} ~@{userid} ~@{password} : manipulate jobs on the host");
    }

    /**
     * What we do
     */
    protected enum FUNCTIONS {

        /**
         * Get all info
         */
        INFO,
        /**
         * End the job
         */
        END,
        /**
         * Get individual job properties
         */
        GET,
        /**
         * Return the AS400 object associated with the Job object
         */
        GETSYS,
        /**
         * hold job
         */
        HOLD,
        /**
         * Create an instance to manipulate in a tuple var
         */
        INSTANCE,
        /**
         * Do nothing
         */
        NOOP,
        /**
         * Query some property of the job
         */
        QUERY,
        /**
         * Reload job info from server
         */
        REFRESH,
        /**
         * release held job
         */
        RELEASE,
        /**
         * return the jobnumber/jobuser/jobname spec of the job
         */
        SPEC
    }

    /**
     * Perform the work of getting a Job object and manipulating it.
     *
     * @param argArray the input arg array
     * @return what's left of the arg array
     */
    public ArgArray job(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.INSTANCE;
        String query = null;
        Job myJob = null;
        int delay = -1;
        String propertyToGet = "";
        boolean holdSpooledFiles = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
//                case "-from":
//                    String srcName = argArray.next();
//                    setDataSrc(DataSink.fromSinkName(srcName));
//                    break;
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-end":
                    function = FUNCTIONS.END;
                    delay = argArray.nextIntMaybeTupleString();
                    break;
                case "-get":
                    function = FUNCTIONS.GET;
                    propertyToGet = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-getsys":
                    function = FUNCTIONS.GETSYS;
                    break;
                case "-hold":
                    function = FUNCTIONS.HOLD;
                    holdSpooledFiles = argArray.nextTupleOrPop().getValue().equals(true);
                    break;
                case "-info":
                    function = FUNCTIONS.INFO;
                    break;
                case "-new":
                case "-instance":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "--":
                case "-job":
                    myJob = argArray.nextTupleOrPop().value(Job.class);
                    if (myJob == null) {
                        getLogger().log(Level.SEVERE, "Valued tuple which is not a Job tuple provided to -job in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case "-noop":
                    function = FUNCTIONS.NOOP;
                    break;
                case "-query":
                    function = FUNCTIONS.QUERY;
                    query = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-refresh":
                    function = FUNCTIONS.REFRESH;
                    break;
                case "-release":
                    function = FUNCTIONS.RELEASE;
                    break;
                case "-spec":
                    function = FUNCTIONS.SPEC;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            if (myJob == null) { // no provided Job instance
                if (argArray.size() < 3) {
                    logArgArrayTooShortError(argArray);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else { // get the Job factors
                    String jobName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    String userName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    String jobNumber = argArray.nextMaybeQuotationTuplePopStringTrim();
                    if (getAs400() == null) { // no AS400 instance
                        if (argArray.size() < 3) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else { // Get the AS400 instance
                            try {
                                setAs400FromArgs(argArray);
                            } catch (PropertyVetoException ex) {
                                getLogger().log(Level.SEVERE, "Couldn't create AS400 instance in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    if (getAs400() != null) {
                        myJob = new Job(getAs400(), jobName, userName, jobNumber);
                    }
                }
            }
            if (myJob != null) {
                switch (function) {
                    case END:
                        try {
                            myJob.end(delay);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error ending Job " + myJob.getName() + " " + myJob.getNumber() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case GET:
                        try {
                            put(get(propertyToGet, myJob));
                        } catch (AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | IOException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error fetching property " + propertyToGet + " from the job in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case GETSYS:
                        try {
                            put(myJob.getSystem());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error fetching system object from the job " + myJob + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case HOLD:
                        try {
                            myJob.hold(holdSpooledFiles);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException ex) {
                            getLogger().log(Level.SEVERE, "Error holding job " + myJob + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case INFO:
                    case INSTANCE:
                        try {
                            put(myJob);
                        } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error putting Job info in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case NOOP:
                        break;
                    case QUERY:
                        try {
                            Object o = query(query, myJob);
                            if (getCommandResult() == COMMANDRESULT.SUCCESS) {
                                put(o);
                            }
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error querying or putting job query in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    case REFRESH:
                        myJob.loadInformation();
                        break;
                    case RELEASE:
                        try {
                            myJob.release();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException ex) {
                            getLogger().log(Level.SEVERE, "Error holding job " + myJob + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case SPEC:
                        try {
                            StringBuilder sb = new StringBuilder();
                            sb.append(myJob.getNumber())
                                    .append("/")
                                    .append(myJob.getUser())
                                    .append("/")
                                    .append(myJob.getName());
                            put(sb.toString());
                        } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error putting Job info in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                }
            } else {
                getLogger().log(Level.SEVERE, "Unable to get Job instance in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private String get(String property, Job job) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, IOException {
        String result = "";
        switch (property) {
            case "name":
                result = job.getName();
                break;
            case "number":
                result = job.getNumber();
                break;
            case "subsystem":
                result = job.getSubsystem();
                break;
            case "user":
                result = job.getUser();
                break;
            case "description":
                result = job.getJobDescription();
                break;
            case "type":
                result = job.getType();
                break;
//            case "jobdescription":
//                result = job.getJobDescription();
//                break;
//            case "jobdescription":
//                result = job.getJobDescription();
//                break;
//            case "jobdescription":
//                result = job.getJobDescription();
//                break;
            }
        return result;
    }

    private Object query(String property, Job job) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        Object o = null;
        switch (property) {
            case "name":
                o = job.getName();
                break;
            case "number":
                o = job.getNumber();
                break;
            case "subsystem":
                o = job.getSubsystem();
                break;
            case "status":
                o = job.getStatus();
                break;
            case "activejobstatus":
                o = job.getValue(Job.ACTIVE_JOB_STATUS);
                break;
            case "user":
                o = job.getUser();
                break;
            case "description":
                o = job.getJobDescription();
                break;
            case "type":
                o = job.getType();
                break;
            case "auxioreq":
                o = job.getAuxiliaryIORequests();
                break;
            case "breakmsghandling":
                o = job.getBreakMessageHandling();
                break;
            case "cachechanges":
                o = job.getCacheChanges();
                break;
            case "callstack":
                o = job.getCallStack(Job.INITIAL_THREAD);
                break;
            case "ccsid":
                o = job.getCodedCharacterSetID();
                break;
            case "completionstatus":
                o = job.getCompletionStatus();
                break;
            case "countryid":
                o = job.getCountryID();
                break;
            case "cpuused":
                o = job.getCPUUsed();
                break;
            case "curlib":
                o = job.getCurrentLibrary();
                break;
            case "date":
                o = job.getDate();
                break;
            case "defaultwait":
                o = job.getDefaultWait();
                break;
            case "endseverity":
                o = job.getEndSeverity();
                break;
            case "funcname":
                o = job.getFunctionName();
                break;
            case "functype":
                o = job.getFunctionType();
                break;
            case "inqmsgreply":
                job.getInquiryMessageReply();
                break;
            case "internaljobident":
                o = job.getInternalJobIdentifier();
                break;
            case "jobactivedate":
                o = job.getJobActiveDate();
                break;
            case "jobdate":
                o = job.getJobDate();
                break;
            case "jobenddate":
                o = job.getJobEndedDate();
                break;
            case "jobentersysdate":
                o = job.getJobEnterSystemDate();
                break;
            case "joblog":
                o = job.getJobLog();
                break;
            case "msgqfullaction":
                o = job.getJobMessageQueueFullAction();
                break;
            case "msgqmaxsize":
                o = job.getJobMessageQueueMaximumSize();
                break;
            case "jobqueuedate":
                o = job.getJobPutOnJobQueueDate();
                break;
            case "statusinjobq":
                o = job.getJobStatusInJobQueue();
                break;
            case "switches":
                o = job.getJobSwitches();
                break;
            case "outqpriority":
                o = job.getOutputQueuePriority();
                break;
            case "poolident":
                o = job.getPoolIdentifier();
                break;
            case "prtdevname":
                o = job.getPrinterDeviceName();
                break;
            case "purge":
                o = job.getPurge();
                break;
            case "q":
                o = job.getQueue();
                break;
            case "qpriority":
                o = job.getQueuePriority();
                break;
            case "routingdata":
                o = job.getRoutingData();
                break;
            case "runpriority":
                o = job.getRunPriority();
                break;
            case "scheddate":
                o = job.getScheduleDate();
                break;
            case "timeslice":
                o = job.getTimeSlice();
                break;
            case "workidunit":
                o = job.getWorkIDUnit();
                break;
            default:
                getLogger().log(Level.SEVERE, "Unknown query {0} in {1}", new Object[]{property, getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
        }
        return o;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return job(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
