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

import ublu.command.CommandInterface;
import ublu.command.CommandMap;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ObjectLockListEntry;
import com.ibm.as400.access.QueuedMessage;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.Subsystem;
import com.ibm.as400.access.SystemValue;
import com.ibm.as400.access.User;
import com.ibm.as400.access.UserList;
import com.softwoehr.pigiron.access.VSMParm;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.tn5250j.Session5250;

/**
 * Typedefs of collection generics
 *
 * <p>
 * Some have extended functionality specific to the type being manipulated.
 *
 * @author jwoehr
 */
public class Generics {

    /**
     * typedef
     *
     * @see ublu.db.DbHelper
     * @see ublu.db.ResultSetHelper
     */
    public static class ByteArrayList extends ArrayList<Byte> {

        /**
         * Default ctor
         */
        public ByteArrayList() {
            super();
        }

        /**
         * Instance with an initial size
         *
         * @param initialCapacity initial size
         */
        public ByteArrayList(int initialCapacity) {
            super(initialCapacity);
        }

        /**
         * Instance from a Byte collection
         *
         * @param c
         */
        public ByteArrayList(Collection<? extends Byte> c) {
            super(c);
        }

        /**
         * Instance from array of byte
         *
         * @param src array of byte to assimilate
         */
        public ByteArrayList(byte[] src) {
            this();
            for (byte b : src) {
                add(b);
            }
        }

        /**
         * Instance a certain number from array of byte
         *
         * @param src array of byte to assimilate
         * @param count count to read in from byte array
         */
        public ByteArrayList(byte[] src, int count) {
            this();
            for (byte b : src) {
                if (count-- < 1) {
                    break;
                }
                add(b);
            }
        }

        /**
         * Return the contents as a simple byte array.
         *
         * @return the contents as a simple byte array
         */
        public byte[] byteArray() {
            byte[] result = new byte[size()];
            for (int i = 0; i < size(); i++) {
                result[i] = get(i);
            }
            return result;
        }
    }

    /**
     * typedef
     *
     * @see ByteArraySplitter
     * @see ublu.db.ResultSetHelper
     */
    public static class ByteArrayListArrayList extends ArrayList<ByteArrayList> {
    }

    /**
     * typedef
     *
     * @see ublu.db.DbHelper
     */
    public static class ColumnNameList extends StringArrayList {
    }

    /**
     * typedef with some conversions available.
     * <p>
     * This class used to handle the list of column types so we can convert them
     * in order to their SQL string for table replication/creation SQL
     * statements</p>
     * <p>
     * Note that column indexes in JDBC are one's-based but this list is
     * zero-based.</p>
     */
    public static class ColumnTypeList extends ArrayList<Integer> {

        /**
         * Add an int to the end of Integer list.
         * <p>
         * SQL types are ints in JDBC.</p>
         *
         * @param i the JDBC type integer
         * @return true always
         */
        public boolean add(int i) {
            return add(new Integer(i));
        }

        /**
         * Get the int from the list that represents the indexed column.
         * <p>
         * Note that column indexes in JDBC are one's based but this list is
         * zero-based.</p>
         *
         * @param index zero-based column index
         * @return the int JDBC column type
         */
        public int getColumnType(int index) {
            return get(index); //To change body of generated methods, choose Tools | Templates.
        }

        /**
         * Get the int JDBC type from the indexed element and return as an SQL
         * string for the formal SQL type.
         * <p>
         * Note that column indexes in JDBC are one's based but this list is
         * zero-based.</p>
         *
         * @param index
         * @return SQL type string
         */
        public String getColumnSQLType(int index) {
            String type;
            switch (getColumnType(index)) {
                case java.sql.Types.ARRAY:
                    type = "ARRAY";
                    break;
                case java.sql.Types.BIGINT:
                    type = "BIGINT";
                    break;
                case java.sql.Types.BINARY:
                    type = "BINARY";
                    break;
                case java.sql.Types.BIT:
                    type = "BIT";
                    break;
                case java.sql.Types.BLOB:
                    type = "BLOB";
                    break;
                case java.sql.Types.BOOLEAN:
                    type = "BOOLEAN";
                    break;
                case java.sql.Types.CHAR:
                    type = "CHAR";
                    break;
                case java.sql.Types.CLOB:
                    type = "CLOB";
                    break;
                case java.sql.Types.DATALINK:
                    type = "DATALINK";
                    break;
                case java.sql.Types.DATE:
                    type = "DATE";
                    break;
                case java.sql.Types.DECIMAL:
                    type = "DECIMAL";
                    break;
                case java.sql.Types.DISTINCT:
                    type = "DISTINCT";
                    break;
                case java.sql.Types.DOUBLE:
                    type = "DOUBLE";
                    break;
                case java.sql.Types.FLOAT:
                    type = "FLOAT";
                    break;
                case java.sql.Types.INTEGER:
                    type = "INTEGER";
                    break;
                case java.sql.Types.JAVA_OBJECT:
                    type = "JAVA_OBJECT";
                    break;
                case java.sql.Types.LONGNVARCHAR:
                    type = "LONGNVARCHAR";
                    break;
                case java.sql.Types.LONGVARBINARY:
                    type = "LONGVARBINARY";
                    break;
                case java.sql.Types.LONGVARCHAR:
                    type = "LONGVARCHAR";
                    break;
                case java.sql.Types.NCHAR:
                    type = "NCHAR";
                    break;
                case java.sql.Types.NCLOB:
                    type = "NCLOB";
                    break;
                case java.sql.Types.NULL:
                    type = "NULL";
                    break;
                case java.sql.Types.NUMERIC:
                    type = "NUMERIC";
                    break;
                case java.sql.Types.NVARCHAR:
                    type = "NVARCHAR";
                    break;
                case java.sql.Types.OTHER:
                    type = "OTHER";
                    break;
                case java.sql.Types.REAL:
                    type = "REAL";
                    break;
                case java.sql.Types.REF:
                    type = "REF";
                    break;
                case java.sql.Types.ROWID:
                    type = "ROWID";
                    break;
                case java.sql.Types.SMALLINT:
                    type = "SMALLINT";
                    break;
                case java.sql.Types.SQLXML:
                    type = "SQLXML";
                    break;
                case java.sql.Types.STRUCT:
                    type = "STRUCT";
                    break;
                case java.sql.Types.TIME:
                    type = "TIME";
                    break;
                case java.sql.Types.TIMESTAMP:
                    type = "TIMESTAMP";
                    break;
                case java.sql.Types.TINYINT:
                    type = "TINYINT";
                    break;
                case java.sql.Types.VARBINARY:
                    type = "VARBINARY";
                    break;
                case java.sql.Types.VARCHAR:
                    type = "VARCHAR";
                    break;
                default:
                    type = "UNKNOWN";
            }
            return type;
        }

        /**
         * Get the int JDBC type from the indexed element and return as an SQL
         * string for the Postgresql type.
         * <p>
         * Note that column indexes in JDBC are one's based but this list is
         * zero-based.</p>
         *
         * @param index zero-based column index
         * @return SQL type string
         */
        public String getColumnPostgresqlType(int index) {
            String type;
            switch (getColumnType(index)) {
                case java.sql.Types.ARRAY:
                    type = "bytea";
                    break;
                case java.sql.Types.BIGINT:
                    type = "bigint";
                    break;
                case java.sql.Types.BINARY:
                    type = "bytea";
                    break;
                case java.sql.Types.BIT:
                    type = "varbit";
                    break;
                case java.sql.Types.BLOB:
                    type = "BLOB";
                    break;
                case java.sql.Types.BOOLEAN:
                    type = "boolean";
                    break;
                case java.sql.Types.CHAR:
                    type = "char"; // tried "text" already
                    break;
                case java.sql.Types.CLOB:
                    type = "text";
                    break;
                case java.sql.Types.DATALINK:
                    type = "DATALINK";
                    break;
                case java.sql.Types.DATE:
                    type = "date";
                    break;
                case java.sql.Types.DECIMAL:
                    type = "decimal";
                    break;
                case java.sql.Types.DISTINCT:
                    type = "DISTINCT";
                    break;
                case java.sql.Types.DOUBLE:
                    type = "float8";
                    break;
                case java.sql.Types.FLOAT:
                    type = "float8";
                    break;
                case java.sql.Types.INTEGER:
                    type = "integer";
                    break;
                case java.sql.Types.JAVA_OBJECT:
                    type = "JAVA_OBJECT";
                    break;
                case java.sql.Types.LONGNVARCHAR:
                    type = "varchar";
                    break;
                case java.sql.Types.LONGVARBINARY:
                    type = "bytea";
                    break;
                case java.sql.Types.LONGVARCHAR:
                    type = "varchar";
                    break;
                case java.sql.Types.NCHAR:
                    type = "char";
                    break;
                case java.sql.Types.NCLOB:
                    type = "NCLOB";
                    break;
                case java.sql.Types.NULL:
                    type = "NULL";
                    break;
                case java.sql.Types.NUMERIC:
                    type = "numeric";
                    break;
                case java.sql.Types.NVARCHAR:
                    type = "varchar";
                    break;
                case java.sql.Types.OTHER:
                    type = "OTHER";
                    break;
                case java.sql.Types.REAL:
                    type = "real";
                    break;
                case java.sql.Types.REF:
                    type = "REF";
                    break;
                case java.sql.Types.ROWID:
                    type = "ROWID";
                    break;
                case java.sql.Types.SMALLINT:
                    type = "smallint";
                    break;
                case java.sql.Types.SQLXML:
                    type = "SQLXML";
                    break;
                case java.sql.Types.STRUCT:
                    type = "STRUCT";
                    break;
                case java.sql.Types.TIME:
                    type = "timetz";
                    break;
                case java.sql.Types.TIMESTAMP:
                    type = "text";
                    break;
                case java.sql.Types.TINYINT:
                    type = "smallint";
                    break;
                case java.sql.Types.VARBINARY:
                    type = "bytea";
                    break;
                case java.sql.Types.VARCHAR:
                    type = "varchar";
                    break;
                default:
                    type = "UNKNOWN";
            }
            return type;
        }
    }

    /**
     * typedef
     *
     * @see ublu.db.Db
     */
    public static class ConnectionProperties extends Properties {
    }

    /**
     * typedef
     *
     * @see ublu.db.ResultSetHelper
     */
    public static class IndexList extends ArrayList<Integer> {

        /**
         * Add an index to the list
         *
         * @param index
         * @return true
         */
        public boolean add(int index) {
            add(new Integer(index));
            return true;
        }

        /**
         * Add indices
         *
         * @param indices
         * @return true if added
         */
        public boolean addAll(int[] indices) {
            for (int i : indices) {
                add(i);
            }
            return true;
        }

        /**
         * Does the list contain the index?
         *
         * @param index
         * @return true if index is found in the list
         */
        public boolean contains(int index) {
            return contains(new Integer(index));
        }
    }

    /**
     * typedef
     *
     * @see ublu.command.CmdMsgQ
     * @see Renderer
     */
    public static class QueuedMessageList extends ArrayList<QueuedMessage> {

        public QueuedMessageList() {
            super();
        }

        public QueuedMessageList(QueuedMessage[] qma) {
            addAll(Arrays.asList(qma));
        }

    }

    /**
     * typedef
     *
     * @see ublu.db.Csv
     * @see ublu.db.DbHelper
     * @see ublu.db.ResultSetFormatter
     *
     */
    public static class StringArrayList extends ArrayList<String> {

        /**
         * ctor/0
         */
        public StringArrayList() {
            super();
        }

        /**
         *
         * @param c
         */
        public StringArrayList(Collection<? extends String> c) {
            super(c);
        }

        /**
         * Create a list from an array of strings
         *
         * @param sar an array of strings to be assimilated.
         */
        public StringArrayList(String[] sar) {
            super();
            if (sar != null) {
                addAll(sar);
            }
        }

        /**
         * Create over a string
         *
         * @param s the string to assimilate
         */
        public StringArrayList(String s) {
            super();
            splitIn(s.trim(), "\\p{Space}+");
        }

        /**
         * Split a string into a list over a split expression
         *
         * @param input the original string
         * @param splitRegEx the split expression
         * @return list of strings
         */
        public final StringArrayList splitIn(String input, String splitRegEx) {
            this.addAll(Arrays.asList(input.split(splitRegEx)));
            return this;
        }

        /**
         * Add all the strings in String [] to the Array List
         *
         * @param sar array of String to add
         * @return this
         */
        public final StringArrayList addAll(String[] sar) {
            addAll(Arrays.asList(sar));
            return this;
        }

        /**
         * Trim all the strings in the list
         *
         * @return the new StringArrayList of trimmed strings.
         */
        public final StringArrayList trimAll() {
            StringArrayList sal = new StringArrayList();
            Iterator<String> it = this.iterator();
            while (it.hasNext()) {
                sal.add(it.next().trim());
            }
            return sal;
        }

        /**
         * Return contents as array of String
         *
         * @return contents as array of String
         */
        public String[] toStringArray() {
            return this.toArray(new String[size()]);
        }

        /**
         * Return contents of the array as an execution block, currently of type
         * String
         *
         * @return contents of the array as an execution block
         */
        public String toBlock() {
            StringBuilder sb = new StringBuilder();
            for (String s : this) {
                sb.append(s).append(' ');
            }
            return sb.toString();

        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (String s : this) {
                sb.append(s).append(",");
            }
            if (sb.length() == 1) {
                sb.append(" ");
            }
            sb.replace(sb.length() - 1, sb.length(), "]");
            return sb.toString();
        }
    }

    /**
     * typedef
     *
     * @see ublu.db.TableReplicator
     */
    public static class PrimaryKeyList extends StringArrayList {
    }

    /**
     * typedef
     *
     * @see ublu.db.DbHelper
     */
    public static class ColumnTypeNameList extends StringArrayList {
    }

    /**
     * typedef
     *
     * @see ublu.db.DbHelper
     */
    public static class TableNameList extends StringArrayList {
    }

    /**
     * Represents a collection of lines constituting a program.
     */
    public static class UbluProgram extends StringArrayList {

        /**
         * Default ctor
         */
        public UbluProgram() {
            super();
        }

        /**
         * Ctor from a single string containing all lines
         *
         * @param input a single string containing all lines of a program
         */
        public UbluProgram(String input) {
            this();
            splitIn(input, "[\n]");
        }

        /**
         * Factory method to produce a new program of lines split from single
         * string input
         *
         * @param input the lines
         * @return a program of lines
         */
        public static UbluProgram newUbluProgram(String input) {
            return new UbluProgram(input);
        }
    }

    /**
     * Basically just here to 'uniq' the lexicon because some commands pop up
     * with two names and we only want to show the name once in help.
     */
    public static class CommandLexicon extends LinkedHashMap<String, String> {

        /**
         * Object to lexiconize the command map which latter has duplicate
         * commands under two different names.
         *
         * @param i controlling interpreter
         * @param cm the command map to lexiconize
         */
        public CommandLexicon(Interpreter i, CommandMap cm) {
            Set<String> keys = cm.keySet();
            for (String key : keys) {
                CommandInterface ci = cm.getCmd(i, key);
                put(ci.getCommandName(), ci.getCommandDescription());
            }
        }
    }

    /**
     * Represents the 4-byte key to a QueuedMessage in a MessageQueue.
     */
    public static class QueuedMessageKey extends ArrayList<Byte> {

        /**
         * ctor from 4-byte key returned by jtOpen
         *
         * @param bytes the 4-byte array that is the message key
         */
        public QueuedMessageKey(byte[] bytes) {
            fromByteArray(bytes);
        }

        /**
         * Assimilate the four bytes of the message key
         *
         * @param bytes the four bytes of the message key
         */
        public final void fromByteArray(byte[] bytes) {
            if (bytes != null) {
                for (byte b : bytes) {
                    add(b);
                }
            }
        }

        /**
         * ctor from hex string
         *
         * @param s hex string representing 4-byte key
         */
        public QueuedMessageKey(String s) {
            byte[] bytes = new byte[4];
            Long l = Long.valueOf(s, 0x10);
            for (int i = 0; i < 4; i++) {
                bytes[3 - i] = l.byteValue();
                l = l >> 8;
            }
            fromByteArray(bytes);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (this.isEmpty()) {
                sb.append("        ");
            } else {
                for (Byte b : this) {
                    sb.append(String.format("%02X", b));
                }
            }
            return sb.toString();
        }

        /**
         * Get the 4-byte key suitable for use in CmdMsgQ
         *
         * @return the 4-byte key suitable for use in CmdMsgQ
         */
        public byte[] toMessageKey() {
            byte[] bytes = null;
            if (!this.isEmpty()) {
                bytes = new byte[4];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = get(i);
                }
            }
            return bytes;
        }
    }

    /**
     * Holds interpreter frames for control flow
     */
    public static class InterpreterFrameStack extends Stack<InterpreterFrame> {
    }

    /**
     * Holds interpreter frames for control flow
     */
    public static class TupleStack extends Stack<Tuple> {
    }

    /**
     * Holds params for Functors
     */
    public static class FunctorParamList extends StringArrayList {
    }

    /**
     * Holds Tuple names to match to the param list in Functors
     */
    public static class TupleNameList extends StringArrayList {

        /**
         * Replaces entries consisting of "~" with the key of the tuple popped
         * from the tuple stack.
         *
         * @param interpreter the interpreter with the tuple stack
         * @return this tuple name list de-lifo-ized
         */
        public TupleNameList delifoize(Interpreter interpreter) {
            for (int i = 0; i < this.size(); i++) {
                if (get(i).equals(ArgArray.POPTUPLE)) {
                    set(i, interpreter.getTupleStack().pop().getKey());
                }
            }
            return this;
        }
    }

    /**
     * Keep functors with name keys, i.e., a function dictionary
     */
    public static class FunctorMap extends LinkedHashMap<String, Functor> implements Serializable {

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
        }

        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
        }

        private void readObjectNoData()
                throws ObjectStreamException {
        }

        /**
         * True if name is in dictionary
         *
         * @param name name of function
         * @return true iff name is in dictionary
         */
        public boolean hasFunctor(String name) {
            return containsKey(name);
        }

        /**
         * Add named function to dictionary
         *
         * @param name name of function
         * @param f functor
         */
        public void addFunctor(String name, Functor f) {
            put(name, f);
        }

        /**
         * Get named functor from dictionary
         *
         * @param name name of function
         * @return the functor
         */
        public Functor getFunctor(String name) {
            return get(name);
        }

        /**
         * Show one function in a code re-usable form
         *
         * @param funcName function to show
         * @return the function in a code re-usable form
         */
        public String showFunction(String funcName) {
            StringBuilder sb = new StringBuilder();
            if (hasFunctor(funcName)) {
                sb.append("# ").append(funcName).append(" ").append(getFunctor(funcName).superString());
                sb.append("\n");
                sb.append("FUNC ").append(funcName);
                sb.append(getFunctor(funcName));
                sb.append("\n");
            } else {
                sb.append(funcName).append(" not found.\n");
            }
            return sb.toString();
        }

        /**
         * List all named functions
         *
         * @return String describing all known functions
         */
        public String listFunctions() {
            StringBuilder sb = new StringBuilder();
            for (String key : keySet()) {
                sb.append(showFunction(key));
            }
            return sb.toString();
        }

        /**
         * Remove a function from the dictionary
         *
         * @param name function name
         * @return boolean if was found (and removed)
         */
        public boolean deleteFunction(String name) {
            boolean found = hasFunctor(name);
            if (found) {
                remove(name);
            }
            return found;
        }
    }

    /**
     * A map to hold the Consts the user defines.
     */
    public static class ConstMap extends LinkedHashMap<String, Const> implements Serializable {

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
        }

        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
        }

        private void readObjectNoData()
                throws ObjectStreamException {
        }

        /**
         * List all consts
         *
         * @return String describing all known consts
         */
        public String listConsts() {
            StringBuilder sb = new StringBuilder();
            for (String key : keySet()) {
                Const c = get(key);
                String value = c.getValue();
                sb.append((key)).append(" : ")
                        .append(c).append(" : ")
                        .append(value)
                        .append('\n');
            }
            return sb.toString();
        }

        /**
         * ctor/0
         */
        public ConstMap() {
            super();
        }

        /**
         * Copy ctor
         *
         * @param cm the map to copy
         */
        public ConstMap(ConstMap cm) {
            super(cm);
        }
    }

    /**
     * Encapsulate array of messages from command / program execution
     */
    public static class AS400MessageList extends ArrayList<AS400Message> {

        /**
         * ctor/0
         */
        public AS400MessageList() {
            super();
        }

        /**
         * ctor from an array of messages
         *
         * @param aS400Messages
         */
        public AS400MessageList(AS400Message[] aS400Messages) {
            this();
            addAll(Arrays.asList(aS400Messages));
        }
    }

    /**
     * Array of spooled file objects
     */
    public static class SpooledFileArrayList extends ArrayList<SpooledFile> {
    }

    /**
     * Encapsulate a Set of JMX ObjectName objects
     */
    public static class ObjectNameHashSet extends HashSet<ObjectName> {

        /**
         * Encapsulate a Set of JMX ObjectName objects
         *
         * @param c a Set of JMX ObjectName objects
         */
        public ObjectNameHashSet(Collection<? extends ObjectName> c) {
            super(c);
        }
    }

    /**
     * Encapsulate a Set of JMX ObjectName objects
     */
    public static class ObjectInstanceHashSet extends HashSet<ObjectInstance> {

        /**
         * ctor/0
         */
        public ObjectInstanceHashSet() {
            super();
        }

        /**
         * Encapsulate a Set of JMX ObjectName objects
         *
         * @param c a Set of JMX ObjectName objects
         */
        public ObjectInstanceHashSet(Collection<? extends ObjectInstance> c) {
            super(c);
        }
    }

    /**
     * typedef for list of miscellaneous objects
     */
    public static class ThingArrayList extends ArrayList<Object> {

        /**
         * ctor/0
         */
        public ThingArrayList() {
        }

        /**
         * ctor/1 Create from Collection
         *
         * @param c collection of objects to assimilate
         */
        public ThingArrayList(Collection<? extends Object> c) {
            super(c);
        }

        /**
         * ctor/1 Create from Enumeration
         *
         * @param e
         */
        public ThingArrayList(Enumeration e) {
            this();
            while (e.hasMoreElements()) {
                add(e.nextElement());
            }
        }

        /**
         * ctor/1 Create from Object array
         *
         * @param oa source Object array
         */
        public ThingArrayList(Object[] oa) {
            this();
            addAll(Arrays.asList(oa));
        }

        /**
         * ctor/1 Create with capacity
         *
         * @param initialCapacity
         */
        public ThingArrayList(int initialCapacity) {
            super(initialCapacity);
        }
    }

    /**
     * Collection of OS400 system values.
     */
    public static class SystemValueHashMap extends HashMap<String, SystemValue> {

        /**
         * Typedef
         */
        public SystemValueHashMap() {
        }

        /**
         * Instance from the Vectors returned by JTOpen
         *
         * @param v Vector of system values
         */
        public SystemValueHashMap(Vector<SystemValue> v) {
            this();
            Iterator<SystemValue> it = v.iterator();
            while (it.hasNext()) {
                SystemValue sv = it.next();
                put(sv.getName(), sv);
            }
        }
    }

    /**
     * Wrapper
     */
    public static class ObjectLockListEntryArrayList extends ArrayList<ObjectLockListEntry> {

        /**
         * ctor/1
         *
         * @param c list to array
         */
        public ObjectLockListEntryArrayList(ObjectLockListEntry[] c) {
            addAll(Arrays.asList(c));
        }
    }

    /**
     * Wrapper
     */
    public static class UserArrayList extends ArrayList<User> {

        /**
         * Instance a UAL from the enumeration returned by JTOpen Userlist class
         * (which is not itself iterable)
         *
         * @param e the enum returned by JTOpen
         * com.ibm.as400.access.Userlist.getUsers()
         */
        private UserArrayList(Enumeration e) {
            super();
            while (e.hasMoreElements()) {
                Object o = e.nextElement();
                if (o instanceof User) {
                    add(User.class.cast(o));
                } else {
                    throw new ClassCastException("UserArrayList couldn't convert an enum object " + o + "in its ctor.");
                }
            }
        }

        /**
         * ctor from UserList itself grabbing the enum
         *
         * @param ul A UserList object
         * @throws AS400SecurityException
         * @throws ErrorCompletingRequestException
         * @throws InterruptedException
         * @throws IOException
         * @throws ObjectDoesNotExistException
         * @throws RequestNotSupportedException
         */
        public UserArrayList(UserList ul) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException, RequestNotSupportedException {
            this(ul.getUsers());
        }

    }

    /**
     * Wrapper
     */
    public static class SubsystemArrayList extends ArrayList<Subsystem> {

        /**
         * ctor/1 construct on an array of Subsystem
         *
         * @param sarray
         */
        public SubsystemArrayList(Subsystem[] sarray) {
            addAll(Arrays.asList(sarray));
        }
    }

    /**
     * Wrapper
     */
    public static class VSMParmList extends ArrayList<VSMParm> {
    }

    /**
     * Typedef for handling lists of tn5250 sessions from tn5250j itself
     */
    public static class Session5250ArrayList extends ArrayList<Session5250> {

        /**
         *
         * @param c
         */
        public Session5250ArrayList(Collection<? extends Session5250> c) {
            super(c);
        }
    }

    /**
     * Wrapper
     */
    public static class RecordArrayList extends ArrayList<Record> {

        /**
         * ctor/0
         */
        public RecordArrayList() {
            super();
        }

        /**
         * ctor/1 construct on an array of Record
         *
         * @param ra the source record array
         */
        public RecordArrayList(Record[] ra) {
            this();
            addAll(Arrays.asList(ra));
        }
    }
}
