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

import ublu.util.Generics;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents a Microsoft SQL Server database
 *
 * @author jwoehr
 */
public class DbMSSQL extends Db {

    /**
     * MSSQL default port
     */
    public static String MSSQL_DEFAULT_PORT = "1433";

    /**
     * Instance a representation of a Microsoft SQL Server database
     */
    public DbMSSQL() {
        super();
        this.dbType = DBTYPE.MSSQL;
    }

    @Override
    public Connection connect(String system, String port, String database, Generics.ConnectionProperties connectionProperties, String userid, String password)
            throws ClassNotFoundException, SQLException {
        loadDriver();
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setUser(userid);
        ds.setPassword(password);
        ds.setServerName(system);
        ds.setPortNumber(Integer.parseInt(port == null ? MSSQL_DEFAULT_PORT : port));
        ds.setDatabaseName(database);
        setConnection(ds.getConnection());
        return getConnection();
    }
}
