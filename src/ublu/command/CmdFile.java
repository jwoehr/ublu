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
import java.util.logging.Logger;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Tuple;

/**
 * Command to access Integrated File System stream files
 *
 * @author jwoehr
 */
public class CmdFile extends Command {

    {
        setNameAndDescription("file",
                "/4? [-to @var ] [--,-file @file] [-as400 @as400] [-keyed | -sequential] [-instance | -create | -del | -delmemb | -delrec | -getfmt | -setfmt ~@format | -open ~@{R|W|RW} | -close | -list | -pos ~@{BF|F|P|N|L|A} | -recfmtnum ~@{int} | -read ~@{CURR|FIRST|LAST|NEXT|PREV|ALL} | -write ~@record ] [-to datasink] ~@{/fully/qualified/ifspathname} ~@{system} ~@{user} ~@{password} : record file access");
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
         * List members of physical file
         */
        LIST,
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
         * Write a Physical file
         */
        WRITE
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
        Tuple fileTuple;
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
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "--":
                case "-file":
                    fileTuple = argArray.nextTupleOrPop();
                    o = fileTuple.getValue();
                    if (o instanceof AS400File) {
                        aS400File = AS400File.class.cast(o);
                    } else {
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
                case "-instance":
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
                case "-write":
                    function = FUNCTIONS.WRITE;
                    recordTuple = argArray.nextTupleOrPop();
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
                        try {
                            put("Not Implemented Yet");
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception creating an AS400File in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400File object in {0} to create.", getNameAndDescription());
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
                            aS400File.setRecordFormat(recordFormatNumber);
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
                                case "BF":
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
                                    put(aS400File.readFirst());
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
                                    put(aS400File.readAll());
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
                case WRITE:
                    if (aS400File == null) {
                        getLogger().log(Level.SEVERE, "No AS400File instance provided to write in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (aS400File.isOpen()) {
                        Record record = null;
                        if (recordTuple != null) {
                            o = recordTuple.getValue();
                            if (o instanceof Record) {
                                record = Record.class.cast(o);
                            }
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
                case SETFORMAT:
                    RecordFormat recordFormat;
                    if (aS400File != null) {
                        if (formatTuple != null) {
                            o = formatTuple.getValue();
                            if (o instanceof RecordFormat) {
                                recordFormat = RecordFormat.class.cast(o);
                                try {
                                    aS400File.setRecordFormat(recordFormat);
                                } catch (PropertyVetoException ex) {
                                    getLogger().log(Level.SEVERE,
                                            "Encountered an exception setting format for AS400File  " + aS400File + "in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            } else {
                                getLogger().log(Level.SEVERE, "No format instance from the supplied command arguments for set format in {0}", getNameAndDescription());
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
