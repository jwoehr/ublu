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
import com.ibm.as400.access.JobList;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to get a list of active jobs from the host.
 *
 * @author jwoehr
 */
public class CmdJobList extends Command {

    {
        setNameAndDescription("joblist", "/3? [-as400 @as400] [-to datasink] [-username userfilter ] [-jobname jobfilter] [-jobnumber jobnumfilter] [-jobtype JOBTYPE] [-active [-disconnected]] system userid passwd : retrieve a (filtered) joblist");

    }

    /**
     * Arity-0 ctor
     */
    public CmdJobList() {
    }

    /**
     * fetch a joblist filtered by user and/or jobname and/or jobnumber
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray joblist(ArgArray argArray) {
        String username = "";
        String jobname = "";
        String jobnumber = "";
        String jobtype = "";
        boolean active = false;
        boolean disconnected = false;
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
                case "-username":
                    username = argArray.nextMaybeTupleString();
                    break;
                case "-jobname":
                    jobname = argArray.nextMaybeTupleString();
                    break;
                case "-jobnumber":
                    jobnumber = argArray.nextMaybeTupleString();
                    break;
                case "-jobtype":
                    jobtype = argArray.nextMaybeTupleString();
                    break;
                case "-active":
                    active = true;
                    break;
                case "-disconnected":
                    disconnected = true;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            try {
                if (getAs400() == null) {
                    if (argArray.size() < 3) {
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        setAs400FromArgs(argArray);
                    }
                }
                if (getAs400() != null) {
                    JobList jl = new JobList(getAs400());
                    if (!username.equals("")) {
                        jl.addJobSelectionCriteria(JobList.SELECTION_USER_NAME, username);
                    }
                    if (!jobname.equals("")) {
                        jl.addJobSelectionCriteria(JobList.SELECTION_JOB_NAME, jobname);
                    }
                    if (!jobnumber.equals("")) {
                        jl.addJobSelectionCriteria(JobList.SELECTION_JOB_NUMBER, jobnumber);
                    }
                    if (!jobtype.equals("")) {
                        setSelectionActiveJobStatus(jl, jobtype);
                    }
                    if (active) {
                        jl.addJobSelectionCriteria(JobList.SELECTION_PRIMARY_JOB_STATUS_ACTIVE, Boolean.TRUE);
                        jl.addJobSelectionCriteria(JobList.SELECTION_PRIMARY_JOB_STATUS_JOBQ, Boolean.FALSE);
                        jl.addJobSelectionCriteria(JobList.SELECTION_PRIMARY_JOB_STATUS_OUTQ, Boolean.FALSE);

                    }
                    if (disconnected) {
                        jl.addJobSelectionCriteria(JobList.SELECTION_ACTIVE_JOB_STATUS, Job.ACTIVE_JOB_STATUS_DISCONNECTED);
                    }
                    put(jl);
                }
            } catch (AS400SecurityException | RequestNotSupportedException | PropertyVetoException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                getLogger().log(Level.INFO, "Exception in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, "SQL Exception in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private void setSelectionActiveJobStatus(JobList jl, String jobtype) throws PropertyVetoException {
        switch (jobtype) {
            case "AUTOSTART":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, Job.JOB_TYPE_AUTOSTART);
                break;
            case "BATCH":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, Job.JOB_TYPE_BATCH);
                break;
            case "INTERACTIVE":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, Job.JOB_TYPE_INTERACTIVE);
                break;
            case "SUBSYSTEM_MONITOR":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, Job.JOB_TYPE_SUBSYSTEM_MONITOR);
                break;
            case "SPOOLED_READER":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, Job.JOB_TYPE_SPOOLED_READER);
                break;
            case "SYSTEM":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, Job.JOB_TYPE_SYSTEM);
                break;
            case "SPOOLED_WRITER":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, Job.JOB_TYPE_SPOOLED_WRITER);
                break;
            case "SCPF_SYSTEM":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, Job.JOB_TYPE_SCPF_SYSTEM);
                break;
            case "ALL":
                jl.addJobSelectionCriteria(JobList.SELECTION_JOB_TYPE, JobList.SELECTION_JOB_TYPE_ALL);
                break;
            default:
                getLogger().log(Level.WARNING, "Unknown jobtype {0} in {1}", new Object[]{jobtype, getNameAndDescription()});
        }
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return joblist(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}