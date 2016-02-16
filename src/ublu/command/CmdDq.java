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
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IllegalObjectTypeException;
import com.ibm.as400.access.ObjectAlreadyExistsException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to manipulate data queues
 *
 * @author jwoehr
 */
public class CmdDq extends Command {

    {
        setNameAndDescription("dq", "/4? [-as400 @as400] [--,-dq ~@dq] -clear | -create ~@{maxentrylength} | -delete | -exists | -instance | -peek | -query [ ccsid | description | fifo | forceauxstorage | maxentrylength | name | path | savesender | system ] | -read | -write ~@{data to write} ~@{dataqueuepath} ~@{system} ~@{userid} ~@{password} : manipulate a data queue on the host");
    }

    /**
     * What we do
     */
    protected enum FUNCTIONS {

        /**
         * Clear queue
         */
        CLEAR,
        /**
         * Create queue
         */
        CREATE,
        /**
         * Delete queue
         */
        DELETE,
        /**
         * Test for existence
         */
        EXISTS,
        /**
         * Instance a queue
         */
        INSTANCE,
        /**
         * Do nothing
         */
        NOOP,
        /**
         * Peek next entry
         */
        PEEK,
        /**
         * Get info
         */
        QUERY,
        /**
         * Read next entry
         */
        READ,
        /**
         * Write to the queue
         */
        WRITE
    }

    /**
     * Perform the work of getting a Data Queue object and manipulating it.
     *
     * @param argArray the input arg array
     * @return what's left of the arg array
     */
    public ArgArray dq(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.INSTANCE;
        Tuple dqTuple = null;
        int dqMaxLen = 0;
        String theQuery = null;
        String dataToWrite = null;
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
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
                case "--":
                case "-dq":
                    dqTuple = argArray.nextTupleOrPop();
                    break;
                case "-clear":
                    function = FUNCTIONS.CLEAR;
                    break;
                case "-create":
                    function = FUNCTIONS.CREATE;
                    dqMaxLen = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-delete":
                    function = FUNCTIONS.DELETE;
                    break;
                case "-exists":
                    function = FUNCTIONS.EXISTS;
                    break;
                case "-instance":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-noop":
                    function = FUNCTIONS.NOOP;
                    break;
                case "-peek":
                    function = FUNCTIONS.PEEK;
                    break;
                case "-query":
                    function = FUNCTIONS.QUERY;
                    theQuery = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-read":
                    function = FUNCTIONS.READ;
                    break;
                case "-write":
                    function = FUNCTIONS.WRITE;
                    dataToWrite = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            DataQueue myDq = null;
            if (dqTuple != null) {
                Object tupleValue = dqTuple.getValue();
                if (tupleValue instanceof DataQueue) {
                    myDq = DataQueue.class.cast(tupleValue);
                } else {
                    getLogger().log(Level.WARNING, "Valued tuple which is not a data queue tuple provided to -dq in {0}", getNameAndDescription());
                }
            }
            if (myDq == null) { // no provided DQ instance
                if (argArray.size() < 1) {
                    logArgArrayTooShortError(argArray);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else { // get the Job factors
                    String dqPath = argArray.nextMaybeQuotationTuplePopString();
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
                        myDq = new DataQueue(getAs400(), dqPath);
                    }
                }
            }
            if (myDq != null) {
                switch (function) {
                    case CLEAR:
                        try {
                            myDq.clear();
                        } catch (IllegalObjectTypeException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error testing dataqueue existence or putting result for " + myDq.getName() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case CREATE:
                        try {
                            myDq.create(dqMaxLen);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectAlreadyExistsException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error creating dataqueue " + myDq.getName() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case DELETE:
                        try {
                            myDq.delete();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | IllegalObjectTypeException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error deleting dataqueue " + myDq.getName() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case EXISTS:
                        try {
                            put(myDq.exists());
                        } catch (IllegalObjectTypeException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error testing dataqueue existence or putting result for " + myDq.getName() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case INSTANCE:
                        try {
                            put(myDq);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting dataqueue instance " + myDq.getName() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case PEEK:
                        try {
                            put(myDq.peek());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | IllegalObjectTypeException ex) {
                            getLogger().log(Level.SEVERE, "Error peeking dataqueue instance " + myDq.getName() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case QUERY:
                        try {
                            myDq.refreshAttributes();
                            switch (theQuery) {
                                case "ccsid":
                                    put(myDq.getCcsid());
                                    break;
                                case "description":
                                    put(myDq.getDescription());
                                    break;
                                case "fifo":
                                    put(myDq.isFIFO());
                                    break;
                                case "forceauxstorage":
                                    put(myDq.getForceToAuxiliaryStorage());
                                    break;
                                case "maxentrylength":
                                    put(myDq.getMaxEntryLength());
                                    break;
                                case "name":
                                    put(myDq.getName());
                                    break;
                                case "path":
                                    put(myDq.getPath());
                                    break;
                                case "savesender":
                                    put(myDq.getSaveSenderInformation());
                                    break;
                                case "system":
                                    put(myDq.getSystem());
                                    break;
                                default:
                                    getLogger().log(Level.SEVERE, "Unknown dataqueue query for {0} in {1}", new Object[]{myDq.getName(), getNameAndDescription()});
                                    setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } catch (AS400SecurityException | SQLException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | IllegalObjectTypeException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error querying dataqueue instance " + myDq.getName() + "or putting result in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case READ:
                        try {
                            put(myDq.read());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | IllegalObjectTypeException ex) {
                            getLogger().log(Level.SEVERE, "Error peeking reading instance " + myDq.getName() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case WRITE:
                        try {
                            myDq.write(dataToWrite);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | IllegalObjectTypeException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error writing to data queue " + myDq.getName() + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case NOOP:
                        break;
                }
            } else {
                getLogger().log(Level.SEVERE, "Unable to get Job instance in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return dq(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}