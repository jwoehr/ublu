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
import ublu.util.DataSink;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Create and manage lists
 *
 * @author jwoehr
 */
public class CmdJson extends Command {

    {
        setNameAndDescription("json",
                "/0 [-to datasink] [--,-json @json] [ [-array] | [-cdl ~@{cdl}] | [-object] ]: create and unpack JSON");
    }

    /**
     * Operations
     */
    protected enum OPERATIONS {

        /**
         * Create JSON Array
         */
        ARRAY,
        /**
         * Comma-delimited list
         */
        CDL,
        /**
         * Array length
         */
        LENGTH,
        /**
         * Create JSON object
         */
        OBJECT
    }

    /**
     * The list command
     *
     * @param argArray
     * @return remnant of argArray
     */
    public ArgArray doCmdList(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.OBJECT;
        String commaDelimitedList = null;
        Tuple jsonTuple = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-from":
                    String srcName = argArray.next();
                    setDataSrc(DataSink.fromSinkName(srcName));
                    break;
                case "--":
                case "-json":
                    jsonTuple = argArray.nextTupleOrPop();
                    break;
                case "-array":
                    operation = OPERATIONS.ARRAY;
                    break;
                case "-length":
                    operation = OPERATIONS.LENGTH;
                    break;
                case "-object":
                    operation = OPERATIONS.OBJECT;
                    break;
                case "-cdl":
                    operation = OPERATIONS.CDL;
                    commaDelimitedList = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            JSONObject jO;
            JSONArray jA;
            switch (operation) {
                case OBJECT:
                    jO = null;
                    switch (getDataSrc().getType()) {
                        case STD:
                            jO = new JSONObject();
                            break;
                        case TUPLE:
                            Tuple t = getTuple(getDataSrc().getName());
                            if (t != null) {
                                Object o = t.getValue();
                                if (o instanceof JSONObject) {
                                    jO = new JSONObject(JSONObject.class.cast(o));
                                } else if (o instanceof JSONTokener) {
                                    try {
                                        jO = new JSONObject(JSONTokener.class.cast(o));
                                    } catch (JSONException ex) {
                                        getLogger().log(Level.SEVERE, "Error creating JSON object from JSONTokener in " + getNameAndDescription(), ex);
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    }
                                } else {
                                    try {
                                        jO = new JSONObject(new JSONTokener(o.toString()));
                                    } catch (JSONException ex) {
                                        getLogger().log(Level.SEVERE, "Error creating JSON object from String in " + getNameAndDescription(), ex);
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    }
                                }
                            } else {
                                getLogger().log(Level.SEVERE, "Null tuple provided to -object in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            break;
                        case FILE:
                            try {
                                File f = new File(getDataSrc().getName());
                                FileReader fr = new FileReader(f);
                                int length = new Long(f.length()).intValue();
                                char[] in = new char[length];
                                fr.read(in);
                                jO = new JSONObject(in);
                            } catch (IOException ex) {
                                getLogger().log(Level.SEVERE, "Error creating JSON object from File in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            break;
                        default:
                            getLogger().log(Level.SEVERE, "Unsupported data source provided to -object in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    if (getCommandResult() != COMMANDRESULT.FAILURE) {
                        try {
                            put(jO);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting JSON object in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case ARRAY:
                    try {
                        put(new JSONArray());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting JSON array in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CDL:
                    JSONArray jSONArray = null;
                    if (commaDelimitedList != null) {
                        JSONTokener jSONTokener = new JSONTokener(commaDelimitedList);
                        try {
                            jSONArray = CDL.rowToJSONArray(jSONTokener);
                        } catch (JSONException ex) {
                            getLogger().log(Level.SEVERE, "Exception tokenizing comma list in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (jSONArray != null) {
                            try {
                                put(jSONArray);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception putting CDL JSON Array in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case LENGTH:
                    jA = arrayFromTuple(jsonTuple);
                    if (jA != null) {
                        try {
                            put(jA.length());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting JSON Array length in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JSON Array passed to -length in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unknown operation unhandled in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private JSONArray arrayFromTuple(Tuple t) {
        JSONArray result = null;
        if (t != null) {
            Object o = t.getValue();
            if (o instanceof JSONArray) {
                result = JSONArray.class.cast(o);
            }
        }
        return result;
    }

    private JSONObject objectFromTuple(Tuple t) {
        JSONObject result = null;
        if (t != null) {
            Object o = t.getValue();
            if (o instanceof JSONObject) {
                result = JSONObject.class.cast(o);
            }
        }
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doCmdList(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
