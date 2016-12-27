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
import com.ibm.as400.access.DataArea;
import com.ibm.as400.access.DecimalDataArea;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IllegalObjectTypeException;
import com.ibm.as400.access.LocalDataArea;
import com.ibm.as400.access.LogicalDataArea;
import com.ibm.as400.access.ObjectAlreadyExistsException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.ArgArray;
import ublu.util.Generics.ByteArrayList;
import ublu.util.Tuple;
import ublu.util.Utils;

/**
 * Manipulates an OS400 Object Description
 *
 * @author jwoehr
 */
public class CmdDataArea extends Command {

    {
        setNameAndDescription("dta", "/0 [-as400 ~@as400] [-to datasink] [--,-dataarea ~@dataarea] [-path ~@{ifspath}] [-bytes] [-biditype ~@{biditype}] [-buffoffset ~@{buffoffset}] [-offset ~@{offset}] [-length ~@{length}] [-initlen ~@{initlen}] [-initdecpos ~@{initdecpos}] [-initval ~@{initval}] [-initauth ~@{initval}] [-initdesc ~@{initdesc}] [-new,-instance CHAR|DEC|LOC|LOG | -create | -delete | -refresh | -query ~@{query(name|sys|length|path)} | -write ~@data | -read | -clear] : create and use data areas");

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
         * Delete the area
         */
        DELETE,
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
        int readWriteOffset = 0;
        int buffOffset = 0;
        Integer readWriteLength = null;
        Tuple writeObjectTuple = null;
        boolean readInBytes = false;
        String initVal = null;
        Integer initLen = null;
        String initAuth = "*EXCLUDE";
        String initDesc = null;
        Integer initDecPos = 5;
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
                case "-bytes":
                    readInBytes = true;
                    break;
                case "-biditype":
                    bidiType = argArray.nextMaybeQuotationTuplePopStringTrim().toUpperCase();
                    break;
                case "-query":
                    op = OPS.QUERY;
                    queryString = argArray.nextMaybeQuotationTuplePopStringTrim().toLowerCase();
                    break;
                case "-read":
                    op = OPS.READ;
                    break;
                case "-clear":
                    op = OPS.CLEAR;
                    break;
                case "-initlen":
                    initLen = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-initdecpos":
                    initDecPos = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-initval":
                    initVal = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-initauth":
                    initAuth = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-initdesc":
                    initDesc = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-new":
                case "-instance":
                    op = OPS.INSTANCE;
                    type = argArray.nextMaybeQuotationTuplePopStringTrim().toUpperCase();
                    break;
                case "-create":
                    op = OPS.CREATE;
                    break;
                case "-delete":
                    op = OPS.DELETE;
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
                    readWriteOffset = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-length":
                    readWriteLength = argArray.nextIntMaybeQuotationTuplePopString();
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
                            initDesc = initDesc == null ? new String() : initDesc;
                            if (da instanceof CharacterDataArea) {
                                initLen = initLen == null ? 32 : initLen;
                                initVal = initVal == null ? Utils.fillString(' ', initLen) : initVal;
                                ((CharacterDataArea) da).create(initLen, initVal, initDesc, initAuth);
                                // ((CharacterDataArea) da).create();
                            }
                            if (da instanceof DecimalDataArea) {
                                initLen = initLen == null ? 15 : initLen;
                                BigDecimal initDecVal = initVal == null ? new BigDecimal(BigInteger.ZERO) : new BigDecimal(initVal);
                                ((DecimalDataArea) da).create(initLen, initDecPos, initDecVal, initDesc, initAuth);
                            }
                            if (da instanceof LocalDataArea) {
                                getLogger().log(Level.WARNING, "It is not necessary to -create a local data area in {0}", getNameAndDescription());
                            }
                            if (da instanceof LogicalDataArea) {
                                boolean initBoolVal = initVal == null ? false : initVal.equals("true");
                                ((LogicalDataArea) da).create(initBoolVal, initDesc, initAuth);
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectAlreadyExistsException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error clearing data area in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case DELETE:
                    if (da == null) {
                        getLogger().log(Level.SEVERE, "No data area instance provided for create in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {

                        try {
                            if (da instanceof CharacterDataArea) {
                                ((CharacterDataArea) da).delete();
                            }
                            if (da instanceof DecimalDataArea) {
                                ((DecimalDataArea) da).delete();
                            }
                            if (da instanceof LocalDataArea) {
                                getLogger().log(Level.WARNING, "You cannot -delete a local data area in {0}", getNameAndDescription());
                            }
                            if (da instanceof LogicalDataArea) {
                                ((LogicalDataArea) da).delete();
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
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
                    if (da == null) {
                        getLogger().log(Level.SEVERE, "No data area instance provided for query in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        Object result = null;
                        switch (queryString == null ? "" : queryString) {
                            case "name":
                                result = da.getName();
                                break;
                            case "sys":
                                result = da.getSystem();
                                break;
                            case "length": {
                                try {
                                    result = da.getLength();
                                } catch (AS400SecurityException | ErrorCompletingRequestException | IllegalObjectTypeException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                    getLogger().log(Level.SEVERE, "Error getting data area length in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                            break;
                            case "path":
                                if (da instanceof CharacterDataArea) {
                                    result = ((CharacterDataArea) da).getPath();
                                }
                                if (da instanceof DecimalDataArea) {
                                    result = ((DecimalDataArea) da).getPath();
                                }
                                if (da instanceof LocalDataArea) {
                                    getLogger().log(Level.WARNING, "You cannot get path for a local data area in {0}", getNameAndDescription());
                                }
                                if (da instanceof LogicalDataArea) {
                                    result = ((LogicalDataArea) da).getPath();
                                }
                                break;
                            default:
                                getLogger().log(Level.SEVERE, "Query must be one of name|sys|length|path in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(result);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting query result in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case READ:
                    if (da == null) {
                        getLogger().log(Level.SEVERE, "No data area instance provided for read in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        Object readResult = null;
                        try {
                            if (da instanceof CharacterDataArea) {
                                if (readInBytes == true) {
                                    readResult = ((CharacterDataArea) da).read(new byte[readWriteLength], 0, readWriteOffset, readWriteLength);
                                } else if (bidiType != null) {
                                    readResult = ((CharacterDataArea) da).read(readWriteOffset, readWriteLength, bidiInt(bidiType));
                                } else if (readWriteLength != null) {
                                    readResult = ((CharacterDataArea) da).read(readWriteOffset, readWriteLength);
                                } else {
                                    readResult = ((CharacterDataArea) da).read();
                                }
                            }
                            if (da instanceof DecimalDataArea) {
                                readResult = ((DecimalDataArea) da).read();
                            }
                            if (da instanceof LocalDataArea) {
                                if (readInBytes == true) {
                                    readResult = ((LocalDataArea) da).read(new byte[readWriteLength], 0, readWriteOffset, readWriteLength);
                                } else if (bidiType != null) {
                                    readResult = ((LocalDataArea) da).read(readWriteOffset, readWriteLength, bidiInt(bidiType));
                                } else if (readWriteLength != null) {
                                    readResult = ((LocalDataArea) da).read(readWriteOffset, readWriteLength);
                                } else {
                                    readResult = ((LocalDataArea) da).read();
                                }
                            }
                            if (da instanceof LogicalDataArea) {
                                readResult = ((LogicalDataArea) da).read();
                            }
                            put(readResult);
                        } catch (SQLException | RequestNotSupportedException | AS400SecurityException | ErrorCompletingRequestException | IllegalAccessException | IllegalObjectTypeException | IOException | InterruptedException | NoSuchFieldException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error reading or putting data area in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case REFRESH:
                    if (da == null) {
                        getLogger().log(Level.SEVERE, "No data area instance provided for refresh in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {

                        try {
                            da.refreshAttributes();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IllegalObjectTypeException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error refreshing data area attributes in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
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
                                        ((CharacterDataArea) da).write((String) writeValue, readWriteOffset);
                                    } else {
                                        try {
                                            ((CharacterDataArea) da).write((String) writeValue, readWriteOffset, bidiInt(bidiType));
                                        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                                            getLogger().log(Level.SEVERE, "Error bidi type in character data area write in " + getNameAndDescription(), ex);
                                        }
                                    }
                                } else {
                                    if (writeValue != null && writeValue instanceof ByteArrayList) {
                                        writeValue = ((ByteArrayList) writeValue).byteArray();
                                    }
                                    if (writeValue != null && writeValue instanceof byte[]) {
                                        ((CharacterDataArea) da).write((byte[]) writeValue, buffOffset, readWriteOffset, readWriteLength);
                                    }
                                }
                            } else if (da instanceof DecimalDataArea) {
                                ((DecimalDataArea) da).write((writeObjectTuple.value(BigDecimal.class)));
                            } else if (da instanceof LocalDataArea) {
                                if (writeValue != null && writeValue instanceof String) {
                                    if (bidiType == null) {
                                        ((CharacterDataArea) da).write((String) writeValue, readWriteOffset);
                                    } else {
                                        try {
                                            ((CharacterDataArea) da).write((String) writeValue, readWriteOffset, bidiInt(bidiType));
                                        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                                            getLogger().log(Level.SEVERE, "Error bidi type in character data area write in " + getNameAndDescription(), ex);
                                        }
                                    }
                                } else if (writeValue != null && writeValue instanceof byte[]) {
                                    ((CharacterDataArea) da).write((byte[]) writeValue, buffOffset, readWriteOffset, readWriteLength);
                                }
                            } else if (da instanceof LogicalDataArea) {
                                if (writeValue.getClass().equals(boolean.class)) {
                                    writeObjectTuple.setValue(new Boolean((boolean) writeValue));
                                }
                                ((LogicalDataArea) da).write(writeObjectTuple.value(Boolean.class));
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
