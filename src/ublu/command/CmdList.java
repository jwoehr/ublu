/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
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
import ublu.util.Generics.ThingArrayList;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * Create and manage lists
 *
 * @author jwoehr
 */
public class CmdList extends Command {

    {
        setNameAndDescription("list",
                "/0 [-to datasink] [-source ~@enumeration|~@collection] [--,-list @list] [-add (@)object ] [-addstr ~@${ some string }$] [-clear] [-get (@)intindex] [-instance] [-remove (@)object] : create and manage lists of objects");
    }

    /**
     * Operations
     */
    protected enum OPERATIONS {

        /**
         * Add object to list
         */
        ADD,
        /**
         * Add string to list
         */
        ADDSTR,
        /**
         * Empty list
         */
        CLEAR,
        /**
         * Get object from list
         */
        GET,
        /**
         * Remove object from list
         */
        REMOVE,
        /**
         * Create list
         */
        INSTANCE,
        /**
         * Source enum or collection
         */
        SOURCE
    }

    /**
     * The list command
     *
     * @param argArray
     * @return remnant of argArray
     */
    public ArgArray doCmdList(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.INSTANCE;
        ThingArrayList myThingArrayList = null;
        Tuple talTuple = null;
        Tuple toAdd = null;
        Tuple toRemove = null;
        Integer toGet = null;
        String stringToAdd = null;
        Tuple sourceTuple = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-source":
                    operation = OPERATIONS.SOURCE;
                    sourceTuple = argArray.nextTupleOrPop();
                    break;
                case "--":
                case "-list":
                    talTuple = argArray.nextTupleOrPop();
                    break;
                case "-add":
                    toAdd = argArray.nextTupleOrPop();
                    operation = OPERATIONS.ADD;
                    break;
                case "-addstr":
                    stringToAdd = argArray.nextMaybeQuotationTuplePopString();
                    operation = OPERATIONS.ADDSTR;
                    break;
                case "-clear":
                    operation = OPERATIONS.CLEAR;
                    break;
                case "-get":
                    operation = OPERATIONS.GET;
                    toGet = argArray.nextIntMaybeTupleString();
                    break;
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-remove":
                    toRemove = argArray.nextTupleOrPop();
                    operation = OPERATIONS.REMOVE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (talTuple == null) {
                myThingArrayList = new ThingArrayList();
            } else {
                Object maybeList = talTuple.getValue();
                if (maybeList instanceof ThingArrayList) {
                    myThingArrayList = ThingArrayList.class.cast(maybeList);
                } else {
                    getLogger().log(Level.SEVERE, "Tuple provided to -list does not contain a List in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (myThingArrayList == null) {
                getLogger().log(Level.SEVERE, "Could not instance or dereference List in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                switch (operation) {
                    case ADD:
                        if (toAdd == null) {
                            myThingArrayList.add(null);
                        } else {
                            myThingArrayList.add(toAdd.getValue());
                        }
                        try {
                            put(myThingArrayList);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting List in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case ADDSTR:
                        myThingArrayList.add(stringToAdd);
                        try {
                            put(myThingArrayList);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting List in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case CLEAR:
                        myThingArrayList.clear();
                        break;
                    case GET:
                        Object o = myThingArrayList.get(toGet);
                        try {
                            put(o);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting Object in get from List in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case INSTANCE:
                        try {
                            put(myThingArrayList);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting List instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case REMOVE:
                        if (toRemove == null) {
                            getLogger().log(Level.SEVERE, "Null tuple to remove from List in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                put(myThingArrayList.remove(toRemove.getValue()));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting removed " + toAdd.getValue() + " from List in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case SOURCE:
                        if (sourceTuple == null) {
                            getLogger().log(Level.SEVERE, "Null tuple for List source in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            Object sourceObj = sourceTuple.getValue();
                            if (sourceObj instanceof Enumeration || sourceObj instanceof Collection) {
                                myThingArrayList = listFromSource(sourceObj);
                            }
                            if (myThingArrayList == null) {
                                getLogger().log(Level.SEVERE, "No List can be created from source in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else {
                                try {
                                    put(myThingArrayList);
                                } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                    getLogger().log(Level.SEVERE, "Error putting List instance in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                            break;
                        }
                    default:
                        getLogger().log(Level.SEVERE, "Unknown operation unhandled in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    private ThingArrayList listFromSource(Object o) {
        ThingArrayList tal = null;
        if (o instanceof Collection) {
            tal = new ThingArrayList(Collection.class.cast(o));
        } else if (o instanceof Enumeration) {
            tal = new ThingArrayList(Enumeration.class.cast(o));
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
