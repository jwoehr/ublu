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

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLException;
// import java.sql.SQLType;
import java.util.logging.Level;
import ublu.db.Db;
import ublu.db.ResultSetClosure;
import ublu.util.ArgArray;
import ublu.util.Generics.ByteArrayList;
import ublu.util.Tuple;

/**
 * Command to instance and execute JDBC Callable Statements.
 *
 * @author jwoehr
 */
public class CmdCs extends Command {

    {
        setNameAndDescription("cs",
                "/4? [-to @var ] [--,-cs ~@cs] [-db,-dbconnected ~@db] [[[-new] -sql ~@{ SQL code ... }] "
                        + "| [-call] | [-in ~@{index} ~@object ~@{sqltypename}] | [-innull ~@{index} ~@{sqltypename}] "
                        + "| [-out ~@{index} ~@{sql_type} [-scale ~@{scale}] [-typedescription ~@{user_typename}]] "
                        + "| [-rs] | [-nextrs] | [-uc]] "
                        + ": instance and execute callable statements which JDBC uses to execute SQL stored procedures");
    }

    /**
     * The functions performed by the file command
     */
    protected enum FUNCTIONS {
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
         * Get next of multiple result sets
         */
        NEXTRS,
        /**
         * Get the update count
         */
        UC,
        /**
         * set inparam
         */
        IN,
        /**
         * set inparam null
         */
        INNULL,
        /**
         * register outparam
         */
        OUT,
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
        CallableStatement cs = null;
        Db db = null;
        String sql = null;
        Integer index = null;
        String sqlTypeName = null;
        String typeDescription = null;
        Tuple inParameterTuple = null;
        Integer scale = null;
        Integer length = null;
        while (argArray.hasDashCommand() && getCommandResult() != COMMANDRESULT.FAILURE) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                case "-cs":
                    cs = argArray.nextTupleOrPop().value(CallableStatement.class);
                    break;
                case "-db":
                case "-dbconnected":
                    db = argArray.nextTupleOrPop().value(Db.class);
                    break;
                case "-new":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-in":
                    function = FUNCTIONS.IN;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    inParameterTuple = argArray.nextTupleOrPop();
                    sqlTypeName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-inarray":
                    function = FUNCTIONS.NOOP;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    inParameterTuple = argArray.nextTupleOrPop();
                    sqlTypeName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-innull":
                    function = FUNCTIONS.INNULL;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    sqlTypeName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    typeDescription = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-call":
                    function = FUNCTIONS.CALL;
                    break;
                case "-nextrs":
                    function = FUNCTIONS.NEXTRS;
                    break;
                case "-out":
                    function = FUNCTIONS.OUT;
                    index = argArray.nextIntMaybeQuotationTuplePopString();
                    sqlTypeName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-scale":
                    scale = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-typedescription":
                    typeDescription = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-rs":
                    function = FUNCTIONS.RS;
                    break;
                case "-sql":
                    sql = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-uc":
                    function = FUNCTIONS.UC;
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
                                    "Encountered an exception preparing call of SQL " + sql + inNameAndDescription(), ex);
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
                case NEXTRS:
                    if (cs != null) {
                        try {
                            if (cs.getMoreResults()) {
                                put(new ResultSetClosure(cs.getConnection(), cs.getResultSet(), cs));
                            } else {
                                put(null);
                            }
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Encountered an exception putting result set in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No Callable Statement proved to -nextrs in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case UC:
                    if (cs != null) {
                        try {
                            put(cs.getUpdateCount());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Encountered an exception putting result set in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No Callable Statement proved to -uc in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case IN:
                    if (cs != null) {
                        if (inParameterTuple != null) {
                            o = inParameterTuple.getValue();
                            try {
                                setIn(cs, index, o, sqlTypeName, length);
                            } catch (SQLException ex) {
                                getLogger().log(Level.SEVERE, "Encountered an exception registering out param in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No in parameter tuple proved to -in in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No Callable Statement proved to -in in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case INNULL:
                    if (cs != null) {
                        try {
                            setInNull(cs, index, sqlTypeName, typeDescription);
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "Encountered an exception registering out param in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No Callable Statement proved to -in in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case OUT:
                    if (cs != null) {
                        try {
                            setOut(cs, index, sqlTypeName, typeDescription, scale);
                        } catch (SQLException ex) {
                            getLogger().log(Level.SEVERE, "Encountered an exception registering out param in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No Callable Statement proved to -out in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NOOP:
                    break;
            }
        }
        return argArray;
    }

    private void setIn(CallableStatement cs, int parameterIndex, Object x, String sqlTypeName, Integer length) throws SQLException {
        switch (sqlTypeName.toUpperCase()) {
            case "BOOLEAN":
                cs.setBoolean(parameterIndex, (Boolean) x);
            case "BYTE":
                cs.setByte(parameterIndex, (Byte) x);
            case "BYTES":
                cs.setBytes(parameterIndex, ((ByteArrayList) x).byteArray());
            case "DOUBLE":
                cs.setDouble(parameterIndex, (Double) x);
            case "FLOAT":
                cs.setFloat(parameterIndex, (Float) x);
            case "INT":
                cs.setInt(parameterIndex, (Integer) x);
            case "LONG":
                cs.setLong(parameterIndex, (Long) x);
            case "ARRAY":
                cs.setArray(parameterIndex, (Array) x);
                break;
            case "STRING":
                cs.setString(parameterIndex, (String) x);
                break;
            case "ASCIISTREAM":
                if (length == null) {
                    cs.setAsciiStream(parameterIndex, (InputStream) x);
                } else {
                    cs.setAsciiStream(parameterIndex, (InputStream) x, length);
                }
                break;
            case "BIGDECIMAL":
                cs.setBigDecimal(parameterIndex, (BigDecimal) x);
                break;
            case "BINARYSTREAM":
                if (length == null) {
                    cs.setBinaryStream(parameterIndex, (InputStream) x);
                } else {
                    cs.setBinaryStream(parameterIndex, (InputStream) x, length);
                }
                break;
            case "BLOB":
                cs.setBlob(parameterIndex, (Blob) x);
                break;
            case "CLOB":
                cs.setClob(parameterIndex, (Clob) x);
                break;
            case "DATE":
                cs.setDate(parameterIndex, (Date) x);
                break;
            case "OBJECT":
                cs.setObject(parameterIndex, x);
                break;
            default:
                getLogger().log(Level.SEVERE, "Unknown SQL type name: {0} in {1}", new Object[]{sqlTypeName, getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
        }

        //cs.setClob(int parameterIndex, Reader reader)
        //cs.setClob(int parameterIndex, Reader reader, long length)
        // if (x instanceof InputStream": cs.setAsciiStream(parameterIndex, InputStream x, long length); } else
        //cs.setBlob(int parameterIndex, InputStream inputStream)
        //cs.setBlob(int parameterIndex, InputStream inputStream, long length)
        //cs.setCharacterStream(int parameterIndex, Reader reader)
        //cs.setCharacterStream(int parameterIndex, Reader reader, int length)
        //cs.setCharacterStream(int parameterIndex, Reader reader, long length)
        // if (x instanceof InputStream) { cs.setBinaryStream(parameterIndex, InputStream x, long length); } else          
        //if (x instanceof Date) { cs.setDate(parameterIndex, Date x, Calendar cal); } else
        //cs.setNCharacterStream(int parameterIndex, Reader value)
        //cs.setNCharacterStream(int parameterIndex, Reader value, long length)
        //cs.setNClob(int parameterIndex, NClob value)
        //cs.setNClob(int parameterIndex, Reader reader)
        //cs.setNClob(int parameterIndex, Reader reader, long length)
        //cs.setNString(int parameterIndex, String value)     
//if (x instanceof Object) { cs.setObject(parameterIndex, Object x, targetSqlType); } else
//if (x instanceof Object) { cs.setObject(parameterIndex, Object x, targetSqlType, scaleOrLength); } else
//if (x instanceof Object) { cs.setObject(parameterIndex, Object x, SQLType targetSqlType); } else
//if (x instanceof Object) { cs.setObject(parameterIndex, Object x, SQLType targetSqlType, scaleOrLength); } else
//if (x instanceof Ref) { cs.setRef(parameterIndex, Ref x); }
    }

//    private void setInArray(CallableStatement cs, int index, Array inParameter) throws SQLException {
//        cs.setArray(index, inParameter);
//    }
    private void setInNull(CallableStatement cs, int index, String typename, String typeDescription) throws SQLException {
        int sqlType = typenameToSQLTypeInt(typename);
        if (typeDescription.trim().equals("null")) {
            cs.setNull(index, sqlType);
        } else {
            cs.setNull(index, sqlType, typeDescription);
        }
    }

    private void setOut(CallableStatement cs, int index, String typename, String typeDescription, Integer scale) throws SQLException {
        // 1.8 // SQLType sqlType = java.sql.JDBCType.valueOf(typename);
        int sqlType = typenameToSQLTypeInt(typename);
        if (typeDescription != null) {
            cs.registerOutParameter(index, sqlType, typeDescription);
        } else if (scale != null) {
            cs.registerOutParameter(index, sqlType, scale);
        } else {
            cs.registerOutParameter(index, sqlType);
        }
    }

    private Integer typenameToSQLTypeInt(String typename) {
        Integer sqlTypeInt = null;
        /* Not sure this is correct */
//        SQLType sQLType = java.sql.JDBCType.valueOf(typename); // java 1.8 !!
//        sqlTypeInt = sQLType.getVendorTypeNumber();
        /* Not sure the above is correct */
        switch (typename.toUpperCase()) {
            case "ARRAY":
                sqlTypeInt = java.sql.Types.ARRAY;
                break;
            case "BIGINT":
                sqlTypeInt = java.sql.Types.BIGINT;
                break;
            case "BINARY":
                sqlTypeInt = java.sql.Types.BINARY;
                break;
            case "BIT":
                sqlTypeInt = java.sql.Types.BIT;
                break;
            case "BLOB":
                sqlTypeInt = java.sql.Types.BLOB;
                break;
            case "BOOLEAN":
                sqlTypeInt = java.sql.Types.BOOLEAN;
                break;
            case "CHAR":
                sqlTypeInt = java.sql.Types.CHAR;
                break;
            case "CLOB":
                sqlTypeInt = java.sql.Types.CLOB;
                break;
            case "DATALINK":
                sqlTypeInt = java.sql.Types.DATALINK;
                break;
            case "DATE":
                sqlTypeInt = java.sql.Types.DATE;
                break;
            case "DECIMAL":
                sqlTypeInt = java.sql.Types.DECIMAL;
                break;
            case "DISTINCT":
                sqlTypeInt = java.sql.Types.DISTINCT;
                break;
            case "DOUBLE":
                sqlTypeInt = java.sql.Types.DOUBLE;
                break;
            case "FLOAT":
                sqlTypeInt = java.sql.Types.FLOAT;
                break;
            case "INTEGER":
                sqlTypeInt = java.sql.Types.INTEGER;
                break;
            case "JAVA_OBJECT":
                sqlTypeInt = java.sql.Types.JAVA_OBJECT;
                break;
            case "LONGNVARCHAR":
                sqlTypeInt = java.sql.Types.LONGNVARCHAR;
                break;
            case "LONGVARBINARY":
                sqlTypeInt = java.sql.Types.LONGVARBINARY;
                break;
            case "LONGVARCHAR":
                sqlTypeInt = java.sql.Types.LONGVARCHAR;
                break;
            case "NCHAR":
                sqlTypeInt = java.sql.Types.NCHAR;
                break;
            case "NCLOB":
                sqlTypeInt = java.sql.Types.NCLOB;
                break;
            case "NULL":
                sqlTypeInt = java.sql.Types.NULL;
                break;
            case "NUMERIC":
                sqlTypeInt = java.sql.Types.NUMERIC;
                break;
            case "NVARCHAR":
                sqlTypeInt = java.sql.Types.NVARCHAR;
                break;
            case "OTHER":
                sqlTypeInt = java.sql.Types.OTHER;
                break;
            case "REAL":
                sqlTypeInt = java.sql.Types.REAL;
                break;
            case "REF":
                sqlTypeInt = java.sql.Types.REF;
                break;
//            case "REF_CURSOR": // 1.8
//                sqlTypeInt = java.sql.Types.REF_CURSOR;
//                break;
            case "ROWID":
                sqlTypeInt = java.sql.Types.ROWID;
                break;
            case "SMALLINT":
                sqlTypeInt = java.sql.Types.SMALLINT;
                break;
            case "SQLXML":
                sqlTypeInt = java.sql.Types.SQLXML;
                break;
            case "STRUCT":
                sqlTypeInt = java.sql.Types.STRUCT;
                break;
            case "TIME":
                sqlTypeInt = java.sql.Types.TIME;
                break;
//            case "TIME_WITH_TIMEZONE": // 1.8
//                sqlTypeInt = java.sql.Types.TIME_WITH_TIMEZONE;
//                break;
            case "TIMESTAMP":
                sqlTypeInt = java.sql.Types.TIMESTAMP;
                break;
//            case "TIMESTAMP_WITH_TIMEZONE": // 1.8
//                sqlTypeInt = java.sql.Types.TIMESTAMP_WITH_TIMEZONE;
//                break;
            case "TINYINT":
                sqlTypeInt = java.sql.Types.TINYINT;
                break;
            case "VARBINARY":
                sqlTypeInt = java.sql.Types.VARBINARY;
                break;
            case "VARCHAR":
                sqlTypeInt = java.sql.Types.VARCHAR;
                break;

        }
        return sqlTypeInt;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doCs(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
