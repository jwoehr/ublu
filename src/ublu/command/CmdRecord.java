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

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Tuple;

/**
 * Command to manipulate record file records..
 *
 * @author jax
 */
public class CmdRecord extends Command {

    {
        setNameAndDescription("record",
                "/0 [-to @var] [--,-record ~@record] [ -getfmt | -new | -setcontents ~@{contents} | -setfield ~@{index} ~@object | -setfieldbyname ~@{fieldname} ~@object | -setfmt ~@format | -tostring ] : manipulate record file records.");
    }

    /**
     * Operations we know
     */
    protected enum OPERATIONS {

        /**
         * Get format
         */
        GETFMT,
        /**
         * Create instance
         */
        INSTANCE,
        /**
         * Set record contsnts
         */
        SETCONTENTS,
        /**
         * Set one field by index
         */
        SETFIELD,
        /**
         * Set one field by name
         */
        SETFIELDBYNAME,
        /**
         * Set format
         */
        SETFMT,
        /**
         * String value of record
         */
        TOSTRING
    }

    /**
     * Manipulate record file records.
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     * @author jax
     */
    public ArgArray doRecord(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.INSTANCE; // the default
        Tuple recordTuple = null;
        Tuple formatTuple = null;
        String contents = null;
        Integer fieldIndex = null;
        String fieldName = null;
        Object fieldContents = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "--":
                case "-record":
                    recordTuple = argArray.nextTupleOrPop();
                    break;
                case "-new":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-getfmt":
                    operation = OPERATIONS.GETFMT;
                    break;
                case "-setcontents":
                    operation = OPERATIONS.SETCONTENTS;
                    contents = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-setfield":
                    operation = OPERATIONS.SETFIELD;
                    fieldIndex = argArray.nextIntMaybeQuotationTuplePopString();
                    fieldContents = argArray.nextTupleOrPop().getValue();
                    break;
                case "-setfieldbyname":
                    operation = OPERATIONS.SETFIELDBYNAME;
                    fieldName = argArray.nextMaybeQuotationTuplePopString();
                    fieldContents = argArray.nextTupleOrPop().getValue();
                    break;
                case "-setfmt":
                    operation = OPERATIONS.SETFMT;
                    formatTuple = argArray.nextTupleOrPop();
                    break;
                case "-tostring":
                    operation = OPERATIONS.TOSTRING;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Record record = null;
            if (recordTuple != null) {
                Object o = recordTuple.getValue();
                if (o instanceof Record) {
                    record = Record.class.cast(o);
                }
            }
            switch (operation) {
                case GETFMT:
                    if (record != null) {
                        try {
                            put(record.getRecordFormat());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting record format in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.INFO, "No record object provided to get format in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SETFMT:
                    RecordFormat recordFormat = null;
                    if (formatTuple != null) {
                        Object o = formatTuple.getValue();
                        if (o instanceof RecordFormat) {
                            recordFormat = RecordFormat.class.cast(o);
                        }
                    }
                    if (recordFormat == null) {
                        getLogger().log(Level.INFO, "No record format provided to set format in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (record != null) {
                        try {
                            record.setRecordFormat(recordFormat);
                        } catch (PropertyVetoException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting record format in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.INFO, "No record object provided to set format in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    }
                    break;
                case INSTANCE:
                    try {
                        put(new Record());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE,
                                "Encountered an exception putting new record in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SETCONTENTS:
                    if (record != null) {
                        try {
                            if (contents != null) {
                                record.setContents(contents.getBytes());
                            } else {
                                getLogger().log(Level.INFO, "Null contents provided to set contents in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } catch (UnsupportedEncodingException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception setting record contents in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.INFO, "No record object provided to set contents in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    }
                    break;
                case SETFIELD:
                    if (record != null) {
                        record.setField(fieldIndex, fieldContents);
                    } else {
                        getLogger().log(Level.INFO, "No record object provided to set field by index in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    }
                    break;
                case SETFIELDBYNAME:
                    if (record != null) {
                        record.setField(fieldName, fieldContents);
                    } else {
                        getLogger().log(Level.INFO, "No record object provided to set field by name in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    }
                    break;
                case TOSTRING:
                    if (record != null) {
                        try {
                            put(record.toString());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception putting record as string in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.INFO, "No record object provided to {0}", getNameAndDescription());
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
        return doRecord(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }

}
