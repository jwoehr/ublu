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
import ublu.util.Generics.ColumnTypeList;
import ublu.util.Generics.PrimaryKeyList;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to replicate table structure
 *
 * @author jwoehr
 */
public class TableReplicator extends DbHelper {

    // only used in debugging
    // all methods throw to their callers
    private static Logger getLogger() {
        return Ublu.getMainInterpreter().getLogger();
    }
    // Postgres won't allow more than 32 fields in primary key
    private static final int maxPrimaryKeyFields = 32;
    /**
     * The target database instance
     */
    protected Db destDb;
    /**
     * Name of table to replicate structure
     */
    protected String tableName;
    /**
     *
     */
    /*  protected TableNameList srcTableNameList;*/
    /**
     * List of int jdbc types of columns
     */
    protected ColumnTypeList srcColumnTypeList;
    /**
     *
     * @return
     */
    /*  public TableNameList getSrcTableNameList() {
     * return srcTableNameList;
     * }*/
    /**
     *
     * @param tableNameList
     */
    /*   public void setSrcTableNameList(TableNameList tableNameList) {
     * this.srcTableNameList = tableNameList;
     * }*/
    private PrimaryKeyList primaryKeyList;

    /**
     * Get the list of int jdbc types if it has been instanced
     *
     * @return the list of int jdbc types if it has been instanced or null
     */
    protected ColumnTypeList getSrcColumnTypeList() {
        return srcColumnTypeList;
    }

    /**
     * Set the list of int jdbc types
     *
     * @param columnTypeList the list of int jdbc types
     */
    protected void setSrcColumnTypeList(ColumnTypeList columnTypeList) {
        this.srcColumnTypeList = columnTypeList;
    }

    /**
     *
     * @return
     */
    protected PrimaryKeyList getPrimaryKeyList() {
        return primaryKeyList;
    }

    /**
     *
     * @param primaryKeyList
     */
    protected final void setPrimaryKeyList(PrimaryKeyList primaryKeyList) {
        this.primaryKeyList = primaryKeyList;
    }

    /**
     * Get the destination db instance
     *
     * @return the destination db instance
     */
    public Db getDestDb() {
        return destDb;
    }

    /**
     * Set the destination db instance
     *
     * @param destDb the destination db instance
     */
    public final void setDestDb(Db destDb) {
        this.destDb = destDb;
    }

    /**
     * Get name of table whose structure to replicate
     *
     * @return name of table whose structure to replicate
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Set name of table whose structure to replicate
     *
     * @param destTableName name of table whose structure to replicate
     */
    public final void setTableName(String destTableName) {
        this.tableName = destTableName;
    }

    /**
     * Not used
     */
    protected TableReplicator() {
    }

    /**
     * Instance with the important factors set
     *
     * @param srcDb source database instance
     * @param tableName target table
     * @param resultSet result set SELECT * FROM source
     * @param resultSetMetaData metadata for the result set
     * @param destDb destination database instance
     * @throws SQLException
     */
    public TableReplicator(Db srcDb, ResultSet resultSet, ResultSetMetaData resultSetMetaData, Db destDb, String tableName) throws SQLException {
        super(srcDb, resultSet, resultSetMetaData);
        setDestDb(destDb);
        setTableName(tableName);
        setPrimaryKeyList(new PrimaryKeyList());
    }

    /**
     * Instance with the important factors set plus a primary key list
     *
     * @param srcDb source database instance
     * @param tableName target table
     * @param resultSet result set SELECT * FROM source
     * @param resultSetMetaData metadata for the result set
     * @param destDb destination database instance
     * @param primaryKeyList a list of primary keys for table creation
     * @throws SQLException
     */
    public TableReplicator(Db srcDb, ResultSet resultSet, ResultSetMetaData resultSetMetaData, Db destDb, String tableName, PrimaryKeyList primaryKeyList) throws SQLException {
        this(srcDb, resultSet, resultSetMetaData, destDb, tableName);
        setPrimaryKeyList(primaryKeyList);
    }

    /**
     * Set up a table replicator from the minimum.
     * <p>
     * Generates its own result set and metadata </p>
     * <p>
     * Will either fetch the primary key from the source or generate an
     * arbitrary primary key.</p>
     *
     * @param tableName target table
     * @param srcdB source db
     * @param destDb dest db
     * @return the new instance ready to rock and roll
     * @throws SQLException
     */
    public static TableReplicator newTableReplicator(Db srcdB, Db destDb, String tableName) throws SQLException {
        ResultSet rs = srcdB.selectStarFrom(tableName, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSetMetaData rsmd = rs.getMetaData();
        TableReplicator tr = new TableReplicator(srcdB, rs, rsmd, destDb, tableName);
        return tr;
    }

    /**
     * Get a TableNameList of the tables
     *
     * <p>
     * Assumes {@link fetchTablesResultSet} was already executed.</p>
     *
     * @throws SQLException
     */
    /*  protected void fetchSrcTablesNameList() throws SQLException {
     * TableNameList tableNameList = null;
     * ResultSet rs = getTablesResultSet();
     * if (rs != null) {
     * tableNameList = generateTableNameList(rs);
     * }
     * setSrcTableNameList(tableNameList);
     * }*/
    /**
     * Create the list of in jdbc types for all the columns
     *
     * @throws SQLException
     */
    protected void fetchSrcColumnTypeList() throws SQLException {
        setSrcColumnTypeList(generateColumnTypeList());
    }

    /**
     * Create a primary key from the list provided, or if none provided,
     * generate one consisting of all columns (up to 32) to keep Postgres happy
     * in those instances where we cannot derive a primary key from the source
     * table.
     *
     * @return String SQL for primary key
     * @throws SQLException
     */
    protected String generatePrimaryKey() throws SQLException {
        StringBuilder sb = new StringBuilder("PRIMARY KEY (");
        PrimaryKeyList pkl = getPrimaryKeyList();
        if (!pkl.isEmpty()) {
            for (int i = 0; i < pkl.size(); i++) {
                sb.append(pkl.get(i));
                if (i < (pkl.size() - 1)) {
                    sb.append(", ");
                }
            }
        } else {
            ResultSetMetaData rsmd = getResultSetMetaData();
            int numPrimaryKeyFields = Math.min(maxPrimaryKeyFields, rsmd.getColumnCount());
            for (int i = 1; i <= numPrimaryKeyFields; i++) { // columns are one's-based
                sb.append(rsmd.getColumnName(i));
                if (i < numPrimaryKeyFields) {
                    sb.append(", ");
                }
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Generate the columns portion of the table creation SQL.
     * <p>
     * Assumes {@link #fetchSrcColumnTypeList() } has already been executed.</p>
     *
     * @return the columns portion of the table creation SQL.
     * @throws SQLException
     */
    protected String generateColumnSQL() throws SQLException {
        StringBuilder sb = new StringBuilder();
        ResultSetMetaData rsmd = getResultSetMetaData();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) { // columns are one's-based
            if (i != 1) {
                sb.append(", ");
            }
            sb.append(rsmd.getColumnName(i).replace('#', '_'))
                    .append(" ");
            switch (getDestDb().getDbType()) {
                case AS400:
                    sb.append(getSrcColumnTypeList().getColumnSQLType(i - 1)); // but this list is zeroes-based!
                    break;
                case PGSQL:
                    String pgDataTypeName = getSrcColumnTypeList().getColumnPostgresqlType(i - 1); // but this list is zeroes-based!
                    if (pgDataTypeName.equals("integer") && rsmd.getPrecision(i) > 0) {
                        pgDataTypeName = "bigint";
                    }
                    sb.append(pgDataTypeName);
                    break;
                case MSSQL:
                    sb.append(getSrcColumnTypeList().getColumnSQLType(i - 1)); // but this list is zeroes-based!
            }
            // if (getDestDb().getDbType() != Db.DBTYPE.PGSQL) {
            int preciz = rsmd.getPrecision(i);
            if (preciz > 0) {
                // If Postgres, append any precision>0 UNLESS type is among the following
                // This list is not complete. Probably should be turned around to
                // only append precision for small set of types like character and numeric
                // which accept precision.
                if (getDestDb().getDbType() == Db.DBTYPE.PGSQL) {
                    String pgDataTypeName = getSrcColumnTypeList().getColumnPostgresqlType(i - 1);
                    switch (pgDataTypeName) {
                        case "integer": // no precision for this datatype in Postgres
                            break;
                        case "bigint": // no precision for this datatype in Postgres
                            break;
                        case "smallint": // no precision for this datatype in Postgres
                            break;
                        case "text": // no precision for this datatype in Postgres
                            break;
                        case "bytea": // no precision for this datatype in Postgres
                            break;
                        default: // other types get precision
                            sb.append("(").append(preciz).append(")");
                    }
                } else { // For databases other than Postgres, always append precision if non-zero
                    // At least, we think that's how it works, until we have tested more!!!
                    sb.append("(").append(preciz).append(")");
                }
            }
            if (Db.isNumeric(rsmd.getColumnType(i))) {
                if (!rsmd.isSigned(i)) {
                    sb.append(" UNSIGNED");
                }
            }
            if (rsmd.isNullable(i) == ResultSetMetaData.columnNoNulls) {
                sb.append(" NOT NULL");
            } else {
                sb.append(" NULL");
            }
            if (rsmd.isAutoIncrement(i)) {
                sb.append(" auto_increment");
            }
        }
        ResultSet pk = getDatabaseMetaData().getPrimaryKeys(null, null, getTableName());
        boolean first = true;
        while (pk.next()) {
            {
                if (first) {
                    first = false;
                    sb.append(", ");
                    sb.append("PRIMARY KEY(");
                } else {
                    sb.append(" , ");
                }
                sb.append(pk.getString("COLUMN_NAME"));
            }
        }
        if (!first) { // if not-first then we replicated a primary key
            sb.append(')');
        } else { // otherwise, we have to generate one
            sb.append(", ")
                    .append(generatePrimaryKey());
        }
        return sb.toString();
    }

    /**
     * Manufacture all the SQL to create the new target table.
     *
     * @return SQL string
     * @throws SQLException
     */
    public String createDestTableSQL() throws SQLException {
        StringBuilder sb = new StringBuilder();
        fetchSrcColumnTypeList();
        sb.append("CREATE TABLE ")
                .append("\"")
                .append(getTableName())
                .append("\"")
                .append(" ( ")
                .append(generateColumnSQL())
                .append(" )");
        return sb.toString();
    }

    /**
     * Create the target table which replicates as best we can the source table.
     *
     * @throws SQLException
     */
    public void replicate() throws SQLException {
        // String types[] = {"TABLE"};
        // fetchSrcTablesResultSet(null, null, "", types);
        // fetchSrcTableNameList();
        // fetchSrcColumnList();
        String destTableSQL = createDestTableSQL();
        /* Debug */ getLogger().log(Level.INFO, "Table creation SQL is {0}", destTableSQL);
        Statement s = getDestDb().createStatement();
        s.execute(destTableSQL);
    }

    /**
     * Test routine
     *
     * @param args
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        if (args.length != 11) {
            System.err.println(" args are: srcSystem srcDbName srcDbType srcUserId srcPasswd destSystem destDbName destDbType destUserId destPasswd tableName");
            System.exit(1);
        }

        String srcSystem = args[0];
        String srcDbName = args[1];
        String srcDbType = args[2];
        String srcUserId = args[3];
        String srcPasswd = args[4];
        String destSystem = args[5];
        String destDbName = args[6];
        String destDbType = args[7];
        String destUserId = args[8];
        String destPasswd = args[9];
        String tableName = args[10];

        Db srcDb = null;
        switch (srcDbType) {
            case "as400":
                srcDb = new DbAS400();
                break;
            case "postgres":
                srcDb = new DbPostgres();
                break;
        }

        Db destDb = null;
        switch (destDbType) {
            case "as400":
                destDb = new DbAS400();
                break;
            case "postgres":
                destDb = new DbPostgres();
                break;
        }
        if (srcDb == null || destDb == null) {
            System.err.println("Couldn't instance one or both databases");
            System.exit(1);
        }
        if (srcDb != null && destDb != null) {
            srcDb.connect(srcSystem, null, srcDbName, null, srcUserId, srcPasswd);
            destDb.connect(destSystem, null, destDbName, null, destUserId, destPasswd);
        }
        TableReplicator tr = newTableReplicator(srcDb, destDb, tableName);
        tr.replicate();
        // tr.closeAll();
    }
}
