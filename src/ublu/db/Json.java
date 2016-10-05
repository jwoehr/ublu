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
package ublu.db;

import ublu.util.Generics.ColumnNameList;
import ublu.util.Generics.StringArrayList;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used for extracting comma-separated values from result sets
 *
 * @author jwoehr
 */
public class Json extends DbHelper {

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
     * Construct with result set, meta data, and source db instanced
     *
     * @param dB
     * @param resultSet
     * @param resultSetMetaData
     * @throws SQLException
     */
    protected Json(Db dB, ResultSet resultSet, ResultSetMetaData resultSetMetaData) throws SQLException {
        super(dB, resultSet, resultSetMetaData);
    }

    /**
     * Construct with result set, meta data, source db and table instanced
     *
     * @param dB
     * @param resultSet
     * @param resultSetMetaData
     * @param tableName
     * @throws SQLException
     */
    public Json(Db dB, ResultSet resultSet, ResultSetMetaData resultSetMetaData, String tableName) throws SQLException {
        this(dB, resultSet, resultSetMetaData);
        setTableName(tableName);
    }

    /**
     * Convert a table to JSON. Assumes a valid connection exists already.
     *
     * @return the table as CSV
     * @throws SQLException
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws org.json.JSONException
     */
    public JSONObject tableJSON() throws SQLException, UnsupportedEncodingException, IOException, JSONException {
        JSONObject jSONObject = new JSONObject();
        ColumnNameList cnl = new ColumnNameList();
        for (int i = 1; i <= getResultSetMetaData().getColumnCount(); i++) {
            cnl.add(getResultSetMetaData().getColumnName(i));
        }
        setColumnNameList(cnl);
        if (getColumnNameList() != null) {
            jSONObject.put("column_names", new JSONArray(cnl));
        }
        JSONArray jsonTempArray = new JSONArray();
        for (int i = 1; i <= getResultSetMetaData().getColumnCount(); i++) {
            jsonTempArray.put(getResultSetMetaData().getColumnTypeName(i));
        }
        jSONObject.put("column_typenames", jsonTempArray);
        jsonTempArray = new JSONArray();

        for (int i = 1; i <= getResultSetMetaData().getColumnCount(); i++) {
            jsonTempArray.put(getResultSetMetaData().getColumnType(i));
        }
        jSONObject.put("column_jdbc_types", jsonTempArray);
        int index = 0;
        JSONObject jsonRowsObject = new JSONObject();
        while (getResultSet().next()) {
            StringArrayList rowArrayList = rowToStringArrayList(getColumnNameList());
            jsonTempArray = new JSONArray();
            for (String datum : rowArrayList) {
                jsonTempArray.put(datum);
            }
            jsonRowsObject.put(Integer.toString(index++), jsonTempArray);
        }
        jSONObject.put("rows", jsonRowsObject);
        return jSONObject;
    }
}
