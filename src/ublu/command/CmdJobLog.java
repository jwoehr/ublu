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

import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.JobLog;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics.QueuedMessageList;
import ublu.util.Tuple;

/**
 * Command to manipulate job logs on the server.
 *
 * @author jax
 */
public class CmdJobLog extends Command {

    {
        setNameAndDescription("joblog", "/0 [-as400 ~@as400] [--,-joblog ~@joblog] [-to datasink] [-msgfile ~@{/full/ifs/path/}] [-onthread ~@tf] [-subst ~@{message_substitution}] [ -add ~@{int_attrib} | -clear | -close | -dir ~@tf | -length | -new ~@{jobname} ~@{jobuser} ~@{jobnumber} | -qm ~@{offset} ~@{number} | -query ~@{dir|name|user|number|sys} | -write ~@{message_id} ~@{COMPLETION|DIAGNOSTIC|INFORMATIONAL|ESCAPE} ] : manipulate job logs on the host");
    }

    enum OPS {
        ADD, CLEAR, DIR, CLOSE, LENGTH, NEW, NOOP, QM, QUERY, WRITE
    }

    /**
     * Manipulate job logs on the server
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray jobLog(ArgArray argArray) {
        Tuple jobLogTuple = null;
        OPS op = OPS.NOOP;
        String jobName = null;
        String jobUser = null;
        String jobNumber = null;
        Integer attrib = null;
        Integer messageOffset = null;
        Integer numberMessages = null;
        String substitutionData = null;
        String messageFileIFSPath = null;
        String message_id = null;
        String message_type = null;
        Boolean onThread = null;
        Boolean direction = null;
        String query = null;
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
                case "--":
                case "-joblog":
                    jobLogTuple = argArray.nextTupleOrPop();
                    break;
                case "-add":
                    op = OPS.ADD;
                    attrib = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-clear":
                    op = OPS.CLEAR;
                    break;
                case "-close":
                    op = OPS.CLOSE;
                    break;
                case "-dir":
                    op = OPS.DIR;
                    direction = argArray.nextBooleanTupleOrPop();
                    break;
                case "-length":
                    op = OPS.LENGTH;
                    break;
                case "-msgfile":
                    messageFileIFSPath = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-new":
                    op = OPS.NEW;
                    jobName = argArray.nextMaybeQuotationTuplePopString();
                    jobUser = argArray.nextMaybeQuotationTuplePopString();
                    jobNumber = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-onthread":
                    onThread = argArray.nextBooleanTupleOrPop();
                    break;
                case "-qm":
                    op = OPS.QM;
                    messageOffset = argArray.nextIntMaybeQuotationTuplePopString();
                    numberMessages = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-query":
                    op = OPS.QUERY;
                    query = argArray.nextMaybeQuotationTupleString();
                    break;
                case "-subst":
                    substitutionData = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-write":
                    message_id = argArray.nextMaybeQuotationTuplePopString();
                    message_type = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            JobLog jobLog = jobLogTuple.value(JobLog.class);
            switch (op) {
                case ADD:
                    if (jobLog != null) {
                        try {
                            jobLog.addAttributeToRetrieve(attrib);
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't set JobLog attribute in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JobLog instance for -add in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CLEAR:
                    if (jobLog != null) {
                        jobLog.clearAttributesToRetrieve();
                    } else {
                        getLogger().log(Level.SEVERE, "No JobLog instance for -clear in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CLOSE:
                    if (jobLog != null) {
                        try {
                            jobLog.close();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't close JobLog instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JobLog instance for -length in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DIR:
                    if (jobLog != null) {
                        jobLog.setListDirection(direction);
                    } else {
                        getLogger().log(Level.SEVERE, "No JobLog instance for -clear in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NEW:
                    jobLog = new JobLog(getAs400(), jobName, jobUser, jobNumber);
                     {
                        try {
                            put(jobLog);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put JobLog instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case LENGTH:
                    if (jobLog != null) {
                        try {
                            put(jobLog.getLength());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put JobLog length in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JobLog for -length in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case QM:
                    if (jobLog != null) {
                        try {
                            jobLog.load();
                            put(new QueuedMessageList(jobLog.getMessages(messageOffset, numberMessages)));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put JobLog instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JobLog for queued message list in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case QUERY:
                    if (jobLog != null && query != null) {
                        Object q = null;
                        switch (query.toLowerCase()) {
                            case "dir":
                                q = jobLog.getListDirection();
                                break;
                            case "name":
                                q = jobLog.getName();
                                break;
                            case "user":
                                q = jobLog.getUser();
                                break;
                            case "number":
                                q = jobLog.getNumber();
                                break;
                            case "sys":
                                q = jobLog.getSystem();
                                break;
                            default:
                                getLogger().log(Level.SEVERE, "Unknown query {0} in {1}", new Object[]{query, getNameAndDescription()});
                                setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (q != null) {
                            try {
                                put(q);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Couldn't put query result in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JobLog  or query for query in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case WRITE:
                    if (getAs400() != null) {
                        try {
                            JobLog.writeMessage(getAs400(),
                                    message_id,
                                    messageTypeFromString(message_type),
                                    messageFileIFSPath,
                                    substitutionData == null ? null : substitutionData.getBytes(),
                                    onThread
                            );
                        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't write message to job log in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No as400 for write in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NOOP:
                    break;
            }
        }
        return argArray;
    }

    private Integer messageTypeFromString(String messageType) {
        Integer result = null;
        switch (messageType.toUpperCase()) {
            case "COMPLETION":
                result = AS400Message.COMPLETION;
                break;
            case "DIAGNOSTIC":
                result = AS400Message.DIAGNOSTIC;
                break;
            case "INFORMATIONAL":
                result = AS400Message.INFORMATIONAL;
                break;
            case "ESCAPE":
                result = AS400Message.ESCAPE;
                break;
        }
        return result;

    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return jobLog(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
