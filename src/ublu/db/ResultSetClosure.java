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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Encapsulates a ResultSet with the Db and Statement that fetched it. Used to
 * keep the Db and Statement from going out of scope which closes the ResultSet.
 *
 * @author jwoehr
 */
public class ResultSetClosure implements AutoCloseable {

    private Db db;
    private ResultSet resultSet;
    private Statement statement;

    /**
     * Get the Db which originated the ResultSet
     *
     * @return the Db which originated the ResultSet
     */
    public Db getDb() {
        return db;
    }

    /**
     * Set the Db which originated the ResultSet
     *
     * @param db the Db which originated the ResultSet
     */
    public final void setDb(Db db) {
        this.db = db;
    }

    /**
     * Get the ResultSet
     *
     * @return the ResultSet
     */
    public ResultSet getResultSet() {
        return resultSet;
    }

    /**
     * Store the ResultSet
     *
     * @param resultSet the ResultSet
     */
    public final void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    /**
     * Return the Statement which originated the ResultSet.
     *
     * @return the Statement which originated the ResultSet
     */
    public Statement getStatement() {
        return statement;
    }

    /**
     * Store the Statement which originated the ResultSet.
     *
     * @param statement the Statement which originated the ResultSet
     */
    public final void setStatement(Statement statement) {
        this.statement = statement;
    }

    /**
     * Create a ResultSetClosure encapsulating the ResultSet, the Db and
     * Statement it came from.
     *
     * @param db
     * @param resultSet
     * @param statement
     */
    public ResultSetClosure(Db db, ResultSet resultSet, Statement statement) {
        this.db = db;
        this.resultSet = resultSet;
        this.statement = statement;
    }

    /**
     * Set the result set's autocommit mode
     *
     * @param autoCommitValue true if autocommit, false if not autocommit
     * @throws SQLException
     */
    public void setAutoCommit(boolean autoCommitValue) throws SQLException {
        getDb().getConnection().setAutoCommit(autoCommitValue);
    }

    /**
     * Commit the result set if autocommit is off.
     *
     * @throws SQLException
     */
    public void commit() throws SQLException {
        getDb().getConnection().commit();
    }

    /**
     * Set hint for how many rows to fetch at a time in result set
     *
     * @param rows number of rows to fetch at a time in result set
     * @throws SQLException
     */
    public void setFetchSize(int rows) throws SQLException {
        getResultSet().setFetchSize(rows);
    }

    /**
     * Close the result set and null the instance.
     *
     * @return this
     * @throws SQLException
     */
    public ResultSetClosure closeRS() throws SQLException {
        ResultSet rs = getResultSet();
        if (rs != null) {
            rs.close();
            setResultSet(null);
        }
        return this;
    }

    /**
     * Close the originating statement and null the instance variable.
     *
     * @return this
     * @throws SQLException
     */
    public ResultSetClosure closeStatement() throws SQLException {
        Statement s = getStatement();
        if (s != null) {
            s.close();
            setStatement(null);
        }
        return this;
    }

    /**
     * Disconnect the underlying Db and null the instance variable.
     *
     * @return this
     * @throws SQLException
     */
    public ResultSetClosure disconnectDb() throws SQLException {
        Db dB = getDb();
        if (dB != null) {
            dB.disconnect();
            setDb(null);
        }
        return this;
    }

    /**
     * Closes the result set, the statement, and disconnects the db, nulling all
     * instance variables.
     *
     * @throws SQLException
     */
    @Override
    public void close() throws SQLException {
        ResultSet rs = getResultSet();
        if (rs != null) {
            rs.close();
        }
        Statement s = getStatement();
        if (s != null) {
            s.close();
        }
        Db dB = getDb();
        if (dB != null) {
            dB.disconnect();
        }
    }
}
