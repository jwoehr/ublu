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

import ublu.util.ArgArray;
import ublu.db.Csv;
import ublu.db.Db;
import ublu.db.DbAS400;
import ublu.db.DbHelper;
import ublu.db.DbPostgres;
import ublu.db.ResultSetClosure;
import ublu.db.TableReplicator;
import ublu.util.Generics;
import ublu.util.Generics.ConnectionProperties;
import ublu.util.Generics.PrimaryKeyList;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.json.JSONException;
import org.json.JSONObject;
import ublu.db.Json;

/* Uncomment the following if you are adding MSSQL support */
import ublu.db.DbMSSQL;
/* End Uncomment */
/**
 * Command to perform certain database operations.
 *
 * @author jwoehr
 */
public class CmdDb extends Command {

    {
        setNameAndDescription("db",
                "/4? [--,-dbconnected ~@dbconnected] [-as400 ~@as400] -dbtype,-db ~@{type} [-charsetname ~@{charsetname}] "
                + "[-qopt ~@{close|hold|ro|update|forward|insensitive|sensitive}] [-rdb ~@{rdbname}] "
                + "[-destqopt ~@{close|hold|ro|update|forward|insensitive|sensitive}] "
                + "[-catalog | -columnnames ~@{tablename} | -columntypes ~@{tablename} "
                + "| -connect | -csv ~@{tablename} [-separator ~@{separator} ] |  -json ~@{tablename} | -disconnect | -metadata "
                + "| -primarykeys ~@{tablename} | -query ~@{SQL string} | -query_nors ~@{SQL string} "
                + "| -replicate ~@{tableName} ~@{destDbName} ~@{destDbType} ~@{destDatabaseName} ~@{destUser} ~@{destPassword} "
                + "| -star ~@{tablename}] [-pklist ~@{ space separated primary keys }] "
                + "[-port ~@{portnum}] [-destport ~@{destportnum}] [-property ~@{key} ~@{value} [-property ~@{key} ~@{value}] ..] "
                + "[-ssl @tf | -usessl] "
                + "~@{system} ~@{schema} ~@{userid} ~@{password} : perform various operations on databases");
    }

    /**
     * ctor/0
     */
    public CmdDb() {
    }
    private Db db = null;
    private String dbType;

    /**
     * Get the type of the database we are accessing
     *
     * @return type of the database
     */
    private String getDbType() {
        return dbType;
    }

    /**
     * Set the type of the database we are accessing
     *
     * @param dbType
     */
    private void setDbType(String dbType) {
        this.dbType = dbType;
    }

    /**
     * Get the database instance
     *
     * @return the database instance
     */
    private Db getDb() {
        return db;
    }

    /**
     * Set the database instance
     *
     * @param db the database instance
     */
    private void setDb(Db db) {
        this.db = db;
    }

    /**
     * The functions this command knows
     */
    private static enum FUNCTIONS {

        /**
         * Do nothing
         */
        NULL,
        /**
         * Deliver a catalog
         */
        CATALOG,
        /**
         * Deliver a string of column names
         */
        COLUMNNAMES,
        /**
         * Deliver a string of column types
         */
        COLUMNTYPES,
        /**
         * Connect and return the connection object
         */
        CONNECT,
        /**
         * Disconnect a connection object
         */
        DISCONNECT,
        /**
         * Deliver metadata
         */
        METADATA,
        /**
         * List the primary keys
         */
        PRIMARYKEYS,
        /**
         * Execute a query with a result set
         */
        QUERY,
        /**
         * Execute a query with no result set, e.g., a CREATE
         */
        QUERY_NORS,
        /**
         * "SELECT * FROM" shortcut
         */
        STAR,
        /**
         * Replicate table structure
         */
        REPLICATE,
        /**
         * Convert a table to comma-separated values
         */
        TABLECSV,
        /**
         * Convert a table to JSON
         */
        TABLEJSON
    }
    private FUNCTIONS function;
    private ConnectionProperties connectionProperties;
    private ConnectionProperties destConnectionProperties;
    private String port;
    private String destPort;
    private boolean usessl = false;
    private String rbdName = null;

    /**
     * Get remote db name (AS400 only)
     *
     * @return remote db name (AS400 only)
     */
    public String getRbdName() {
        return rbdName;
    }

    /**
     * Set remote db name (AS400 only)
     *
     * @param rbdName
     */
    public void setRbdName(String rbdName) {
        this.rbdName = rbdName;
    }

    /**
     * Get ssl flag
     *
     * @return ssl flag
     */
    public boolean getUsessl() {
        return usessl;
    }

    /**
     * Set ssl flag
     *
     * @param usessl
     */
    public void setUsessl(Boolean usessl) {
        this.usessl = usessl;
    }

    /**
     * Get db port
     *
     * @return db port
     */
    private String getPort() {
        return port;
    }

    /**
     * Set db port
     *
     * @param port db port
     */
    private void setPort(String port) {
        this.port = port;
    }
    private String csvTableName;

    private String getDestPort() {
        return destPort;
    }

    private void setDestPort(String destPort) {
        this.destPort = destPort;
    }

    /**
     * Get name of table we want
     *
     * @return name of table we want
     */
    private String getCsvTableName() {
        return csvTableName;
    }

    /**
     * Set name of table we want
     *
     * @param csvTableName name of table we want
     */
    private void setCsvTableName(String csvTableName) {
        this.csvTableName = csvTableName;
    }
    private String csvSeparator;

    /**
     * Get char to use as separator
     *
     * @return char to use as separator
     */
    private String getCsvSeparator() {
        return csvSeparator;
    }

    /**
     * Set char to use as separator
     *
     * @param csvSeparator char to use as separator
     */
    private void setCsvSeparator(String csvSeparator) {
        this.csvSeparator = csvSeparator;
    }

    /**
     * Get list of connection properties
     *
     * @return list of connection properties
     */
    private ConnectionProperties getConnectionProperties() {
        return connectionProperties;
    }

    /**
     * Set list of connection properties
     *
     * @param connectionProperties list of connection properties
     */
    private void setConnectionProperties(ConnectionProperties connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    private ConnectionProperties getDestConnectionProperties() {
        return destConnectionProperties;
    }

//    private void setDestConnectionProperties(ConnectionProperties destConnectionProperties) {
//        this.destConnectionProperties = destConnectionProperties;
//    }
    private FUNCTIONS getFunction() {
        return function;
    }

    private void setFunction(FUNCTIONS function) {
        this.function = function;
    }
    private String sqlQuery;

    private String getSqlQuery() {
        return sqlQuery;
    }

    private void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    Integer resultSetType = ResultSet.TYPE_SCROLL_SENSITIVE;
    Integer resultSetConcurrency = ResultSet.CONCUR_UPDATABLE;
    Integer resultSetHoldability = null;
    Integer destResultSetType = ResultSet.TYPE_SCROLL_SENSITIVE;
    Integer destResultSetConcurrency = ResultSet.CONCUR_UPDATABLE;
    Integer destResultSetHoldability = null;

    private Integer getResultSetType() {
        return resultSetType;
    }

    private void setResultSetType(Integer resultSetType) {
        this.resultSetType = resultSetType;
    }

    private Integer getResultSetConcurrency() {
        return resultSetConcurrency;
    }

    private void setResultSetConcurrency(Integer resultSetConcurrency) {
        this.resultSetConcurrency = resultSetConcurrency;
    }

    private Integer getResultSetHoldability() {
        return resultSetHoldability;
    }

    private void setResultSetHoldability(Integer resultSetHoldability) {
        this.resultSetHoldability = resultSetHoldability;
    }

    private Integer getDestResultSetType() {
        return destResultSetType;
    }

    private void setDestResultSetType(Integer destResultSetType) {
        this.destResultSetType = destResultSetType;
    }

    private Integer getDestResultSetConcurrency() {
        return destResultSetConcurrency;
    }

    private void setDestResultSetConcurrency(Integer destResultSetConcurrency) {
        this.destResultSetConcurrency = destResultSetConcurrency;
    }

    private Integer getDestResultSetHoldability() {
        return destResultSetHoldability;
    }

    private void setDestResultSetHoldability(Integer destResultSetHoldability) {
        this.destResultSetHoldability = destResultSetHoldability;
    }

    /**
     * Execute a db operation
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray db(ArgArray argArray) {
        String charsetName = null;
        // This is for FUNCTIONS.STAR or FUNCTIONS.REPLICATE or FUNCTIONS.PRIMARYKEYS
        String starTableName = "";
        // These are for FUNCTIONS.REPLICATE
        String destDbName = "";
        String destDbType = "";
        String destDatabaseName = "";
        String destUser = "";
        String destPassword = "";
        PrimaryKeyList primaryKeyList = new PrimaryKeyList(); // used by REPLICATE       
        while (argArray.hasDashCommand() && getCommandResult() != COMMANDRESULT.FAILURE) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-dbtype":
                case "-db":
                    if (getDb() == null) {
                        setDbType(argArray.nextMaybeQuotationTuplePopString());
                        switch (getDbType()) {
                            case "as400":
                                setDb(new DbAS400());
                                break;
                            case "postgres":
                                setDb(new DbPostgres());
                                break;
                            /* Uncomment the following if you are adding MSSQL support */
                            case "mssql":
                                setDb(new DbMSSQL());
                                break;
                            /* End Uncomment */
                        }
                    }
                    break;
                case "--":
                case "-dbconnected":
                    setDb(argArray.nextTupleOrPop().value(Db.class));
                    break;
                case "-catalog":
                    setFunction(FUNCTIONS.CATALOG);
                    break;
                case "-charsetname":
                    charsetName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-columnnames":
                    setFunction(FUNCTIONS.COLUMNNAMES);
                    starTableName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-columntypes":
                    setFunction(FUNCTIONS.COLUMNTYPES);
                    starTableName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-connect":
                    setFunction(FUNCTIONS.CONNECT);
                    break;
                case "-csv":
                    setFunction(FUNCTIONS.TABLECSV);
                    setCsvTableName(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-json":
                    setFunction(FUNCTIONS.TABLEJSON);
                    setCsvTableName(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-disconnect":
                    setFunction(FUNCTIONS.DISCONNECT);
                    break;
                case "-metadata":
                    setFunction(FUNCTIONS.METADATA);
                    break;
                case "-primarykeys":
                    setFunction(FUNCTIONS.PRIMARYKEYS);
                    starTableName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-pklist":
                    primaryKeyList.splitIn(argArray.nextMaybeQuotationTuplePopString(), "\\p{Space}+");
                    break;
                case "-port":
                    setPort(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-destport":
                    setDestPort(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-property":
                    getConnectionProperties().put(argArray.nextMaybeQuotationTuplePopString(), argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-replicate":
                    setFunction(FUNCTIONS.REPLICATE);
                    starTableName = argArray.nextMaybeQuotationTuplePopString();
                    destDbName = argArray.nextMaybeQuotationTuplePopString();
                    destDbType = argArray.nextMaybeQuotationTuplePopString();
                    destDatabaseName = argArray.nextMaybeQuotationTuplePopString();
                    destUser = argArray.nextMaybeQuotationTuplePopString();
                    destPassword = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-separator":
                    setCsvSeparator(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-star":
                    setFunction(FUNCTIONS.STAR);
                    starTableName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-query":
                    setFunction(FUNCTIONS.QUERY);
                    setSqlQuery(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-query_nors":
                    setFunction(FUNCTIONS.QUERY_NORS);
                    setSqlQuery(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-qopt":
                    resultSetOption(argArray.nextMaybeQuotationTuplePopStringTrim());
                    break;
                case "-destqopt":
                    destResultSetOption(argArray.nextMaybeQuotationTuplePopStringTrim());
                    break;
                case "-rdb":
                    setRbdName(argArray.nextMaybeQuotationTuplePopStringTrim());
                    break;
                case "-ssl":
                    setUsessl(argArray.nextBooleanTupleOrPop());
                    break;
                case "-usessl":
                    setUsessl(true);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (getCommandResult() != COMMANDRESULT.FAILURE) {
            if (!getDb().isConnected()) {
                if (getDb() instanceof DbAS400 && getAs400() != null) {
                    try {
                        getDb().connect(getAs400(), getPort(), getUsessl(), getRbdName(), getConnectionProperties());
                    } catch (ClassNotFoundException | SQLException ex) {
                        getLogger().log(Level.SEVERE, "Could not connect to " + getAs400() + " " /* + schema */ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                } else {
                    if (argArray.size() < 4) { // here's where we fall out if new ArgArray()
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        String system = argArray.nextMaybeQuotationTuplePopString();
                        String schema = argArray.nextMaybeQuotationTuplePopString();
                        String userid = argArray.nextMaybeQuotationTuplePopString();
                        String password = argArray.nextMaybeQuotationTuplePopString();
                        if (getDb() == null || getFunction() == null) {
                            getLogger().log(Level.SEVERE, "-db dbtype and a choice of function required for {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            /* Uncomment the following if you are adding MSSQL support */
                        if (getDb().getDbType() == Db.DBTYPE.MSSQL && getPort() == null) {
                            setPort(DbMSSQL.MSSQL_DEFAULT_PORT); // MSSQL connect needs a specific port number
                        }
                            /* End Uncomment */
                            try {
                                getDb().connect(system, getPort(), schema, getConnectionProperties(), userid, password);
                            } catch (ClassNotFoundException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Could not connect to " + system + " " + schema + inNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                }
            }
            if (getDb().isConnected()) {
                if (charsetName == null) {
                    charsetName = charsetNameFromDb();
                }
                try {
                    ResultSet rs;
                    ResultSetClosure rsc;
                    DatabaseMetaData dbMetaData;
                    DbHelper dbHelper;
                    switch (getFunction()) {
                        case CATALOG:
                            String catalog = getDb().getCatalog();
                            put(catalog, charsetName);
                            break;
                        case COLUMNNAMES:
                            dbHelper = DbHelper.newDbHelperStarFrom(db, starTableName);
                            Generics.ColumnNameList columnNameList = dbHelper.generateColumnNameList();
                            put(columnNameList);
                            break;
                        case COLUMNTYPES:
                            dbHelper = DbHelper.newDbHelperStarFrom(db, starTableName);
                            Generics.ColumnTypeNameList columnTypeNameList = dbHelper.fetchColumnTypeNameList(null, null, starTableName, null).getColumnTypeNameList();
                            put(columnTypeNameList);
                            break;
                        case CONNECT:
                            put(getDb());
                            break;
                        case DISCONNECT:
                            getDb().disconnect();
                            break;
                        case METADATA:
                            dbMetaData = getDb().getMetaData();
                            put(dbMetaData);
                            break;
                        case PRIMARYKEYS:
                            // String catalogName =  getDb().getDbType() == Db.DBTYPE.AS400 ? "SYSKEYCST" :  null;
                            ResultSet primaryKeys = getDb().getMetaData().getPrimaryKeys( /* catalogName */null, /* schema */ null, starTableName);
                            put(primaryKeys);
                            break;
                        case QUERY:
                            // /* Debug */ getLogger().log(Level.INFO, "The query is {0}", getSqlQuery());
                            Statement statement;
                            if (resultSetHoldability == null) {
                                statement = getDb().createStatement(getResultSetType(), getResultSetConcurrency());
                            } else {
                                statement = getDb().createStatement(getResultSetType(), getResultSetConcurrency(), getResultSetHoldability());
                            }
                            rs = statement.executeQuery(getSqlQuery());
                            rsc = new ResultSetClosure(getDb(), rs, statement);
                            // put(rsc, charsetName == null ? charsetNameFromDb() : charsetName);
                            put(rsc, charsetName);
                            break;
                        case QUERY_NORS:
                            // /* Debug */ getLogger().log(Level.INFO, "The query is {0}", getSqlQuery());
                            // CallableStatement cs = getDb().prepareCall(getSqlQuery());
                            Statement statement_nors = getDb().createStatement();
                            statement_nors.execute(getSqlQuery());
                            break;
                        case REPLICATE:
                            Db destDb = null;
                            switch (destDbType) {
                                case "as400":
                                    destDb = new DbAS400();
                                    break;
                                case "postgres":
                                    destDb = new DbPostgres();
                                    break;
                                default:
                                    getLogger().log(Level.SEVERE, "Unknown destination database type for replication: {0}", destDbType);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            if (destDb != null) {
                                rs = getDb().selectStarFrom(starTableName);
                                destDb.connect(destDbName, getDestPort(), destDatabaseName, getDestConnectionProperties(), destUser, destPassword);
                                new TableReplicator(destDb, rs, rs.getMetaData(), destDb, starTableName, primaryKeyList, getDestResultSetType(), getDestResultSetConcurrency(),
                                        getDestResultSetHoldability()).replicate();
                                rs.close();
                                getDb().disconnect();
                                destDb.disconnect();
                            }
                            break;
                        case STAR:
                            rsc = DbHelper.selectStarFrom(db, starTableName, getResultSetType(), getResultSetConcurrency(), getResultSetHoldability());
                            put(rsc, charsetName);
                            break;
                        case TABLECSV:
                            Csv cSV = getDb().newStarCsv(getCsvTableName(), csvSeparator);
                            String csvTable = cSV.tableCSV();
                            put(csvTable, charsetName);
                            cSV.close();
                            break;
                        case TABLEJSON:
                            Json json = getDb().newStarJSON(getCsvTableName());
                            JSONObject jsonTable = null;
                            try {
                                jsonTable = json.tableJSON();
                            } catch (JSONException ex) {
                                getLogger().log(Level.SEVERE, "Exception converting table to JSON", ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            put(jsonTable, charsetName);
                            json.close();
                            break;
                    }
                    // getDb().disconnect(); can't do this since result sets hang around in tuple vars from QUERY and QUERY_NORS
                } catch (ClassNotFoundException ex) {
                    getLogger().log(Level.SEVERE, "The db command could not load or find the necessary driver", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } catch (SQLException ex) {
                    getLogger().log(Level.SEVERE, "The db command encountered an exception in database operations", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } catch (UnsupportedEncodingException ex) {
                    getLogger().log(Level.SEVERE, "The db command encountered an exception in character set decoding", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "The db command encountered an exception in i/o", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } catch (AS400SecurityException ex) {
                    getLogger().log(Level.SEVERE, "The db command encountered a security exception", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } catch (ErrorCompletingRequestException ex) {
                    getLogger().log(Level.SEVERE, "The db command encountered an exception", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } catch (InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                    getLogger().log(Level.SEVERE, "The db command encountered an exception", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    private String charsetNameFromDb() {
        String charsetName;
        switch (getDb().getDbType()) {
            case AS400:
                charsetName = Db.cpEBCDIC;
                break;
            default:
                charsetName = "ASCII";
        }
        return charsetName;
    }

    private void resultSetOption(String s) {
        switch (s.toLowerCase()) {
            case "close":
                setResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
                break;
            case "hold":
                setResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
                break;
            case "ro":
                setResultSetConcurrency(ResultSet.CONCUR_READ_ONLY);
                break;
            case "update":
                setResultSetConcurrency(ResultSet.CONCUR_UPDATABLE);
                break;
            case "forward":
                setResultSetType(ResultSet.TYPE_FORWARD_ONLY);
                break;
            case "insensitive":
                setResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE);
                break;
            case "sensitive":
                setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
                break;
            default:
                getLogger().log(Level.SEVERE, "unknown qopt {0} in {1}", new Object[]{s, getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    private void destResultSetOption(String s) {
        switch (s.toLowerCase()) {
            case "close":
                setDestResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
                break;
            case "hold":
                setDestResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
                break;
            case "ro":
                setDestResultSetConcurrency(ResultSet.CONCUR_READ_ONLY);
                break;
            case "update":
                setDestResultSetConcurrency(ResultSet.CONCUR_UPDATABLE);
                break;
            case "forward":
                setDestResultSetType(ResultSet.TYPE_FORWARD_ONLY);
                break;
            case "insensitive":
                setDestResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE);
                break;
            case "sensitive":
                setDestResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
                break;
            default:
                getLogger().log(Level.SEVERE, "unknown qopt {0} in {1}", new Object[]{s, getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    @Override
    protected void reinit() {
        super.reinit();
        setCsvSeparator(",");
        setDb(null);
        setFunction(FUNCTIONS.NULL);
        setPort(null);
        setConnectionProperties(new ConnectionProperties());
        setSqlQuery("");
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return db(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
