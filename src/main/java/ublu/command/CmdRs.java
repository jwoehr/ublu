/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 82, Beulah CO 81023-0082 http://www.softwoehr.com
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
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import org.json.JSONException;
import ublu.db.Db;
import ublu.util.Generics.ByteArrayList;

/**
 * Performs result set to result set operations
 *
 * @author jwoehr
 */
public class CmdRs extends Command {

    {
        // setNameAndDescription("rs", "/0 [--,-rs ~@rs] [-to datasink] [-tofile ~@filepath] [-from datasink] [[-abs ~@{row}] | [-rel ~@{rows}] | [-autocommit 0|1] | [-bytes ~@{index}] | [-close{|db|st} [tuplename]] | [-commit ~@resultSet] | [-fetchsize numrows] | [-fileblob ~@{index} ~@{blobfilepath}] | [-get ~@{index}] | [-lget ~@{label}] | [-getblob ~@{index}] | [-lgetblob ~@{label}] | -insert | [-json ~@db ~@{tablename}] | [-next] | [-split split_specification] | [-toascii numindices index index ..] | [-metadata]] : operate on result sets)");
        setNameAndDescription("rs", "/0 [--,-rs ~@rs] [-to datasink] [-tofile ~@filepath] [-from datasink] [[-abs ~@{row}] | [-rel ~@{rows}] | [-before] | [-after] | [-first] | [-last] | [-rownum] | [-rawrs] | [-autocommit 0|1] | [-bytes ~@{index}] | [-close{|db|st} [tuplename]] | [-commit ~@resultSet] | [-fetchsize numrows] | [-get ~@{index}] | [-lget ~@{label}] | [-getblob ~@{index}] | [-lgetblob ~@{label}] | -insert | [-json ~@db ~@{tablename}] | [-next] | [-split split_specification] | [-toascii numindices index index ..] | [-metadata]] : operate on result sets");
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
    private int getSplitTarget() {
        return splitTarget;
    }

    /**
     * Set one's-based index of column to split for SPLIT function.
     *
     * @param splitTarget one's-based index of column to split
     */
    private void setSplitTarget(int splitTarget) {
        this.splitTarget = splitTarget;
    }

    /**
     * Return array of widths into which to split column.
     *
     * @return array of widths into which to split column.
     */
    private int[] getSplitWidths() {
        return splitWidths;
    }

    /**
     * Set array of widths into which to split column.
     *
     * @param splitWidths array of widths into which to split column.
     */
    private void setSplitWidths(int[] splitWidths) {
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
         * move cursor absolute
         */
        ABS,
        /**
         * move cursor relative
         */
        REL,
        /**
         * Get raw bytes from field
         */
        BYTES,
        /**
         * Set the autocommit mode for result sets
         */
        AUTOCOMMIT,
        /**
         * Set fetchsize for result sets and/or report the fetchsize (only
         * report if size &lt;= 0).
         */
        FETCHSIZE,
        /**
         * Move before first row
         */
        BEFORE,
        /**
         * Move after last row
         */
        AFTER,
        /**
         * Move to first row
         */
        FIRST,
        /**
         * Move to lase row
         */
        LAST,
        /**
         * Get current row number
         */
        ROWNUM,
        /**
         * Get raw result set object
         */
        RAWRS,
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
         * get object in field by index
         */
        GET,
        /**
         * get object in field by label
         */
        LGET,
        //        /**
        //         * write blob in field by index to file
        //         */
        //        FILEBLOB,
        /**
         * fetch blob in field by index or fieldname
         */
        GETBLOB,
        /**
         * Dump the result set as JSON
         */
        JSON,
        /**
         * Get the result set metadata
         */
        METADATA,
        /**
         * next row
         */
        NEXT
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
        int rowsToFetch = 0;
        String commitTupleName = "";
        boolean autoCommitValue = true;
        Db myDb = null;
        ResultSetClosure myRs = null;
        String tableName = null;
        Integer index = null;
        String fieldLabel = null;
        Integer cursorinc = null;
        String blobFileName;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-tofile":
                    setDataDest(DataSink.fileSinkFromTuple(argArray.nextTupleOrPop()));
                    break;
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    break;
                case "--":
                case "-rs":
                    myRs = argArray.nextTupleOrPop().value(ResultSetClosure.class);
                    break;
                case "-abs":
                    setFunction(FUNCTIONS.ABS);
                    cursorinc = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-rel":
                    setFunction(FUNCTIONS.REL);
                    cursorinc = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-next":
                    setFunction(FUNCTIONS.NEXT);
                    break;
                case "-autocommit":
                    setFunction(FUNCTIONS.AUTOCOMMIT);
                    autoCommitValue = argArray.nextInt() == 0;
                    break;
                case "-bytes":
                    setFunction(FUNCTIONS.BYTES);
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-close":
                    setFunction(FUNCTIONS.CLOSE);
                    if (myRs == null) {
                        myRs = argArray.nextTupleOrPop().value(ResultSetClosure.class);
                    }
                    break;
                case "-closedb":
                    setFunction(FUNCTIONS.CLOSEDB);
                    if (myRs == null) {
                        myRs = argArray.nextTupleOrPop().value(ResultSetClosure.class);
                    }
                    break;
                case "-closest":
                    setFunction(FUNCTIONS.CLOSEST);
                    if (myRs == null) {
                        myRs = argArray.nextTupleOrPop().value(ResultSetClosure.class);
                    }
                    break;
                case "-commit":
                    setFunction(FUNCTIONS.COMMIT);
                    commitTupleName = argArray.next();
                    break;
                case "-fetchsize":
                    setFunction(FUNCTIONS.FETCHSIZE);
                    rowsToFetch = argArray.nextInt();
                    break;
                case "-get":
                    setFunction(FUNCTIONS.GET);
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-lget":
                    setFunction(FUNCTIONS.LGET);
                    fieldLabel = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-getblob":
                    setFunction(FUNCTIONS.GETBLOB);
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-lgetblob":
                    setFunction(FUNCTIONS.GETBLOB);
                    fieldLabel = argArray.nextMaybeQuotationTuplePopString();
                    break;
//                case "-fileblob":
//                    setFunction(FUNCTIONS.FILEBLOB);
//                    index = argArray.nextIntMaybeQuotationTuplePopString();
//                    blobFileName = argArray.nextMaybeQuotationTuplePopString();
//                    break;
                case "-insert":
                    setFunction(FUNCTIONS.INSERT);
                    break;
                case "-json":
                    setFunction(FUNCTIONS.JSON);
                    myDb = argArray.nextTupleOrPop().value(Db.class);
                    tableName = argArray.nextMaybeQuotationTuplePopString();
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
                case "-before":
                    function = FUNCTIONS.BEFORE;
                    break;
                case "-after":
                    function = FUNCTIONS.AFTER;
                    break;
                case "-first":
                    function = FUNCTIONS.FIRST;
                    break;
                case "-last":
                    function = FUNCTIONS.LAST;
                    break;
                case "-rownum":
                    function = FUNCTIONS.ROWNUM;
                    break;
                case "-rawrs":
                    function = FUNCTIONS.RAWRS;
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
            switch (getFunction()) {
                case ABS:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -abs in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().absolute(cursorinc));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not set cursor absolute or put result in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case REL:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -rel in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().relative(cursorinc));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not set cursor relative or put result in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case BEFORE:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -before in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            myRs.getResultSet().beforeFirst();
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "Could not set cursor before first in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case AFTER:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -after in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            myRs.getResultSet().afterLast();
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "Could not set cursor after last in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case FIRST:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -first in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().first());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not set cursor first or put result in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case LAST:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -last in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().last());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not set cursor last or put result in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case ROWNUM:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -rownum in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().getRow());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not get rownum or put result in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case RAWRS:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -rawrs in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not get raw result set or put result in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
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

                case BYTES:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -bytes in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().getBytes(index));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not get or put bytes for column index " + index + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case CLOSE:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -close in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            myRs.closeRS();
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "Could not close result set from tuple in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case CLOSEDB:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -closedb in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            myRs.close();
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "Could not close result set from tuple in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case CLOSEST:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -closest in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {

                        try {
                            myRs.closeRS().closeStatement();
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "Could not close result set from tuple in " + getNameAndDescription(), ex);
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

                case GET:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -bytes in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().getObject(index));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not get or put Object for column index " + index + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case LGET:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -bytes in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().getObject(fieldLabel));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not get or put Object for column index " + fieldLabel + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case GETBLOB:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Result set not found for getting blob in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        Blob b;
                        try {
                            if (index != null) {
                                b = (myRs.getResultSet().getBlob(index));
                            } else {
                                b = (myRs.getResultSet().getBlob(fieldLabel));
                            }
                            if (b != null) {
                                switch (getDataDest().getType()) {
                                    case LIFO:
                                    case TUPLE:
                                        put(arrayBlob(b));
                                        break;
                                    case FILE:
                                        blobFileName = getDataDest().getName();
                                        fileBlob(b, blobFileName);
                                        if (getCommandResult() == COMMANDRESULT.FAILURE) {
                                            getLogger().log(Level.SEVERE, "Could not get or write Blob from "
                                                    + index == null ? ("label " + fieldLabel) : (" index " + index)
                                                            + inNameAndDescription());
                                            setCommandResult(COMMANDRESULT.FAILURE);
                                        }
                                        break;
                                    case STD:
                                        streamBlob(b, getInterpreter().getOutputStream());
                                        break;
                                    case ERR:
                                        streamBlob(b, getInterpreter().getErroutStream());
                                        break;
                                    case NULL:
                                        break;
                                    default:
                                        getLogger().log(Level.SEVERE, "Unsupported data destination for Blob in {0}", getNameAndDescription());
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            } else {
                                getLogger().log(Level.SEVERE, "Null Blob in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } catch (SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | IOException ex) {
                            getLogger().log(Level.SEVERE, "Could not get or write Blob in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

//                case FILEBLOB:
//                    if (myRs == null) {
//                        getLogger().log(Level.SEVERE, "Result set not found for -fileblob in {0}", getNameAndDescription());
//                        setCommandResult(COMMANDRESULT.FAILURE);
//                    } else {
//                        try {
//                            Blob b = (myRs.getResultSet().getBlob(index));
//                            if (b != null) {
//                                try (BufferedInputStream bis = new BufferedInputStream(b.getBinaryStream())) {
//                                    FileOutputStream fout = new FileOutputStream(blobFileName);
//                                    while (bis.available() > 0) {
//                                        fout.write(bis.read());
//                                    }
//                                    fout.close();
//                                }
//                            }
//                        } catch (SQLException | IOException ex) {
//                            getLogger().log(Level.SEVERE, "Could not get or write Blob to file from index " + index + inNameAndDescription(), ex);
//                            setCommandResult(COMMANDRESULT.FAILURE);
//                        }
//                    }
//                    break;
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
                            getLogger().log(Level.SEVERE, "Character set conversion failed", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case JSON:
                    if (myRs != null && myDb != null && tableName != null) {
                        try {
                            put(myRs.toJSON(myDb, tableName));
                        } catch (JSONException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception converting or putting JSON in " + getNameAndDescription(), ex);
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
                    if (myRs == null) {
                        if (getDataSrc().getType() == DataSink.SINKTYPE.TUPLE) {
                            myRs = getTuple(getDataSrc().getName()).value(ResultSetClosure.class);
                        }
                    }
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Missing data source in rs -metadata command");
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    }
                    try {
                        put(myRs.getResultSet().getMetaData());
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception getting or putting result set metadata", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (ClassCastException ex) {
                        getLogger().log(Level.SEVERE, "Source command rs -metadata was not a result set", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case NEXT:
                    if (myRs == null) {
                        getLogger().log(Level.SEVERE, "Tuple not found for -next in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myRs.getResultSet().next());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Could not get or put next in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
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
                    ResultSetClosure.class
                            .cast(hopefullyAResultSetClosure).setAutoCommit(ac);
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
                ResultSetClosure.class
                        .cast(hopefullyAResultSetClosure).commit();
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
                    ResultSetClosure rsc = ResultSetClosure.class
                            .cast(hopefullyAResultSetClosure);
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

    private byte[] arrayBlob(Blob b) {
        ByteArrayList bal = new ByteArrayList();
        try (BufferedInputStream bis = new BufferedInputStream(b.getBinaryStream())) {
            while (bis.available() > 0) {
                bal.add((byte) (bis.read()));
            }
        } catch (SQLException | IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading blob in " + getNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        return bal.byteArray();
    }

    private long streamBlob(Blob b, OutputStream os) {
        long l = 0;
        try (BufferedInputStream bis = new BufferedInputStream(b.getBinaryStream())) {

            while (bis.available() > 0) {
                os.write(bis.read());
                l++;
            }
        } catch (SQLException | IOException ex) {
            getLogger().log(Level.SEVERE, "Could not get or write Blob to stream in " + getNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        return l;
    }

    private long fileBlob(Blob b, String blobFileName) {
        long l = 0;
        try (BufferedInputStream bis = new BufferedInputStream(b.getBinaryStream());
                FileOutputStream fout = new FileOutputStream(blobFileName)) {

            while (bis.available() > 0) {
                fout.write(bis.read());
                l++;
            }
        } catch (SQLException | IOException ex) {
            getLogger().log(Level.SEVERE, "Could not get or write Blob to file in " + getNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        return l;
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
