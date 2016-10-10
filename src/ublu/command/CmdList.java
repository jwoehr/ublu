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
import ublu.util.Generics.StringArrayList;

/**
 * Create and manage lists
 *
 * @author jwoehr
 */
public class CmdList extends Command {

    {
        setNameAndDescription("list",
                "/0 [-to datasink] [--,-list ~@list] [[-new,-instance] | [-source ~@enumeration|~@collection|~@string|-@array] | [-add ~@object ] | [-addstr ~@{ some string }] | [-clear] | [-get ~@{intindex}] | [-set ~@{intindex} ~@object] | [-remove ~@object] | [-removeat ~@{intindex}] | [-size] | [-toarray]]: create and manage lists of objects");
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
         * Set object to list at index
         */
        SET,
        /**
         * Remove object from list
         */
        REMOVE,
        /**
         * Remove object at index from list
         */
        REMOVEAT,
        /**
         * Create list
         */
        INSTANCE,
        /**
         * Source enum or collection
         */
        SOURCE,
        /**
         * Size of list
         */
        SIZE,
        /**
         * Convert to Object[]
         */
        TOARRAY
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
        Tuple toAddRemove = null;
        Integer toGetSet = null;
        String stringToAdd = null;
        Tuple sourceTuple = null;
        int removeIndex = 0;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "--":
                case "-list":
                    myThingArrayList = argArray.nextTupleOrPop().value(ThingArrayList.class);
                    break;
                case "-new":
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-source":
                    operation = OPERATIONS.SOURCE;
                    sourceTuple = argArray.nextTupleOrPop();
                    break;
                case "-add":
                    toAddRemove = argArray.nextTupleOrPop();
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
                    toGetSet = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-set":
                    toGetSet = argArray.nextIntMaybeQuotationTuplePopString();
                    toAddRemove = argArray.nextTupleOrPop();
                    operation = OPERATIONS.SET;
                    break;
                case "-remove":
                    toAddRemove = argArray.nextTupleOrPop();
                    operation = OPERATIONS.REMOVE;
                    break;
                case "-removeat":
                    removeIndex = argArray.nextIntMaybeQuotationTuplePopString();
                    operation = OPERATIONS.REMOVEAT;
                    break;
                case "-size":
                    operation = OPERATIONS.SIZE;
                    break;
                case "-toarray":
                    operation = OPERATIONS.TOARRAY;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (operation) {
                case ADD:
                    if (myThingArrayList == null) {
                        noListError();
                    } else if (toAddRemove == null) {
                        myThingArrayList.add(null);
                    } else {
                        myThingArrayList.add(toAddRemove.getValue());
                    }
                    break;
                case ADDSTR:
                    if (myThingArrayList == null) {
                        noListError();
                    } else {
                        myThingArrayList.add(stringToAdd);
                    }
                    break;
                case CLEAR:
                    if (myThingArrayList == null) {
                        noListError();
                    } else {
                        myThingArrayList.clear();
                    }
                    break;
                case GET:
                    if (myThingArrayList == null) {
                        noListError();
                    } else {
                        Object o = myThingArrayList.get(toGetSet);
                        try {
                            put(o);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting Object in get from List in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case SET:
                    if (myThingArrayList == null) {
                        noListError();
                    } else {
                        Object o = myThingArrayList.set(toGetSet, toAddRemove);
                        try {
                            put(o);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting Object in set from List in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case INSTANCE:
                    try {
                        put(new ThingArrayList());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting List instance in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case REMOVE:
                    if (myThingArrayList == null) {
                        noListError();
                    } else if (toAddRemove == null) {
                        getLogger().log(Level.SEVERE, "Null tuple to remove from List in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myThingArrayList.remove(toAddRemove.getValue()));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting removed " + toAddRemove.getValue() + " from List in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case REMOVEAT:
                    if (myThingArrayList == null) {
                        noListError();
                    } else {
                        try {
                            put(myThingArrayList.remove(removeIndex));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting removed " + toAddRemove == null ? null : toAddRemove.getValue() + " from List in " + getNameAndDescription(), ex);
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
                        myThingArrayList = listFromSource(sourceObj);
                        try {
                            put(myThingArrayList);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting List instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case SIZE:
                    if (myThingArrayList == null) {
                        noListError();
                    } else {
                        try {
                            put(myThingArrayList.size());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting List size in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case TOARRAY:
                    if (myThingArrayList == null) {
                        noListError();
                    } else {
                        try {
                            put(myThingArrayList.toArray());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting List as an Object Array in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unknown operation unhandled in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private void noListError() {
        getLogger().log(Level.SEVERE, "No List in {0}", getNameAndDescription());
        setCommandResult(COMMANDRESULT.FAILURE);
    }

    private static final Object MODELARRAY[] = new Object[0];
    private static final Class ARRAYCLASS = MODELARRAY.getClass();

    private ThingArrayList listFromSource(Object o) {
        ThingArrayList tal = null;
        if (o instanceof Collection) {
            tal = new ThingArrayList(Collection.class
                    .cast(o));
        } else if (o instanceof Enumeration) {
            tal = new ThingArrayList(Enumeration.class
                    .cast(o));
        } else if (o instanceof String) {
            tal = new ThingArrayList(new StringArrayList(String.class.cast(o)));
        } else if (o.getClass().equals(ARRAYCLASS)) {
            tal = new ThingArrayList((Object[]) o);
        } else {
            getLogger().log(Level.SEVERE, "Cannot create List from {0} in {1}", new Object[]{o, getNameAndDescription()});
            setCommandResult(COMMANDRESULT.FAILURE);
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
