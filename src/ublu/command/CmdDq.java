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
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IllegalObjectTypeException;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.ObjectAlreadyExistsException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.Generics.ByteArrayList;
import ublu.util.Tuple;

/**
 * Command to manipulate data queues
 *
 * @author jwoehr
 */
public class CmdDq extends Command {

    {
        setNameAndDescription("dq", "/4? [-as400 @as400] [--,-dq ~@dq] [-wait ~@{intwaitseconds}] [-authority *ALL|*CHANGE|*EXCLUDE|*USE|*LIBCRTAUT]"
                + " [-saveSenderInformation ~@tf] [-FIFO ~@tf] [-forceToAuxiliaryStorage ~@tf] [-desc ~@{description}] [-keyed ~@tf] [-keylen ~@{intlength}]"
                + " [-key ~@{key}] [-bkey ~@bytekey] [-searchtype  EQ|NE|LT|LE|GT|GE]"
                + " [-clear | -create ~@{maxentrylength} | -delete | -exists | -new,-instance | -peek | -query [ ccsid | description | fifo | forceauxstorage"
                + " | keylen | maxentrylength | name | path | savesender | system ] | -read | -write ~@{data to write} | writeb ~@bytedata] "
                + "~@{dataqueuepath} ~@{system} ~@{userid} ~@{password} : manipulate a data queue on the host");
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
        Tuple dqTuple;
        byte[] byteData = null;
        byte[] byteKey = null;
        DataQueue myDq = null;
        KeyedDataQueue myKDq = null;
        // ---------------- //
        // Creation attributes
        int maxEntryLength = 0;
        String authority = "*LIBCRTAUT";
        boolean saveSenderInformation = false;
        boolean FIFO = true;
        boolean forceToAuxiliaryStorage = false;
        String description = "Created by Ublu";
        // Keyed Q?
        boolean keyed = false;
        Integer keyLength = null;
        String key = null;
        // byte[] bkey = null;
        String searchType = "EQ";
        // ---------------- //

        int waitSeconds = 0;
        String theQuery = null;
        String dataToWrite = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
//                case "-from":
//                    String srcName = argArray.next();
//                    setDataSrc(DataSink.fromSinkName(srcName));
//                    break;
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "--":
                case "-dq":
                    dqTuple = argArray.nextTupleOrPop();
                    myDq = dqTuple.value(DataQueue.class);
                    if (myDq == null) {
                        myKDq = dqTuple.value(KeyedDataQueue.class);
                    }
                    break;
                case "-authority":
                    authority = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-saveSenderInformation":
                    saveSenderInformation = argArray.nextBooleanTupleOrPop();
                    break;
                case "-FIFO":
                    FIFO = argArray.nextBooleanTupleOrPop();
                    break;
                case "-forceToAuxiliaryStorage":
                    forceToAuxiliaryStorage = argArray.nextBooleanTupleOrPop();
                    break;
                case "-desc":
                    description = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-keylen":
                    keyLength = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-clear":
                    function = FUNCTIONS.CLEAR;
                    break;
                case "-create":
                    function = FUNCTIONS.CREATE;
                    maxEntryLength = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-delete":
                    function = FUNCTIONS.DELETE;
                    break;
                case "-exists":
                    function = FUNCTIONS.EXISTS;
                    break;
                case "-new":
                case "-instance":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-bkey":
                    byteKey = byteTupleToArray(argArray.nextTupleOrPop());
                    break;
                case "-key":
                    key = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-keyed":
                    keyed = argArray.nextBooleanTupleOrPop();
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
                case "-searchtype":
                    searchType = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-write":
                    function = FUNCTIONS.WRITE;
                    dataToWrite = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-writeb":
                    function = FUNCTIONS.WRITE;
                    byteData = byteTupleToArray(argArray.nextTupleOrPop());
                    break;
                case "-wait":
                    waitSeconds = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (myDq == null && myKDq == null) { // no provided DQ instance
                if (argArray.size() < 1) {
                    logArgArrayTooShortError(argArray);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
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
                        if (keyed) {
                            myKDq = new KeyedDataQueue(getAs400(), dqPath);
                        } else {
                            myDq = new DataQueue(getAs400(), dqPath);
                        }
                    }
                }
            }
            if (myDq != null || myKDq != null) {
                switch (function) {
                    case CLEAR:
                        try {
                            if (byteKey != null) {
                                myKDq.clear(byteKey);
                            } else {
                                (myDq != null ? myDq : myKDq).clear();
                            }
                        } catch (IllegalObjectTypeException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error testing dataqueue existence or putting result for " + (myDq != null ? myDq : myKDq).getName() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case CREATE:
                        try {
                            if (myDq != null) {
                                myDq.create(maxEntryLength, authority, saveSenderInformation, FIFO, forceToAuxiliaryStorage, description);
                            } else {
                                myKDq.create(keyLength, maxEntryLength, authority, saveSenderInformation, forceToAuxiliaryStorage, description);
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectAlreadyExistsException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error creating dataqueue " + myDq.getName() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case DELETE:
                        try {
                            (myDq != null ? myDq : myKDq).delete();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | IllegalObjectTypeException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error deleting dataqueue " + myDq.getName() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case EXISTS:
                        try {
                            put((myDq != null ? myDq : myKDq).exists());
                        } catch (IllegalObjectTypeException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error testing dataqueue existence or putting result for " + myDq.getName() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case INSTANCE:
                        try {
                            if (myDq != null) {
                                put(myDq);
                            } else {
                                put(myKDq);
                            }
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting dataqueue instance " + myDq.getName() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case PEEK:
                        try {
                            if (myDq != null) {
                                put(myDq.peek(waitSeconds));
                            } else {
                                if (byteKey == null) {
                                    put(myKDq.peek(key, waitSeconds, searchType));
                                } else {
                                    put(myKDq.peek(byteKey, waitSeconds, searchType));
                                }
                            }
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | IllegalObjectTypeException ex) {
                            getLogger().log(Level.SEVERE, "Error peeking dataqueue instance " + myDq.getName() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case QUERY:
                        try {
                            (myDq != null ? myDq : myKDq).refreshAttributes();
                            switch (theQuery) {
                                case "ccsid":
                                    put((myDq != null ? myDq : myKDq).getCcsid());
                                    break;
                                case "description":
                                    put((myDq != null ? myDq : myKDq).getDescription());
                                    break;
                                case "fifo":
                                    put((myDq != null ? myDq : myKDq).isFIFO());
                                    break;
                                case "forceauxstorage":
                                    put((myDq != null ? myDq : myKDq).getForceToAuxiliaryStorage());
                                    break;
                                case "keylen":
                                    if (myKDq == null) {
                                        getLogger().log(Level.SEVERE, "query keylen only applies to keyed dataqueue in {0}", getNameAndDescription());
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    } else {
                                        put(myKDq.getKeyLength());
                                    }
                                    break;
                                case "maxentrylength":
                                    put((myDq != null ? myDq : myKDq).getMaxEntryLength());
                                    break;
                                case "name":
                                    put((myDq != null ? myDq : myKDq).getName());
                                    break;
                                case "path":
                                    put((myDq != null ? myDq : myKDq).getPath());
                                    break;
                                case "savesender":
                                    put((myDq != null ? myDq : myKDq).getSaveSenderInformation());
                                    break;
                                case "system":
                                    put((myDq != null ? myDq : myKDq).getSystem());
                                    break;
                                default:
                                    getLogger().log(Level.SEVERE, "Unknown dataqueue query for {0} in {1}", new Object[]{(myDq != null ? myDq : myKDq).getName(), getNameAndDescription()});
                                    setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } catch (AS400SecurityException | SQLException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | IllegalObjectTypeException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error querying dataqueue instance " + (myDq != null ? myDq : myKDq).getName() + "or putting result in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case READ:
                        try {
                            if (myDq != null) {
                                put(myDq.read(waitSeconds));
                            } else {
                                if (byteKey != null) {
                                    put(myKDq.read(byteKey, waitSeconds, searchType));
                                } else {
                                    put(myKDq.read(key, waitSeconds, searchType));
                                }
                            }
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | IllegalObjectTypeException ex) {
                            getLogger().log(Level.SEVERE, "Error peeking reading instance " + (myDq != null ? myDq : myKDq).getName() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case WRITE:
                        try {
                            if (myDq != null) {
                                if (byteData == null) {
                                    myDq.write(dataToWrite);
                                } else {
                                    myDq.write(byteData);
                                }
                            } else {
                                if (byteKey == null) {
                                    if (dataToWrite == null) {
                                        getLogger().log(Level.SEVERE, "String key for keyed dataqueue but no string data to write in {0}", getNameAndDescription());
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    } else {
                                        myKDq.write(key, dataToWrite);
                                    }
                                } else {
                                    if (byteData == null) {
                                        getLogger().log(Level.SEVERE, "Byte key for keyed dataqueue but no byte data to write in {0}", getNameAndDescription());
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    } else {
                                        myKDq.write(byteKey, byteData);
                                    }
                                }
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | IllegalObjectTypeException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error writing to data queue " + (myDq != null ? myDq : myKDq).getName() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case NOOP:
                        break;
                }
            } else {
                getLogger().log(Level.SEVERE, "Unable to get Dq instance in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private byte[] byteTupleToArray(Tuple byteTuple) {
        byte[] result;
        ByteArrayList bal = byteTuple.value(ByteArrayList.class);
        if (bal != null) {
            result = bal.byteArray();
        } else {
            result = byteTuple.value(byte[].class);
        }
        return result;
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
