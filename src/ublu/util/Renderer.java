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
package ublu.util;

import ublu.db.ResultSetClosure;
import ublu.db.ResultSetFormatter;
import ublu.util.Generics.AS400MessageList;
import ublu.util.Generics.ColumnTypeList;
import ublu.util.Generics.ColumnTypeNameList;
import ublu.util.Generics.QueuedMessageKey;
import ublu.util.Generics.QueuedMessageList;
import ublu.util.Generics.SpooledFileArrayList;
import ublu.util.Generics.StringArrayList;
import ublu.util.Generics.SubsystemArrayList;
import ublu.util.Generics.UserArrayList;
import ublu.util.SystemHelper.ProcessClosure;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.FieldDescription;
import com.ibm.as400.access.HistoryLog;
import com.ibm.as400.access.JobList;
import com.ibm.as400.access.MemberDescription;
import com.ibm.as400.access.MemberList;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ObjectLockListEntry;
import com.ibm.as400.access.OutputQueue;
import com.ibm.as400.access.QueuedMessage;
import com.ibm.as400.access.RecordFormat;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SaveFileEntry;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.Subsystem;
import com.ibm.as400.access.User;
import com.softwoehr.pigiron.access.ParameterArray;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to render data as string
 *
 * @author jwoehr
 */
public class Renderer {

    /**
     * Charset name for conversion
     */
    public String charsetName;

    /**
     * Get source charset name
     *
     * @return source charset name
     */
    public String getCharsetName() {
        return charsetName;
    }

    /**
     * Set source charset name
     *
     * @param charsetName source charset name
     */
    public final void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }
    private Object object;

    /**
     * Get the object we are rendering
     *
     * @return the object we are rendering
     */
    public Object getObject() {
        return object;
    }

    /**
     * Set the object we are rendering
     *
     * @param object the object we are rendering
     */
    public final void setObject(Object object) {
        this.object = object;
    }

    /**
     * Instance with an object to render
     *
     * @param object object to render
     */
    public Renderer(Object object) {
        setObject(object);
        setCharsetName("ASCII");
    }

    /**
     * Instance with an object to render and a charset name
     *
     * @param object object to render
     * @param charsetName charset name
     */
    public Renderer(Object object, String charsetName) {
        this(object);
        setCharsetName(charsetName);
    }

    /**
     * Render any object in Ublu as a string since we're object-disoriented
     *
     * @return object as an appropriate string representation
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws InterruptedException
     * @throws ErrorCompletingRequestException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     */
    public String asString() throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        Object theObject = getObject();
        String s = "Unable to render object as string.";
        if (theObject instanceof ParameterArray) {
            s = stringFrom(ParameterArray.class.cast(theObject));
        } else if (theObject instanceof HistoryLog) {
            s = stringFrom(HistoryLog.class.cast(theObject));
        } else if (theObject instanceof AS400MessageList) {
            s = stringFrom(AS400MessageList.class.cast(theObject));
        } else if (theObject instanceof User) {
            s = stringFrom(User.class.cast(theObject));
        } else if (theObject instanceof UserArrayList) {
            s = stringFrom(UserArrayList.class.cast(theObject));
        } else if (theObject instanceof DatabaseMetaData) {
            s = stringFrom(DatabaseMetaData.class.cast(theObject));
        } else if (theObject instanceof ResultSetClosure) {
            ResultSetClosure rsc = ResultSetClosure.class.cast(theObject);
            s = stringFrom(rsc.getResultSet());
        } else if (theObject instanceof JobList) {
            JobList jl = JobList.class.cast(theObject);
            s = stringFrom(jl);
        } else if (theObject instanceof ResultSet) {
            s = stringFrom(ResultSet.class.cast(theObject));
        } else if (theObject instanceof OutputQueue) {
            s = stringFrom(OutputQueue.class.cast(theObject));
        } else if (theObject instanceof QueuedMessage) {
            QueuedMessage qm = QueuedMessage.class.cast(theObject);
            s = stringFrom(qm);
        } else if (theObject instanceof QueuedMessageList) {
            QueuedMessageList queuedMessageList = QueuedMessageList.class.cast(theObject);
            s = stringFrom(queuedMessageList);
        } else if (theObject instanceof ResultSetFormatter) {
            s = stringFrom(ResultSetFormatter.class.cast(theObject));
        } else if (theObject instanceof ProcessClosure) {
            s = stringFrom(ProcessClosure.class.cast(theObject));
        } else if (theObject instanceof ColumnTypeNameList) {
            s = stringFrom(ColumnTypeNameList.class.cast(theObject));
        } else if (theObject instanceof ResultSetMetaData) {
            s = stringFrom(ResultSetMetaData.class.cast(theObject));
        } else if (theObject instanceof SaveFileEntry) {
            s = stringFrom(SaveFileEntry.class.cast(theObject));
        } else if (theObject instanceof SaveFileEntry[]) {
            s = stringFrom(SaveFileEntry[].class.cast(theObject));
        } else if (theObject instanceof StringArrayList) {
            s = stringFrom(StringArrayList.class.cast(theObject));
        } else if (theObject instanceof SpooledFile) {
            s = stringFrom(SpooledFile.class.cast(theObject));
        } else if (theObject instanceof SpooledFileArrayList) {
            s = stringFrom(SpooledFileArrayList.class.cast(theObject));
        } else if (theObject instanceof DataQueueEntry) {
            s = stringFrom(DataQueueEntry.class.cast(theObject));
        } else if (theObject instanceof ObjectLockListEntry) {
            s = stringFrom(ObjectLockListEntry.class.cast(theObject));
        } else if (theObject instanceof String[]) {
            s = stringFrom(String[].class.cast(theObject));
        } else if (theObject instanceof Subsystem) {
            s = stringFrom(Subsystem.class.cast(theObject));
        } else if (theObject instanceof SubsystemArrayList) {
            s = stringFrom(SubsystemArrayList.class.cast(theObject));
        } else if (theObject instanceof MemberList) {
            s = stringFrom(MemberList.class.cast(theObject));
        } else if (theObject instanceof RecordFormat) {
            s = stringFrom(RecordFormat.class.cast(theObject));
        } else if (theObject instanceof FieldDescription) {
            s = stringFrom(FieldDescription.class.cast(theObject));
        } else if (theObject instanceof byte[]) {
            s = stringFrom(byte[].class.cast(theObject));
        } else if (theObject instanceof Byte[]) {
            s = stringFrom(Byte[].class.cast(theObject));
        } else if (theObject instanceof Object) {
            s = theObject.toString();
        } else if (theObject == null) {
            s = "null";
        }
        return s;
    }

    /**
     * Render a byte array
     *
     * @param bytes the byte array to render
     * @return string representation
     */
    public String stringFrom(byte[] bytes) {
        StringBuilder sb = new StringBuilder("[ ");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)).append(' ');
        }
        sb.append("]").append(" ( ").append(bytes).append(" )");
        return sb.toString();
    }

    /**
     * Render a B(capital-b)yte array
     *
     * @param bytes
     * @return string representation
     */
    public String stringFrom(Byte[] bytes) {
        StringBuilder sb = new StringBuilder("[ ");
        for (Byte b : bytes) {
            sb.append(String.format("%02x", b.byteValue()));
        }
        sb.append("]").append(" ( ").append(bytes).append(" )");
        return sb.toString();
    }

    /**
     * Render an AS400MessageList
     *
     * @param aS400MessageList
     * @return string representation
     */
    public String stringFrom(AS400MessageList aS400MessageList) {
        StringBuilder sb = new StringBuilder();
        for (AS400Message aS400Message : aS400MessageList) {
            sb.append(aS400Message).append("\n");
        }
        return sb.toString();
    }

    /**
     * Render a result set
     *
     * @param resultSet
     * @return string representation
     */
    public String stringFrom(ResultSet resultSet) {
        String result = "";
        try {
            result = new ResultSetFormatter(resultSet, getCharsetName()).format();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Renderer.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    /**
     * Render a result set formatter
     *
     * @param resultSetFormatter
     * @return string representation
     * @throws SQLException
     * @throws IOException
     */
    public String stringFrom(ResultSetFormatter resultSetFormatter) throws SQLException, IOException {
        return resultSetFormatter.format();
    }

    /**
     * Render a queued message
     *
     * @param qm
     * @return string representation
     */
    public String stringFrom(QueuedMessage qm) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ");
        sb.append(qm.getID());
        sb.append(" | Key: ");
        sb.append(new QueuedMessageKey(qm.getKey()));
        sb.append(" | Severity: ");
        sb.append(qm.getSeverity());
        sb.append(" | From Job: ");
        sb.append(qm.getFromJobName());
        sb.append(" | From Job Number: ");
        sb.append(qm.getFromJobNumber());
        sb.append(" | From Program: ");
        sb.append(qm.getFromProgram());
        sb.append(" | Date: ");
        sb.append(qm.getDate().getTime());
        sb.append(" | Sending User: ");
        sb.append(qm.getUser());
        sb.append(" | Message Help: ");
        sb.append(qm.getMessageHelp());
        sb.append(" | Text: ");
        sb.append(qm.getText());
        return sb.toString();
    }

    /**
     * Render a QueuedMessageList
     *
     * @param queuedMessageList
     * @return string representation
     */
    public String stringFrom(QueuedMessageList queuedMessageList) {
        StringBuilder sb = new StringBuilder();
        for (QueuedMessage qm : queuedMessageList) {
            sb.append(stringFrom(qm));
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Render db metadata
     *
     * @param dbMetaData
     * @return string representation
     * @throws SQLException
     */
    public String stringFrom(DatabaseMetaData dbMetaData) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("DBMS URL: ").append(dbMetaData.getURL()).append("\n");
        sb.append("Database Product: ")
                .append(dbMetaData.getDatabaseProductName())
                .append(" Database Major Version: ")
                .append(dbMetaData.getDatabaseMajorVersion())
                .append(" Minor Version: ")
                .append(dbMetaData.getDatabaseMinorVersion())
                .append("\n");
        sb.append("--------\n");
        sb.append("Catalogs\n");
        sb.append("--------\n");
        try (ResultSet rs = dbMetaData.getCatalogs()) {
            while (rs.next()) {
                sb.append(rs.getString(1)).append("\n");
            }
        }
        sb.append("--------\n");
        sb.append("Table Types\n");
        sb.append("-----------\n");
        try (ResultSet rs = dbMetaData.getTableTypes()) {
            while (rs.next()) {
                sb.append(rs.getString(1)).append("\n");
            }
        }
        sb.append("-----------\n");
        sb.append("Schemas\n");
        sb.append("-------\n");
        try (ResultSet rs = dbMetaData.getSchemas()) {
            sb.append("TABLE_SCHEMA\tTABLE_CATALOG\n");
            sb.append("-----------\t-------------\n");
            while (rs.next()) {
                sb.append(rs.getString(1)).append("\t").append(rs.getString(2)).append("\n");
            }
        }
        sb.append("-----------\t-------------\n");
        sb.append("All procedures are callable: ").append(dbMetaData.allProceduresAreCallable()).append("\n");
        sb.append("All tables are selectable: ").append(dbMetaData.allTablesAreSelectable()).append("\n");

        return sb.toString();
    }

    /**
     * Render a process closure
     *
     * @param pc
     * @return string representation
     */
    public String stringFrom(ProcessClosure pc) {
        StringBuilder sb = new StringBuilder();
        sb.append(pc.getOutput())
                .append("\n")
                .append("return code: ")
                .append(pc.getRc());
        return sb.toString();
    }

    /**
     * Render a ColumnTypeNameList
     *
     * @param ctnl the ColumnTypeNameList
     * @return string representation
     */
    public String stringFrom(ColumnTypeNameList ctnl) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ctnl.size(); i++) {
            sb.append(ctnl.get(i));
            if (i < ctnl.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Render Result Set Metadata
     *
     * @param rsmd
     * @return string representation
     * @throws SQLException
     */
    public String stringFrom(ResultSetMetaData rsmd) throws SQLException {
        StringBuilder sb = new StringBuilder();
        int columnCount = rsmd.getColumnCount();
        sb.append("Catalog name by column:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getCatalogName(i)).append("\n");
        }
        sb.append("---------------------\n");
        sb.append("Class name by column:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getColumnClassName(i)).append("\n");
        }
        sb.append("------------\n");
        sb.append("Column name:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getColumnName(i)).append("\n");
        }
        sb.append("-------------\n");
        sb.append("Column label:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getColumnLabel(i)).append("\n");
        }
        sb.append("------------\n");
        sb.append("Column type:\n");
        ColumnTypeList ctl = new Generics.ColumnTypeList();
        for (int i = 1; i <= columnCount; i++) {
            int type = rsmd.getColumnType(i);
            ctl.add(type);
            sb.append(i).append(". ")
                    .append("JDBC type ")
                    .append(type).append("\t")
                    .append(ctl.getColumnSQLType(i - 1))
                    .append("\n");
        }
        sb.append("------------------------------\n");
        sb.append("Column database-specific type:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i)
                    .append(". ")
                    .append(rsmd.getColumnTypeName(i))
                    .append("\n");
        }
        sb.append("------------\n");
        sb.append("Column size:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getPrecision(i)).append("\n");
        }
        sb.append("--------------------\n");
        sb.append("Column display size:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getColumnDisplaySize(i)).append("\n");
        }
        sb.append("-------------\n");
        sb.append("Column scale:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getScale(i)).append("\n");
        }
        sb.append("-------------------\n");
        sb.append("Column schema name:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getSchemaName(i)).append("\n");
        }
        sb.append("------------------\n");
        sb.append("Column table name:\n");
        for (int i = 1; i <= columnCount; i++) {
            sb.append(i).append(". ").append(rsmd.getTableName(i)).append("\n");
        }

        /* more to do ..... */
 /*
         * String 	getCatalogName(int column)
         Gets the designated column's table's catalog name.
         String 	getColumnClassName(int column)
         Returns the fully-qualified name of the Java class whose instances are manufactured if the method ResultSet.getObject is called to retrieve a value from the column.
         int 	getColumnCount()
         Returns the number of columns in this ResultSet object.
         int 	getColumnDisplaySize(int column)
         Indicates the designated column's normal maximum width in characters.
         String 	getColumnLabel(int column)
         Gets the designated column's suggested title for use in printouts and displays.
         String 	getColumnName(int column)
         Get the designated column's name.
         int 	getColumnType(int column)
         Retrieves the designated column's SQL type.
         String 	getColumnTypeName(int column)
         Retrieves the designated column's database-specific type name.
         int 	getPrecision(int column)
         Get the designated column's specified column size.
         int 	getScale(int column)
         Gets the designated column's number of digits to right of the decimal point.
         String 	getSchemaName(int column)
         Get the designated column's table's schema.
         String 	getTableName(int column)
         Gets the designated column's table name.
         boolean 	isAutoIncrement(int column)
         Indicates whether the designated column is automatically numbered.
         boolean 	isCaseSensitive(int column)
         Indicates whether a column's case matters.
         boolean 	isCurrency(int column)
         Indicates whether the designated column is a cash value.
         boolean 	isDefinitelyWritable(int column)
         Indicates whether a write on the designated column will definitely succeed.
         int 	isNullable(int column)
         Indicates the nullability of values in the designated column.
         boolean 	isReadOnly(int column)
         Indicates whether the designated column is definitely not writable.
         boolean 	isSearchable(int column)
         Indicates whether the designated column can be used in a where clause.
         boolean 	isSigned(int column)
         Indicates whether values in the designated column are signed numbers.
         boolean 	isWritable(int column)
         Indicates whether it is possible for a write on the designated column to succeed.
         * 
         * 
         */
        return sb.toString();
    }

    /**
     * Render Save File Entry
     *
     * @param sfe Save File Entry
     * @return String representing Save File Entry
     */
    public String stringFrom(SaveFileEntry sfe) {
        StringBuilder sb = new StringBuilder();
        sb.append(sfe.getLibrary()).append('/').append(sfe.getName()).append(sfe.getType().replace('*', '.')).append(" ").append(sfe.getSize()).append("\n");
        sb.append(sfe.getSaveDate()).append("\n");
        sb.append(sfe.getExtendedObjectAttribute()).append("\n");
        sb.append(sfe.getASPDevice()).append(":").append(sfe.getASP()).append("\n");
        sb.append(sfe.getDescription()).append("\n");
        sb.append(sfe.getFolder()).append(" ").append(sfe.getDLOName()).append("\n");
        return sb.toString();
    }

    /**
     * Render Save File Entry array
     *
     * @param sfea Save File Entry array
     * @return String representing Save File Entry array
     */
    public String stringFrom(SaveFileEntry[] sfea) {
        StringBuilder sb = new StringBuilder();
        for (SaveFileEntry sfe : sfea) {
            sb.append(stringFrom(sfe)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Render a StringArrayList a line at a time
     *
     * @param sal StringArrayList to render
     * @return String rendering
     */
    public String stringFrom(StringArrayList sal) {
        StringBuilder sb = new StringBuilder();
        for (String s : sal) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    /**
     * Render a JobList
     *
     * @param jl the JobList to render
     * @return String rendering
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws ObjectDoesNotExistException
     */
    public String stringFrom(JobList jl) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        Enumeration jobs = jl.getJobs();
        StringBuilder sb = new StringBuilder();
        while (jobs.hasMoreElements()) {
            sb.append(jobs.nextElement());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Render a User
     *
     * @param u the User
     * @return String rendering
     */
    public String stringFrom(User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(u.getName()).append(" | ");
        sb.append("Status: ").append(u.getStatus()).append(" | ");
        sb.append("Description: ").append(u.getDescription()).append(" | ");
        sb.append("User Class Name: ").append(u.getUserClassName()).append(" | ");
        sb.append("Limited capabilities: ").append(u.getLimitCapabilities()).append(" | ");
        sb.append("User Id: ").append(u.getUserID()).append(" | ");
        sb.append("Group Profile: ").append(u.getGroupProfileName()).append(" | ");
        sb.append("Group Id: ").append(u.getGroupID()).append(" | ");
        sb.append("Group Authority: ").append(u.getGroupAuthority()).append(" | ");
        sb.append("Group Authority Type: ").append(u.getGroupAuthorityType()).append(" | ");
        sb.append("Supplemental Groups: ").append(new StringArrayList(u.getSupplementalGroups())).append(" | ");
        sb.append("Special Authority: ").append(new StringArrayList(u.getSpecialAuthority())).append(" | ");
        sb.append("Highest Scheduling Priority: ").append(u.getHighestSchedulingPriority()).append(" | ");
        sb.append("Current Library: ").append(u.getCurrentLibraryName()).append(" | ");
        sb.append("Home Directory: ").append(u.getHomeDirectory()).append(" | ");
        sb.append("Days until password expires: ").append(u.getDaysUntilPasswordExpire()).append(" | ");
        sb.append("Password Last Changed: ").append(u.getPasswordLastChangedDate()).append(" | ");
        sb.append("Display SignOn Information: ").append(u.getDisplaySignOnInformation()).append(" | ");
        sb.append("Initial menu: ").append(u.getInitialMenu()).append(" | ");
        sb.append("Initial program: ").append(u.getInitialProgram()).append(" | ");
        sb.append("Job Description: ").append(u.getJobDescription()).append(" | ");
        sb.append("Message Queue: ").append(u.getMessageQueue()).append(" | ");
        sb.append("Message Queue Severity: ").append(u.getMessageQueueSeverity()).append(" | ");
        sb.append("Message Queue Delivery Method: ").append(u.getMessageQueueDeliveryMethod()).append(" | ");
        sb.append("Output Queue: ").append(u.getOutputQueue()).append(" | ");
        sb.append("Special Environment: ").append(u.getSpecialEnvironment()).append(" | ");
        sb.append("Maximum Storage Allowed: ").append(u.getMaximumStorageAllowed()).append(" | ");
        sb.append("Assistance Level: ").append(u.getAssistanceLevel()).append(" | ");
        sb.append("Accounting Code: ").append(u.getAccountingCode()).append(" | ");
        sb.append("Attention Key Handling Program: ").append(u.getAttentionKeyHandlingProgram()).append(" | ");
        sb.append("Country ID: ").append(u.getCountryID()).append(" | ");
        sb.append("Language ID: ").append(u.getLanguageID()).append(" | ");
        sb.append("Print Device: ").append(u.getPrintDevice())/* .append(" | ")*/;
        return sb.toString();
    }

    /**
     * Make an output string from a user array list returned from userlist
     * command.
     *
     * @param ual returned from userlist command
     * @return String representation thereof
     */
    public String stringFrom(UserArrayList ual) {
        StringBuilder sb = new StringBuilder();
        for (User u : ual) {
            sb.append(stringFrom(u)).append('\n');
        }

        return sb.substring(0, sb.length() - 1); // remove last '\n'
    }

    /**
     * Make an output string from MemberList
     *
     * @param ml MemberList from file command
     * @return String representation thereof
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws AS400Exception
     * @throws IOException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     */
    public String stringFrom(MemberList ml) throws AS400SecurityException, ErrorCompletingRequestException, AS400Exception, IOException, InterruptedException, ObjectDoesNotExistException {
        StringBuilder sb = new StringBuilder();
        for (MemberDescription md : ml.getMemberDescriptions()) {
            sb.append(stringFrom(md)).append('\n');
        }
        return sb.toString();
    }

    /**
     * Make an output string from RecordFormat
     *
     * @param rf RecordFormat from file command
     * @return String representation thereof
     */
    public String stringFrom(RecordFormat rf) {
        StringBuilder sb = new StringBuilder();
        String[] fieldNames = rf.getFieldNames();
        for (String fieldName : fieldNames) {
            sb.append(rf.getIndexOfFieldName(fieldName))
                    .append('\t')
                    .append(fieldName)
                    .append('\t')
                    .append(stringFrom(rf.getFieldDescription(fieldName)))
                    .append('\n');
        }
        return sb.toString();
    }

    /**
     * Make an output string from FieldDescription
     *
     * @param fd FieldDescription
     * @return String representation thereof
     */
    public String stringFrom(FieldDescription fd) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALWNULL: ").append(fd.getALWNULL()).append(";");
        sb.append("COLHDG: ").append(fd.getCOLHDG()).append(";");
        sb.append("DDSName: ").append(fd.getDDSName()).append(";");
        sb.append("DFT: ").append(fd.getDFT()).append(";");
        sb.append("DataType: ").append(fd.getDataType()).append(";");
        sb.append("KeyFieldFunctions: ").append(fd.getKeyFieldFunctions()).append(";");
        sb.append("Length: ").append(fd.getLength()).append(";");
        sb.append("TEXT: ").append(fd.getTEXT()).append(";");
        return sb.toString();
    }

    /**
     * Make an output string from MemberDescription
     *
     * @param md MemberDescription
     * @return String representation thereof
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws AS400Exception
     * @throws IOException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     */
    public String stringFrom(MemberDescription md) throws AS400SecurityException, ErrorCompletingRequestException, AS400Exception, IOException, InterruptedException, ObjectDoesNotExistException {
        StringBuilder sb = new StringBuilder();
        sb.append("ACCESS_PATH_MAINTENANCE:\t").append(md.getValue(MemberDescription.ACCESS_PATH_MAINTENANCE)).append('\n');
        sb.append("ACCESS_PATH_SIZE:\t").append(md.getValue(MemberDescription.ACCESS_PATH_SIZE)).append('\n');
        sb.append("ACCESS_PATH_SIZE_MULTIPLIER:\t").append(md.getValue(MemberDescription.ACCESS_PATH_SIZE_MULTIPLIER)).append('\n');
        sb.append("ALLOW_DELETE_OPERATION:\t").append(md.getValue(MemberDescription.ALLOW_DELETE_OPERATION)).append('\n');
        sb.append("ALLOW_READ_OPERATION:\t").append(md.getValue(MemberDescription.ALLOW_READ_OPERATION)).append('\n');
        sb.append("ALLOW_UPDATE_OPERATION:\t").append(md.getValue(MemberDescription.ALLOW_UPDATE_OPERATION)).append('\n');
        sb.append("ALLOW_WRITE_OPERATION:\t").append(md.getValue(MemberDescription.ALLOW_WRITE_OPERATION)).append('\n');
        sb.append("CHANGE_DATE_AND_TIME:\t").append(md.getValue(MemberDescription.CHANGE_DATE_AND_TIME)).append('\n');
        sb.append("CREATION_DATE_TIME:\t").append(md.getValue(MemberDescription.CREATION_DATE_TIME)).append('\n');
        sb.append("CURRENT_NUMBER_OF_INCREMENTS:\t").append(md.getValue(MemberDescription.CURRENT_NUMBER_OF_INCREMENTS)).append('\n');
        sb.append("CURRENT_NUMBER_OF_RECORDS:\t").append(md.getValue(MemberDescription.CURRENT_NUMBER_OF_RECORDS)).append('\n');
        sb.append("DATA_SPACE_SIZE:\t").append(md.getValue(MemberDescription.DATA_SPACE_SIZE)).append('\n');
        sb.append("DATA_SPACE_SIZE_MULTIPLIER:\t").append(md.getValue(MemberDescription.DATA_SPACE_SIZE_MULTIPLIER)).append('\n');
        sb.append("DATE_LAST_USED:\t").append(md.getValue(MemberDescription.DATE_LAST_USED)).append('\n');
        sb.append("EXPIRATION_DATE:\t").append(md.getValue(MemberDescription.EXPIRATION_DATE)).append('\n');
        sb.append("FILE_ATTRIBUTE:\t").append(md.getValue(MemberDescription.FILE_ATTRIBUTE)).append('\n');
        sb.append("FILE_NAME:\t").append(md.getValue(MemberDescription.FILE_NAME)).append('\n');
        sb.append("INCREMENT_NUMBER_OF_RECORDS:\t").append(md.getValue(MemberDescription.INCREMENT_NUMBER_OF_RECORDS)).append('\n');
        sb.append("INITIAL_NUMBER_OF_RECORDS:\t").append(md.getValue(MemberDescription.INITIAL_NUMBER_OF_RECORDS)).append('\n');
        sb.append("JOIN_MEMBER:\t").append(md.getValue(MemberDescription.JOIN_MEMBER)).append('\n');
        sb.append("LAST_SOURCE_CHANGE_DATE:\t").append(md.getValue(MemberDescription.LAST_SOURCE_CHANGE_DATE)).append('\n');
        sb.append("LIBRARY_NAME:\t").append(md.getValue(MemberDescription.LIBRARY_NAME)).append('\n');
        sb.append("LOGICAL_FILE:\t").append(md.getValue(MemberDescription.LOGICAL_FILE)).append('\n');
        sb.append("MAXIMUM_NUMBER_OF_INCREMENTS:\t").append(md.getValue(MemberDescription.MAXIMUM_NUMBER_OF_INCREMENTS)).append('\n');
        sb.append("MAXIMUM_PERCENT_DELETED_RECORDS_ALLOWED:\t").append(md.getValue(MemberDescription.MAXIMUM_PERCENT_DELETED_RECORDS_ALLOWED)).append('\n');
        sb.append("MEMBER_NAME:\t").append(md.getValue(MemberDescription.MEMBER_NAME)).append('\n');
        sb.append("MEMBER_TEXT_DESCRIPTION:\t").append(md.getValue(MemberDescription.MEMBER_TEXT_DESCRIPTION)).append('\n');
        sb.append("MEMBER_TEXT_DESCRIPTION_CCSID:\t").append(md.getValue(MemberDescription.MEMBER_TEXT_DESCRIPTION_CCSID)).append('\n');
        sb.append("NUMBER_OF_BASED_ON_PHYICAL_FILE_MEMBERS:\t").append(md.getValue(MemberDescription.NUMBER_OF_BASED_ON_PHYICAL_FILE_MEMBERS)).append('\n');
        sb.append("NUMBER_OF_DAYS_USED:\t").append(md.getValue(MemberDescription.NUMBER_OF_DAYS_USED)).append('\n');
        sb.append("NUMBER_OF_DELETED_RECORDS:\t").append(md.getValue(MemberDescription.NUMBER_OF_DELETED_RECORDS)).append('\n');
        sb.append("ODP_SHARING:\t").append(md.getValue(MemberDescription.ODP_SHARING)).append('\n');
        sb.append("RECORD_CAPACITY:\t").append(md.getValue(MemberDescription.RECORD_CAPACITY)).append('\n');
        sb.append("RECORD_FORMAT_SELECTOR_LIBRARY_NAME:\t").append(md.getValue(MemberDescription.RECORD_FORMAT_SELECTOR_LIBRARY_NAME)).append('\n');
        sb.append("RECORD_FORMAT_SELECTOR_PROGRAM_NAME:\t").append(md.getValue(MemberDescription.RECORD_FORMAT_SELECTOR_PROGRAM_NAME)).append('\n');
        sb.append("RECORDS_TO_FORCE_A_WRITE:\t").append(md.getValue(MemberDescription.RECORDS_TO_FORCE_A_WRITE)).append('\n');
        sb.append("REMOTE_FILE:\t").append(md.getValue(MemberDescription.REMOTE_FILE)).append('\n');
        sb.append("RESTORE_DATE_AND_TIME:\t").append(md.getValue(MemberDescription.RESTORE_DATE_AND_TIME)).append('\n');
        sb.append("SAVE_DATE_AND_TIME:\t").append(md.getValue(MemberDescription.SAVE_DATE_AND_TIME)).append('\n');
        sb.append("SOURCE_FILE:\t").append(md.getValue(MemberDescription.SOURCE_FILE)).append('\n');
        sb.append("SOURCE_TYPE:\t").append(md.getValue(MemberDescription.SOURCE_TYPE)).append('\n');
        sb.append("SQL_FILE_TYPE:\t").append(md.getValue(MemberDescription.SQL_FILE_TYPE)).append('\n');
        sb.append("USE_RESET_DATE:\t").append(md.getValue(MemberDescription.USE_RESET_DATE));
        return sb.toString();
    }

// Not used anymore as CmdUserList returns a UserArrayList now.
//    /**
//     * Render a UserList
//     *
//     * @param ul the UserList to render
//     * @return String rendering
//     * @throws AS400SecurityException
//     * @throws ErrorCompletingRequestException
//     * @throws InterruptedException
//     * @throws IOException
//     * @throws ObjectDoesNotExistException
//     * @throws RequestNotSupportedException
//     */
//    public String stringFrom(UserList ul) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException, RequestNotSupportedException {
//        Enumeration users = ul.getUsers();
//        StringBuilder sb = new StringBuilder();
//        while (users.hasMoreElements()) {
//            User user = User.class.cast(users.nextElement());
//            sb.append(stringFrom(user));
//            sb.append("\n");
//        }
//        return sb.toString();
//    }
    /**
     * Make an output string from a spooled file description
     *
     * @param splf a spooled file
     * @return string representation
     */
    public String stringFrom(SpooledFile splf) {
        StringBuilder sb = new StringBuilder();
        sb.append(splf.getName()).append(' ');
        sb.append(splf.getNumber()).append(' ');
        sb.append(splf.getJobName()).append(' ');
        sb.append(splf.getJobUser()).append(' ');
        sb.append(splf.getJobNumber()).append(' ');
        sb.append(splf.getCreateDate()).append(' ');
        sb.append(splf.getCreateTime());
        return sb.toString();
    }

    /**
     * Make an output string of descriptions from an array list of spooled files
     *
     * @param splfal an array list of spooled files
     * @return string representation
     */
    public String stringFrom(SpooledFileArrayList splfal) {
        StringBuilder sb = new StringBuilder();
        for (SpooledFile splf : splfal) {
            sb.append(stringFrom(splf)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Make an output string of descriptions from an array list of spooled files
     *
     * @param dqe an array list of spooled files
     * @return string representation
     * @throws UnsupportedEncodingException
     */
    public String stringFrom(DataQueueEntry dqe) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(dqe.toString())
                .append(" :: ")
                .append(dqe.getSenderInformation())
                .append(" :: ")
                .append(dqe.getString());
        return sb.toString();
    }

    /**
     * Make an output string from a string array
     *
     * @param sa string array
     * @return output string
     */
    public String stringFrom(String[] sa) {
        StringBuilder sb = new StringBuilder("[");
        for (String s : sa) {
            sb.append(s).append(',');
        }
        sb.replace(sb.length() - 1, sb.length(), "]");
        return sb.toString();
    }

    /**
     * Make an output string from a history log
     *
     * @param hl history log
     * @return output string
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws ObjectDoesNotExistException
     */
    public String stringFrom(HistoryLog hl) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        StringBuilder sb = new StringBuilder();
        sb.append(hl.toString()).append('\n');
        sb.append(hl.getSystem()).append(' ').append(hl.getStartingDate()).append(' ').append(hl.getEndingDate()).append('\n');
        Enumeration<QueuedMessage> e = hl.getMessages();
        while (e.hasMoreElements()) {
            QueuedMessage qm = e.nextElement();
            sb.append(stringFrom(qm)).append('\n');
        }
        return sb.toString();
    }

    /**
     * Make an output string from a Subsystem
     *
     * @param ss Subsystem
     * @return output string
     */
    public String stringFrom(Subsystem ss) {
        StringBuilder sb = new StringBuilder();
        sb.append(ss).append(":")
                .append(ss.getSystem().getSystemName()).append('/')
                .append(ss.getName()).append(':')
                .append(ss.getPath()).append(":\"")
                .append(ss.getDescriptionText()).append("\"");
        return sb.toString();
    }

    /**
     * Make an output string from a SubsystemArrayList
     *
     * @param sal SubsystemArrayList
     * @return output string
     */
    public String stringFrom(SubsystemArrayList sal) {
        StringBuilder sb = new StringBuilder();
        for (Subsystem ss : sal) {
            sb.append(stringFrom(ss)).append('\n');
        }
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * Make an output string from a PigIron ParameterArray
     *
     * @param pa PigIron ParameterArray
     * @return string representation
     */
    public String stringFrom(ParameterArray pa) {
        return pa.prettyPrintAll();
    }

    public String stringFrom(ObjectLockListEntry olle) {
        StringBuilder sb = new StringBuilder();
        sb.append(olle.getJobName()).append('|')
                .append(olle.getJobNumber()).append('|')
                .append(olle.getJobUserName()).append('|')
                .append(olle.getLockScope()).append('|')
                .append(olle.getLockState()).append('|')
                .append(olle.getLockStatus()).append('|')
                .append(olle.getLockType()).append('|')
                .append(olle.getShare());
        return sb.toString();
    }

    /**
     * Make an output string from an output queue
     *
     * @param outq output queue
     * @return String representation thereof
     * @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    public String stringFrom(OutputQueue outq) throws AS400Exception, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, RequestNotSupportedException {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(outq.getName()).append("\n");
        sb.append("Path: ").append(outq.getPath()).append("\n");
//        sb.append("ATTR_3812SCS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_3812SCS)).append("\n");
//        sb.append("ATTR_ACCOUNT_CODE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ACCOUNT_CODE)).append("\n");
//        sb.append("ATTR_AFP: ").append(outq.getIntegerAttribute(PrintObject.ATTR_AFP)).append("\n");
//        sb.append("ATTR_AFP_RESOURCE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_AFP_RESOURCE)).append("\n");
//        sb.append("ATTR_AFPRESOURCE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_AFPRESOURCE)).append("\n");
//        sb.append("ATTR_ALIGN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ALIGN)).append("\n");
//        sb.append("ATTR_ALIGNFORMS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ALIGNFORMS)).append("\n");
//        sb.append("ATTR_ALWDRTPRT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ALWDRTPRT)).append("\n");
//        sb.append("ATTR_ASCIITRANS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ASCIITRANS)).append("\n");
//        sb.append("ATTR_ASPDEVICE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ASPDEVICE)).append("\n");
//        sb.append("ATTR_AUTHCHCK: ").append(outq.getIntegerAttribute(PrintObject.ATTR_AUTHCHCK)).append("\n");
//        sb.append("ATTR_AUTHORITY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_AUTHORITY)).append("\n");
//        sb.append("ATTR_AUTOEND: ").append(outq.getIntegerAttribute(PrintObject.ATTR_AUTOEND)).append("\n");
//        sb.append("ATTR_AUX_POOL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_AUX_POOL)).append("\n");
//        sb.append("ATTR_BACK_OVERLAY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_BACK_OVERLAY)).append("\n");
//        sb.append("ATTR_BARCODE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_BARCODE)).append("\n");
//        sb.append("ATTR_BKMGN_ACR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_BKMGN_ACR)).append("\n");
//        sb.append("ATTR_BKMGN_DWN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_BKMGN_DWN)).append("\n");
//        sb.append("ATTR_BKOVL_ACR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_BKOVL_ACR)).append("\n");
//        sb.append("ATTR_BKOVL_DWN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_BKOVL_DWN)).append("\n");
//        sb.append("ATTR_BTWNCPYSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_BTWNCPYSTS)).append("\n");
//        sb.append("ATTR_BTWNFILESTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_BTWNFILESTS)).append("\n");
//        sb.append("ATTR_CHANGES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CHANGES)).append("\n");
//        sb.append("ATTR_CHAR_ID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CHAR_ID)).append("\n");
//        sb.append("ATTR_CHARID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CHARID)).append("\n");
//        sb.append("ATTR_CHR_RTT_CMDS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CHR_RTT_CMDS)).append("\n");
//        sb.append("ATTR_CHRSET: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CHRSET)).append("\n");
//        sb.append("ATTR_CHRSET_LIB: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CHRSET_LIB)).append("\n");
//        sb.append("ATTR_CHRSET_SIZE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CHRSET_SIZE)).append("\n");
//        sb.append("ATTR_CODEDFNT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CODEDFNT)).append("\n");
//        sb.append("ATTR_CODEDFNTLIB: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CODEDFNTLIB)).append("\n");
//        sb.append("ATTR_CODEDFONT_SIZE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CODEDFONT_SIZE)).append("\n");
//        sb.append("ATTR_CODEPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CODEPAGE)).append("\n");
//        sb.append("ATTR_CODEPAGE_NAME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CODEPAGE_NAME)).append("\n");
//        sb.append("ATTR_CODEPAGE_NAME_LIB: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CODEPAGE_NAME_LIB)).append("\n");
//        sb.append("ATTR_CODFNT_ARRAY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CODFNT_ARRAY)).append("\n");
//        sb.append("ATTR_COLOR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_COLOR)).append("\n");
//        sb.append("ATTR_CONSTBCK_OVL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CONSTBCK_OVL)).append("\n");
//        sb.append("ATTR_CONTROLCHAR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CONTROLCHAR)).append("\n");
//        sb.append("ATTR_CONVERT_LINEDATA: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CONVERT_LINEDATA)).append("\n");
//        sb.append("ATTR_COPIES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_COPIES)).append("\n");
//        sb.append("ATTR_COPIESLEFT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_COPIESLEFT)).append("\n");
//        sb.append("ATTR_CORNER_STAPLE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CORNER_STAPLE)).append("\n");
//        sb.append("ATTR_CPI: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CPI)).append("\n");
//        sb.append("ATTR_CPI_CHANGES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CPI_CHANGES)).append("\n");
//        sb.append("ATTR_CURPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_CURPAGE)).append("\n");
//        sb.append("ATTR_DATA_QUEUE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DATA_QUEUE)).append("\n");
//        sb.append("ATTR_DATAFORMAT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DATAFORMAT)).append("\n");
//        sb.append("ATTR_DATE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DATE)).append("\n");
//        sb.append("ATTR_DATE_END: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DATE_END)).append("\n");
//        sb.append("ATTR_DATE_USED: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DATE_USED)).append("\n");
//        sb.append("ATTR_DATE_WTR_BEGAN_FILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DATE_WTR_BEGAN_FILE)).append("\n");
//        sb.append("ATTR_DATE_WTR_CMPL_FILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DATE_WTR_CMPL_FILE)).append("\n");
//        sb.append("ATTR_DAYS_UNTIL_EXPIRE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DAYS_UNTIL_EXPIRE)).append("\n");
//        sb.append("ATTR_DBCS_FNT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DBCS_FNT)).append("\n");
//        sb.append("ATTR_DBCS_FNT_LIB: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DBCS_FNT_LIB)).append("\n");
//        sb.append("ATTR_DBCS_FNT_SIZE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DBCS_FNT_SIZE)).append("\n");
//        sb.append("ATTR_DBCSCPI: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DBCSCPI)).append("\n");
//        sb.append("ATTR_DBCSDATA: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DBCSDATA)).append("\n");
//        sb.append("ATTR_DBCSEXTENSN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DBCSEXTENSN)).append("\n");
//        sb.append("ATTR_DBCSROTATE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DBCSROTATE)).append("\n");
//        sb.append("ATTR_DBCSSISO: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DBCSSISO)).append("\n");
//        sb.append("ATTR_DDS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DDS)).append("\n");
//        sb.append("ATTR_DECIMAL_FMT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DECIMAL_FMT)).append("\n");
//        sb.append("ATTR_DELETESPLF: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DELETESPLF)).append("\n");
//        sb.append("ATTR_DESCRIPTION: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DESCRIPTION)).append("\n");
//        sb.append("ATTR_DESTINATION: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DESTINATION)).append("\n");
//        sb.append("ATTR_DESTOPTION: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DESTOPTION)).append("\n");
//        sb.append("ATTR_DEVCLASS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DEVCLASS)).append("\n");
//        sb.append("ATTR_DEVMODEL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DEVMODEL)).append("\n");
//        sb.append("ATTR_DEVSTATUS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DEVSTATUS)).append("\n");
//        sb.append("ATTR_DEVTYPE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DEVTYPE)).append("\n");
//        sb.append("ATTR_DFR_WRITE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DFR_WRITE)).append("\n");
//        sb.append("ATTR_DISPLAYANY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DISPLAYANY)).append("\n");
//        sb.append("ATTR_DOUBLEWIDE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DOUBLEWIDE)).append("\n");
//        sb.append("ATTR_DRAWERCHANGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DRAWERCHANGE)).append("\n");
//        sb.append("ATTR_DRWRSEP: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DRWRSEP)).append("\n");
//        sb.append("ATTR_DUPLEX: ").append(outq.getIntegerAttribute(PrintObject.ATTR_DUPLEX)).append("\n");
//        sb.append("ATTR_EDGESTITCH_NUMSTAPLES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_EDGESTITCH_NUMSTAPLES)).append("\n");
//        sb.append("ATTR_EDGESTITCH_REF: ").append(outq.getIntegerAttribute(PrintObject.ATTR_EDGESTITCH_REF)).append("\n");
//        sb.append("ATTR_EDGESTITCH_REFOFF: ").append(outq.getIntegerAttribute(PrintObject.ATTR_EDGESTITCH_REFOFF)).append("\n");
//        sb.append("ATTR_ENDPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ENDPAGE)).append("\n");
//        sb.append("ATTR_ENDPNDSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ENDPNDSTS)).append("\n");
//        sb.append("ATTR_ENVLP_SOURCE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ENVLP_SOURCE)).append("\n");
//        sb.append("ATTR_EXPIRATION_DATE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_EXPIRATION_DATE)).append("\n");
//        sb.append("ATTR_FIDELITY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FIDELITY)).append("\n");
//        sb.append("ATTR_FIELD_OUTLIN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FIELD_OUTLIN)).append("\n");
//        sb.append("ATTR_FILESEP: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FILESEP)).append("\n");
//        sb.append("ATTR_FOLDREC: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FOLDREC)).append("\n");
//        sb.append("ATTR_FONT_CHANGES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FONT_CHANGES)).append("\n");
//        sb.append("ATTR_FONTID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FONTID)).append("\n");
//        sb.append("ATTR_FONTRESFMT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FONTRESFMT)).append("\n");
//        sb.append("ATTR_FORM_DEFINITION: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FORM_DEFINITION)).append("\n");
//        sb.append("ATTR_FORMFEED: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FORMFEED)).append("\n");
//        sb.append("ATTR_FORMTYPE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FORMTYPE)).append("\n");
//        sb.append("ATTR_FORMTYPEMSG: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FORMTYPEMSG)).append("\n");
//        sb.append("ATTR_FRONT_OVERLAY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FRONT_OVERLAY)).append("\n");
//        sb.append("ATTR_FTMGN_ACR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FTMGN_ACR)).append("\n");
//        sb.append("ATTR_FTMGN_DWN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FTMGN_DWN)).append("\n");
//        sb.append("ATTR_FTOVL_ACR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FTOVL_ACR)).append("\n");
//        sb.append("ATTR_FTOVL_DWN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_FTOVL_DWN)).append("\n");
//        sb.append("ATTR_GRAPHICS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_GRAPHICS)).append("\n");
//        sb.append("ATTR_GRAPHICS_TOK: ").append(outq.getIntegerAttribute(PrintObject.ATTR_GRAPHICS_TOK)).append("\n");
//        sb.append("ATTR_GRPLVL_IDXTAG: ").append(outq.getIntegerAttribute(PrintObject.ATTR_GRPLVL_IDXTAG)).append("\n");
//        sb.append("ATTR_HELDSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_HELDSTS)).append("\n");
//        sb.append("ATTR_HIGHLIGHT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_HIGHLIGHT)).append("\n");
//        sb.append("ATTR_HOLD: ").append(outq.getIntegerAttribute(PrintObject.ATTR_HOLD)).append("\n");
//        sb.append("ATTR_HOLDPNDSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_HOLDPNDSTS)).append("\n");
//        sb.append("ATTR_HOLDTYPE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_HOLDTYPE)).append("\n");
//        sb.append("ATTR_IMGCFG: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IMGCFG)).append("\n");
//        sb.append("ATTR_INTERNETADDR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_INTERNETADDR)).append("\n");
//        sb.append("ATTR_IPDSPASSTHRU: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPDSPASSTHRU)).append("\n");
//        sb.append("ATTR_IPP_ATTR_CCSID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPP_ATTR_CCSID)).append("\n");
//        sb.append("ATTR_IPP_ATTR_NL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPP_ATTR_NL)).append("\n");
//        sb.append("ATTR_IPP_JOB_ID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPP_JOB_ID)).append("\n");
//        sb.append("ATTR_IPP_JOB_NAME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPP_JOB_NAME)).append("\n");
//        sb.append("ATTR_IPP_JOB_NAME_NL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPP_JOB_NAME_NL)).append("\n");
//        sb.append("ATTR_IPP_JOB_ORIGUSER: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPP_JOB_ORIGUSER)).append("\n");
//        sb.append("ATTR_IPP_JOB_ORIGUSER_NL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPP_JOB_ORIGUSER_NL)).append("\n");
//        sb.append("ATTR_IPP_PRINTER_NAME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_IPP_PRINTER_NAME)).append("\n");
//        sb.append("ATTR_JOBCCSID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_JOBCCSID)).append("\n");
//        sb.append("ATTR_JOBNAME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_JOBNAME)).append("\n");
//        sb.append("ATTR_JOBNUMBER: ").append(outq.getIntegerAttribute(PrintObject.ATTR_JOBNUMBER)).append("\n");
//        sb.append("ATTR_JOBSEPRATR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_JOBSEPRATR)).append("\n");
//        sb.append("ATTR_JOBSYSTEM: ").append(outq.getIntegerAttribute(PrintObject.ATTR_JOBSYSTEM)).append("\n");
//        sb.append("ATTR_JOBUSER: ").append(outq.getIntegerAttribute(PrintObject.ATTR_JOBUSER)).append("\n");
//        sb.append("ATTR_JUSTIFY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_JUSTIFY)).append("\n");
//        sb.append("ATTR_LASTPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_LASTPAGE)).append("\n");
//        sb.append("ATTR_LIBRARY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_LIBRARY)).append("\n");
//        sb.append("ATTR_LINESPACING: ").append(outq.getIntegerAttribute(PrintObject.ATTR_LINESPACING)).append("\n");
//        sb.append("ATTR_LPI: ").append(outq.getIntegerAttribute(PrintObject.ATTR_LPI)).append("\n");
//        sb.append("ATTR_LPI_CHANGES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_LPI_CHANGES)).append("\n");
//        sb.append("ATTR_MAX_JOBS_PER_CLIENT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MAX_JOBS_PER_CLIENT)).append("\n");
//        sb.append("ATTR_MAXRCDS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MAXRCDS)).append("\n");
//        sb.append("ATTR_MEASMETHOD: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MEASMETHOD)).append("\n");
//        sb.append("ATTR_MESSAGE_QUEUE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MESSAGE_QUEUE)).append("\n");
//        sb.append("ATTR_MFGTYPE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MFGTYPE)).append("\n");
//        sb.append("ATTR_MSGHELP: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MSGHELP)).append("\n");
//        sb.append("ATTR_MSGID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MSGID)).append("\n");
//        sb.append("ATTR_MSGREPLY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MSGREPLY)).append("\n");
//        sb.append("ATTR_MSGSEV: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MSGSEV)).append("\n");
//        sb.append("ATTR_MSGTEXT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MSGTEXT)).append("\n");
//        sb.append("ATTR_MSGTYPE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MSGTYPE)).append("\n");
//        sb.append("ATTR_MULTI_ITEM_REPLY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MULTI_ITEM_REPLY)).append("\n");
//        sb.append("ATTR_MULTIUP: ").append(outq.getIntegerAttribute(PrintObject.ATTR_MULTIUP)).append("\n");
//        sb.append("ATTR_NETWORK: ").append(outq.getIntegerAttribute(PrintObject.ATTR_NETWORK)).append("\n");
//        sb.append("ATTR_NPSCCSID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_NPSCCSID)).append("\n");
//        sb.append("ATTR_NPSLEVEL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_NPSLEVEL)).append("\n");
//        sb.append("ATTR_NUMBYTES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_NUMBYTES)).append("\n");
//        sb.append("ATTR_NUMBYTES_SPLF: ").append(outq.getIntegerAttribute(PrintObject.ATTR_NUMBYTES_SPLF)).append("\n");
//        sb.append("ATTR_NUMFILES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_NUMFILES)).append("\n");
//        sb.append("ATTR_NUMRSC_LIB_ENT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_NUMRSC_LIB_ENT)).append("\n");
//        sb.append("ATTR_NUMWRITERS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_NUMWRITERS)).append("\n");
//        sb.append("ATTR_OBJEXTATTR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OBJEXTATTR)).append("\n");
//        sb.append("ATTR_OFFICEVISION: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OFFICEVISION)).append("\n");
//        sb.append("ATTR_ONJOBQSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ONJOBQSTS)).append("\n");
//        sb.append("ATTR_OPCNTRL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OPCNTRL)).append("\n");
//        sb.append("ATTR_OPENCMDS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OPENCMDS)).append("\n");
//        sb.append("ATTR_ORDER: ").append(outq.getIntegerAttribute(PrintObject.ATTR_ORDER)).append("\n");
//        sb.append("ATTR_OS4_CRT_AFP: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OS4_CRT_AFP)).append("\n");
//        sb.append("ATTR_OUTPTY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OUTPTY)).append("\n");
//        sb.append("ATTR_OUTPUT_QUEUE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OUTPUT_QUEUE)).append("\n");
//        sb.append("ATTR_OUTPUTBIN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OUTPUTBIN)).append("\n");
//        sb.append("ATTR_OUTQSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OUTQSTS)).append("\n");
//        sb.append("ATTR_OVERALLSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OVERALLSTS)).append("\n");
//        sb.append("ATTR_OVERFLOW: ").append(outq.getIntegerAttribute(PrintObject.ATTR_OVERFLOW)).append("\n");
//        sb.append("ATTR_PAGE_AT_A_TIME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGE_AT_A_TIME)).append("\n");
//        sb.append("ATTR_PAGE_DEFINITION: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGE_DEFINITION)).append("\n");
//        sb.append("ATTR_PAGE_GROUPS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGE_GROUPS)).append("\n");
//        sb.append("ATTR_PAGE_ROTATE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGE_ROTATE)).append("\n");
//        sb.append("ATTR_PAGELEN: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGELEN)).append("\n");
//        sb.append("ATTR_PAGELVLIDXTAG: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGELVLIDXTAG)).append("\n");
//        sb.append("ATTR_PAGENUMBER: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGENUMBER)).append("\n");
//        sb.append("ATTR_PAGES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGES)).append("\n");
//        sb.append("ATTR_PAGES_EST: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGES_EST)).append("\n");
//        sb.append("ATTR_PAGEWIDTH: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGEWIDTH)).append("\n");
//        sb.append("ATTR_PAGRTT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAGRTT)).append("\n");
//        sb.append("ATTR_PAPER_SOURCE_1: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAPER_SOURCE_1)).append("\n");
//        sb.append("ATTR_PAPER_SOURCE_2: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PAPER_SOURCE_2)).append("\n");
//        sb.append("ATTR_PELDENSITY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PELDENSITY)).append("\n");
//        sb.append("ATTR_PGM_OPN_FILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PGM_OPN_FILE)).append("\n");
//        sb.append("ATTR_PGM_OPN_LIB: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PGM_OPN_LIB)).append("\n");
//        sb.append("ATTR_POINTSIZE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_POINTSIZE)).append("\n");
//        sb.append("ATTR_PRINTER: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PRINTER)).append("\n");
//        sb.append("ATTR_PRINTER_FILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PRINTER_FILE)).append("\n");
//        sb.append("ATTR_PRTASSIGNED: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PRTASSIGNED)).append("\n");
//        sb.append("ATTR_PRTDEVTYPE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PRTDEVTYPE)).append("\n");
//        sb.append("ATTR_PRTQUALITY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PRTQUALITY)).append("\n");
//        sb.append("ATTR_PRTSEQUENCE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PRTSEQUENCE)).append("\n");
//        sb.append("ATTR_PRTTEXT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PRTTEXT)).append("\n");
//        sb.append("ATTR_PUBINF: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PUBINF)).append("\n");
//        sb.append("ATTR_PUBINF_COLOR_SUP: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PUBINF_COLOR_SUP)).append("\n");
//        sb.append("ATTR_PUBINF_DS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PUBINF_DS)).append("\n");
//        sb.append("ATTR_PUBINF_DUPLEX_SUP: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PUBINF_DUPLEX_SUP)).append("\n");
//        sb.append("ATTR_PUBINF_LOCATION: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PUBINF_LOCATION)).append("\n");
//        sb.append("ATTR_PUBINF_PPM: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PUBINF_PPM)).append("\n");
//        sb.append("ATTR_PUBINF_PPM_COLOR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_PUBINF_PPM_COLOR)).append("\n");
//        sb.append("ATTR_RCDFMT_DATA: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RCDFMT_DATA)).append("\n");
//        sb.append("ATTR_RECLENGTH: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RECLENGTH)).append("\n");
//        sb.append("ATTR_REDUCE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_REDUCE)).append("\n");
//        sb.append("ATTR_RESTART: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RESTART)).append("\n");
//        sb.append("ATTR_RMTLOCNAME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RMTLOCNAME)).append("\n");
//        sb.append("ATTR_RMTPRTQ: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RMTPRTQ)).append("\n");
//        sb.append("ATTR_RMTSYSTEM: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RMTSYSTEM)).append("\n");
//        sb.append("ATTR_RPLCHAR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RPLCHAR)).append("\n");
//        sb.append("ATTR_RPLUNPRT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RPLUNPRT)).append("\n");
//        sb.append("ATTR_RSC_LIB_LIST: ").append(outq.getIntegerAttribute(PrintObject.ATTR_RSC_LIB_LIST)).append("\n");
//        sb.append("ATTR_SADDLESTITCH_NUMSTAPLES: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SADDLESTITCH_NUMSTAPLES)).append("\n");
//        sb.append("ATTR_SADDLESTITCH_REF: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SADDLESTITCH_REF)).append("\n");
//        sb.append("ATTR_SADDLESTITCH_STPL_OFFSEINFO: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SADDLESTITCH_STPL_OFFSEINFO)).append("\n");
//        sb.append("ATTR_SAVE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SAVE)).append("\n");
//        sb.append("ATTR_SAVE_COMMAND: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SAVE_COMMAND)).append("\n");
//        sb.append("ATTR_SAVE_DEVICE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SAVE_DEVICE)).append("\n");
//        sb.append("ATTR_SAVE_FILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SAVE_FILE)).append("\n");
//        sb.append("ATTR_SAVE_LABEL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SAVE_LABEL)).append("\n");
//        sb.append("ATTR_SAVE_SEQUENCE_NUMBER: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SAVE_SEQUENCE_NUMBER)).append("\n");
//        sb.append("ATTR_SAVE_VOLUME_FORMAT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SAVE_VOLUME_FORMAT)).append("\n");
//        sb.append("ATTR_SAVE_VOLUME_ID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SAVE_VOLUME_ID)).append("\n");
//        sb.append("ATTR_SCHEDULE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SCHEDULE)).append("\n");
//        sb.append("ATTR_SCS2ASCII: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SCS2ASCII)).append("\n");
//        sb.append("ATTR_SEEKOFF: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SEEKOFF)).append("\n");
//        sb.append("ATTR_SEEKORG: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SEEKORG)).append("\n");
//        sb.append("ATTR_SENDPTY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SENDPTY)).append("\n");
//        sb.append("ATTR_SEPPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SEPPAGE)).append("\n");
//        sb.append("ATTR_SPLF_AUTH_METHOD: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_AUTH_METHOD)).append("\n");
//        sb.append("ATTR_SPLF_CREATOR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_CREATOR)).append("\n");
//        sb.append("ATTR_SPLF_RESTORED_DATE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_RESTORED_DATE)).append("\n");
//        sb.append("ATTR_SPLF_RESTORED_TIME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_RESTORED_TIME)).append("\n");
//        sb.append("ATTR_SPLF_SAVED_DATE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_SAVED_DATE)).append("\n");
//        sb.append("ATTR_SPLF_SAVED_TIME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_SAVED_TIME)).append("\n");
//        sb.append("ATTR_SPLF_SECURITY_METHOD: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_SECURITY_METHOD)).append("\n");
//        sb.append("ATTR_SPLF_SIZE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_SIZE)).append("\n");
//        sb.append("ATTR_SPLF_SIZE_MULT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLF_SIZE_MULT)).append("\n");
//        sb.append("ATTR_SPLFNUM: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLFNUM)).append("\n");
//        sb.append("ATTR_SPLFSTATUS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLFSTATUS)).append("\n");
//        sb.append("ATTR_SPLSCS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPLSCS)).append("\n");
//        sb.append("ATTR_SPOOL: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPOOL)).append("\n");
//        sb.append("ATTR_SPOOLFILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SPOOLFILE)).append("\n");
//        sb.append("ATTR_SRC_CODEPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SRC_CODEPAGE)).append("\n");
//        sb.append("ATTR_SRCDRWR: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SRCDRWR)).append("\n");
//        sb.append("ATTR_STARTEDBY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_STARTEDBY)).append("\n");
//        sb.append("ATTR_STARTPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_STARTPAGE)).append("\n");
//        sb.append("ATTR_SYS_DRV_PGM: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SYS_DRV_PGM)).append("\n");
//        sb.append("ATTR_SYSTEM: ").append(outq.getIntegerAttribute(PrintObject.ATTR_SYSTEM)).append("\n");
//        sb.append("ATTR_TGT_CODEPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_TGT_CODEPAGE)).append("\n");
//        sb.append("ATTR_TIME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_TIME)).append("\n");
//        sb.append("ATTR_TIME_END: ").append(outq.getIntegerAttribute(PrintObject.ATTR_TIME_END)).append("\n");
//        sb.append("ATTR_TIME_WTR_BEGAN_FILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_TIME_WTR_BEGAN_FILE)).append("\n");
//        sb.append("ATTR_TIME_WTR_CMPL_FILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_TIME_WTR_CMPL_FILE)).append("\n");
//        sb.append("ATTR_TOADDRESS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_TOADDRESS)).append("\n");
//        sb.append("ATTR_TOUSERID: ").append(outq.getIntegerAttribute(PrintObject.ATTR_TOUSERID)).append("\n");
//        sb.append("ATTR_TRC1403: ").append(outq.getIntegerAttribute(PrintObject.ATTR_TRC1403)).append("\n");
//        sb.append("ATTR_UNITOFMEAS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_UNITOFMEAS)).append("\n");
//        sb.append("ATTR_USER_DEFINED_OBJECT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USER_DEFINED_OBJECT)).append("\n");
//        sb.append("ATTR_USER_DFN_TXT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USER_DFN_TXT)).append("\n");
//        sb.append("ATTR_USER_DRIVER_PROG: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USER_DRIVER_PROG)).append("\n");
//        sb.append("ATTR_USER_TRANSFORM_PROG: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USER_TRANSFORM_PROG)).append("\n");
//        sb.append("ATTR_USERCMT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USERCMT)).append("\n");
//        sb.append("ATTR_USERDATA: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USERDATA)).append("\n");
//        sb.append("ATTR_USERGEN_DATA: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USERGEN_DATA)).append("\n");
//        sb.append("ATTR_USRDEFDATA: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USRDEFDATA)).append("\n");
//        sb.append("ATTR_USRDEFFILE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USRDEFFILE)).append("\n");
//        sb.append("ATTR_USRDEFOPT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USRDEFOPT)).append("\n");
//        sb.append("ATTR_USRDRVDATA: ").append(outq.getIntegerAttribute(PrintObject.ATTR_USRDRVDATA)).append("\n");
//        sb.append("ATTR_VIEWING_FIDELITY: ").append(outq.getIntegerAttribute(PrintObject.ATTR_VIEWING_FIDELITY)).append("\n");
//        sb.append("ATTR_VMMVSCLASS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_VMMVSCLASS)).append("\n");
//        sb.append("ATTR_WORKSTATION_CUST_OBJECT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WORKSTATION_CUST_OBJECT)).append("\n");
//        sb.append("ATTR_WRTNGSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WRTNGSTS)).append("\n");
//        sb.append("ATTR_WTNGDATASTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTNGDATASTS)).append("\n");
//        sb.append("ATTR_WTNGDEVSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTNGDEVSTS)).append("\n");
//        sb.append("ATTR_WTNGMSGSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTNGMSGSTS)).append("\n");
//        sb.append("ATTR_WTRAUTOEND: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTRAUTOEND)).append("\n");
//        sb.append("ATTR_WTREND: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTREND)).append("\n");
//        sb.append("ATTR_WTRINIT: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTRINIT)).append("\n");
//        sb.append("ATTR_WTRJOBNAME: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTRJOBNAME)).append("\n");
//        sb.append("ATTR_WTRJOBNUM: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTRJOBNUM)).append("\n");
//        sb.append("ATTR_WTRJOBSTS: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTRJOBSTS)).append("\n");
//        sb.append("ATTR_WTRJOBUSER: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTRJOBUSER)).append("\n");
//        sb.append("ATTR_WTRSTRPAGE: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTRSTRPAGE)).append("\n");
//        sb.append("ATTR_WTRSTRTD: ").append(outq.getIntegerAttribute(PrintObject.ATTR_WTRSTRTD)).append("\n");
        return sb.toString();
    }
}
