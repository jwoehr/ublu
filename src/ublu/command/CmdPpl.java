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

import ublu.util.ArgArray;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to manipulate jobs
 *
 * @author jwoehr
 */
public class CmdPpl extends Command {

    {
        setNameAndDescription("ppl", "/0 [--,-ppl ~@ppl] [-new,-instance] [-get[int|float|string] ~@{paramid}] | -set[int|float|string] ~@{paramid} ~@{value} : create and manipulate print parameter list");
    }

    /**
     * What we do
     */
    protected enum FUNCTIONS {

        /**
         * Create a PrintParameterList
         */
        INSTANCE,
        /**
         * Get a print parameter from the PrintParameterList
         */
        /**
         * Set an int print parameter in the PrintParameterList
         */
        SETINT,
        /**
         * Set a float print parameter in the PrintParameterList
         */
        SETFLOAT,
        /**
         * Set a String print parameter in the PrintParameterList
         */
        SETSTRING,
        /**
         * Get an int print parameter in the PrintParameterList
         */
        GETINT,
        /**
         * Get a float print parameter in the PrintParameterList
         */
        GETFLOAT,
        /**
         * Get a String print parameter in the PrintParameterList
         */
        GETSTRING

    }

    /**
     * Perform the work of getting a Job object and manipulating it.
     *
     * @param argArray the input arg array
     * @return what's left of the arg array
     */
    public ArgArray job(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.INSTANCE;
        Tuple pplTuple = null;
        PrintParameterList instancePPL = null;
        int attributeId;
        String stringVal;
        Integer intVal;
        Float floatVal;
        Object getResult = null;
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
                case "-getint":
                    function = FUNCTIONS.GETINT;
                    attributeId = argArray.nextIntMaybeQuotationTuplePopString();
                    getResult = getInt(pplTuple, attributeId);
                    break;
                case "-getfloat":
                    function = FUNCTIONS.GETFLOAT;
                    attributeId = argArray.nextIntMaybeQuotationTuplePopString();
                    getResult = getFloat(pplTuple, attributeId);
                    break;
                case "-getstring":
                    function = FUNCTIONS.GETSTRING;
                    attributeId = argArray.nextIntMaybeQuotationTuplePopString();
                    getResult = getString(pplTuple, attributeId);
                    break;
                case "-new":
                case "-instance":
                    function = FUNCTIONS.INSTANCE;
                    instancePPL = new PrintParameterList();
                    break;
                case "--":
                case "-ppl":
                    pplTuple = argArray.nextTupleOrPop();
                    break;
                case "-setint":
                    function = FUNCTIONS.SETINT;
                    attributeId = argArray.nextIntMaybeQuotationTuplePopString();
                    intVal = argArray.nextIntMaybeQuotationTuplePopString();
                    setInt(pplTuple, attributeId, intVal);
                    break;
                case "-setfloat":
                    function = FUNCTIONS.SETFLOAT;
                    attributeId = argArray.nextIntMaybeQuotationTuplePopString();
                    floatVal = Float.parseFloat(argArray.nextMaybeQuotationTuplePopString());
                    setFloat(pplTuple, attributeId, floatVal);
                    break;
                case "-setstring":
                    function = FUNCTIONS.SETSTRING;
                    attributeId = argArray.nextIntMaybeQuotationTuplePopString();
                    stringVal = argArray.nextMaybeQuotationTuplePopString();
                    setString(pplTuple, attributeId, stringVal);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (getCommandResult() != COMMANDRESULT.FAILURE) {
            switch (function) {
                case INSTANCE:
                    try {
                        put(instancePPL);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting PrintParameterList in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SETFLOAT:
                case SETINT:
                case SETSTRING:
                    try {
                        if (pplTuple != null) {
                            put(pplTuple.getValue());
                        } else {
                            put(null);
                        }
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting PrintParameterList in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case GETFLOAT:
                case GETINT:
                case GETSTRING:
                    try {
                        put(getResult);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting PrintParameterList value in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }
        return argArray;
    }

    private Tuple validatePPLTuple(Tuple pplTuple) {
        if (pplTuple.getValue() == null) {
            pplTuple.setValue(new PrintParameterList());
        } else if (!(pplTuple.getValue() instanceof PrintParameterList)) {
            getLogger().log(Level.SEVERE, "-ppl tuple is not a PrintParameterList in ", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        return pplTuple;
    }

    private void setInt(Tuple pplTuple, int attributeId, int intVal) {
        pplTuple = validatePPLTuple(pplTuple);

        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            PrintParameterList p = PrintParameterList.class
                    .cast(pplTuple.getValue());
            p.setParameter(attributeId, intVal);
        }
    }

    private void setFloat(Tuple pplTuple, int attributeId, float floatVal) {
        pplTuple = validatePPLTuple(pplTuple);

        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            PrintParameterList p = PrintParameterList.class
                    .cast(pplTuple.getValue());
            p.setParameter(attributeId, floatVal);
        }
    }

    private void setString(Tuple pplTuple, int attributeId, String stringVal) {
        pplTuple = validatePPLTuple(pplTuple);

        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            PrintParameterList p = PrintParameterList.class
                    .cast(pplTuple.getValue());
            p.setParameter(attributeId, stringVal);
        }
    }

    private Integer getInt(Tuple pplTuple, int attributeId) {
        Integer result = null;
        pplTuple = validatePPLTuple(pplTuple);

        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            PrintParameterList p = PrintParameterList.class
                    .cast(pplTuple.getValue());
            result = p.getIntegerParameter(attributeId);
        }
        return result;
    }

    private Float getFloat(Tuple pplTuple, int attributeId) {
        Float result = null;
        pplTuple = validatePPLTuple(pplTuple);

        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            PrintParameterList p = PrintParameterList.class
                    .cast(pplTuple.getValue());
            result = p.getFloatParameter(attributeId);
        }
        return result;
    }

    private String getString(Tuple pplTuple, int attributeId) {
        String result = null;
        pplTuple = validatePPLTuple(pplTuple);

        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            PrintParameterList p = PrintParameterList.class
                    .cast(pplTuple.getValue());
            result = p.getStringParameter(attributeId);
        }
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return job(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
