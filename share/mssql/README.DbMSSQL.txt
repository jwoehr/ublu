2018-11-11

Ublu now by default compiles in Microsoft MSSQL [TM] support.

The MSSQL JDBC driver is not included in the distribution. As of this writing,
the download url is:

    https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server?view=sql-server-2017

Ublu did not use to compile in MSSQL support by default. The instructions below
starting at ADDING IN MSSQL SUPPORT describe how to add that support in. You
do not have to do this now, but if you want to use MSSQL support, you must
download the driver on your own and put it in the lib directory under 
directory where ublu.jar is located.

This means:
    1. As built and distributed, if you have downloaded on your own the MSSQL
       JDBC driver, the db command will work for -dbtype mssql.
    2. As built and distributed, if you have *not* downloaded on your own the MSSQL
       JDBC driver, the db command will fail for -dbtype mssql.
    3. To build Ublu from soure, you must either:
        a. Comment out MSSQL support, or
        b. Download the MSSQL JDBC driver (any recent level) and place in the
        lib directory of the source distribution. You will need to modify the
        lines referring to this jar in nbproject/project.properties if you
        are using a different level of the driver than the one we are using.

Therefor you may wish to execute the REMOVING MSSQL SUPPORT instructions in order
to remove MSSQL support if you do not want to download the driver.

REMOVING MSSQL SUPPORT

1. Use NetBeans project properties to remove the jar from the libraries sourced
	by the build. Alternatively, you can remove it to nbproject/project.properties
	at the end of the classpath property:

javac.classpath=\
    ${file.reference.jt400.jar}:\
    ${file.reference.pigiron.jar}:\
    ${file.reference.tn5250j.jar}:\
    ${file.reference.sblim-cim-client2-HEAD.jar}:\
    ${file.reference.postgresql-42.2.5.jre7.jar}:\
    ${file.reference.mssql-jdbc-7.0.0.jre8.jar} # <<< REMOVE

    Also remove the line:

file.reference.mssql-jdbc-7.0.0.jre8.jar=lib/mssql-jdbc-7.0.0.jre8.jar

2. Remove the file DbMSSQL.java from the directory src/ublu/db

3. Edit src/ublu/command/CmdDb.java and comment out the lines between each
   "/* Uncomment the following if you are adding MSSQL support */" and
   "/* End Uncomment */"

4. Build and run.

( Old instructions for adding in MSSQL Support )
ADDING IN MSSQL SUPPORT
Ublu as compiled and set up in the source tree supports IBM i database and
PostgreSQL database.

It can also support Microsoft's MSSQL [TM]. However, the distribution of the
JDBC driver for MSSQL carries a sufficiently complicated license that I have
chosen not to redistribute it myself.

If you want MSSQL support:

	1. Download the MSSQL JDBC driver from Microsoft Developer Network. As of
	this writing, the url is:
	https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server?view=sql-server-2017

	2. Copy the jdbc driver jar, e.g., mssql-jdbc-7.0.0.jre8.jar to Ublu's lib directory.

	3. Use NetBeans project properties to add the jar to the libraries sourced
	by the build. Alternatively, you can add it to nbproject/project.properties
	at the end of the classpath property:

javac.classpath=\
    ${file.reference.jt400.jar}:\
    ${file.reference.pigiron.jar}:\
    ${file.reference.tn5250j.jar}:\
    ${file.reference.sblim-cim-client2-HEAD.jar}:\
    ${file.reference.postgresql-42.2.5.jre7.jar}:\
    ${file.reference.mssql-jdbc-7.0.0.jre8.jar} # <<< ADD
		
	** NOTE that if you choose to download and use Microsoft's MSSQL JDBC driver
	in Ublu, then any and all licensing requirements are Your Responsiblity. **

	4. Copy the file DbMSSQL.java from this directory to src/ublu/db

	5. Edit src/ublu/command/CmdDb.java and uncomment the lines between each
	   "/* Uncomment the following if you are adding MSSQL support */" and
	   "/* End Uncomment */"

	6. Build and run.

As usual, there is NO WARRANTY nor GUARANTEE of correctness nor fitness etc.
a la Open Source always!!

Jack Woehr
2016-08-24
