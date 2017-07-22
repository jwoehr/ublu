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
//import ublu.util.DataSink;
//import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.Generics.UserTupleMap;
import ublu.util.Tuple;

/**
 * Command to manipulate Maps
 *
 * @author jwoehr
 */
public class CmdMap extends Command {

    {
        setNameAndDescription("map", "/0? [--,-map ~@map] [-to datasink] [-new | -> ~@tuple | -clear | -add ~@key ~@tuple | -~,-push ~@key | -.,-get ~@key | -drop ~@key | -keys | -size]  : create and manipulate maps of tuples");
    }

    private enum OPS {

        NEW, ADD, CLEAR, GET, PUT, PUSH, DROP, KEYS, SIZE
    }

    /**
     * Perform the work of manipulating a doMap.
     *
     * @param argArray the input arg array
     * @return what's left of the arg array
     */
    public ArgArray doMap(ArgArray argArray) {
        OPS op = OPS.NEW;
        UserTupleMap map = null;
        Tuple tuple = null;
        String key = null;
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
                case "--":
                case "-map":
                    map = argArray.nextTupleOrPop().value(UserTupleMap.class);
                    break;
                case "-new":
                    op = OPS.NEW;
                    break;
                case "-clear":
                    op = OPS.CLEAR;
                    break;
                case "-add":
                    op = OPS.ADD;
                    key = argArray.nextMaybeQuotationTuplePopStringTrim();
                    tuple = argArray.nextTupleOrPop();
                    break;
                case "->":
                    op = OPS.PUT;
                    tuple = argArray.nextTupleOrPop();
                    break;
                case "-.":
                case "-get":
                    op = OPS.GET;
                    key = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-~":
                case "-push":
                    op = OPS.PUSH;
                    key = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-keys":
                    op = OPS.KEYS;
                    break;
                case "-size":
                    op = OPS.SIZE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (map == null && op != OPS.NEW) {
            getLogger().log(Level.SEVERE, "No Map provided to --map in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (op) {
                case SIZE:
                    try {
                        put(map.size());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting size of Map in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NEW:
                    try {
                        put(new UserTupleMap());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting new Map in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CLEAR:
                    map.clear();
                case ADD:
                    map.put(key, tuple);
                    break;
                case DROP:
                    map.remove(key);
                    break;
                case GET:
                    try {
                        put(map.get(key));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting tuple with key " + key + " from Map in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case KEYS:
                    try {
                        put(map.keySet());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting size of Map in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case PUSH:
                    getTupleStack().push(map.get(key));
                    break;
                case PUT:
                    map.put(tuple.getKey().substring(1), tuple); // remove the "@"
                    break;
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doMap(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
