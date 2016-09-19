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

import ublu.db.ResultSetClosure;
import ublu.db.ResultSetHelper;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Performs result set to result set operations
 *
 * @author jwoehr
 */
public class CmdRs extends Command {

    {
        setNameAndDescription("rs", "/0 [-autocommit 0|1 | -close{|db|st} tuplename | -commit @resultSetTuple | -fetchsize numrows | -insert | -split split_specification] [-toascii numindices index index ..] [-metadata] -from tuplename -to @tuplename : tuples assumed to hold result sets, performs the indicated operation (such as commit, set autocommit mode, set&get fetchsize) out of the 'from' result set into the 'to' result set (splitting if -split is chosen instead of -insert) or closes the result set represented by the @tuplename argument to -close (and the statement if -closest and also disconnects db instance if -closedb)");
    }

    /**
     * Arity-0 ctor
     */
    public CmdRs() {
    }
    private int splitTarget;
    private int[] splitWidths;

    /**
     * Return one's-based index of column to split for SPLIT function.
     *
     * @return one's-based index of column to split
     */
    protected int getSplitTarget() {
        return splitTarget;
    }

    /**
     * Set one's-based index of column to split for SPLIT function.
     *
     * @param splitTarget one's-based index of column to split
     */
    protected void setSplitTarget(int splitTarget) {
        this.splitTarget = splitTarget;
    }

    /**
     * Return array of widths into which to split column.
     *
     * @return array of widths into which to split column.
     */
    protected int[] getSplitWidths() {
        return splitWidths;
    }

    /**
     * Set array of widths into which to split column.
     *
     * @param splitWidths array of widths into which to split column.
     */
    protected void setSplitWidths(int[] splitWidths) {
        this.splitWidths = splitWidths;
    }

    /**
     * The functions this command knows
     */
    protected enum FUNCTIONS {

        /**
         * Do nothing.
         */
        NULL,
        /**
         * Set the autocommit mode for result sets
         */
        AUTOCOMMIT,
        /**
         * Set fetchsize for result sets and/or report the fetchsize (only
         * report if size &lt;&equals; 0).
         */
        FETCHSIZE,
        /**
         * Commit a result set
         */
        COMMIT,
        /**
         * Insert the source result set into the dest result set.
         */
        INSERT,
        /**
         * Insert the source result set into the dest result set splitting one
         * (char/byte) column into multiple columns.
         */
        SPLIT,
        /**
         * Close a result set, deleting the tuple.
         */
        CLOSE,
        /**
         * Close a result set and the statement that originated it, deleting the
         * tuple.
         */
        CLOSEST,
        /**
         * Close a result set, the statement that originated it, and disconnect
         * the underlying database instance, deleting the tuple.
         */
        CLOSEDB,
        /**
         * Get the result set metadata
         */
        METADATA
    }
    /**
     * The function this pass through the command is going to perform
     */
    protected FUNCTIONS function;

    /**
     * Get the function this pass through the command is going to perform.
     *
     * @return the function this pass through the command is going to perform
     */
    public FUNCTIONS getFunction() {
        return function;
    }

    /**
     * Set the function this pass through the command is going to perform.
     *
     * @param function the function this pass through the command is going to
     * perform
     */
    public final void setFunction(FUNCTIONS function) {
        this.function = function;
    }

    /**
     * Operate on result sets
     *
     * @param argArray the args to the interpreter
     * @return what's left of the args
     */
    public ArgArray rs(ArgArray argArray) {
        ResultSetHelper.CONVERSION conversion = null;
        Generics.IndexList charConversionIndexList = null;
        String closeTupleName = "";
        int rowsToFetch = 0;
        String commitTupleName = "";
        boolean autoCommitValue = true;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            String srcName;
            String destName;
            switch (dashCommand) {
                case "-to":
                    destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-from":
                    srcName = argArray.next();
                    setDataSrc(DataSink.fromSinkName(srcName));
                    break;
                case "-autocommit":
                    setFunction(FUNCTIONS.AUTOCOMMIT);
                    autoCommitValue = argArray.nextInt() == 0 ? false : true;
                    break;
                case "-close":
                    closeTupleName = argArray.next();
                    setFunction(FUNCTIONS.CLOSE);
                    break;
                case "-closedb":
                    closeTupleName = argArray.next();
                    setFunction(FUNCTIONS.CLOSEDB);
                    break;
                case "-closest":
                    closeTupleName = argArray.next();
                    setFunction(FUNCTIONS.CLOSEST);
                    break;
                case "-commit":
                    setFunction(FUNCTIONS.COMMIT);
                    commitTupleName = argArray.next();
                    break;
                case "-fetchsize":
                    setFunction(FUNCTIONS.FETCHSIZE);
                    rowsToFetch = argArray.nextInt();
                    break;
                case "-insert":
                    setFunction(FUNCTIONS.INSERT);
                    break;
                case "-metadata":
                    setFunction(FUNCTIONS.METADATA);
                    break;
                case "-split":
                    setFunction(FUNCTIONS.SPLIT);
                    setSplitTarget(argArray.nextInt());
                    setSplitWidths(argArray.parseIntArray());
                    break;
                case "-toascii":
                    conversion = ResultSetHelper.CONVERSION.TOASCII;
                    charConversionIndexList = new Generics.IndexList();
                    charConversionIndexList.addAll(argArray.parseIntArray());
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            ResultSetClosure srcResultSetClosure;
            ResultSetClosure destResultSetClosure;
            Tuple closeTuple;
            switch (getFunction()) {
                case AUTOCOMMIT:
                    try {
                        setAutoCommit(getDataSrc(), autoCommitValue);
                    } catch (SQLException ex) {
                        getLogger().log(Level.SEVERE, "Exception setting autocommit mode in CmdRs for source result set", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    try {
                        setAutoCommit(getDataDest(), autoCommitValue);
                    } catch (SQLException ex) {
                        getLogger().log(Level.SEVERE, "Exception setting autocommit mode in CmdRs for destination result set", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case CLOSE:
                    closeTuple = getTuple(closeTupleName);
                    if (closeTuple == null) {
                        getLogger().log(Level.WARNING, "Tuple {0} not found", closeTupleName);
                    } else {
                        Object hopefullyAResultSetClosure = closeTuple.getValue();
                        if (hopefullyAResultSetClosure instanceof ResultSetClosure) {
                            try {
                                ResultSetClosure.class.cast(hopefullyAResultSetClosure).closeRS();
                                getInterpreter().deleteTuple(closeTupleName);
                            } catch (SQLException ex) {
                                getLogger().log(Level.SEVERE, "Could not close result set from tuple " + closeTupleName, ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "Tuple {0} is not a result set.", closeTupleName);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case CLOSEDB:
                    closeTuple = getTuple(closeTupleName);
                    if (closeTuple == null) {
                        getLogger().log(Level.WARNING, "Tuple {0} not found", closeTupleName);
                    } else {
                        Object hopefullyAResultSetClosure = closeTuple.getValue();
                        if (hopefullyAResultSetClosure instanceof ResultSetClosure) {
                            try {
                                ResultSetClosure.class.cast(hopefullyAResultSetClosure).close();
                                getInterpreter().deleteTuple(closeTupleName);
                            } catch (SQLException ex) {
                                getLogger().log(Level.SEVERE, "Could not close result set from tuple " + closeTupleName, ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "Tuple {0} is not a result set.", closeTupleName);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case CLOSEST:
                    closeTuple = getTuple(closeTupleName);
                    if (closeTuple == null) {
                        getLogger().log(Level.WARNING, "Tuple {0} not found", closeTupleName);
                    } else {
                        Object hopefullyAResultSetClosure = closeTuple.getValue();
                        if (hopefullyAResultSetClosure instanceof ResultSetClosure) {
                            try {
                                ResultSetClosure.class.cast(hopefullyAResultSetClosure).
                                        closeRS().closeStatement();
                                getInterpreter().deleteTuple(closeTupleName);
                            } catch (SQLException ex) {
                                getLogger().log(Level.SEVERE, "Could not close result set from tuple " + closeTupleName, ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "Tuple {0} is not a result set.", closeTupleName);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case COMMIT:
                    try {
                        commit(commitTupleName);
                    } catch (SQLException ex) {
                        getLogger().log(Level.SEVERE, "Error committing result set from tuple " + commitTupleName, ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case FETCHSIZE:
                    try {
                        setFetchSize(getDataSrc(), rowsToFetch);
                    } catch (SQLException ex) {
                        getLogger().log(Level.SEVERE, "Exception setting autocommit mode in CmdRs for source result set", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    try {
                        setFetchSize(getDataDest(), rowsToFetch);
                    } catch (SQLException ex) {
                        getLogger().log(Level.SEVERE, "Exception setting autocommit mode in CmdRs for destination result set", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case INSERT:
                    if (getDataDest() == null | getDataSrc() == null) {
                        getLogger().log(Level.SEVERE, "Missing data source or data dest in rs command");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (!getDataDest().getType().equals(DataSink.SINKTYPE.TUPLE)
                            || !getDataSrc().getType().equals(DataSink.SINKTYPE.TUPLE)) {
                        getLogger().log(Level.SEVERE, "Source dest or data dest in rs command not a tuple");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            srcResultSetClosure = ResultSetClosure.class.cast(getTuple(getDataSrc().getName()).getValue());
                            destResultSetClosure = ResultSetClosure.class.cast(getTuple(getDataDest().getName()).getValue());
                            if (srcResultSetClosure == null || destResultSetClosure == null) {
                                getLogger().log(Level.SEVERE, "Null instead of result set for one or both of source and/or destination in rs");
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else {
                                ResultSet srcResultSet = srcResultSetClosure.getResultSet();
                                ResultSet destResultSet = destResultSetClosure.getResultSet();
                                ResultSetHelper rsh = new ResultSetHelper(srcResultSet, destResultSet, charConversionIndexList, conversion);
                                rsh.updateInsertingTable();
                            }
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "SQL exception in command rs", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (ClassCastException ex) {
                            getLogger().log(Level.SEVERE, "Either the source or destination in command rs was not a result set", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (UnsupportedEncodingException ex) {
                            getLogger().log(Level.SEVERE, "Charcter set conversion failed", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case SPLIT:
                    if (getDataDest() == null | getDataSrc() == null) {
                        getLogger().log(Level.SEVERE, "Missing data source or data dest in rs command");
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    } else if (!getDataDest().getType().equals(DataSink.SINKTYPE.TUPLE)
                            || !getDataSrc().getType().equals(DataSink.SINKTYPE.TUPLE)) {
                        getLogger().log(Level.SEVERE, "Source dest or data dest in rs command not a tuple");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            srcResultSetClosure = ResultSetClosure.class.cast(getTuple(getDataSrc().getName()).getValue());
                            destResultSetClosure = ResultSetClosure.class.cast(getTuple(getDataDest().getName()).getValue());
                            if (srcResultSetClosure == null || destResultSetClosure == null) {
                                getLogger().log(Level.SEVERE, "Null instead of result set for one or both of source and/or destination in rs");
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else {
                                ResultSet srcResultSet = srcResultSetClosure.getResultSet();
                                ResultSet destResultSet = destResultSetClosure.getResultSet();
                                ResultSetHelper rsh = new ResultSetHelper(srcResultSet, destResultSet, charConversionIndexList, conversion);
                                rsh.updateSplittingInsertingTable(getSplitTarget(), getSplitWidths());
                            }
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "SQL exception in command rs", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (ClassCastException ex) {
                            getLogger().log(Level.SEVERE, "Either the source or destination in command rs was not a result set", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (UnsupportedEncodingException ex) {
                            getLogger().log(Level.SEVERE, "Charcter set conversion failed", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case METADATA:
                    if (getDataSrc() == null) {
                        getLogger().log(Level.SEVERE, "Missing data source in rs -metadata command");
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    }
                    try {
                        srcResultSetClosure = ResultSetClosure.class.cast(getTuple(getDataSrc().getName()).getValue());
                        put(srcResultSetClosure.getResultSet().getMetaData());
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception getting or putting result set metadata", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (ClassCastException ex) {
                        getLogger().log(Level.SEVERE, "Source command rs -metadata was not a result set", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }
        return argArray;
    }

    private void setAutoCommit(DataSink ds, boolean ac) throws SQLException {
        Tuple autoCommitTuple;
        Object hopefullyAResultSetClosure;
        if (ds.getType() == DataSink.SINKTYPE.TUPLE) {
            autoCommitTuple = getTuple(getDataSrc().getName());
            if (autoCommitTuple == null) {
                getLogger().log(Level.WARNING, "Tuple {0} not found", getDataSrc().getName());
            } else {
                hopefullyAResultSetClosure = autoCommitTuple.getValue();
                if (hopefullyAResultSetClosure instanceof ResultSetClosure) {
                    ResultSetClosure.class.cast(hopefullyAResultSetClosure).setAutoCommit(ac);
                } else {
                    getLogger().log(Level.WARNING, "Source tuple {0} is not a ResultSetClosure", autoCommitTuple.getKey());
                }
            }
        } else {
            getLogger().log(Level.WARNING, "DataSink {0} is not a Tuple", ds.getName());
        }
    }

    private void commit(String commitTupleName) throws SQLException {
        Tuple commitTuple;
        Object hopefullyAResultSetClosure;
        commitTuple = getTuple(commitTupleName);
        if (commitTuple == null) {
            getLogger().log(Level.WARNING, "Tuple {0} not found", commitTupleName);
        } else {
            hopefullyAResultSetClosure = commitTuple.getValue();
            if (hopefullyAResultSetClosure instanceof ResultSetClosure) {
                ResultSetClosure.class.cast(hopefullyAResultSetClosure).commit();
            } else {
                getLogger().log(Level.WARNING, "Source tuple {0} is not a ResultSetClosure", commitTuple.getKey());
            }
        }
    }

    private void setFetchSize(DataSink ds, int fetchSize) throws SQLException {
        Tuple fetchSizeTuple;
        Object hopefullyAResultSetClosure;
        if (ds.getType() == DataSink.SINKTYPE.TUPLE) {
            fetchSizeTuple = getTuple(getDataSrc().getName());
            if (fetchSizeTuple == null) {
                getLogger().log(Level.WARNING, "Tuple {0} not found", getDataSrc().getName());
            } else {
                hopefullyAResultSetClosure = fetchSizeTuple.getValue();
                if (hopefullyAResultSetClosure instanceof ResultSetClosure) {
                    ResultSetClosure rsc = ResultSetClosure.class.cast(hopefullyAResultSetClosure);
                    if (fetchSize > 0) {
                        rsc.setFetchSize(fetchSize);
                    }
                    getLogger().log(Level.INFO, "Fetch size for result set in tuple {0} is {1}", new Object[]{fetchSizeTuple.getKey(), rsc.getResultSet().getFetchSize()});
                } else {
                    getLogger().log(Level.WARNING, "Source tuple {0} is not a ResultSetClosure", fetchSizeTuple.getKey());
                }
            }
        } else {
            getLogger().log(Level.WARNING, "DataSink {0} is not a Tuple", ds.getName());
        }
    }

    @Override
    public void reinit() {
        super.reinit();
        setFunction(FUNCTIONS.NULL);
        setSplitTarget(0); // not strictly necessary since -split instances
        setSplitWidths(null); // not strictly necessary since -split instances
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return rs(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
