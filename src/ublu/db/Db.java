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

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDataSource;
import ublu.Ublu;
import ublu.util.Generics.ConnectionProperties;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a database instance, abstract without type set in ctor. The type
 * is set in the extenders of Db.
 *
 * @author jwoehr
 */
public abstract class Db {

    private String sqlCollectionName;

    /**
     * Get the value of sqlCollectionName
     *
     * @return the value of sqlCollectionName
     */
    public String getSqlCollectionName() {
        return sqlCollectionName;
    }

    /**
     * Set the value of sqlCollectionName
     *
     * @param sqlCollectionName new value of sqlCollectionName
     */
    public void setSqlCollectionName(String sqlCollectionName) {
        this.sqlCollectionName = sqlCollectionName;
    }

    // only used in debugging
    // all Db methods throw to their callers
    private static Logger getLogger() {
        return Ublu.getMainInterpreter().getLogger();
    }
    /**
     * <a
     * href="http://www.iana.org/assignments/character-sets/character-sets.xhtml">ebcdic-cp-us</a>
     */
    public static String cpEBCDIC = "CP037";

    /**
     * Instance a database with driver, type and connection not yet set
     */
    public Db() {
        this.driver = null;
        this.dbType = null;
        this.connection = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        try {
            sb.append('\n')
                    .append(this.driver).append('\n')
                    .append(this.dbType).append('\n')
                    .append(this.connection).append('\n')
                    .append(this.connection.getMetaData()).append('\n');
        } catch (SQLException ex) {
            Logger.getLogger(Db.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    /**
     * The Database types we support
     */
    public static enum DBTYPE {

        /**
         * DB400
         */
        AS400,
        /**
         * Postgresql
         */
        PGSQL,
        /**
         * Microsoft
         */
        MSSQL
    }
    /**
     * The database type of this Db instance
     */
    protected DBTYPE dbType;
    /**
     * The driver instance we use
     */
    protected Driver driver;
    /**
     * Our connection if connected
     */
    protected Connection connection;

    /**
     * Return our connection
     *
     * @return connection, or null if none
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Set the connection instance
     *
     * @param connection a Connection
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * True if is connected.
     *
     * @return True if is connected.
     */
    public boolean isConnected() {
        return getConnection() != null;
    }

    /**
     * Get database type
     *
     * @return database type
     */
    public DBTYPE getDbType() {
        return dbType;
    }

    /**
     * Translate type into string name of driver class
     *
     * @return Driver name so we can instance it.
     */
    public String getDriverName() {
        String driverName = "";
        switch (getDbType()) {
            case AS400:
                driverName = "com.ibm.as400.access.AS400JDBCDriver";
                break;
            case PGSQL:
                driverName = "org.postgresql.Driver";
                break;
            case MSSQL:
                driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }
        // /* Debug */ getLogger().log(Level.INFO, "Driver name is " + getDriverName);
        return driverName;
    }

    /**
     * Translate the driver type for composing the connection url
     *
     * @return the driver string for the connection url
     */
    public String getDriverType() {
        String type = "";
        switch (getDbType()) {
            case AS400:
                type = "as400";
                break;
            case PGSQL:
                type = "postgresql";
                break;
            case MSSQL:
                type = "sqlserver";
                break;
        }
        return type;
    }

    /**
     * Make sure the driver is loaded. Apparently unnecessary in modern JDBC.
     *
     * @return the driver class
     * @throws ClassNotFoundException
     */
    public Class loadDriver() throws ClassNotFoundException {
        return Class.forName(getDriverName());
    }

    /**
     * Get the driver by looking through the driver manager if we don't already
     * have its instance stored locally.
     *
     * @return the driver for this database
     */
    public Driver getDriver() {
        if (this.driver == null) {
            Enumeration<Driver> ed = DriverManager.getDrivers();
            String driverName = getDriverName();
            while (ed.hasMoreElements()) {
                Driver d = ed.nextElement();
                // /* Debug */ getLogger().log(Level.INFO, "Enumerated driver's name is " + dd.getClass().getName());
                if (d.getClass().getName().equals(driverName)) {
                    this.driver = d;
                    break;
                }
            }
        }
        return this.driver;
    }

    /**
     * Check if driver is loaded
     *
     * @return true if loaded
     */
    public boolean isDriverLoaded() {
        return getDriver() != null;
    }

    /**
     * Unload the driver. This is apparently not necessary in modern JDBC.
     */
    public void unloadDriver() {
        if (this.driver != null) {
            try {
                DriverManager.deregisterDriver(getDriver());
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
            this.driver = null;
        }
    }

    /**
     * Compose the URL for a connection.
     *
     * @param system host
     * @param port jdbc port for database instance
     * @param database the name of the database to access
     * @param connectionProperties any connection properties desired
     * @return formulated URL
     */
    public String buildURL(String system, String port, String database, ConnectionProperties connectionProperties) {
        setSqlCollectionName(database);
        StringBuilder sb = new StringBuilder("jdbc:");
        sb.append(getDriverType());
        sb.append(":");
        if (system != null) {
            sb.append("//");
            sb.append(system);
            if (port != null) {
                sb.append(":");
                sb.append(port);
            }
            sb.append("/");
        }
        sb.append(database);
        if (connectionProperties != null) {
            for (String key : connectionProperties.stringPropertyNames()) {
                sb.append(";")
                        .append(key)
                        .append("=")
                        .append(connectionProperties.getProperty(key));
            }
        }
        return sb.toString();
    }

    /**
     * Connect to the database specifying by strings
     *
     * @param system host
     * @param port jdbc port
     * @param database name of database
     * @param connectionProperties any desired connection properties
     * @param userid userid for database
     * @param password password
     * @return the connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public Connection connect(String system, String port, String database, ConnectionProperties connectionProperties, String userid, String password)
            throws ClassNotFoundException, SQLException {
        loadDriver();
        String url = buildURL(system, port, database, connectionProperties);
        // /* Debug */ getLogger().log(Level.INFO, "URL is " + url);
        setConnection(DriverManager.getConnection(url, userid, password));
        return getConnection();
    }

    /**
     * Connect to the database passing an AS400 object
     *
     * @param as400
     * @param port jdbc port
     * @param database name of database
     * @param connectionProperties any desired connection properties
     * @return the connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public Connection connect(AS400 as400, String port, String database, ConnectionProperties connectionProperties)
            throws ClassNotFoundException, SQLException {
        AS400JDBCDataSource asjdbcds = new AS400JDBCDataSource(as400);
        if (port != null) {
            asjdbcds.setPortNumber(Integer.parseInt(port));
        }
        if (database != null) {
            asjdbcds.setDatabaseName(database);
        }
        asjdbcds.setProperties(connectionProperties);
        setConnection(asjdbcds.getConnection());
        return getConnection();
    }

    /**
     * Connect to the database passing an AS400 object
     *
     * @param as400
     * @param port jdbc port
     * @param connectionProperties any desired connection properties
     * @return the connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public Connection connect(AS400 as400, String port, ConnectionProperties connectionProperties)
            throws ClassNotFoundException, SQLException {
        AS400JDBCDataSource asjdbcds = new AS400JDBCDataSource(as400);
        if (port != null) {
            asjdbcds.setPortNumber(Integer.parseInt(port));
        }
        asjdbcds.setProperties(connectionProperties);
        setConnection(asjdbcds.getConnection());
        return getConnection();
    }

    /**
     * Connect to the database passing an AS400 object and an SSL flag
     *
     * @param as400
     * @param port jdbc port
     * @param useSSL true means use ssl to connect
     * @param connectionProperties any desired connection properties
     * @return the connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public Connection connect(AS400 as400, String port, boolean useSSL, ConnectionProperties connectionProperties)
            throws ClassNotFoundException, SQLException {
        AS400JDBCDataSource asjdbcds = new AS400JDBCDataSource(as400);
        asjdbcds.setSecure(useSSL);
        if (port != null) {
            asjdbcds.setPortNumber(Integer.parseInt(port));
        }
        asjdbcds.setProperties(connectionProperties);
        setConnection(asjdbcds.getConnection());
        return getConnection();
    }

    /**
     * Connect to the database passing an AS400 object and an SSL flag
     *
     * @param as400
     * @param port jdbc port
     * @param useSSL true means use ssl to connect
     * @param rdbName remote db dir entry name (if any)
     * @param connectionProperties any desired connection properties
     * @return the connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public Connection connect(AS400 as400, String port, boolean useSSL, String rdbName, ConnectionProperties connectionProperties)
            throws ClassNotFoundException, SQLException {
        AS400JDBCDataSource asjdbcds = new AS400JDBCDataSource(as400);
        asjdbcds.setSecure(useSSL);
        if (port != null) {
            asjdbcds.setPortNumber(Integer.parseInt(port));
        }
        asjdbcds.setProperties(connectionProperties);
        if (rdbName != null) {
            /* Debug */ System.err.println("rdbName is " + rdbName);
            asjdbcds.setDatabaseName(rdbName);
        }
        setConnection(asjdbcds.getConnection());
        return getConnection();
    }

    /**
     * Disconnect from the database.
     *
     * @throws SQLException
     */
    public void disconnect() throws SQLException {
        if (getConnection() != null) {
            getConnection().close();
            setConnection(null);
        }
    }

    /**
     * Return a string representing the catalog for this database.
     *
     * @return a string representing the catalog for this database
     * @throws SQLException
     */
    public String getCatalog() throws SQLException {
        String s = null;
        if (getConnection() != null) {
            s = getConnection().getCatalog();
        }
        return s;
    }

    /**
     * Get the database metadata.
     *
     * @return the database metadata
     * @throws SQLException
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        DatabaseMetaData d = null;
        if (getConnection() != null) {
            d = getConnection().getMetaData();
        }
        return d;
    }

    /**
     * Compose a query as a CallableStatement
     *
     * <p>
     * CallableStatements are optimized for calling stored procedures.
     *
     * @param sqlQuery the query text
     * @param resultSetType scroll-sensitive | scroll-insensitive
     * @param resultSetConcurrency concurrency-updatable | concurrency-readonly
     * @return the Statement representing the query
     * @throws SQLException
     */
    public CallableStatement prepareCall(String sqlQuery, int resultSetType, int resultSetConcurrency) throws SQLException {
        CallableStatement cs = null;
        if (getConnection() != null) {
            cs = getConnection().prepareCall(sqlQuery, resultSetType, resultSetConcurrency);
        }
        return cs;
    }

    /**
     * Compose a query as a PreparedStatement
     * <p>
     * PreparedStatements<p>
     * are general purpose queries
     *
     * @param sqlQuery the query text
     * @param resultSetType scroll sensitive or no
     * @param resultSetConcurrency updateability
     * @return the Statement representing the query
     * @throws SQLException
     */
    public PreparedStatement prepareStatement(String sqlQuery, int resultSetType, int resultSetConcurrency) throws SQLException {
        PreparedStatement ps = null;
        if (getConnection() != null) {
            ps = getConnection().prepareStatement(sqlQuery, resultSetType, resultSetConcurrency);
        }
        return ps;
    }

    /**
     * Create a Statement for use with a query via Statement.execute(String
     * sqlQuery).
     * <p>
     * The statement is instanced with the scroll sensitivy and updatability
     * desired.</p>
     *
     * @param resultSetType scroll sensitivity per {@link java.sql.ResultSet}
     * @param resultSetConcurrency updatability per {@link java.sql.ResultSet}
     * @return the Statement
     * @throws SQLException
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return getConnection().createStatement(resultSetType, resultSetConcurrency);
    }

    /**
     * Create a Statement for use with a query via Statement.execute(String
     * sqlQuery).
     * <p>
     * The statement is instanced with the scroll sensitivy and updatability
     * desired.</p>
     *
     * @param resultSetType scroll sensitivity per {@link java.sql.ResultSet}
     * @param resultSetConcurrency updatability per {@link java.sql.ResultSet}
     * @param resultSetHoldability holdability per {@link java.sql.ResultSet}
     * @return the Statement
     * @throws SQLException
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return getConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /**
     * Create a Statement for use with a query via Statement.execute(String
     * sqlQuery).
     * <p>
     * The statement is instanced with scroll insensitivy and no updatability.
     * This is the simplest, for read-only-once ResultSets or no result
     * sets.</p>
     *
     * @return the Statement
     * @throws SQLException
     */
    public Statement createStatement() throws SQLException {
        return getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * Create a scroll-sensitive, updateable Statement for use with a query.
     * <p>
     * The statement can only open one result set at a time.</p>
     *
     * @return the Statement
     * @throws SQLException
     */
    public Statement createScrollableUpdateableStatement() throws SQLException {
        return getConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }

    /**
     * Execute an SQL statement accepting defaults for scroll sensitivity and
     * updatability (default for both is none).
     *
     * @param sqlQuery
     * @return the result set
     * @throws SQLException
     */
    public ResultSet executeSQL(String sqlQuery) throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = prepareStatement(sqlQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        if (ps != null) {
            rs = ps.executeQuery();
        }
        return rs;
    }

    /**
     * Gets a result set of tables filtered by the arguments
     *
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @param types
     * @return ResultSet of tables matching the filters
     * @throws SQLException
     */
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
            throws SQLException {
        ResultSet rs = null;
        DatabaseMetaData dmd = getMetaData();
        if (dmd != null) {
            rs = dmd.getTables(catalog, schemaPattern, tableNamePattern, types);
        }
        return rs;
    }

    /**
     * Get a result set identifying the columns of this table
     *
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @param columnNamePattern
     * @return the column info
     * @throws SQLException
     */
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        ResultSet rs = null;
        DatabaseMetaData dmd = getMetaData();
        if (dmd != null) {
            rs = dmd.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
        }
        return rs;
    }

    /**
     * SELECT * FROM a table on this Db using defaults for scrollability and
     * updatability, i.e., none.
     *
     * @param tableName name of table
     * @return the result set
     * @throws SQLException
     */
    public ResultSet selectStarFrom(String tableName) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT * FROM \"");
        query.append(tableName).append("\"");
        return executeSQL(query.toString());
    }

    /**
     * Selects * from a table on this Db with a customized result set
     *
     * @param tableName Name of table
     * @param resultSetType scroll sensitivity per {@link java.sql.ResultSet}
     * @param resultSetConcurrency updatability per {@link java.sql.ResultSet}
     * @return the customized result set
     * @throws SQLException
     */
    public ResultSet selectStarFrom(String tableName, int resultSetType, int resultSetConcurrency) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT * FROM \"");
        query.append(tableName).append("\"");
        return createStatement(resultSetType, resultSetConcurrency).executeQuery(query.toString());
    }

    /**
     * Convert EBCDIC string to ASCII
     *
     * @param input EBCDIC string as a byte array
     * @return ASCII string equivalent
     * @throws UnsupportedEncodingException
     */
    public static String fromEBCDIC(byte[] input) throws UnsupportedEncodingException {
        return new String(input, cpEBCDIC);
    }

    /**
     * Return a new Csv based on a SELECT * FROM statement
     *
     * @param tableName
     * @param columnSeparator
     * @return the Csv object
     * @throws SQLException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public Csv newStarCsv(String tableName, String columnSeparator) throws SQLException, UnsupportedEncodingException, IOException {
        ResultSet rsStarFrom = selectStarFrom(tableName);
        ResultSetMetaData rsmd = rsStarFrom.getMetaData();
        return new Csv(this, rsStarFrom, rsmd, tableName, columnSeparator);
    }

    /**
     * Return a new JSONObject based on a SELECT * FROM statement
     *
     * @param tableName
     * @return the JSON object
     * @throws SQLException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public Json newStarJSON(String tableName) throws SQLException, UnsupportedEncodingException, IOException {
        ResultSet rsStarFrom = selectStarFrom(tableName);
        ResultSetMetaData rsmd = rsStarFrom.getMetaData();
        return new Json(this, rsStarFrom, rsmd, tableName);
    }

    /**
     * Check to see if the specified JDBC type is numeric.
     *
     * @param type The type to check.
     * @return Returns true if the type is numeric.
     */
    public static boolean isNumeric(int type) {
        return type == java.sql.Types.BIGINT || type == java.sql.Types.DECIMAL
                || type == java.sql.Types.DOUBLE || type == java.sql.Types.FLOAT
                || type == java.sql.Types.INTEGER || type == java.sql.Types.NUMERIC
                || type == java.sql.Types.SMALLINT || type == java.sql.Types.TINYINT;
    }
}
