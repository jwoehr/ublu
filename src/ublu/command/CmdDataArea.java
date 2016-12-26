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
import com.ibm.as400.access.BidiStringType;
import com.ibm.as400.access.CharacterDataArea;
import ublu.util.ArgArray;
import com.ibm.as400.access.DataArea;
import com.ibm.as400.access.DecimalDataArea;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.LocalDataArea;
import com.ibm.as400.access.LogicalDataArea;
import com.ibm.as400.access.ObjectAlreadyExistsException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ublu.util.Tuple;

/**
 * Manipulates an OS400 Object Description
 *
 * @author jwoehr
 */
public class CmdDataArea extends Command {

    {
        setNameAndDescription("dta", "/0 [-as400 ~@as400] [-to datasink] [--,-dataarea ~@dataarea] [-path ~@{ifspath}] [-biditype ~@{biditype}] [-buffoffset ~@{buffoffset}] [-offset ~@{offset}] [-length ~@{length}] [-new,-instance CHAR|DEC|LOC|LOG | -create | -refresh | -query  name| system|length | -write ~@{data}] | -read | -clear] : create and use data areas");

    }

    /**
     * the operations we know
     */
    protected enum OPS {
        /**
         * Create the Object Description
         */
        INSTANCE,
        /**
         * Create the area
         */
        CREATE,
        /**
         * Query various aspects
         */
        QUERY,
        /**
         * Read data
         */
        READ,
        /**
         * Write data
         */
        WRITE,
        /**
         * Refresh all info
         */
        REFRESH,
        /**
         * Clear area
         */
        CLEAR
    }

    /**
     * Arity-0 ctor
     */
    public CmdDataArea() {
    }

    /**
     * retrieve a (filtered) list of OS400 Objects on the system
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray dta(ArgArray argArray) {
        OPS op = OPS.INSTANCE;
        String ifspath = null;
        String queryString = null;
        String type = null;
        String bidiType = null;
        int writeOffset = 0;
        int buffOffset = 0;
        Integer writeLength = null;
        Tuple writeObjectTuple = null;
//        String attributeName = null;
        DataArea da = null;
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
                case "-dataarea":
                    da = argArray.nextTupleOrPop().value(DataArea.class);
                    break;
                case "-path":
                    ifspath = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-biditype":
                    bidiType = argArray.nextMaybeQuotationTuplePopStringTrim().toUpperCase();
                    break;
                case "-query":
                    op = OPS.QUERY;
                    queryString = argArray.nextMaybeQuotationTuplePopStringTrim().toUpperCase();
                    break;
                case "-read":
                    op = OPS.READ;
                    break;
                case "-clear":
                    op = OPS.CLEAR;
                    break;
                case "-new":
                case "-instance":
                    op = OPS.INSTANCE;
                    type = argArray.nextMaybeQuotationTuplePopStringTrim().toUpperCase();
                    break;
                case "-create":
                    op = OPS.CREATE;
                    break;
                case "-refresh":
                    op = OPS.REFRESH;
                    break;
                case "-write":
                    op = OPS.WRITE;
                    writeObjectTuple = argArray.nextTupleOrPop();
                    break;
                case "-buffoffset":
                    buffOffset = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-offset":
                    writeOffset = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-length":
                    writeLength = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (op) {
                case CLEAR:
                    if (da == null) {
                        getLogger().log(Level.SEVERE, "No data area instance provided for clear in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            if (da instanceof CharacterDataArea) {
                                ((CharacterDataArea) da).clear();
                            }
                            if (da instanceof DecimalDataArea) {
                                ((DecimalDataArea) da).clear();
                            }
                            if (da instanceof LocalDataArea) {
                                ((LocalDataArea) da).clear();
                            }
                            if (da instanceof LogicalDataArea) {
                                ((LogicalDataArea) da).clear();
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error clearing data area in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case CREATE:
                    if (da == null) {
                        getLogger().log(Level.SEVERE, "No data area instance provided for create in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            if (da instanceof CharacterDataArea) {
                                ((CharacterDataArea) da).create();
                            }
                            if (da instanceof DecimalDataArea) {
                                ((DecimalDataArea) da).create();
                            }
                            if (da instanceof LocalDataArea) {
                                getLogger().log(Level.WARNING, "It is not necessary to -create a local data area in {0}", getNameAndDescription());
                            }
                            if (da instanceof LogicalDataArea) {
                                ((LogicalDataArea) da).create();
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectAlreadyExistsException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error clearing data area in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case INSTANCE:
                    if (getAs400() == null) {
                        getLogger().log(Level.SEVERE, "No AS400 instance provided for new in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        if (type != null) {
                            switch (type) {
                                case "CHAR":
                                    da = new CharacterDataArea(getAs400(), ifspath);
                                    break;
                                case "DEC":
                                    da = new DecimalDataArea(getAs400(), ifspath);
                                    break;
                                case "LOC":
                                    da = new LocalDataArea(getAs400());
                                    break;
                                case "LOG":
                                    da = new LogicalDataArea(getAs400(), ifspath);
                                    break;
                                default:
                                    getLogger().log(Level.SEVERE, "Data area type must be one of CHAR|DEC|LOC|LOG for new in {0}", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            try {
                                put(da);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting data area in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "Data area type must be one of CHAR|DEC|LOC|LOG for new in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case QUERY:
                    break;
                case READ:
                    break;
                case REFRESH:
                    break;
                case WRITE:
                    if (da == null || writeObjectTuple == null || writeObjectTuple.getValue() == null) {
                        getLogger().log(Level.SEVERE, "Data area instance or write value is null in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        Object writeValue = writeObjectTuple.getValue();
                        try {
                            if (da instanceof CharacterDataArea) {
                                if (writeValue != null && writeValue instanceof String) {
                                    if (bidiType == null) {
                                        ((CharacterDataArea) da).write((String) writeValue, writeOffset);
                                    } else {
                                        try {
                                            ((CharacterDataArea) da).write((String) writeValue, writeOffset, bidiInt(bidiType));
                                        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                                            getLogger().log(Level.SEVERE, "Error bidi type in character data area write in " + getNameAndDescription(), ex);
                                        }
                                    }
                                } else if (writeValue != null && writeValue instanceof byte[]) {
                                    ((CharacterDataArea) da).write((byte[]) writeValue, buffOffset, writeOffset, writeLength);
                                }
                            } else if (da instanceof DecimalDataArea) {
                                ((DecimalDataArea) da).write((writeObjectTuple.value(BigDecimal.class)));
                            } else if (da instanceof LocalDataArea) {
                                if (writeValue != null && writeValue instanceof String) {
                                    if (bidiType == null) {
                                        ((CharacterDataArea) da).write((String) writeValue, writeOffset);
                                    } else {
                                        try {
                                            ((CharacterDataArea) da).write((String) writeValue, writeOffset, bidiInt(bidiType));
                                        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                                            getLogger().log(Level.SEVERE, "Error bidi type in character data area write in " + getNameAndDescription(), ex);
                                        }
                                    }
                                } else if (writeValue != null && writeValue instanceof byte[]) {
                                    ((CharacterDataArea) da).write((byte[]) writeValue, buffOffset, writeOffset, writeLength);
                                }
                            } else if (da instanceof LogicalDataArea) {
                                ((LogicalDataArea) da).write(writeObjectTuple.value(boolean.class));
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error clearing data area in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                default:
                    getLogger().log(Level.WARNING, "Unknown operation in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private int bidiInt(String bidiType) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        return BidiStringType.class.getField(bidiType).getInt(BidiStringType.class);
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return dta(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
