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

import ublu.Ublu;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import ublu.util.Generics.StringArrayList;
import java.util.logging.Logger;

/**
 * Formats result sets into human-readable String form.
 *
 * @author jwoehr
 */
public class ResultSetFormatter extends DbHelper {

    // only used in debugging
    // all methods throw to their callers
    private static Logger getLogger() {
        return Ublu.getMainInterpreter().getLogger();
    }
    /**
     * name of charset we are converting from
     */
    protected String charsetName;

    /**
     * Return name of charset we are converting from
     *
     * @return name of charset we are converting from
     */
    protected String getCharsetName() {
        return charsetName;
    }

    /**
     * Set name of charset we are converting from
     *
     * @param charsetName name of charset we are converting from
     */
    protected final void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }
    /**
     * Count of columns in result set
     */
    protected int columnCount;
    /**
     * List of column labels
     */
    protected StringArrayList columnLabels;
    /**
     * List of column display sizes
     */
    protected int[] columnDisplaySizes;

    /**
     * List of table names
     */
    /*protected StringArrayList tableNames;*/
    /**
     * Set the count of columns
     *
     * @return the count of columns
     */
    public int getColumnCount() {
        return columnCount;
    }

    /**
     * Get the count of columns
     *
     * @param columnCount the count of columns
     */
    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    /**
     * Get list of column labels
     *
     * @return list of column labels
     */
    public StringArrayList getColumnLabels() {
        return columnLabels;
    }

    /**
     * Set list of column labels
     *
     * @param columnLabels list of column labels
     */
    public void setColumnLabels(StringArrayList columnLabels) {
        this.columnLabels = columnLabels;
    }

    /**
     * Get array of column display sizes
     *
     * @return array of column display sizes
     */
    public int[] getColumnDisplaySizes() {
        return columnDisplaySizes;
    }

    /**
     * Set array of column display sizes
     *
     * @param columnDisplaySizes array of column display sizes
     */
    public void setColumnDisplaySizes(int[] columnDisplaySizes) {
        this.columnDisplaySizes = columnDisplaySizes;
    }

    /**
     *
     * @return
     */
    /*    public StringArrayList getTableNames() {
     * return tableNames;
     * }*/
    /**
     *
     * @param tableNames
     */
    /*    public void setTableNames(StringArrayList tableNames) {
     * this.tableNames = tableNames;
     * }*/
    /**
     * Instance with only charset member instanced
     */
    protected ResultSetFormatter() {
        super();
        setCharsetName("ASCII");
    }

    /**
     * Instance with a result set and the default character set.
     *
     * @param rs the result set we are going to operate upon
     * @throws SQLException
     */
    public ResultSetFormatter(ResultSet rs) throws SQLException {
        this();
        setResultSet(rs);
        populate();
    }

    /**
     * Instance with a result set and a specified character set.
     *
     * @param rs the result set we are going to operate upon
     * @param charsetName charset of the result set
     * @throws SQLException
     */
    public ResultSetFormatter(ResultSet rs, String charsetName) throws SQLException {
        this(rs);
        setCharsetName(charsetName);
        populate();
    }

    /**
     * Instance with a full set of member data.
     *
     * @param dB source database
     * @param resultSet the result set we are going to operate upon
     * @param resultSetMetaData the result set's metadata
     * @param charsetName charset of the result set
     * @throws SQLException
     */
    public ResultSetFormatter(Db dB, ResultSet resultSet, ResultSetMetaData resultSetMetaData, String charsetName) throws SQLException {
        super(dB, resultSet, resultSetMetaData);
        setCharsetName(charsetName);
        populate();
    }

    /**
     * Populate display factors (e.g., column labels and display sizes) by
     * examining the metadata.
     *
     * @throws SQLException
     */
    public final void populate() throws SQLException {
        if (getResultSetMetaData() == null) {
            setResultSetMetaData(getResultSet().getMetaData());
        }
        setColumnCount(getResultSetMetaData().getColumnCount());
        setColumnLabels(new StringArrayList());
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnLabels().add(getResultSetMetaData().getColumnLabel(i + 1));
        }
        setColumnDisplaySizes(new int[getColumnCount()]);
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnDisplaySizes()[i] = getResultSetMetaData().getColumnDisplaySize(i + 1);
        }
    }

    /**
     * Render the contents of the current column of the current row
     *
     * @param contents the data
     * @param width of the column
     * @return string representation
     */
    public String formatField(String contents, int width) {
        int effectiveWidth = Math.min(width, contents.length());
        StringBuilder sb = new StringBuilder(effectiveWidth);
        for (int i = 0; i < effectiveWidth; i++) {
            sb.append(" ");
        }
        sb.replace(0, effectiveWidth, contents);
        return sb.toString();
    }

    /**
     * String some headers for the column display.
     *
     * @return The headers as string.
     */
    public String formatHeaders() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getColumnCount(); i++) {
            sb.append(formatField(getColumnLabels().get(i), getColumnDisplaySizes()[i]));
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * String all the columns of the current row.
     *
     * @return a string representing all the columns of the current row
     * @throws SQLException
     * @throws IOException
     */
    public String formatRow() throws SQLException, IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getColumnCount(); i++) {
            byte[] bytes = getResultSet().getBytes(i + 1);
            // /* Debug */ getLogger().log(Level.INFO, "Bytes are {0} and charset name is {1}", new Object[]{bytes, getCharsetName()});
            sb.append(formatField(new String(bytes == null ? "null".getBytes() : bytes, getCharsetName()), getColumnDisplaySizes()[i]));
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Builds a string representing the whole visual display of the result set
     *
     * @return string representing the whole visual display of the result set
     * @throws SQLException
     * @throws IOException
     */
    public String format() throws SQLException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(formatHeaders()).append("\n");
        getResultSet().beforeFirst();
        // /* Debug */ getLogger().log(Level.INFO, "Currently at row {0} of the result set, about to enter row formatting loop", getResultSet().getRow());
        boolean hasNextRow = getResultSet().next();
        // /* Debug */ getLogger().log(Level.INFO, "Is there a next row? hasNextRow is {0} and getResultSet().getRow() returns {1}", new Object[]{hasNextRow, getResultSet().getRow()});
        while (hasNextRow) {
            // /* Debug */ getLogger().log(Level.INFO, "Currently at row {0} about to format a row of a result set", getResultSet().getRow());
            sb.append(formatRow()).append("\n");
            hasNextRow = getResultSet().next();
        }
        return sb.toString();
    }
}
