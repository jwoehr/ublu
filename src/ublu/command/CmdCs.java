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
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ublu.db.Db;
import ublu.db.ResultSetClosure;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Tuple;

/**
 * Command to instance and execute JDBC Callable Statements.
 *
 * @author jwoehr
 */
public class CmdCs extends Command {

    {
        setNameAndDescription("file",
                "/4? [-to @var ] [--,-cs @cs] [-dbconnected @db] [[-instance] -sq1 ~@{ SQL code ... }] | [-call] | [-in ~@{index} ~@object] [-inarray ~@{index} ~@array ~@{type_description}] [-innull ~@{index} ~@{type_description}] [-out ~@{index} ~@{type_description} ~@{scale}] [-rs] : instance and execute callable statements which JDBC uses to execute SQL stored procedures");
    }

    /**
     * The functions performed by the file command
     */
    protected static enum FUNCTIONS {
        /**
         * Instance callable statement
         */
        INSTANCE,
        /**
         * Call callable statement
         */
        CALL,
        /**
         * Get the result set
         */
        RS,
        /**
         * Do nothing
         */
        NOOP
    }

    /**
     * Create instance
     */
    public CmdCs() {
    }

    /**
     * Parse arguments and perform AS400 File operations
     *
     * @param argArray passed-in arg array
     * @return rest of arg array
     */
    public ArgArray doCs(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.INSTANCE;
        Object o; // used for unloading tuples
        Tuple csTuple;
        CallableStatement cs = null;
        Tuple dbTuple;
        Db db = null;
        String sql = null;
        Integer index;
        String typeDescription;
        Object inParameter;
        Integer scale;
        while (argArray.hasDashCommand() && getCommandResult() != COMMANDRESULT.FAILURE) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "--":
                case "-cs":
                    csTuple = argArray.nextTupleOrPop();
                    o = csTuple.getValue();
                    if (o instanceof CallableStatement) {
                        cs = CallableStatement.class.cast(o);
                    } else {
                        getLogger().log(Level.SEVERE, "Supplied tuple is not a Callable Statement instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case "-dbconnected":
                    dbTuple = argArray.nextTupleOrPop();
                    o = dbTuple.getValue();
                    if (o instanceof Db) {
                        db = Db.class.cast(o);
                    } else {
                        getLogger().log(Level.SEVERE, "Supplied tuple is not a Database instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case "-instance":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-in":
                    function = FUNCTIONS.NOOP;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    inParameter = argArray.nextTupleOrPop().getValue();
                    break;
                case "-inarray":
                    function = FUNCTIONS.NOOP;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    inParameter = argArray.nextTupleOrPop().getValue();
                    typeDescription = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-innull":
                    function = FUNCTIONS.NOOP;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    typeDescription = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-out":
                    function = FUNCTIONS.NOOP;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    typeDescription = argArray.nextMaybeQuotationTuplePopString();
                    scale = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-rs":
                    function = FUNCTIONS.RS;
                    break;
                case "-sql":
                    sql = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            switch (function) {
                case INSTANCE:
                    if (db == null) {
                        getLogger().log(Level.SEVERE, "No Database instance supplied via -dbconnected in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            cs = db.getConnection().prepareCall(sql);
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE,
                                    "Encountered an exception preparing call of SQL " + sql + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(cs);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Encountered an exception putting Callable Statement in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;
                case CALL:
                    if (cs != null) {
                        try {
                            put(cs.execute()); // the boolean result
                        } catch (SQLException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Encountered an exception calling Callable Statement in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No Callable Statement proved to -call in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case RS:
                    if (cs != null) {
                        try {
                            put(new ResultSetClosure(cs.getConnection(), cs.getResultSet(), cs));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Encountered an exception putting result set in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No Callable Statement proved to -rs in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NOOP:
                    break;
            }
        }
        return argArray;
    }

    private boolean setIn(int index, Object inParameter) {
        boolean success = false;
        return success;
    }

    private boolean setInArray(int index, Object inParameter, String typeDescription) {
        boolean success = false;
        return success;
    }

    private boolean setInNull(int index, String typeDescription) {
        boolean success = false;
        return success;
    }

    private boolean setOut() {
        boolean success = false;
        return success;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return doCs(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
