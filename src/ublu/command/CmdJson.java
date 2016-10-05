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
import ublu.util.Generics.ThingArrayList;

/**
 * Create and manage lists
 *
 * @author jwoehr
 */
public class CmdJson extends Command {

    {
        setNameAndDescription("json",
                "/0 [-from datasink] [-to datasink] [--,-json ~@json] [ [-add ~@object ] | [-addkey ~@key ~@object] | [ -at ~@{index} ~@object] | [-array] | [-cdl ~@{cdl}] | [-get ~@{index}] | [-key ~@{key}] | [-keys] | [-length] | [-list] | [-object] ] : create and unpack JSON");
    }

    /**
     * Operations
     */
    protected enum OPERATIONS {
        /**
         * append to JSON Array
         */
        ADD,
        /**
         * insert to JSON Object
         */
        ADDKEY,
        /**
         * add at index in JSON Array
         */
        AT,
        /**
         * Create JSON Array
         */
        ARRAY,
        /**
         * Comma-delimited list
         */
        CDL,
        /**
         * Get value at index from JSON Array
         */
        GET,
        /**
         * Get value for key JSON Object
         */
        KEY,
        /**
         * Get keys from JSON Object
         */
        KEYS,
        /**
         * Array length
         */
        LENGTH,
        /**
         * Array members as list
         */
        LIST,
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
        Tuple objectTuple = null;
        Integer index = null;
        String key = null;
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
                case "-at":
                    operation = OPERATIONS.AT;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    objectTuple = argArray.nextTupleOrPop();
                    break;
                case "-add":
                    operation = OPERATIONS.ADD;
                    objectTuple = argArray.nextTupleOrPop();
                    break;
                case "-addkey":
                    operation = OPERATIONS.ADDKEY;
                    key = argArray.nextMaybeQuotationTuplePopString();
                    objectTuple = argArray.nextTupleOrPop();
                    break;
                case "-array":
                    operation = OPERATIONS.ARRAY;
                    break;
                case "-get":
                    operation = OPERATIONS.GET;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-key":
                    operation = OPERATIONS.KEY;
                    key = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-keys":
                    operation = OPERATIONS.KEYS;
                    break;
                case "-length":
                    operation = OPERATIONS.LENGTH;
                    break;
                case "-list":
                    operation = OPERATIONS.LIST;
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
            JSONArray jA = null;
            Object o = null;
            Tuple t;
            switch (operation) {
                case OBJECT:
                    jO = null;
                    switch (getDataSrc().getType()) {
                        case STD:
                            jO = new JSONObject();
                            break;
                        case LIFO:
                            t = getTupleStack().pop();
                            jO = jsonObjectFromObject(t.getValue());
                            break;
                        case TUPLE:
                            t = getTuple(getDataSrc().getName());
                            if (t != null) {
                                jO = jsonObjectFromObject(t.getValue());
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
                        switch (getDataSrc().getType()) {
                            case STD:
                                jA = new JSONArray();
                                break;
                            case LIFO:
                                t = getTupleStack().pop();
                                jA = new JSONArray(t.getValue());
                                break;
                            case TUPLE:
                                t = getTuple(getDataSrc().getName());
                                if (t != null) {
                                    jA = new JSONArray(t.getValue());
                                } else {
                                    getLogger().log(Level.SEVERE, "Null tuple provided to -array in {0}", getNameAndDescription());
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
                                    jA = new JSONArray(in);
                                } catch (IOException ex) {
                                    getLogger().log(Level.SEVERE, "Error creating JSON array from File in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                                break;
                            default:
                                getLogger().log(Level.SEVERE, "Unsupported data source provided to -object in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } catch (JSONException ex) {
                        getLogger().log(Level.SEVERE, "Error creating JSON array from datasrc " + getDataSrc().getName() + " in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    if (getCommandResult() != COMMANDRESULT.FAILURE) {
                        try {
                            put(jA);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting JSON array in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case ADD:
                    jA = arrayFromTuple(jsonTuple);
                    if (jA != null && objectTuple != null) {
                        jA.put(objectTuple.getValue());
                    } else {
                        getLogger().log(Level.SEVERE, "No JSON Array or no object passed to -add in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case ADDKEY:
                    jO = objectFromTuple(jsonTuple);
                    if (jO != null && key != null && objectTuple != null) {
                        try {
                            jO.put(key, objectTuple.getValue());
                        } catch (JSONException ex) {
                            getLogger().log(Level.SEVERE, "Error insertion to JSON object in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JSON Array or no key or no object passed to -addkey in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case AT:
                    jA = arrayFromTuple(jsonTuple);
                    if (jA != null && objectTuple != null && index != null) {
                        try {
                            jA.put(index, objectTuple.getValue());
                        } catch (JSONException ex) {
                            getLogger().log(Level.SEVERE, "Error insertion to JSON array in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JSON Array or no object or no index passed to -at in {0}", getNameAndDescription());
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
                case GET:
                    jA = arrayFromTuple(jsonTuple);
                    if (jA != null && index != null) {
                        try {
                            o = jA.get(index);
                        } catch (JSONException ex) {
                            getLogger().log(Level.SEVERE, "Error on get from JSON array in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(o);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception putting JSON Array member from -get in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JSON Array or no index passed to -get in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case KEY:
                    jO = objectFromTuple(jsonTuple);
                    if (jO != null && key != null) {
                        try {
                            o = jO.get(key);
                        } catch (JSONException ex) {
                            getLogger().log(Level.SEVERE, "Error on get from JSON Object in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(o);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception putting JSON Object key value in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JSON Object or no key passed to -key in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case KEYS:
                    jO = objectFromTuple(jsonTuple);
                    if (jO != null) {
                        o = jO.names();
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(o);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception putting JSON Object keys in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JSON Object or no key passed to -names in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
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
                case LIST:
                    jA = arrayFromTuple(jsonTuple);
                    if (jA != null) {
                        try {
                            put(listFromJSONArray(jA));
                        } catch (JSONException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting JSON Array as list in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No JSON Array passed to -list in {0}", getNameAndDescription());
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

    private JSONObject jsonObjectFromObject(Object o) {
        JSONObject jSONObject = null;
        if (o instanceof JSONObject) {
            jSONObject = new JSONObject(JSONObject.class.cast(o));
        } else if (o instanceof JSONTokener) {
            try {
                jSONObject = new JSONObject(JSONTokener.class.cast(o));
            } catch (JSONException ex) {
                getLogger().log(Level.SEVERE, "Error creating JSON object from JSONTokener in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        } else {
            try {
                jSONObject = new JSONObject(new JSONTokener(o.toString()));
            } catch (JSONException ex) {
                getLogger().log(Level.SEVERE, "Error creating JSON object from String in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return jSONObject;
    }

    private ThingArrayList listFromJSONArray(JSONArray jsonArray) throws JSONException {
        ThingArrayList tal = new ThingArrayList();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                tal.add(jsonArray.get(i));
            }
        }
        return tal;
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
