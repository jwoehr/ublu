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
import ublu.util.ByteArraySplitter;
import ublu.util.Generics.ByteArrayList;
import ublu.util.Generics.ByteArrayListArrayList;
import ublu.util.Generics.IndexList;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Operations on a source ResultSet and a destination ResultSet, typically
 * inserts as updates, since it's easy in JDBC to insert from one result into
 * another on the "insert row" of the destination result set.
 *
 * @author jwoehr
 */
public class ResultSetHelper {

    // only used in debugging
    // all  methods throw to their callers
    private static Logger getLogger() {
        return Ublu.getMainInterpreter().getLogger();
    }

    /**
     * The kinds of text conversion we can do
     */
    public enum CONVERSION {

        /**
         * text conversion not needed
         */
        NONE,
        /**
         * text conversion from EBCDIC to ASCII
         */
        TOASCII,
        /**
         * text conversion from ASCII to EBCDIC
         */
        TOEBCDIC
    }
    /**
     * The source result set from whence cometh the data
     */
    protected ResultSet srcResultSet;
    /**
     * The target result set whither the data goeth
     */
    protected ResultSet destResultSet;
    /**
     * List of indexes of columns needing character set conversion
     */
    protected IndexList indexList;
    /**
     * Insert/update code tests this var to determine if char conversion is
     * requested.
     */
    protected CONVERSION conversion;

    /**
     * Get the source result set we stored at ctor time.
     *
     * @return the source result set we stored at ctor time
     */
    public ResultSet getSrcResultSet() {
        return srcResultSet;
    }

    /**
     * Set the source result set we store at ctor time.
     *
     * @param srcResultSet the source result set we store at ctor time
     */
    public final void setSrcResultSet(ResultSet srcResultSet) {
        this.srcResultSet = srcResultSet;
    }

    /**
     * Get the destination result set we stored at ctor time.
     *
     * @return the destination result set we stored at ctor time
     */
    public ResultSet getDestResultSet() {
        return destResultSet;
    }

    /**
     * Set the destination result set we stored at ctor time.
     *
     * @param destResultSet set the destination result set we store at ctor
     * time.
     */
    public final void setDestResultSet(ResultSet destResultSet) {
        this.destResultSet = destResultSet;
    }

    /**
     * Get the list of one's-based indices for the columns which need char
     * conversion.
     *
     * @return the list of one's-based indices for the columns which need char
     * conversion
     */
    public IndexList getIndexList() {
        return indexList;
    }

    /**
     * Set the list of one's-based indices for the columns which need char
     * conversion.
     *
     * @param indexList the list of one's-based indices for the columns which
     * need char conversion
     */
    protected final void setIndexList(IndexList indexList) {
        this.indexList = indexList;
    }

    /**
     * Get the direction of char conversion requested.
     *
     * @return the direction of char conversion requested
     */
    public CONVERSION getConversion() {
        return conversion;
    }

    /**
     * Set the direction of char conversion requested.
     *
     * @param conversion the direction of char conversion requested
     */
    protected final void setConversion(CONVERSION conversion) {
        this.conversion = conversion;
    }

    /**
     * Default ctor, not used
     */
    protected ResultSetHelper() {
    }

    /**
     * Instance with the source and dest result sets instanced.
     *
     * @param srcResultSet
     * @param destResultSet
     * @throws SQLException
     */
    public ResultSetHelper(ResultSet srcResultSet, ResultSet destResultSet) throws SQLException {
        setSrcResultSet(srcResultSet);
        setDestResultSet(destResultSet);
    }

    /**
     * Instance with the source and dest result sets instanced and character
     * conversion set up.
     *
     * @param srcResultSet
     * @param destResultSet
     * @param indexList list of column indexes needing conversion
     * @param conversion direction of the conversion
     * @throws SQLException
     */
    public ResultSetHelper(ResultSet srcResultSet, ResultSet destResultSet, IndexList indexList, CONVERSION conversion) throws SQLException {
        setSrcResultSet(srcResultSet);
        setDestResultSet(destResultSet);
        setIndexList(indexList);
        setConversion(conversion);
    }

    /**
     * Is character conversion required on certain columns?
     *
     * @return true if conversion required
     */
    public boolean isConverting() {
        return conversion != null && indexList != null;
    }

    /**
     * Is the current source column the subject of char conversion?
     *
     * @param index true if current source column is the subject of char
     * conversion
     * @return
     */
    public boolean isConverting(int index) {
        boolean result = false;
        if (isConverting()) {
            result = indexList.contains(index);
        }
        return result;
    }

    /**
     * Perform character conversion on input.
     *
     * @param input String to convert
     * @return converted string
     * @throws UnsupportedEncodingException
     */
    public String doConversion(byte[] input) throws UnsupportedEncodingException {
        String result = null;
        switch (getConversion()) {
            case NONE:
                result = new String(input);
                break;
            case TOASCII:
                result = new String(input, Db.cpEBCDIC);
                break;
            case TOEBCDIC:
                throw new UnsupportedOperationException("ASCII -> EBCDIC conversion not supported yet.");
        }
        return result;
    }

    /**
     * Update a column in this.destResultSet's current row from
     * this.srcResultSet's current row
     *
     * @param index
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    public void updateColumn(int index) throws SQLException, UnsupportedEncodingException {
        String s; // temporary storage
        byte[] srcBytes; // used for conversions
        BigDecimal bigDecimal; // used for numeric conversions
        int type = getSrcResultSet().getMetaData().getColumnType(index);
        switch (type) {
            case java.sql.Types.ARRAY:
                getDestResultSet().updateArray(index, getSrcResultSet().getArray(index));
                break;
            case java.sql.Types.BIGINT:
                getDestResultSet().updateDouble(index, new Double(getSrcResultSet().getDouble(index)));
                break;
            case java.sql.Types.BINARY: // need NULL protection
                srcBytes = getSrcResultSet().getBytes(index);
                if (!getSrcResultSet().wasNull()) {
                    if (isConverting(index)) {
                        srcBytes = doConversion(srcBytes).getBytes();
                    }
                }
                getDestResultSet().updateBytes(index, srcBytes);
                break;
            case java.sql.Types.BIT:
                getDestResultSet().updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.BLOB:
                getDestResultSet().updateBlob(index, getSrcResultSet().getBlob(index));
                break;
            case java.sql.Types.BOOLEAN:
                getDestResultSet().updateBoolean(index, getSrcResultSet().getBoolean(index));
                break;
            case java.sql.Types.CHAR:
                s = getSrcResultSet().getString(index).replace('\000', '\100'); // NULL > ebcdic SPACE 0x40
                if (!getSrcResultSet().wasNull()) {
                    srcBytes = s.getBytes();
                    if (isConverting(index)) {
                        srcBytes = doConversion(srcBytes).getBytes();
                        s = new String(srcBytes);
                    }
                }
                getDestResultSet().updateString(index, s);
                break;
            case java.sql.Types.CLOB:
                Clob myClob = getSrcResultSet().getClob(index);
                if (myClob != null) { // DB400 is careless about nulls.
                    getDestResultSet().updateString(index, myClob.toString()); // updateString because Postgres doesn't have updateClob()
                }
                break;
            case java.sql.Types.DATALINK:
                getDestResultSet().updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.DATE: // need NULL protection? No conversion calls, guess not.
                getDestResultSet().updateDate(index, getSrcResultSet().getDate(index));
                break;
            case java.sql.Types.DECIMAL: // need NULL protection? No conversion calls, guess not.
                try {
                    bigDecimal = getSrcResultSet().getBigDecimal(index);
                } catch (NumberFormatException ex) { // ignore exception
                    bigDecimal = new BigDecimal(0);
                }
                getDestResultSet().updateBigDecimal(index, bigDecimal);
                break;
            case java.sql.Types.DISTINCT:
                getDestResultSet().updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.DOUBLE:
                getDestResultSet().updateDouble(index, getSrcResultSet().getDouble(index));
                break;
            case java.sql.Types.FLOAT:
                getDestResultSet().updateFloat(index, getSrcResultSet().getFloat(index));
                break;
            case java.sql.Types.INTEGER:
                getDestResultSet().updateInt(index, getSrcResultSet().getInt(index));
                break;
            case java.sql.Types.JAVA_OBJECT:
                getDestResultSet().updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.LONGNVARCHAR:
                getDestResultSet().updateNCharacterStream(index, getSrcResultSet().getNCharacterStream(index));
                break;
            case java.sql.Types.LONGVARBINARY: // need NULL protection? No conversion calls, guess not.
                getDestResultSet().updateBinaryStream(index, getSrcResultSet().getBinaryStream(index));
                break;
            case java.sql.Types.LONGVARCHAR:
                getDestResultSet().updateCharacterStream(index, getSrcResultSet().getCharacterStream(index));
                break;
            case java.sql.Types.NCHAR:
                getDestResultSet().updateNCharacterStream(index, getSrcResultSet().getNCharacterStream(index));
                break;
            case java.sql.Types.NCLOB:
                getDestResultSet().updateNClob(index, getSrcResultSet().getNClob(index));
                break;
            case java.sql.Types.NULL:
                getDestResultSet().updateNull(index);
                break;
            case java.sql.Types.NUMERIC:
                try {
                    bigDecimal = getSrcResultSet().getBigDecimal(index);
                } catch (NumberFormatException ex) {
                    bigDecimal = new BigDecimal(0);
                }
                getDestResultSet().updateBigDecimal(index, bigDecimal);
                break;
            case java.sql.Types.NVARCHAR:
                getDestResultSet().updateNCharacterStream(index, getSrcResultSet().getNCharacterStream(index));
                break;
            case java.sql.Types.OTHER:
                getDestResultSet().updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.REAL:
                getDestResultSet().updateFloat(index, getSrcResultSet().getFloat(index));
                break;
            case java.sql.Types.REF:
                getDestResultSet().updateRef(index, getSrcResultSet().getRef(index));
                break;
            case java.sql.Types.ROWID:
                getDestResultSet().updateRowId(index, getSrcResultSet().getRowId(index));
                break;
            case java.sql.Types.SMALLINT:
                getDestResultSet().updateShort(index, getSrcResultSet().getShort(index));
                break;
            case java.sql.Types.SQLXML:
                getDestResultSet().updateSQLXML(index, getSrcResultSet().getSQLXML(index));
                break;
            case java.sql.Types.STRUCT:
                getDestResultSet().updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.TIME:
                getDestResultSet().updateTime(index, getSrcResultSet().getTime(index));
                break;
            case java.sql.Types.TIMESTAMP: // need NULL protection
                getDestResultSet().updateTimestamp(index, getSrcResultSet().getTimestamp(index));
                break;
            case java.sql.Types.TINYINT:
                getDestResultSet().updateByte(index, getSrcResultSet().getByte(index));
                break;
            case java.sql.Types.VARBINARY: // need NULL protection
                getDestResultSet().updateBytes(index, getSrcResultSet().getBytes(index));
                break;
            case java.sql.Types.VARCHAR:
                s = getSrcResultSet().getString(index);
                if (!getSrcResultSet().wasNull()) {
                    srcBytes = s.getBytes();
                    if (isConverting(index)) {
                        srcBytes = doConversion(srcBytes).getBytes();
                        s = new String(srcBytes);
                    }
                }
                getDestResultSet().updateString(index, s);
                break;
        }
    }

    /**
     * Update all columns in the dest row from the source row.
     *
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    public void updateColumns() throws SQLException, UnsupportedEncodingException {
        ResultSetMetaData rsmd = getSrcResultSet().getMetaData();
        for (int i = 1 /*jdbc one-based */; i <= rsmd.getColumnCount(); i++) {
            // /* Debug */ getLogger().log(Level.INFO, "Updating column {0}\n", i);
            updateColumn(i);
        }
    }

    /**
     * Update and insert the row.
     *
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    public void updateInsertRow() throws SQLException, UnsupportedEncodingException {
        getDestResultSet().moveToInsertRow();
        // /* Debug */ getLogger().log(Level.INFO, "Update-inserting row {0}", getDestResultSet().getRow());
        updateColumns();
        getDestResultSet().insertRow();
    }

    /**
     * Update a table via a inserts to the destination result set from the
     * source result.
     *
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    public void updateInsertingTable() throws SQLException, UnsupportedEncodingException {
        getSrcResultSet().beforeFirst();
        while (getSrcResultSet().next()) {
            updateInsertRow();
        }
    }

    /**
     * Update a column in destination result set's current row from
     * this.resultSet's current row, splitting fields if indicated and doing
     * character conversion if requested
     *
     * @param index
     * @param destIndex the one's-based column index in the destination result
     * set, which in this splitting form of the update of course gets offset by
     * any column splitting
     * @param splitting true if we are splitting the current source column
     * @param splitWidths the split widths if we are splitting the current
     * source column per the <tt>splitting</tt> parameter, ignored otherwise
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    public void updateSplittingColumn(int index, int destIndex, boolean splitting, int[] splitWidths) throws SQLException, UnsupportedEncodingException {
        String s; // temp storage
        byte[] srcBytes; // used for conversions
        int type = getSrcResultSet().getMetaData().getColumnType(index);
        switch (type) {
            case java.sql.Types.ARRAY:
                getDestResultSet().updateArray(index, getSrcResultSet().getArray(index));
                break;
            case java.sql.Types.BIGINT:
                getDestResultSet().updateDouble(index, new Double(getSrcResultSet().getDouble(index)));
                break;
            case java.sql.Types.BINARY:
                srcBytes = getSrcResultSet().getBytes(index);
                if (!getSrcResultSet().wasNull()) {
                    if (isConverting(index)) {
                        srcBytes = doConversion(srcBytes).getBytes();
                    }
                    if (splitting) {
                        ByteArraySplitter bas = new ByteArraySplitter(srcBytes);
                        ByteArrayListArrayList balal = bas.split(splitWidths);
                        for (ByteArrayList bal : balal) {
                            getDestResultSet().updateBytes(destIndex++, bal.byteArray());
                        }
                    } else {
                        getDestResultSet().updateBytes(index, srcBytes);
                    }
                } else { // Data was SQL NULL
                    if (splitting) {
                        for (int i : splitWidths) {
                            getDestResultSet().updateBytes(destIndex++, null);
                        }
                    } else {
                        getDestResultSet().updateBytes(index, srcBytes);
                    }
                }
                break;
            case java.sql.Types.BIT:
                getDestResultSet()
                        .updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.BLOB:
                getDestResultSet()
                        .updateBlob(index, getSrcResultSet().getBlob(index));
                break;
            case java.sql.Types.BOOLEAN:
                getDestResultSet()
                        .updateBoolean(index, getSrcResultSet().getBoolean(index));
                break;
            case java.sql.Types.CHAR:
                s = getSrcResultSet().getString(index);
                if (!getSrcResultSet()
                        .wasNull()) {
                    srcBytes = s.getBytes();
                    if (isConverting(index)) {
                        srcBytes = doConversion(srcBytes).getBytes();
                    }
                    if (splitting) {
                        ByteArraySplitter bas = new ByteArraySplitter(srcBytes);
                        ByteArrayListArrayList balal = bas.split(splitWidths);
                        for (ByteArrayList bal : balal) {
                            getDestResultSet().updateString(destIndex++, new String(bal.byteArray()));
                        }
                    } else {
                        getDestResultSet().updateString(index, new String(srcBytes));
                    }
                } else {
                    if (splitting) {
                        for (int i : splitWidths) {
                            getDestResultSet().updateString(destIndex++, null);
                        }
                    } else {
                        getDestResultSet().updateString(index, null);
                    }
                }
                break;
            case java.sql.Types.CLOB:
                getDestResultSet()
                        .updateClob(index, getSrcResultSet().getClob(index));
                break;
            case java.sql.Types.DATALINK:
                getDestResultSet()
                        .updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.DATE:
                getDestResultSet()
                        .updateDate(index, getSrcResultSet().getDate(index));
                break;
            case java.sql.Types.DECIMAL:
                getDestResultSet()
                        .updateBigDecimal(index, getSrcResultSet().getBigDecimal(index));
                break;
            case java.sql.Types.DISTINCT:
                getDestResultSet()
                        .updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.DOUBLE:
                getDestResultSet()
                        .updateDouble(index, getSrcResultSet().getDouble(index));
                break;
            case java.sql.Types.FLOAT:
                getDestResultSet()
                        .updateFloat(index, getSrcResultSet().getFloat(index));
                break;
            case java.sql.Types.INTEGER:
                getDestResultSet()
                        .updateInt(index, getSrcResultSet().getInt(index));
                break;
            case java.sql.Types.JAVA_OBJECT:
                getDestResultSet()
                        .updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.LONGNVARCHAR:
                getDestResultSet()
                        .updateNCharacterStream(index, getSrcResultSet().getNCharacterStream(index));
                break;
            case java.sql.Types.LONGVARBINARY: // need NULL protection
                getDestResultSet()
                        .updateBinaryStream(index, getSrcResultSet().getBinaryStream(index));
                break;
            case java.sql.Types.LONGVARCHAR:
                getDestResultSet()
                        .updateCharacterStream(index, getSrcResultSet().getCharacterStream(index));
                break;
            case java.sql.Types.NCHAR:
                getDestResultSet()
                        .updateNCharacterStream(index, getSrcResultSet().getNCharacterStream(index));
                break;
            case java.sql.Types.NCLOB:
                getDestResultSet()
                        .updateNClob(index, getSrcResultSet().getNClob(index));
                break;
            case java.sql.Types.NULL:
                getDestResultSet()
                        .updateNull(index);
                break;
            case java.sql.Types.NUMERIC:
                getDestResultSet()
                        .updateInt(index, getSrcResultSet().getInt(index));
                break;
            case java.sql.Types.NVARCHAR:
                getDestResultSet()
                        .updateNCharacterStream(index, getSrcResultSet().getNCharacterStream(index));
                break;
            case java.sql.Types.OTHER:
                getDestResultSet()
                        .updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.REAL:
                getDestResultSet()
                        .updateFloat(index, getSrcResultSet().getFloat(index));
                break;
            case java.sql.Types.REF:
                getDestResultSet()
                        .updateRef(index, getSrcResultSet().getRef(index));
                break;
            case java.sql.Types.ROWID:
                getDestResultSet()
                        .updateRowId(index, getSrcResultSet().getRowId(index));
                break;
            case java.sql.Types.SMALLINT:
                getDestResultSet()
                        .updateShort(index, getSrcResultSet().getShort(index));
                break;
            case java.sql.Types.SQLXML:
                getDestResultSet()
                        .updateSQLXML(index, getSrcResultSet().getSQLXML(index));
                break;
            case java.sql.Types.STRUCT:
                getDestResultSet()
                        .updateObject(index, getSrcResultSet().getObject(index));
                break;
            case java.sql.Types.TIME:
                getDestResultSet()
                        .updateTime(index, getSrcResultSet().getTime(index));
                break;
            case java.sql.Types.TIMESTAMP:
                getDestResultSet()
                        .updateTimestamp(index, getSrcResultSet().getTimestamp(index));
                break;
            case java.sql.Types.TINYINT:
                getDestResultSet()
                        .updateByte(index, getSrcResultSet().getByte(index));
                break;
            case java.sql.Types.VARBINARY: // need NULL protection
                getDestResultSet()
                        .updateBinaryStream(index, getSrcResultSet().getBinaryStream(index));
                break;
            case java.sql.Types.VARCHAR:
                s = getSrcResultSet().getString(index);
                if (!getSrcResultSet()
                        .wasNull()) {
                    srcBytes = s.getBytes();
                    if (isConverting(index)) {
                        srcBytes = doConversion(srcBytes).getBytes();
                    }
                    if (splitting) {
                        ByteArraySplitter bas = new ByteArraySplitter(srcBytes);
                        ByteArrayListArrayList balal = bas.split(splitWidths);
                        for (ByteArrayList bal : balal) {
                            getDestResultSet().updateString(destIndex++, new String(bal.byteArray()));
                        }
                    } else {
                        getDestResultSet().updateString(index, new String(srcBytes));
                    }
                } else {
                    if (splitting) {
                        for (int i : splitWidths) {
                            getDestResultSet().updateString(destIndex++, null);
                        }
                    } else {
                        getDestResultSet().updateString(index, null);
                    }
                }
                break;
        }
    }

    /**
     * Update a row taking specifications how to split one column into a new
     * multiple column structure in the target result set.
     *
     * @param columnToSplit which column will be split
     * @param splitWidths an array of split widths for the data
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    public void updateSplittingColumns(int columnToSplit, int[] splitWidths) throws SQLException, UnsupportedEncodingException {
        ResultSetMetaData rsmd = getSrcResultSet().getMetaData();
        int j = 1; // the destination column index
        for (int i = 1 /*jdbc one-based */; i <= rsmd.getColumnCount(); i++) {
            // /* Debug */ getLogger().log(Level.INFO, "Updating splitting column {0}\n", i);
            updateSplittingColumn(i, j, i == columnToSplit, splitWidths);
            // If we just did the split column, bump the dest column index
            // by the number of columns into which the source column is split.
            // Otherwise, just bump the dest column index by the normal 1.
            j += i == columnToSplit ? splitWidths.length : 1;
        }
    }

    /**
     * Move to insert row in dest result set, formulate the update from the
     * source result set splitting the required column and insert.
     *
     * @param columnToSplit the one's based index of the column to split
     * @param splitWidths array of split widths into which to split the single
     * column we have selected for splitting.
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    public void updateSplittingInsertRow(int columnToSplit, int[] splitWidths) throws SQLException, UnsupportedEncodingException {
        getDestResultSet().moveToInsertRow();
        // /* Debug */ getLogger().log(Level.INFO, "Update-inserting row {0}", getDestResultSet().getRow());
        updateSplittingColumns(columnToSplit, splitWidths);
        getDestResultSet().insertRow();
    }

    /**
     * Update a table splitting one column's bytes per split specifications. All
     * other columns are updated normally.
     *
     * @param columnToSplit one's based index of the column to split
     * @param splitWidths array of split widths into which to split the single
     * column we have selected for splitting.
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    public void updateSplittingInsertingTable(int columnToSplit, int[] splitWidths) throws SQLException, UnsupportedEncodingException {
        getSrcResultSet().beforeFirst();
        while (getSrcResultSet().next()) {
            updateSplittingInsertRow(columnToSplit, splitWidths);
        }
    }
}
