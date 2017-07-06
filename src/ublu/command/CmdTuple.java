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
import ublu.util.ParamSubTuple;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.Autonome;

/**
 * A command to display or manipulate {@link Tuple}s
 *
 * @author jwoehr
 */
public class CmdTuple extends Command {

    static {
        setNameAndDescription("tuple", "/0 [-assign targetname ~@valuesource | -delete @tuplename | -exists @tuplename | -istuplename @tuplename | -null @tuplename | -true @tuplename | -false @tuplename | -name @tuplename | -realname @tuplename | -value ~@tuplename | -sub @subname ~@tuple |  -type ~@tuple | -typename ~@tuple | -map | -autonome ~@tuple | -autonomic ~@tuple | -autonomes ] : operations on tuple variables");
    }

    /**
     * Operate on tuples
     */
    public CmdTuple() {
    }

    /**
     * The functions this command knows
     */
    protected enum FUNCTIONS {

        /**
         * set null
         */
        NULL,
        /**
         * Assign the value of one tuple to another
         */
        ASSIGN,
        /**
         * is the argument a valid tuple name
         */
        ISTUPLENAME,
        /**
         * delete a tuple
         */
        DELETE,
        /**
         * return a map of all tuples
         */
        MAP,
        /**
         * set or create a tuple true
         */
        TRUE,
        /**
         * set or create a tuple false
         */
        FALSE,
        /**
         * Tuple already instanced?
         */
        EXISTS,
        /**
         * Return the key of the tuple. If a bound tuple in functor param
         * substitution, will return the functor parameter bound name.
         */
        NAME,
        /**
         * Return the key of the real tuple. If we have a tuple bound to a
         * functor parameter binding temp, we can find the real name.
         */
        REALNAME,
        /**
         * Return the java class of the operand
         */
        TYPE,
        /**
         * Return a typename, usually a java class
         */
        TYPENAME,
        /**
         * Return the value object
         */
        VALUE,
        /**
         * Substitute tuple
         */
        SUB,
        /**
         * Delivers autonome info
         */
        AUTONOME,
        /**
         * Delivers autonome boolean
         */
        AUTONOMIC,
        /**
         * Delivers autonomes info
         */
        AUTONOMES
    }
    /**
     * The function we're executing
     */
    protected FUNCTIONS function;

    /**
     * Get the function we're executing
     *
     * @return the function we're executing
     */
    public FUNCTIONS getFunction() {
        return function;
    }

    /**
     * Set the function we're executing
     *
     * @param function the function we're executing
     */
    public final void setFunction(FUNCTIONS function) {
        this.function = function;
    }

    /**
     * Operate on tuples
     *
     * @param argArray arg list to interpreter
     * @return what's left of the list
     */
    public ArgArray tuple(ArgArray argArray) {
        setFunction(FUNCTIONS.MAP); // the default with no dash-command
        Tuple someTuple = null;
        String someName = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            String destName;
            switch (dashCommand) {
                case "-to":
                    destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                // case "-from":
                //    srcName = argArray.next();
                //    setDataSrc(DataSink.fromSinkName(srcName));
                //    break;
                case "-assign":
                    setFunction(FUNCTIONS.ASSIGN);
                    someName = argArray.next();
                    someTuple = argArray.nextTupleOrPop();
                    break;
                case "-delete":
                    setFunction(FUNCTIONS.DELETE);
                    someName = argArray.next();
                    break;
                case "-exists":
                    setFunction(FUNCTIONS.EXISTS);
                    someName = argArray.next();
                    break;
                case "-istuplename":
                    setFunction(FUNCTIONS.ISTUPLENAME);
                    someName = argArray.next();
                    break;
                case "-map":
                    setFunction(FUNCTIONS.MAP);
                    break;
                case "-null":
                    setFunction(FUNCTIONS.NULL);
                    someName = argArray.next();
                    break;
                case "-name":
                    setFunction(FUNCTIONS.NAME);
                    someName = argArray.next();
                    break;
                case "-realname":
                    setFunction(FUNCTIONS.REALNAME);
                    someName = argArray.next();
                    break;
                case "-true":
                    setFunction(FUNCTIONS.TRUE);
                    someName = argArray.next();
                    break;
                case "-false":
                    setFunction(FUNCTIONS.FALSE);
                    someName = argArray.next();
                    break;
                case "-type":
                    setFunction(FUNCTIONS.TYPE);
                    someTuple = argArray.nextTupleOrPop();
                    break;
                case "-typename":
                    setFunction(FUNCTIONS.TYPENAME);
                    someTuple = argArray.nextTupleOrPop();
                    break;
                case "-value":
                    setFunction(FUNCTIONS.VALUE);
                    someTuple = argArray.nextTupleOrPop();
                    break;
                case "-sub":
                    setFunction(FUNCTIONS.SUB);
                    someName = argArray.next();
                    someTuple = argArray.nextTupleOrPop();
                    break;
                case "-autonome":
                    setFunction(FUNCTIONS.AUTONOME);
                    someTuple = argArray.nextTupleOrPop();
                    break;
                case "-autonomic":
                    setFunction(FUNCTIONS.AUTONOMIC);
                    someTuple = argArray.nextTupleOrPop();
                    break;
                case "-autonomes":
                    setFunction(FUNCTIONS.AUTONOMES);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            String theTupleName;
            Tuple t;
            String autonome = null;
            switch (getFunction()) {
                case ASSIGN:
                    if (Tuple.isTupleName(someName)) {
                        getTuple(someName).setValue(someTuple.getValue());
                    } else if (ArgArray.isPopTupleSign(someName)) {
                        pushTuple(new Tuple(null, someTuple.getValue()));
                    } else {
                        getLogger().log(Level.SEVERE, "Name {0} is not a tuple name.", someName);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case DELETE:
                    theTupleName = someName;
                    t = getTuple(theTupleName);
                    if (t == null) {
                        getLogger().log(Level.WARNING, "{0} is not found or is invalid in the tuple map", theTupleName);
                    } else {
                        t = t.getBoundTuple();
                    }
                    if (t == null) {
                        getLogger().log(Level.WARNING, "{0} is not found or is invalid in the tuple map", theTupleName);
                    } else {
                        getInterpreter().deleteTuple(t);
                    }
                    break;
                case EXISTS:
                    theTupleName = someName;
                    boolean result = false;
                    someTuple = getTuple(theTupleName);
                    if (someTuple != null) {
                        result = someTuple.getBoundTuple() != null;
                    }
                    try {
                        put(result);
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception encountered in putting tuple existence", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case ISTUPLENAME:
                    try {
                        put(Tuple.isTupleName(someName));
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception encountered in putting tuple name validity", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case MAP:
                    String keyDisplay = getInterpreter().getTupleMap().keysAsDisplayString();
                    try {
                        put(keyDisplay);
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception encountered in putting tuple map", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case TRUE:
                    theTupleName = someName;
                    if (Tuple.isTupleName(theTupleName)) {
                        someTuple = getTuple(someName);
                        if (someTuple == null) {
                            setTuple(theTupleName, true);
                        } else {
                            someTuple.setValue(true); // to make sure it gets created if only proposed
                            if (Tuple.isParamSubTupleName(someName)) {
                                getInterpreter().setTupleNoLocal(someTuple.getBoundKey(), someTuple.getValue());
                            }
                        }
                    } else if (ArgArray.isPopTupleSign(theTupleName)) {
                        getTupleStack().push(getTupleStack().pop().setValue(true));
                    } else {
                        getLogger().log(Level.SEVERE, "Name {0} is not a tuple name.", theTupleName);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case FALSE:
                    theTupleName = someName;
                    if (Tuple.isTupleName(theTupleName)) {
                        someTuple = getTuple(someName);
                        if (someTuple == null) {
                            setTuple(theTupleName, false);
                        } else {
                            someTuple.setValue(false); // to make sure it gets created if only proposed
                            if (Tuple.isParamSubTupleName(someName)) {
                                getInterpreter().setTupleNoLocal(someTuple.getBoundKey(), someTuple.getValue());
                            }
                        }
                    } else if (ArgArray.isPopTupleSign(theTupleName)) {
                        getTupleStack().push(getTupleStack().pop().setValue(false));
                    } else {
                        getLogger().log(Level.SEVERE, "Name {0} is not a tuple name.", theTupleName);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NAME:
                    theTupleName = someName;
                    if (Tuple.isTupleName(theTupleName)) {
                        try {
                            put(theTupleName);
                        } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception encountered in finding or putting tuple name", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Name {0} is not a tuple name.", theTupleName);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case REALNAME:
                    theTupleName = someName;
                    if (Tuple.isTupleName(theTupleName)) {
                        try {
                            put(getInterpreter().getTuple(theTupleName).getBoundKey());
                        } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception encountered in finding or putting tuple real name", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Name {0} is not a tuple name.", theTupleName);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case NULL:
                    theTupleName = someName;
                    if (Tuple.isTupleName(theTupleName)) {
                        setTuple(theTupleName, null);
                    } else if (ArgArray.isPopTupleSign(theTupleName)) {
                        getTupleStack().push(getTupleStack().pop().setValue(null));
                    } else {
                        getLogger().log(Level.SEVERE, "Name {0} is not a tuple name.", theTupleName);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case TYPE:
                    if (someTuple != null) {
                        try {
                            Object value = someTuple.getValue();
                            if (value == null) {
                                put(value);
                            } else {
                                put(value.getClass());
                            }

                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put tuple class name in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No tuple found for -typename in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case TYPENAME:
                    if (someTuple != null) {
                        try {
                            Object value = someTuple.getValue();
                            if (value == null) {
                                put(value);
                            } else {
                                put(value.getClass().getName());
                            }

                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put tuple class name in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No tuple found for -typename in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case VALUE:
                    if (someTuple == null) {
                        getLogger().log(Level.SEVERE, "Null tuple found for -value in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(someTuple.getValue());
                        } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception encountered in finding or putting tuple real name", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case SUB:
                    if (someName != null) {
                        if (someTuple != null) {
                            t = new ParamSubTuple(someName, someTuple, someTuple.getProposedKey());
                            try {
                                getInterpreter().getTupleMap().putTuple(t);
                                put(t);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Couldn't put param sub tuple in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "Null tuple found for -sub in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Null name found for -sub in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case AUTONOME:

                    if (someTuple != null) {
                        Object o = someTuple.getValue();
                        if (o != null) {
                            autonome = Autonome.autonomeDescription(o);
                        }
                    }
                    try {
                        put(autonome);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Couldn't put autonome description in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case AUTONOMIC:
                    try {
                        if (someTuple != null) {
                            Object o = someTuple.getValue();
                            if (o != null) {
                                put(Autonome.isAutonomic(o));
                            } else {
                                put(false);
                            }
                        } else {
                            put(false);
                        }
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Couldn't put autonomic flag in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case AUTONOMES:
                    try {
                        put(Autonome.displayAll());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Couldn't put autonomes in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unhandled case in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public void reinit() {
        super.reinit();
        setFunction(FUNCTIONS.NULL);
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return tuple(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
