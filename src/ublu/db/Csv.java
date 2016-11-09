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

import ublu.util.Generics.ColumnNameList;
import ublu.util.Generics.StringArrayList;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Used for extracting comma-separated values from result sets
 *
 * @author jwoehr
 */
public class Csv extends DbHelper {

    /**
     * the table name
     */
    protected String tableName;

    /**
     * Get the table name
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Set the table name
     *
     * @param tableName the table name
     */
    protected final void setTableName(String tableName) {
        this.tableName = tableName;
    }
    /**
     * the string used as a column separator
     */
    protected String columnSeparator;

    /**
     * Return the string used as a column separator.
     *
     * @return the string used as a column separator
     */
    public String getColumnSeparator() {
        return columnSeparator;
    }

    /**
     * Set the string used as a column separator.
     *
     * @param columnSeparator the string used as a column separator
     */
    protected final void setColumnSeparator(String columnSeparator) {
        this.columnSeparator = columnSeparator;
    }

    /**
     * Construct with result set, meta data, and source db instanced
     *
     * @param dB
     * @param resultSet
     * @param resultSetMetaData
     * @throws SQLException
     */
    protected Csv(Db dB, ResultSet resultSet, ResultSetMetaData resultSetMetaData) throws SQLException {
        super(dB, resultSet, resultSetMetaData);
    }

    /**
     *
     * @param dB
     * @param resultSet
     * @param resultSetMetaData
     * @param tableName
     * @param columnSeparator
     * @throws SQLException
     */
    public Csv(Db dB, ResultSet resultSet, ResultSetMetaData resultSetMetaData, String tableName, String columnSeparator) throws SQLException {
        this(dB, resultSet, resultSetMetaData);
        setTableName(tableName);
        setColumnSeparator(columnSeparator);
    }

    /**
     * Convert a table to CSV with configurable separator. Assumes a valid
     * connection exists already.
     *
     * @return the table as CSV
     * @throws SQLException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public String tableCSV() throws SQLException, UnsupportedEncodingException, IOException {
        StringBuilder sb = new StringBuilder();
        String temp;
        setColumnNameList(generateColumnNameList());
        if (getColumnNameList() != null) {
            for (String columnName : getColumnNameList()) {
                sb.append(columnName).append(getColumnSeparator());
            }
            temp = sb.substring(0, sb.lastIndexOf(columnSeparator));
            sb = new StringBuilder(temp);
            sb.append("\n");
            for (int i = 1; i <= getResultSetMetaData().getColumnCount(); i++) {
                sb.append(getResultSetMetaData().getColumnTypeName(i)).append(getColumnSeparator());
            }
            temp = sb.substring(0, sb.lastIndexOf(columnSeparator));
            sb = new StringBuilder(temp);
            sb.append("\n");
            for (int i = 1; i <= getResultSetMetaData().getColumnCount(); i++) {
                sb.append("jdbc type ").append(getResultSetMetaData().getColumnType(i)).append(getColumnSeparator());
            }
            temp = sb.substring(0, sb.lastIndexOf(columnSeparator));
            sb = new StringBuilder(temp);
            sb.append("\n");
            while (getResultSet().next()) {
                StringArrayList rowArrayList = rowToStringArrayList(getColumnNameList());
                for (String datum : rowArrayList) {
                    sb.append(datum).append(getColumnSeparator());
                }
                temp = sb.substring(0, sb.lastIndexOf(columnSeparator));
                sb = new StringBuilder(temp);
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
