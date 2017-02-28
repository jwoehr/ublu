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

import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400File;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.KeyedFile;
import com.ibm.as400.access.MemberList;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SequentialFile;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.ArgArray;
import ublu.util.Generics.RecordArrayList;
import ublu.util.Tuple;

/**
 * Command to access record files
 *
 * @author jwoehr
 */
public class CmdFile extends Command {

    {
        setNameAndDescription("file",
                "/4? [-to @var ] [--,-file ~@file] [-as400 ~@as400] [-keyed | -sequential] [-new | -create ~@{recordLength} ~@{fileType([*DATA|*SOURCE])} ~@{textDescription} | -createdds  ~@{ddsPath}  ~@{textDescription} | -createfmt ~@recFormat  ~@{textDescription} | -commitstart ~@{lockLevel([ALL|CHANGE|STABLE])} | -commit | -rollback | -commitend | -lock ~@{locktype(RX|RSR|RSW|WX|WSR|WSW)} | -unlock | -del | -delmemb | -delrec | -getfmt | -setfmt ~@format | -open ~@{R|W|RW} | -close | -list | -pos ~@{B|F|P|N|L|A} | -recfmtnum ~@{int} | -read ~@{CURR|FIRST|LAST|NEXT|PREV|ALL} | -update ~@record | -write ~@record | -writeall ~@recordarray ] [-to datasink] ~@{/fully/qualified/ifspathname} ~@{system} ~@{user} ~@{password} : record file access");
    }

    /**
     * The functions performed by the file command
     */
    protected static enum FUNCTIONS {
        /**
         * Instance file object
         */
        INSTANCE,
        /**
         * Create a Physical file
         */
        CREATE,
        /**
         * Create a Physical file using DDS
         */
        CREATEDDS,
        /**
         * Create a Physical file using a record format
         */
        CREATEFMT,
        /**
         * Delete a Physical file
         */
        DELETE,
        /**
         * Delete a record
         */
        DELRECORD,
        /**
         * Delete a member
         */
        DELMEMBER,
        /**
         * Get the record format
         */
        GETFORMAT,
        /**
         * Set the record format;
         */
        SETFORMAT,
        /**
         * Open a Physical file
         */
        OPEN,
        /**
         * Close a Physical file
         */
        CLOSE,
        /**
         * Start commit
         */
        COMMITSTART,
        /**
         * Commit
         */
        COMMIT,
        /**
         *
         */
        ROLLBACK,
        /**
         * End commit
         */
        COMMITEND,
        /**
         * List members of physical file
         */
        LIST,
        /**
         * Lock physical file
         */
        LOCK,
        /**
         * Unlock physical file
         */
        UNLOCK,
        /**
         * Nada
         */
        NOOP,
        /**
         * Position cursor
         */
        POS,
        /**
         * Get state of physical file
         */
        QUERY,
        /**
         * Read a Physical file
         */
        READ,
        /**
         * Update a record
         */
        UPDATE,
        /**
         * Write a Physical file
         */
        WRITE,
        /**
         * Write multiple a Physical file
         */
        WRITEALL

    }

    /**
     * Create instance
     */
    public CmdFile() {
    }

    /**
     * Parse arguments and perform AS400 File operations
     *
     * @param argArray passed-in arg array
     * @return rest of arg array
     */
    public ArgArray doFile(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.INSTANCE;
        Object o; // used for unloading tuples
        Boolean keyedNotSequential = null;
//        Tuple fileTuple;
        Tuple formatTuple = null;
        AS400File aS400File = null;
        String readCommand = "";
        String openTypeString;
        int openType = AS400File.READ_ONLY;
        int blockingFactor = 0;
        int commitLockLevel = AS400File.COMMIT_LOCK_LEVEL_NONE;
        String positionString = "";
        int recordFormatNumber = 0;
        Tuple recordTuple = null;
        /* Folling for various create */
        Integer recordLength = null;
        String fileType = null;
        String textDescription = null;
        String ddsPath = null;
        RecordFormat recFormat = null;
        RecordArrayList recordArrayList = null;
        String lockLevel = null;
        String lockTypeString;
        Integer lockType = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                case "-file":
                    aS400File = argArray.nextTupleOrPop().value(AS400File.class);
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "Encountered an exception getting a file instance from the supplied command arguments in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case "-keyed":
                    keyedNotSequential = true;
                    break;
                case "-sequential":
                    keyedNotSequential = false;
                    break;
                case "-create":
                    function = FUNCTIONS.CREATE;
                    recordLength = argArray.nextIntMaybeQuotationTuplePopString();
                    fileType = argArray.nextMaybeQuotationTuplePopString().toUpperCase();
                    textDescription = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-createdds":
                    function = FUNCTIONS.CREATEDDS;
                    ddsPath = argArray.nextMaybeQuotationTuplePopString();
                    textDescription = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-createfmt":
                    function = FUNCTIONS.CREATEFMT;
                    recFormat = argArray.nextTupleOrPop().value(RecordFormat.class);
                    textDescription = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-commitstart":
                    function = FUNCTIONS.COMMITSTART;
                    lockLevel = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-commit":
                    function = FUNCTIONS.COMMIT;
                    break;
                case "-rollback":
                    function = FUNCTIONS.ROLLBACK;
                    break;
                case "-commitend":
                    function = FUNCTIONS.COMMITEND;
                    break;
                case "-del":
                    function = FUNCTIONS.DELETE;
                    break;
                case "-delrec":
                    function = FUNCTIONS.DELRECORD;
                    break;
                case "-delmemb":
                    function = FUNCTIONS.DELMEMBER;
                    break;
                case "-getfmt":
                    function = FUNCTIONS.GETFORMAT;
                    break;
                case "-new":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-open":
                    function = FUNCTIONS.OPEN;
                    openTypeString = argArray.nextMaybeQuotationTuplePopString().toUpperCase().trim();
                    switch (openTypeString) {
                        case "R":
                            openType = AS400File.READ_ONLY;
                            break;
                        case "W":
                            openType = AS400File.WRITE_ONLY;
                            break;
                        case "RW":
                            openType = AS400File.READ_WRITE;
                            break;
                        default:
                            getLogger().log(Level.SEVERE, "Unknown open type {0} in {1}", new Object[]{openTypeString, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case "-lock":
                    function = FUNCTIONS.LOCK;
                    lockTypeString = argArray.nextMaybeQuotationTuplePopStringTrim();
                    lockType = lockLockType(lockTypeString);
                    if (lockType == null) {
                        getLogger().log(Level.SEVERE, "Unknown lock type {0} in {1}", new Object[]{lockTypeString, getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case "-unlock":
                    function = FUNCTIONS.UNLOCK;
                    break;
                case "-recfmtnum":
                    recordFormatNumber = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-close":
                    function = FUNCTIONS.CLOSE;
                    break;
                case "-list":
                    function = FUNCTIONS.LIST;
                    break;
                case "-pos":
                    function = FUNCTIONS.POS;
                    positionString = argArray.nextMaybeQuotationTuplePopString().toUpperCase().trim();
                    break;
                case "-noop":
                    break;
                case "-read":
                    function = FUNCTIONS.READ;
                    readCommand = argArray.nextMaybeQuotationTuplePopString().toUpperCase().trim();
                    break;
                case "-setfmt":
                    function = FUNCTIONS.SETFORMAT;
                    formatTuple = argArray.nextTupleOrPop();
                    break;
                case "-update":
                    function = FUNCTIONS.UPDATE;
                    recordTuple = argArray.nextTupleOrPop();
                    break;
                case "-write":
                    function = FUNCTIONS.WRITE;
                    recordTuple = argArray.nextTupleOrPop();
                    break;
                case "-writeall":
                    function = FUNCTIONS.WRITE;
                    recordArrayList = argArray.nextTupleOrPop().value(RecordArrayList.class);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        String ifsfqp = null;
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            if (aS400File == null) {
                // we need either a file object or a fully qualified IFS path
                // and an as400 to proceed
                ifsfqp = argArray.nextMaybeQuotationTuplePopString(); // ifspath
                if (getAs400() == null) {
                    try {
                        setAs400FromArgs(argArray);
                    } catch (PropertyVetoException ex) {
                        getLogger().log(Level.SEVERE,
                                "Encountered an exception getting an AS400 instance from the supplied command arguments in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
            }
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            switch (function) {
                case INSTANCE:
                    if (keyedNotSequential == null) {
                        getLogger().log(Level.SEVERE, "Either -keyed or -sequential must be set in {0} to create a file.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        if (keyedNotSequential) {
                            aS400File = new KeyedFile(getAs400(), ifsfqp);
                        } else {
                            aS400File = new SequentialFile(getAs400(), ifsfqp);
                        }
                        try {
                            put(aS400File);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case CREATE:
                    if (aS400File != null) {
                        String fileTypeConst = selectFileTypeConst(fileType);
                        if (fileTypeConst == null) {
                            getLogger().log(Level.SEVERE, "Invalid file type for create in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                aS400File.create(recordLength, fileTypeConst, textDescription);
                            } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                                getLogger().log(Level.SEVERE,
                                        "Encountered an exception creating an AS400File in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to create.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CREATEDDS:
                    if (aS400File != null) {
                        try {
                            aS400File.create(ddsPath, textDescription);
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception creating an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to create.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CREATEFMT:
                    if (aS400File != null) {
                        try {
                            aS400File.create(recFormat, textDescription);
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception creating an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to create.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case COMMITSTART:
                    if (aS400File != null) {
                        Integer ll = commitLockLevel(lockLevel);
                        if (ll == null) {
                            getLogger().log(Level.SEVERE, "Unknown lock level {0} to commit start in {1}", new Object[]{lockLevel, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        try {
                            aS400File.startCommitmentControl(ll);
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception to commit start an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to commit start .", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case COMMIT:
                    if (aS400File != null) {
                        try {
                            aS400File.commit();
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception committing an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to commit.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case LOCK:
                    if (aS400File != null) {
                        try {
                            aS400File.lock(lockType);
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception locking an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to lock.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case UNLOCK:
                    if (aS400File != null) {
                        try {
                            aS400File.releaseExplicitLocks();
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception unlocking an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to unlock.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case ROLLBACK:
                    if (aS400File != null) {
                        try {
                            aS400File.rollback();
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception rolling back an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to rollback.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case COMMITEND:
                    if (aS400File != null) {
                        try {
                            aS400File.endCommitmentControl();
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception to commit end an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to commit end.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DELETE:
                    if (aS400File != null) {
                        try {
                            aS400File.delete();
                        } catch (AS400Exception | AS400SecurityException | InterruptedException | IOException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception deleting AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to delete.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DELMEMBER:
                    if (aS400File != null) {
                        try {
                            aS400File.deleteMember();
                        } catch (AS400Exception | AS400SecurityException | InterruptedException | IOException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception deleting AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to delete.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DELRECORD:
                    if (aS400File != null) {
                        try {
                            aS400File.deleteCurrentRecord();
                        } catch (AS400Exception | AS400SecurityException | InterruptedException | IOException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception deleting AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to delete.", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case OPEN:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to open in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            if (aS400File.getRecordFormat() == null) {
                                aS400File.setRecordFormat(recordFormatNumber);
                            }
                            aS400File.open(openType, blockingFactor, commitLockLevel);
                        } catch (AS400Exception | AS400SecurityException | InterruptedException | IOException | PropertyVetoException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception opening AS400File " + aS400File + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case CLOSE:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to open in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            aS400File.close();
                        } catch (AS400Exception | AS400SecurityException | InterruptedException | IOException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception closing AS400File " + aS400File + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case GETFORMAT:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to get format in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(aS400File.getRecordFormat());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception loading or putting RecordFormat for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case LIST:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to list members in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            MemberList m = new MemberList(aS400File);
                            m.load();
                            put(m);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception loading or putting MemberList for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case NOOP:
                    //ifsNoop();
                    break;
                case POS:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to list members in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            switch (positionString) {
                                case "B":
                                    aS400File.positionCursorBeforeFirst();
                                    break;
                                case "F":
                                    aS400File.positionCursorToFirst();
                                    break;
                                case "P":
                                    aS400File.positionCursorToPrevious();
                                    break;
                                case "N":
                                    aS400File.positionCursorToNext();
                                    break;
                                case "L":
                                    aS400File.positionCursorToLast();
                                    break;
                                case "A":
                                    aS400File.positionCursorAfterLast();
                                    break;
                                default:
                                    getLogger().log(Level.SEVERE, "Unknown cursor position command {0} in {1}", new Object[]{positionString, getNameAndDescription()});
                                    setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception loading or putting MemberList for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case QUERY:
                    break;
                case READ:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to read in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            switch (readCommand) {
                                case "CURR":
                                    put(aS400File.read());
                                    break;
                                case "PREV":
                                    put(aS400File.readPrevious());
                                    break;
                                case "FIRST":
                                    put(aS400File.readFirst());
                                    break;
                                case "LAST":
                                    put(aS400File.readLast());
                                    break;
                                case "NEXT":
                                    put(aS400File.readNext());
                                    break;
                                case "ALL": // File must be closed and format set
                                    if (aS400File.getRecordFormat() == null) {
                                        aS400File.setRecordFormat(recordFormatNumber);
                                    }
                                    put(new RecordArrayList(aS400File.readAll()));
                                    break;
                                default:
                                    getLogger().log(Level.SEVERE, "Unknown read command {0} in {1}", new Object[]{readCommand, getNameAndDescription()});
                                    setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | PropertyVetoException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception reading or putting records for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case UPDATE:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to write in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (aS400File.isOpen()) {
                        Record record = null;
                        if (recordTuple != null) {
                            record = recordTuple.value(Record.class);
                        }
                        if (record != null) {
                            try {
                                aS400File.update(record);
                            } catch (AS400Exception | AS400SecurityException | InterruptedException | IOException ex) {
                                getLogger().log(Level.SEVERE,
                                        "Encountered an exception writing record for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No Record provided to write in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "AS400File instance provided to write in {0} is not open", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case WRITE:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to write in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (aS400File.isOpen()) {
                        Record record = null;
                        if (recordTuple != null) {
                            record = recordTuple.value(Record.class);
                        }
                        if (record != null) {
                            try {
                                aS400File.write(record);
                            } catch (AS400Exception | AS400SecurityException | InterruptedException | IOException ex) {
                                getLogger().log(Level.SEVERE,
                                        "Encountered an exception writing record for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No Record provided to write in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "AS400File instance provided to write in {0} is not open", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case WRITEALL:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to write in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (aS400File.isOpen()) {
                        if (recordArrayList != null) {
                            try {
                                aS400File.write(recordArrayList.recordArray());
                            } catch (AS400Exception | AS400SecurityException | InterruptedException | IOException ex) {
                                getLogger().log(Level.SEVERE,
                                        "Encountered an exception writing record for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No Record list provided to write in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "AS400File instance provided to write in {0} is not open", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SETFORMAT:
                    RecordFormat recordFormat;
                    if (aS400File != null) {
                        if (formatTuple != null) {
                            recordFormat = formatTuple.value(RecordFormat.class);
                            if (recordFormat != null) {
                                try {
                                    aS400File.setRecordFormat(recordFormat);
                                } catch (PropertyVetoException ex) {
                                    getLogger().log(Level.SEVERE,
                                            "Encountered an exception setting format for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            } else {
                                getLogger().log(Level.SEVERE, "No valid format instance from the supplied command arguments for set format in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No format instance from the supplied command arguments for set format in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No file instance from the supplied command arguments for set format in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    }
                    break;
            }
        }
        return argArray;
    }

    private Integer commitLockLevel(String lockLevel) {
        Integer result = null;
        switch (lockLevel.toUpperCase()) {
            case "ALL":
                result = AS400File.COMMIT_LOCK_LEVEL_ALL;
                break;
            case "CHANGE":
                result = AS400File.COMMIT_LOCK_LEVEL_CHANGE;
                break;
            case "STABLE":
                result = AS400File.COMMIT_LOCK_LEVEL_CURSOR_STABILITY;
                break;
        }
        return result;
    }

    private Integer lockLockType(String lockType) {
        Integer result = null;
        switch (lockType.toUpperCase()) {
            case "RX":
                result = AS400File.READ_EXCLUSIVE_LOCK;
                break;
            case "RSR":
                result = AS400File.READ_ALLOW_SHARED_READ_LOCK;
                break;
            case "RSW":
                result = AS400File.READ_ALLOW_SHARED_WRITE_LOCK;
                break;
            case "WX":
                result = AS400File.WRITE_EXCLUSIVE_LOCK;
                break;
            case "WSR":
                result = AS400File.WRITE_ALLOW_SHARED_READ_LOCK;
                break;
            case "WSW":
                result = AS400File.WRITE_ALLOW_SHARED_WRITE_LOCK;
                break;
        }
        return result;
    }

    private String selectFileTypeConst(String fileType) {
        String result = null;
        switch (fileType) {
            case "*DATA":
                result = AS400File.TYPE_DATA;
                break;
            case "*SOURCE":
                result = AS400File.TYPE_SOURCE;
                break;
        }
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return doFile(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
