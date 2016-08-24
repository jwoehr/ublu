Ublu as compiled and set up in the source tree supports IBM i database and
PostgreSQL database.

It can also support Microsoft's MSSQL [TM]. However, the distribution of the
JDBC driver for MSSQL carries a sufficiently complicated license that I have
chosen not to redistribute it myself.

If you want MSSQL support:

	1. Download the MSSQL JDBC driver from Microsoft Developer Network. As of
	this writing, the url is:
	https://www.microsoft.com/en-us/download/details.aspx?displaylang=en&id=11774

	2. Copy the jdbc driver jar, e.g., sqljdbc42.jar to Ublu's lib directory.

	3. Use NetBeans project properties to add the the jar to the libraries sourced
	by the build. Alternatively, you can add it to nbproject/project.properties
	at the end of the classpath property:
		javac.classpath=\
		${file.reference.jt400.jar}:\
		${file.reference.postgresql-9.4.1208.jre6.jar}:\
		${file.reference.jtopenlite.jar}:\
		${file.reference.pigiron.jar}:\
		${file.reference.tn5250j.jar}:\
		${file.reference.sqljdbc42.jar} # <<<
		
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
