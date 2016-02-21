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
package ublu.command;

import ublu.util.ArgArray;
import ublu.db.Csv;
import ublu.util.DataSink;
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

/**
 * Command to perform certain database operations.
 *
 * @author jwoehr
 */
public class CmdDb extends Command {

    {
        setNameAndDescription("db",
                "/4 [-dbconnected @dbconnected] -db (@)type [-charsetname (@)charsetname] [-catalog | -columntypes (@)tablename | -connect | -csv (@)tablename [-separator ${ separator}$ ] | -disconnect | -metadata | -primarykeys | -query ${ SQL string }$ | -replicate (@)tableName (@)destDbName (@)destDbType (@)destDatabaseName (@)destUser (@)destPassword | -star (@)tablename] [-pklist ${ space separated primary keys }$] [-port (@)portnum] [-property k v [-property k v] ..] (@)system (@)database (@)userid (@)password : perform various operations on databases");
    }

    /**
     * ctor/0
     */
    public CmdDb() {
    }
    private Db db = null;
    private String dbType;

    /**
     * Get the name of the database we are accessing
     *
     * @return name of the database
     */
    public String getDbType() {
        return dbType;
    }

    /**
     * Set the name of the database we are accessing
     *
     * @param dbType
     */
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    /**
     * Get the database instance
     *
     * @return the database instance
     */
    public Db getDb() {
        return db;
    }

    /**
     * Set the database instance
     *
     * @param db the database instance
     */
    public void setDb(Db db) {
        this.db = db;
    }

    /**
     * The functions this command knows
     */
    protected static enum FUNCTIONS {

        /**
         * Do nothing
         */
        NULL,
        /**
         * Deliver a catalog
         */
        CATALOG,
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
        TABLECSV
    }
    private FUNCTIONS function;
    private ConnectionProperties connectionProperties;
    private String port;

    /**
     * Get db port
     *
     * @return db port
     */
    public String getPort() {
        return port;
    }

    /**
     * Set db port
     *
     * @param port db port
     */
    public void setPort(String port) {
        this.port = port;
    }
    private String csvTableName;

    /**
     * Get name of table we want
     *
     * @return name of table we want
     */
    public String getCsvTableName() {
        return csvTableName;
    }

    /**
     * Set name of table we want
     *
     * @param csvTableName name of table we want
     */
    public void setCsvTableName(String csvTableName) {
        this.csvTableName = csvTableName;
    }
    private String csvSeparator;

    /**
     * Get char to use as separator
     *
     * @return char to use as separator
     */
    public String getCsvSeparator() {
        return csvSeparator;
    }

    /**
     * Set char to use as separator
     *
     * @param csvSeparator char to use as separator
     */
    public final void setCsvSeparator(String csvSeparator) {
        this.csvSeparator = csvSeparator;
    }

    /**
     * Get list of connection properties
     *
     * @return list of connection properties
     */
    public ConnectionProperties getConnectionProperties() {
        return connectionProperties;
    }

    /**
     * Set list of connection properties
     *
     * @param connectionProperties list of connection properties
     */
    public final void setConnectionProperties(ConnectionProperties connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

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
        //
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-db":
                    if (getDb() == null) {
                        setDbType(argArray.nextMaybeTupleString());
                        switch (getDbType()) {
                            case "as400":
                                setDb(new DbAS400());
                                break;
                            case "postgres":
                                setDb(new DbPostgres());
                                break;
                        }
                    }
                    break;
                case "-dbconnected":
                    setDb(Db.class.cast(getTuple(argArray.next()).getValue()));
                    break;
                case "-catalog":
                    setFunction(FUNCTIONS.CATALOG);
                    break;
                case "-charsetname":
                    charsetName = argArray.nextMaybeTupleString();
                    break;
                case "-columntypes":
                    setFunction(FUNCTIONS.COLUMNTYPES);
                    starTableName = argArray.nextMaybeTupleString();
                    break;
                case "-connect":
                    setFunction(FUNCTIONS.CONNECT);
                    break;
                case "-csv":
                    setFunction(FUNCTIONS.TABLECSV);
                    setCsvTableName(argArray.nextMaybeTupleString());
                    break;
                case "-disconnect":
                    setFunction(FUNCTIONS.DISCONNECT);
                    break;
                case "-metadata":
                    setFunction(FUNCTIONS.METADATA);
                    break;
                case "-primarykeys":
                    setFunction(FUNCTIONS.PRIMARYKEYS);
                    starTableName = argArray.next();
                    break;
                case "-pklist":
                    if (!argArray.isOpenQuoteNext()) {
                        getLogger().log(Level.SEVERE, "db -pklist must have a $'{' quoted string of space separated primary keys '}'$)", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        argArray = new ArgArray(getInterpreter()); // so we will fall out below
                        break;
                    } else {
                        argArray.assimilateFullQuotation();
                        primaryKeyList.splitIn(argArray.next(), "\\p{Space}+");
                    }
                    break;
                case "-port":
                    setPort(argArray.nextMaybeTupleString());
                    break;
                case "-property":
                    getConnectionProperties().put(argArray.nextMaybeQuotation(), argArray.nextMaybeQuotation());
                    break;
                case "-replicate":
                    setFunction(FUNCTIONS.REPLICATE);
                    starTableName = argArray.nextMaybeTupleString();
                    destDbName = argArray.nextMaybeTupleString();
                    destDbType = argArray.nextMaybeTupleString();
                    destDatabaseName = argArray.nextMaybeTupleString();
                    destUser = argArray.nextMaybeTupleString();
                    destPassword = argArray.nextMaybeTupleString();
                    break;
                case "-separator":
                    if (!argArray.isOpenQuoteNext()) {
                        getLogger().log(Level.SEVERE, "{0} must have an $'{' SQL string '}'$)", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        argArray = new ArgArray(getInterpreter());
                        break;
                    } else {
                        argArray.assimilateFullQuotation();
                        setCsvSeparator(argArray.next());
                    }
                    break;
                case "-star":
                    setFunction(FUNCTIONS.STAR);
                    starTableName = argArray.nextMaybeTupleString();
                    break;
                case "-query":
                    if (!argArray.isOpenQuoteNext()) {
                        getLogger().log(Level.SEVERE, "{0} must have an $'{' SQL string '}'$)", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        argArray = new ArgArray(getInterpreter()); // so we will fall out below
                        break;
                    } else {
                        setFunction(FUNCTIONS.QUERY);
                        argArray.assimilateFullQuotation();
                        setSqlQuery(argArray.next());
                    }
                    break;
                case "-query_nors":
                    if (!argArray.isOpenQuoteNext()) {
                        getLogger().log(Level.SEVERE, "{0} must have an $'{' SQL string '}'$)", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        argArray = new ArgArray(getInterpreter()); // so we will fall out below
                        break;
                    } else {
                        setFunction(FUNCTIONS.QUERY_NORS);
                        argArray.assimilateFullQuotation();
                        setSqlQuery(argArray.next());
                    }
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (!getDb().isConnected()) {
                if (argArray.size() < 4) { // here's where we fall out if new ArgArray()
                    logArgArrayTooShortError(argArray);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    String system = argArray.nextMaybeTupleString();
                    String database = argArray.nextMaybeTupleString();
                    String userid = argArray.nextMaybeTupleString();
                    String password = argArray.nextMaybeTupleString();
                    if (getDb() == null || getFunction() == null) {
                        getLogger().log(Level.SEVERE, "-db dbtype and a choice of function required for {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            getDb().connect(system, getPort(), database, getConnectionProperties(), userid, password);
                        } catch (ClassNotFoundException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Could not connect to " + system + " " + database + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
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
                    switch (getFunction()) {
                        case CATALOG:
                            String catalog = getDb().getCatalog();
                            put(catalog, charsetName);
                            break;
                        case COLUMNTYPES:
                            DbHelper dbHelper = DbHelper.newDbHelperStarFrom(db, starTableName);
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
                            ResultSet primaryKeys = getDb().getMetaData().getPrimaryKeys( /* catalogName */null, /* database */ null, starTableName);
                            put(primaryKeys);
                            break;
                        case QUERY:
                            // /* Debug */ getLogger().log(Level.INFO, "The query is {0}", getSqlQuery());
                            Statement statement = getDb().createScrollableUpdateableStatement();
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
                                destDb.connect(destDbName, null, destDatabaseName, null, destUser, destPassword);
                                new TableReplicator(destDb, rs, rs.getMetaData(), destDb, starTableName, primaryKeyList).replicate();
                                rs.close();
                                getDb().disconnect();
                                destDb.disconnect();
                            }
                            break;
                        case STAR:
                            rsc = DbHelper.selectStarFrom(db, starTableName);
                            put(rsc, charsetName);
                            break;
                        case TABLECSV:
                            Csv cSV = getDb().newStarCsv(getCsvTableName(), csvSeparator);
                            String csvTable = cSV.tableCSV();
                            put(csvTable, charsetName);
                            cSV.close();
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
