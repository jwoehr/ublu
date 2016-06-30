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

import ublu.ReportFetcher;
import ublu.TransformedSpooledFileFetcher;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.SpoolFHelper;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.OutputQueue;
import com.ibm.as400.access.PrintObject;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.PrinterFile;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.SpooledFileOutputStream;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to operate on individual spooled files. Basically a wrapper for
 * JTOpen SpooledFile object.
 *
 * @author jwoehr
 */
public class CmdSpoolF extends Command {

    {
        setNameAndDescription("spoolf",
                "/8? [-as400 ~@as400] [--,-spoolf ~@spoolf] [-to datasink] [-tofile ~@filename ] [[-answermsg ~@{ some text }] | [-copy] | [-copyto ~@remote_as400] | [-copyq ~@outq] | [-create] | [-delete] | -fetch | [-get createdate | createtime | jobname | jobnumber | jobsysname | jobuser | message | name | number] | [-hold [-immed|-pageend]] | [-instance ] | [-move ~@spoolf_before_me] | [-moveq ~@{outq_on_same_system}] | [-release] | [-sendtcp ~@remotesysname ~@remoteprintqueuepath] [-top] [printerfile ~@printerfile] [-ppl ~@ppl] [-outq ~@outq]] system user password name number jobname jobuser jobnumber  : operate on an individual spooled file");
    }

    /**
     * The ops we know how to perform
     */
    protected enum OPERATIONS {

        /**
         * Replies to the message that caused the spooled file to wait.
         */
        ANSWERMSG,
        /**
         * Creates a copy of the spooled file this object represents in its
         * current queue
         */
        COPY,
        /**
         * Copy spool file to a different system
         */
        COPYTO,
        /**
         * Creates a copy of the spooled file this object represents in a
         * provided queue
         */
        COPYQ,
        /**
         * Deletes from queue
         */
        CREATE,
        /**
         * Create a spooled file from data
         */
        DELETE,
        /**
         * Fetch transformed like CmdFetch
         */
        FETCH,
        /**
         * Get info factors
         */
        GET,
        /**
         * Holds
         */
        HOLD,
        /**
         * Returns instance
         */
        INSTANCE,
        /**
         * Moves to another position in queue
         */
        MOVE,
        /**
         * Moves to another queue
         */
        MOVEQ,
        /**
         * Releases
         */
        RELEASE,
        /**
         * Sends to another system via TCP/IP
         */
        SENDTCP,
        /**
         * Move to top of queue
         */
        TOP
    }

    /**
     * Command to operate on individual spooled files.
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray spoolF(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.INSTANCE;
        Tuple mySpooledFileTuple = null;
        String remoteSys = null;
        String remotePrintQueue = null;
        Tuple otherQueueTuple = null;
        Tuple remoteAS400Tuple = null;
        Tuple spoolfToMoveMeAfterTuple = null;
        String answerMessage = null;
        String holdType = "*IMMED";
        String infoToGet = null;
        /* These for creation of a spool file */
        PrinterFile creationPrinterFile = null;
        Tuple creationPrinterFileTuple = null;
        PrintParameterList creationPPL = null;
        Tuple creationPPLTuple = null;
        OutputQueue creationOutQ = null;
        Tuple creationOutQTuple = null;
        /* ***** */
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
                case "-from":
                    String srcName = argArray.next();
                    setDataSrc(DataSink.fromSinkName(srcName));
                    break;
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-tofile":
                    String destFileName = argArray.nextMaybeQuotationTuplePopString();
                    setDataDest(DataSink.fromSinkName(destFileName));
                    break;
                case "--":
                case "-spoolf":
                    mySpooledFileTuple = argArray.nextTupleOrPop();
                    break;
                case "-answermsg":
                    operation = OPERATIONS.ANSWERMSG;
                    answerMessage = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-copy":
                    operation = OPERATIONS.COPY;
                    break;
                case "-copyto":
                    operation = OPERATIONS.COPYTO;
                    remoteAS400Tuple = argArray.nextTupleOrPop();
                    break;
                case "-copyq":
                    operation = OPERATIONS.COPYQ;
                    otherQueueTuple = argArray.nextTupleOrPop();
                    break;
                case "-create":
                    operation = OPERATIONS.CREATE;
                    break;
                case "-delete":
                    operation = OPERATIONS.DELETE;
                    break;
                case "-fetch":
                    operation = OPERATIONS.FETCH;
                    break;
                case "-get":
                    operation = OPERATIONS.GET;
                    infoToGet = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-hold":
                    operation = OPERATIONS.HOLD;
                    break;
                case "-immed":
                    holdType = "*IMMED";
                    break;
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-move":
                    operation = OPERATIONS.MOVE;
                    spoolfToMoveMeAfterTuple = argArray.nextTupleOrPop();
                    break;
                case "-moveq":
                    operation = OPERATIONS.MOVEQ;
                    otherQueueTuple = argArray.nextTupleOrPop();
                    break;
                case "-pageend":
                    holdType = "*PAGEEND";
                    break;
                case "-printerfile":
                    creationPrinterFileTuple = argArray.nextTupleOrPop();
                    break;
                case "-ppl":
                    creationPPLTuple = argArray.nextTupleOrPop();
                    break;
                case "-outq":
                    creationOutQTuple = argArray.nextTupleOrPop();
                    break;
                case "-release":
                    operation = OPERATIONS.RELEASE;
                    break;
                case "-sendtcp":
                    operation = OPERATIONS.SENDTCP;
                    remoteSys = argArray.nextMaybeQuotationTuplePopString();
                    remotePrintQueue = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-top":
                    operation = OPERATIONS.TOP;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            SpooledFile mySpooledFile = null;
            if (mySpooledFileTuple != null) { // Was a tuple provided to the -spoolf switch?
                Object o = mySpooledFileTuple.getValue();
                if (o instanceof SpooledFile) {
                    mySpooledFile = SpooledFile.class.cast(o);
                } else {
                    getLogger().log(Level.SEVERE, "Tuple was not instance of SpooledFile in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else if (operation != OPERATIONS.CREATE) {
                mySpooledFile = getSpooledFileFromArgs(argArray);
            }
            if (operation != OPERATIONS.CREATE && mySpooledFile == null) {
                getLogger().log(Level.SEVERE, "Unable to instance spooled file in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                SpooledFile theCopy = null;
                Object o = null;
                switch (operation) {
                    case ANSWERMSG:
                        try {
                            mySpooledFile.answerMessage(answerMessage);
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Unable to answer message for spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Unable to answer message for spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Unable to answer message for spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case COPY:
                        try {
                            theCopy = mySpooledFile.copy();
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Unable to copy spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Unable to copy spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        try {
                            put(theCopy);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Unable to put copy of spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case COPYTO:
                        SpooledFile remoteCopy = null;
                        AS400 remoteAS400;
                        if (remoteAS400Tuple == null) {
                            getLogger().log(Level.SEVERE, "No AS400 tuple provided to -copyto in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            o = remoteAS400Tuple.getValue();
                            if (o instanceof AS400) {
                                remoteAS400 = AS400.class.cast(o);
                                try {
                                    SpoolFHelper splfh = new SpoolFHelper(mySpooledFile);
                                    remoteCopy = splfh.copy(remoteAS400, null, splfh.defaultPrinterFile(remoteAS400), splfh.defaultOutputQueue(remoteAS400));
                                } catch (AS400Exception ex) {
                                    getLogger().log(Level.SEVERE, "Unable to copy spooled file to remote system " + remoteAS400 + " in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException ex) {
                                    getLogger().log(Level.SEVERE, "Unable to copy spooled file to remote system " + remoteAS400 + " in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } catch (ErrorCompletingRequestException ex) {
                                    getLogger().log(Level.SEVERE, "Unable to copy spooled file to remote system " + remoteAS400 + " in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                                if (remoteCopy != null) {
                                    try {
                                        put(remoteCopy);
                                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                        getLogger().log(Level.SEVERE, "Unable to put remote spooled file object in " + getNameAndDescription(), ex);
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    }
                                }
                            } else {
                                getLogger().log(Level.SEVERE, "Tuple provided to -copyto is not an AS400 instance in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case COPYQ:
                        if (otherQueueTuple != null) {
                            o = otherQueueTuple.getValue();
                        }
                        if (o == null || !(o instanceof OutputQueue)) {
                            getLogger().log(Level.SEVERE, "Tuple provided to -copyq does not represent an output queue in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                theCopy = mySpooledFile.copy(OutputQueue.class.cast(o));
                            } catch (AS400Exception ex) {
                                getLogger().log(Level.SEVERE, "Unable to -copyq spooled file in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Unable to -copyq spooled file in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } catch (ErrorCompletingRequestException ex) {
                                getLogger().log(Level.SEVERE, "Unable to -copyq spooled file in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            try {
                                put(theCopy);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Unable to put copy of spooled file in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case CREATE:
                        if (getAs400() == null) {
                            getLogger().log(Level.SEVERE, "No as400 instance to create SpooledFileOutputStream in -create operation for {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            SpooledFileOutputStream sfos;
                            if (creationPPLTuple != null) {
                                o = creationPPLTuple.getValue();
                                if (o instanceof PrintParameterList) {
                                    creationPPL = PrintParameterList.class.cast(o);
                                } else {
                                    getLogger().log(Level.SEVERE, "-ppl in -create operation for {0} doesn't represent a PrintParameterList", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                            if (getCommandResult() != COMMANDRESULT.FAILURE && creationPrinterFileTuple != null) {
                                o = creationPrinterFileTuple.getValue();
                                if (o instanceof PrinterFile) {
                                    creationPrinterFile = PrinterFile.class.cast(o);
                                } else {
                                    getLogger().log(Level.SEVERE, "-printerfile in -create operation for {0} doesn't represent a PrinterFile", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                            if (getCommandResult() != COMMANDRESULT.FAILURE && creationOutQTuple != null) {
                                o = creationOutQTuple.getValue();
                                if (o instanceof OutputQueue) {
                                    creationOutQ = OutputQueue.class.cast(o);
                                } else {
                                    getLogger().log(Level.SEVERE, "-outq in -create operation for {0} doesn't represent an OutputQueueF", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                            if (getCommandResult() != COMMANDRESULT.FAILURE) {
                                try {
                                    sfos = new SpooledFileOutputStream(getAs400(), creationPPL, creationPrinterFile, creationOutQ);
                                    sfos.write(dataFromDataSource());
                                    sfos.flush();
                                    SpooledFile justCreated = sfos.getSpooledFile();
                                    sfos.close();
                                    put(justCreated);
                                } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException ex) {
                                    getLogger().log(Level.SEVERE, "Exception creating SpooledFileOutputStream in -create operation for " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } catch (SQLException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                    getLogger().log(Level.SEVERE, "Exception putting SpooledFileOutputStream in -create operation for " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                        }
                        break;
                    case DELETE:
                        try {
                            mySpooledFile.delete();
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Unable to delete spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | IOException | InterruptedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Unable to delete spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case FETCH:
                        TransformedSpooledFileFetcher tsff = new TransformedSpooledFileFetcher(mySpooledFile);
                        try {
                            put(ReportFetcher.fetchTidied(tsff));
                        } catch (AS400Exception | AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Unable to fetch or to put transformed spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (ErrorCompletingRequestException | SQLException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Unable to fetch or to put transformed spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case GET:
                        String gotInfo = getInfoFactor(mySpooledFile, infoToGet);
                        if (gotInfo == null) {
                            getLogger().log(Level.SEVERE, "Unable to get requested info in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                put(gotInfo);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Unable to instance spooled file in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case HOLD:
                        try {
                            mySpooledFile.hold(holdType);
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Unable to hold spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Unable to hold spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Unable to release spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case INSTANCE:
                        try {
                            put(mySpooledFile);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Unable to instance spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case MOVE:
                        if (spoolfToMoveMeAfterTuple != null) {
                            o = spoolfToMoveMeAfterTuple.getValue();
                        }
                        if (o == null) {
                            getLogger().log(Level.SEVERE, "Spooled file to move after is null in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else if (!(o instanceof SpooledFile)) {
                            getLogger().log(Level.SEVERE, "Spooled file to move after tuple did not reference a spooled file in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            SpooledFile spooledFileToMoveMeAfter = SpooledFile.class.cast(o);
                            try {
                                mySpooledFile.move(spooledFileToMoveMeAfter);
                            } catch (AS400Exception ex) {
                                getLogger().log(Level.SEVERE, "Unable to move spooled file after another in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException | ErrorCompletingRequestException ex) {
                                getLogger().log(Level.SEVERE, "Unable to move spooled file after another in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case MOVEQ:
                        if (otherQueueTuple != null) {
                            o = otherQueueTuple.getValue();
                        }
                        if (o == null || !(o instanceof OutputQueue)) {
                            getLogger().log(Level.SEVERE, "Tuple provided to -copyq does not represent an output queue in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                mySpooledFile.move(OutputQueue.class.cast(o));
                            } catch (AS400Exception ex) {
                                getLogger().log(Level.SEVERE, "Unable to -copyq spooled file in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException | ErrorCompletingRequestException ex) {
                                getLogger().log(Level.SEVERE, "Unable to -copyq spooled file in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            try {
                                put(theCopy);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Unable to put copy of spooled file in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case RELEASE:
                        try {
                            mySpooledFile.release();
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Unable to release spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Unable to release spooled file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case SENDTCP:
                        PrintParameterList ppl = new PrintParameterList();
                        ppl.setParameter(PrintObject.ATTR_RMTSYSTEM, remoteSys);
                        ppl.setParameter(PrintObject.ATTR_RMTPRTQ, remotePrintQueue);
                        try {
                            mySpooledFile.sendTCP(ppl);
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Unable to send spooled file via tcp in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | IOException | InterruptedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Unable to send spooled file via tcp in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case TOP:
                        try {
                            mySpooledFile.moveToTop();
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Unable to move spooled file to top in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Unable to move spooled file to top in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                }
            }
        }
        return argArray;
    }

    private SpooledFile getSpooledFileFromArgs(ArgArray argArray) {
        SpooledFile splf = null;
        AS400 a = getAs400();
        if (a == null) {
            try {
                setAs400(as400FromArgs(argArray));
            } catch (PropertyVetoException ex) {
                getLogger().log(Level.SEVERE, "Could not instance AS400 system from provided arguments in " + getNameAndDescription(), ex);
            }
        }
        if (a != null) {
            String name = argArray.nextMaybeQuotationTuplePopString();
            int number = argArray.nextIntMaybeTupleString();
            String jobName = argArray.nextMaybeQuotationTuplePopString();
            String jobUser = argArray.nextMaybeQuotationTuplePopString();
            String jobNumber = argArray.nextMaybeQuotationTuplePopString();
            splf = new SpooledFile(a, name, number, jobName, jobUser, jobNumber);
        }
        return splf;
    }

    private String getInfoFactor(SpooledFile splf, String infoFactor) {
        String result = null;
        if (infoFactor != null) {
            switch (infoFactor) {
                case "createdate":
                    result = splf.getCreateDate();
                    break;
                case "createtime":
                    result = splf.getCreateTime();
                    break;
                case "jobname":
                    result = splf.getJobName();
                    break;
                case "jobnumber":
                    result = splf.getJobNumber();
                    break;
                case "jobsysname":
                    result = splf.getJobSysName();
                    break;
                case "jobuser":
                    result = splf.getJobUser();
                    break;
                case "message":
                    try {
                        AS400Message msg = splf.getMessage();
                        result = msg.toString();
                    } catch (AS400Exception ex) {
                        getLogger().log(Level.SEVERE, "Unable to get message in " + getNameAndDescription(), ex);
                    } catch (AS400SecurityException | IOException | InterruptedException | ErrorCompletingRequestException ex) {
                        getLogger().log(Level.SEVERE, "Unable to get message in " + getNameAndDescription(), ex);
                    }
                    break;
                case "name":
                    result = splf.getName();
                    break;
                case "number":
                    result = Integer.toString(splf.getNumber());
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unknown get info request in {0}", getNameAndDescription());
            }
        }
        return result;
    }

    private byte[] dataFromDataSource() {
        byte[] data = null;
        String filepathspec;
        Path filepath = null;
        switch (getDataSrc().getType()) {
            case FILE:
                filepathspec = getDataSrc().getName();
                filepath = FileSystems.getDefault().getPath(filepathspec);
                File file = filepath.normalize().toFile();
                try (FileReader fileReader = new FileReader(file);
                        BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                    StringBuilder sb = new StringBuilder();
                    while (bufferedReader.ready()) {
                        sb.append(bufferedReader.readLine());
                    }
                    data = sb.toString().getBytes();
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "Exception opening file data source " + filepathspec + " in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
                break;
            case STD:
                getLogger().log(Level.SEVERE, "STD not implemented in " + getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
                break;
            case TUPLE:
                Tuple documentTuple = getInterpreter().getTuple(getDataSrc().getName());
                if (documentTuple == null) {
                    getLogger().log(Level.SEVERE, "Tuple " + getDataSrc().getName() + " does not exist in ", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    data = documentTuple.getValue().toString().getBytes();
                }
                break;
            case URL:
                getLogger().log(Level.SEVERE, "URL not implemented in " + getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
                break;
        }

        return data;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return spoolF(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
