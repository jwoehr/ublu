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

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 *
 * Class to formulate CallableStatements on behalf of the Ublu interpreter.
 *
 * @author jax
 */
public class Statements {

    /**
     * Set array-of-obj input param
     *
     * @param c the db connection the statement
     * @param cs the extant statement
     * @param index param index
     * @param o the value
     * @param typeDescription string representing sql type of members of array
     * @return true on success
     * @throws SQLException
     */
    public static boolean setInArray(Connection c, CallableStatement cs, int index, String typeDescription, Object o) throws SQLException {
        boolean success = false;
        if (c != null && cs != null) {
            cs.setArray(index, c.createArrayOf(typeDescription, (Object[]) o));
        }
        return success;
    }

    /**
     * Set input param by class type getting type by reflection.
     *
     * @param cs the statement
     * @param index param index
     * @param o the value
     * @return true on success
     * @throws SQLException
     */
    public static boolean setIn(CallableStatement cs, int index, Object o) throws SQLException {
        boolean success = false;
        if (cs != null) {
            if (o instanceof BigDecimal) {
                cs.setBigDecimal(index, BigDecimal.class.cast(o));
            } else if (o instanceof String) {
                cs.setString(index, String.class.cast(o));
            } else if (o instanceof Boolean) {
                cs.setBoolean(index, Boolean.class.cast(o));
            } else if (o instanceof Byte) {
                cs.setByte(index, Byte.class.cast(o));
            } else if (o instanceof Short) {
                cs.setShort(index, Short.class.cast(o));
            } else if (o instanceof Integer) {
                cs.setInt(index, Integer.class.cast(o));
            } else if (o instanceof Long) {
                cs.setLong(index, Long.class.cast(o));
            } else if (o instanceof Float) {
                cs.setFloat(index, Float.class.cast(o));
            } else if (o instanceof Double) {
                cs.setDouble(index, Double.class.cast(o));
            } else if (o instanceof Date) {
                cs.setDate(index, Date.class.cast(o));
            } else if (o instanceof Time) {
                cs.setTime(index, Time.class.cast(o));
            } else if (o instanceof Timestamp) {
                cs.setTimestamp(index, Timestamp.class.cast(o));
            }
        }
        return success;
    }

    /**
     * Set input param to null but for some reason we have to name the class
     * type
     *
     *
     * @param cs the statement
     * @param index param index
     * @param typeDescription the type of the data to be null'ed
     * @return true on success
     * @throws SQLException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static boolean setInNull(CallableStatement cs, int index, String typeDescription) throws SQLException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        boolean success = false;
        if (cs != null) {
            cs.setNull(index, java.sql.Types.class.getField(typeDescription).getInt(null));
        }
        return success;
    }
}
