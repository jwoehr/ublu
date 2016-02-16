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
package ublu.db;

import static ublu.db.Db.cpEBCDIC;
import static ublu.db.Db.fromEBCDIC;
import ublu.util.Generics;
import ublu.util.Generics.ByteArrayList;
import ublu.util.Generics.ColumnNameList;
import ublu.util.Generics.ColumnTypeList;
import ublu.util.Generics.ColumnTypeNameList;
import ublu.util.Generics.StringArrayList;
import ublu.util.Generics.TableNameList;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper around data commonly used by db subprograms with useful operations
 * Descendants are helper apps called via Db by CmdDb.
 *
 * @author jwoehr
 */
public class DbHelper {

    private static final Logger LOG = Logger.getLogger(DbHelper.class.getName());
    /**
     * The {@link Db} instance associated with this instance.
     */
    protected Db dB;
    /**
     * The database meta data associated with this instance.
     */
    protected DatabaseMetaData databaseMetaData;
    /**
     * The result set associated with this instance.
     */
    protected ResultSet resultSet;
    /**
     * The result set meta data associated with this instance.
     */
    protected ResultSetMetaData resultSetMetaData;

    /**
     * Get the {@link Db} instance associated with this instance.
     *
     * @return the {@link Db} instance associated with this instance
     */
    public Db getDb() {
        return dB;
    }

    /**
     * Set the {@link Db} instance associated with this instance.
     *
     * @param dB the {@link Db} instance associated with this instance
     */
    protected final void setdB(Db dB) {
        this.dB = dB;
    }

    /**
     * Get the database meta data associated with this instance.
     *
     * @return the database meta data associated with this instance
     */
    public DatabaseMetaData getDatabaseMetaData() {
        return databaseMetaData;
    }

    /**
     * Set the database meta data associated with this instance.
     *
     * @param databaseMetaData he database meta data associated with this
     * instance
     */
    protected final void setDatabaseMetaData(DatabaseMetaData databaseMetaData) {
        this.databaseMetaData = databaseMetaData;
    }

    /**
     * Get the result set associated with this instance.
     *
     * @return the result set associated with this instance
     */
    public ResultSet getResultSet() {
        return resultSet;
    }

    /**
     * Set the result set associated with this instance.
     *
     * @param resultSet the result set associated with this instance
     */
    protected final void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    /**
     * Get the result set meta data associated with this instance.
     *
     * @return the result set meta data associated with this instance
     */
    public ResultSetMetaData getResultSetMetaData() {
        return resultSetMetaData;
    }

    /**
     * Set the result set meta data associated with this instance.
     *
     * @param resultSetMetaData the result set meta data associated with this
     * instance
     */
    protected final void setResultSetMetaData(ResultSetMetaData resultSetMetaData) {
        this.resultSetMetaData = resultSetMetaData;
    }
    /**
     * Holds a result set of table info from the db
     */
    protected ResultSet tablesResultSet;
    /**
     * Holds a list of column type names
     */
    protected ColumnTypeNameList columnTypeNameList;
    /**
     * Holds list of column names
     */
    protected ColumnNameList columnNameList;

    /**
     * Get the tables info, if instanced, else null
     *
     * @return the tables info, if instanced, else null
     * @see fetchTablesResultSet
     */
    public ResultSet getTablesResultSet() {
        return tablesResultSet;
    }

    /**
     * Set the tables info
     *
     * @param tablesResultSet the tables info
     */
    protected final void setTablesResultSet(ResultSet tablesResultSet) {
        this.tablesResultSet = tablesResultSet;
    }

    /**
     * Get the list of column type names if instanced
     *
     * @return the list of column type names if instanced or null
     */
    public ColumnTypeNameList getColumnTypeNameList() {
        return columnTypeNameList;
    }

    /**
     * Set the list of column type names.
     *
     * @param columnTypeNameList list of column type names.
     * @see fetchColumnTypeNameListf
     */
    protected void setColumnTypeNameList(ColumnTypeNameList columnTypeNameList) {
        this.columnTypeNameList = columnTypeNameList;
    }

    /**
     * Get list of column names if instanced
     *
     * @return list of column names if instanced or null
     */
    public ColumnNameList getColumnNameList() {
        return columnNameList;
    }

    /**
     * Set list of column names
     *
     * @param columnNameList list of column names
     */
    protected void setColumnNameList(ColumnNameList columnNameList) {
        this.columnNameList = columnNameList;
    }

    /**
     * Instance simply, not used.
     */
    protected DbHelper() {
    }

    /**
     * Instance with important members set
     *
     * @param dB {@link Db} instance associated with this instance
     * @param resultSet result set instance associated with this instance
     * @param resultSetMetaData result set meta data instance associated with
     * this instance
     * @throws SQLException
     */
    public DbHelper(Db dB, ResultSet resultSet, ResultSetMetaData resultSetMetaData) throws SQLException {
        this.resultSet = resultSet;
        this.resultSetMetaData = resultSetMetaData;
        this.dB = dB;
        this.databaseMetaData = dB.getMetaData();
    }

    /**
     * Instance the result set of table info from the db
     *
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @param types
     * @throws SQLException
     * @see getTablesResultSet
     */
    public void fetchTablesResultSet(String catalog,
            String schemaPattern,
            String tableNamePattern,
            String[] types) throws SQLException {
        setTablesResultSet(getDb().getTables(catalog, schemaPattern, tableNamePattern, types));
    }

    /**
     * Get a TableNameList of the tables
     *
     * @param tablesRs a ResultSet of tables from the db
     * @return TableNameList of tables discovered in the database
     * @throws SQLException
     */
    protected TableNameList generateTableNameList(ResultSet tablesRs) throws SQLException {
        TableNameList tableNameList = null;
        if (tablesRs != null) {
            tableNameList = new TableNameList();
            while (tablesRs.next()) {
                String str = tablesRs.getString("TABLE_NAME");
                tableNameList.add(str);
            }
        }
        return tableNameList;
    }

    /**
     * Create a list of int jdbc types for the columns
     *
     * @return The list or null if no list could be built
     * @throws SQLException
     */
    protected ColumnTypeList generateColumnTypeList() throws SQLException {
        ColumnTypeList columnTypeList = new Generics.ColumnTypeList();
        for (int i = 1; i <= getResultSetMetaData().getColumnCount(); i++) {
            columnTypeList.add(getResultSetMetaData().getColumnType(i));
        }
        return columnTypeList.isEmpty() ? null : columnTypeList;
    }

    /**
     * Fetch the list of column type names from the database meta data and
     * instance the member returned by {@link getColumnTypeNameList}.
     *
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @param columnNamePattern
     * @return this
     * @throws SQLException
     */
    public DbHelper fetchColumnTypeNameList(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        ColumnTypeNameList list = new ColumnTypeNameList();
        DatabaseMetaData dmd = getDatabaseMetaData();
        if (dmd != null) {
            for (int i = 1; i <= getResultSetMetaData().getColumnCount(); i++) {
                String sb = String.valueOf(getResultSetMetaData().getColumnTypeName(i) + "(") + Integer.toString(getResultSetMetaData().getPrecision(i)) + ")";
                list.add(sb);
            }
        } else {
            list = null;
        }
        setColumnTypeNameList(list);
        return this;
    }

    /**
     * Get one column type name by one's-based index if the column type name
     * list was already instanced or null if not.
     *
     * @param index
     * @return column type name or null if the column type name list was not
     * instanced.
     * @throws SQLException
     * @see fetchColumnTypeNameList
     */
    public String getColumnTypeName(int index) throws SQLException {
        String result = null;
        ColumnTypeNameList c = getColumnTypeNameList();
        if (c != null) {
            result = c.get(index);
        }
        return result;
    }

    /**
     * Get the data in one column of a row and convert to a string.
     *
     * @param db
     * @param rs
     * @param index column index, one's-based
     * @param type jdbc int value for the data type
     * @return data in one column of a row and converted to a string
     * @throws UnsupportedEncodingException
     * @throws SQLException
     * @throws IOException
     * @bug not using separator char for fields it inserts
     */
    public static String getColumnDataAsString(Db db, ResultSet rs, int index, int type) throws UnsupportedEncodingException, SQLException, IOException {
        String result = null;
        switch (type) {
            case java.sql.Types.ARRAY:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.BIGINT:
                result = Double.toString(rs.getDouble(index));
                break;
            case java.sql.Types.BINARY:
                InputStream is = rs.getBinaryStream(index);
                int countRead;
                ByteArrayList byteArrayList = new ByteArrayList();
                byte[] buff = new byte[1024];
                countRead = is.read(buff);
                while (countRead > 0) {
                    for (int i = 0; i < countRead; i++) {
                        byteArrayList.add(buff[i]);
                    }
                    countRead = is.read(buff);
                }
                StringBuilder sb = new StringBuilder();
                buff = new byte[byteArrayList.size()];
                for (int i = 0; i < byteArrayList.size(); i++) {
                    byte b = byteArrayList.get(i);
                    sb.append(String.format("%02x ", b));
                    buff[i] = b;
                }
                if (db.getDbType() == Db.DBTYPE.AS400) {
                    sb.append(", ");
                    sb.append(fromEBCDIC(buff));
                    sb.append(" ");
                }
                result = sb.toString();
                break;
            case java.sql.Types.BIT:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.BLOB:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.BOOLEAN:
                result = Boolean.toString(rs.getBoolean(index));
                break;
            case java.sql.Types.CHAR:
                if (db.getDbType().equals(Db.DBTYPE.AS400)) {
                    byte[] data = rs.getBytes(index);
                    result = new String(data, cpEBCDIC);
                } else {
                    result = (rs.getString(index));
                }
                break;
            case java.sql.Types.CLOB:
                result = rs.getClob(index).toString();
                break;
            case java.sql.Types.DATALINK:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.DATE:
                result = rs.getDate(index).toString();
                break;
            case java.sql.Types.DECIMAL:
                result = rs.getString(index);
                break;
            case java.sql.Types.DISTINCT:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.DOUBLE:
                result = Double.toString(rs.getDouble(index));
                break;
            case java.sql.Types.FLOAT:
                result = Float.toString(rs.getFloat(index));
                break;
            case java.sql.Types.INTEGER:
                result = Integer.toString(rs.getInt(index));
                break;
            case java.sql.Types.JAVA_OBJECT:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.LONGNVARCHAR:
                result = rs.getNString(index);
                break;
            case java.sql.Types.LONGVARBINARY:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.LONGVARCHAR:
                result = rs.getString(index);
                break;
            case java.sql.Types.NCHAR:
                result = rs.getNString(index);
                break;
            case java.sql.Types.NCLOB:
                result = rs.getNClob(index).toString();
                break;
            case java.sql.Types.NULL:
                result = "";
                break;
            case java.sql.Types.NUMERIC:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.NVARCHAR:
                result = rs.getNString(index);
                break;
            case java.sql.Types.OTHER:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.REAL:
                result = Float.toString(rs.getFloat(index));
                break;
            case java.sql.Types.REF:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.ROWID:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.SMALLINT:
                result = Integer.toString(rs.getInt(index));
                break;
            case java.sql.Types.SQLXML:
                result = rs.getSQLXML(index).toString();
                break;
            case java.sql.Types.STRUCT:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.TIME:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.TIMESTAMP:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.TINYINT:
                result = Integer.toString(rs.getInt(index));
                break;
            case java.sql.Types.VARBINARY:
                result = rs.getObject(index).toString();
                break;
            case java.sql.Types.VARCHAR:
                result = rs.getString(index);
                break;
        }

        return result;
    }

    /**
     * Convert a row to a list of strings.
     *
     * @param columnNames
     * @return data from each column in a row converted to string
     * @throws SQLException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public StringArrayList rowToStringArrayList(ColumnNameList columnNames) throws SQLException, UnsupportedEncodingException, IOException {
        StringArrayList row = new StringArrayList();
        for (int i = 1; i <= getResultSetMetaData().getColumnCount(); i++) { // one's-based
            int type = getResultSetMetaData().getColumnType(i); // one's-based
            // String typeName = getResultSetMetaData().getColumnTypeName(i);
            row.add(getColumnDataAsString(getDb(), getResultSet(), i, type)); // one's-based
        }
        return row;
    }

    /**
     * Close the result set associated with this instance.
     *
     * @throws SQLException
     */
    public void closeResultSet() throws SQLException {
        if (getResultSet() != null) {
            getResultSet().close();
            setResultSet(null);
        }
    }

    /**
     * Close everything associated with this instance
     */
    public void close() {
        try {
            closeResultSet();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Exception closing DbHelper instance", ex);
        }
    }

    /**
     * Select * from a table on arbitrary db.
     *
     * <p> Making the return a ResultSetClosure is for use with a
     * {@link Putter}.</p>
     *
     * @see CmdDb
     * @param db
     * @param tablename
     * @return result set
     * @throws SQLException
     */
    public static ResultSetClosure selectStarFrom(Db db, String tablename) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append("\"");
        sql.append(tablename);
        sql.append("\"");
        Statement statement = db.getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = statement.executeQuery(sql.toString());
        return new ResultSetClosure(db, rs, statement);

    }

    /**
     * Factory to create a new DbHelper on a SELECT * FROM query
     *
     * @param db database instance
     * @param tablename table name to perform the automatic query upon
     * @return a new instance of DbHelper instanced with result set from the
     * automatic query
     * @throws SQLException
     */
    public static DbHelper newDbHelperStarFrom(Db db, String tablename) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append("\"");
        sql.append(tablename);
        sql.append("\"");
        Statement statement = db.getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = statement.executeQuery(sql.toString());
        return new DbHelper(db, rs, rs.getMetaData());
    }
}
