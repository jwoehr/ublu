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
package ublu.command;

import ublu.AS400Factory;
import ublu.Ublu;
import ublu.util.ArgArray;
import ublu.util.Interpreter;
import ublu.util.DataSink;
import ublu.util.Generics.TupleStack;
import ublu.util.Putter;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Superclass of all commands the interpreter understands.
 *
 * @see ublu.Ublu
 * @author jwoehr
 */
public abstract class Command implements CommandInterface {

    private boolean hasUnknownDashCommand;
    private AS400 as400;
    /**
     * Interpreter instance
     */
    private Interpreter myInterpreter;
    /**
     * Command commandName
     */
    private String commandName;
    /**
     * Command commandDescription
     */
    private String commandDescription;
    /**
     * Data source
     */
    private DataSink dataSrc;
    /**
     * Data dest
     */
    private DataSink dataDest;
    /**
     * result code
     */
    private COMMANDRESULT commandResult;

    /**
     * True if so set that the dash-command parsing found an unknown
     * dash-command.
     *
     * @return True if so set that the dash-command parsing found an unknown
     * dash-command.
     */
    protected boolean havingUnknownDashCommand() {
        return hasUnknownDashCommand;
    }

    /**
     * Set true by command if dash-command parsing default case found an unknown
     * dash-command.
     *
     * @param hasUnknownDashCommand true if dash-command parsing default case
     * found an unknown dash-command.
     */
    protected void setHasUnknownDashCommand(boolean hasUnknownDashCommand) {
        this.hasUnknownDashCommand = hasUnknownDashCommand;
    }

    /**
     * Get the AS400 instance (if any) associated with this command.
     *
     * @return the AS400 instance associated with this command or null if none
     */
    protected final AS400 getAs400() {
        return as400;
    }

    /**
     * Set the AS400 instance (if any) associated with this command.
     *
     * @param as400 the AS400 instance associated with this command or null if
     * none
     */
    protected final void setAs400(AS400 as400) {
        this.as400 = as400;
    }

    /**
     * Set the AS400 instance (if any) associated with this command from the arg
     * array's next tuple or pop. Gets the next tuple or pop from the arg array
     * and instances the as400 member if tuple value is of that class
     *
     * @param args the argument array for this command
     */
    protected final void setAs400fromTupleOrPop(ArgArray args) {
        this.as400 = args.nextTupleOrPop().value(AS400.class);
        // /* debug */ System.err.println(this.as400);
    }

    /**
     * Get the class of the instance so that static methods can be invoked
     *
     * @return the class of the instance
     */
    public Class<? extends Command> getCommandClass() {
        return this.getClass();
    }

    /**
     * Return result code
     *
     * @return result code
     */
    public COMMANDRESULT getCommandResult() {
        return commandResult;
    }

    /**
     * Set result code
     *
     * @param commandResult result code
     */
    public final void setCommandResult(COMMANDRESULT commandResult) {
        this.commandResult = commandResult;
    }

    /**
     * Get data source
     *
     * @return data source
     */
    protected DataSink getDataSrc() {
        return dataSrc;
    }

    /**
     * Set data source
     *
     * @param dataSrc data source
     */
    protected void setDataSrc(DataSink dataSrc) {
        this.dataSrc = dataSrc;
    }

    /**
     * Get data dest
     *
     * @return data dest
     */
    protected DataSink getDataDest() {
        return dataDest;
    }

    /**
     * Set data dest
     *
     * @param dataDest data dest
     */
    protected void setDataDest(DataSink dataDest) {
        this.dataDest = dataDest;
    }

    /**
     * Analyze next lex and return a new data sink. If the lex is "~" it means
     * pop the tuple stack for the data sink.
     *
     * @param argArray the interpreter arg array
     * @return a new data sink based on what was parsed
     */
    protected static DataSink newDataSink(ArgArray argArray) {
        return DataSink.fromSinkName(argArray.next());
    }

    /**
     * Set data dest to data sink specified by next in the arg array
     *
     * @param args the arg array
     */
    protected void setDataDestfromArgArray(ArgArray args) {
        setDataDest(newDataSink(args));
    }

    /**
     * Set data src to data sink specified by next in the arg array
     *
     * @param args the arg array
     */
    protected void setDataSrcfromArgArray(ArgArray args) {
        setDataSrc(newDataSink(args));
    }

    /**
     * Get interpreter
     *
     * @return interpreter
     */
    protected Interpreter getInterpreter() {
        return myInterpreter;
    }

    /**
     * Get the associated application controller instance
     *
     * @return associated application controller instance
     */
    protected Ublu getUblu() {
        return getInterpreter().getMyUblu();
    }

    /**
     * Set interpreter
     *
     * @param myInterpreter interpreter
     */
    @Override
    public final void setInterpreter(Interpreter myInterpreter) {
        this.myInterpreter = myInterpreter;
    }

    /**
     * Get the logger from the Interpreter instance
     *
     * @return the logger from the Interpreter instance
     */
    public Logger getLogger() {
        return getInterpreter().getLogger();
    }

    /**
     * Set a tuple value
     *
     * @param key
     * @param value
     * @return the tuple set or created
     */
    protected Tuple setTuple(String key, Object value) {
        return getInterpreter().setTuple(key, value);
    }

    /**
     * Get a tuple by key. If the key is ~ then pop tuple stack for a tuple
     *
     * @param key a tuple name or ~ for "pop the stack"
     * @return tuple the tuple or null
     */
    protected Tuple getTuple(String key) {
        Tuple t = null;
        if (key.equals(ArgArray.POPTUPLE)) {
            if (getTupleStack().size() > 0) {
                t = getTupleStack().pop();
            }
        } else {
            t = getInterpreter().getTuple(key);
        }
        return t;
    }

    /**
     * Fetch a Tuple by name and return its value if the value is an instance of
     * AS400, otherwise return null.
     *
     * @param key Tuple name
     * @return the AS400 instance or null
     */
    protected AS400 getAS400Tuple(String key) {
        AS400 anAs400 = null;
        Tuple t = getTuple(key);
        Object o = t.getValue();
        if (o instanceof AS400) {
            anAs400 = AS400.class.cast(o);
        }
        return anAs400;
    }

    /**
     * Get command commandName
     *
     * @return command commandName
     */
    @Override
    public String getCommandName() {
        return commandName;
    }

    /**
     * Set command commandName
     *
     * @param name
     */
    public final void setCommandName(String name) {
        commandName = name;
    }

    /**
     * Get command commandDescription
     *
     * @return command commandDescription
     */
    @Override
    public String getCommandDescription() {
        return commandDescription;
    }

    /**
     * Set command commandDescription
     *
     * @param description
     */
    protected final void setCommandDescription(String description) {
        commandDescription = description;
    }

    /**
     *
     * @param name
     * @param description
     */
    protected void setNameAndDescription(String name, String description) {
        setCommandName(name);
        setCommandDescription(description);
    }

    /**
     * Get commandName and commandDescription
     *
     * @return commandName and commandDescription
     */
    public final String getNameAndDescription() {
        StringBuilder sb = new StringBuilder(getCommandName());
        sb.append(getCommandDescription());
        return sb.toString();
    }

    /**
     * Set up the Command's instance data for command instance use.
     * <p>
     * Originally in the code the instances were re-used. So {@code reinit()} is
     * a mixture of inits that need to be done one time and some that don't
     * really need to be done unless the instance is used, which doesn't happen
     * anymore.</p>.
     */
    protected void reinit() {
        setAs400(null);
        setDataDest(new DataSink(DataSink.SINKTYPE.STD, null));
        setDataSrc(new DataSink(DataSink.SINKTYPE.STD, null));
        setCommandResult(COMMANDRESULT.SUCCESS);
        setHasUnknownDashCommand(false);
    }

    /**
     * 0-arity ctor
     */
    public Command() {
        setCommandName("No command name set");
        setCommandDescription("-No description was set for this command.");
    }

    /**
     * Put an object to the data destination
     *
     * @param o object to put
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     *
     */
    protected void put(Object o) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        new Putter(o, getInterpreter()).put(getDataDest());
    }

    /**
     * Put an object to the data destination flagging whether a newline added
     *
     * @param o object to put
     * @param newline true if a newline should be appended when writing to STD,
     * false if a space should be appended
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     *
     */
    protected void put(Object o, boolean newline) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        new Putter(o, getInterpreter()).put(getDataDest(), newline);
    }

    /**
     * Put an object to the data destination flagging whether a space postpended
     * and newline added
     *
     * @param o object to put
     * @param space true if a space should be appended
     * @param newline true if a newline should be appended when writing to STD
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     *
     */
    protected void put(Object o, boolean space, boolean newline) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        new Putter(o, getInterpreter()).put(getDataDest(), space, newline);
    }

    /**
     * Put an object to the data destination flagging whether a space postpended
     * and newline added
     *
     * @param o object to put
     * @param append true if append if data dest is file
     * @param space true if a space should be appended
     * @param newline true if a newline should be appended when writing to STD
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     *
     */
    protected void put(Object o, boolean append, boolean space, boolean newline) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        new Putter(o, getInterpreter()).put(getDataDest(), append, space, newline);
    }

    /**
     * Put an object to the data destination with the specific charset name to
     * use
     *
     * @param o object to put
     * @param charsetName specific charset name to use
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws InterruptedException
     * @throws ErrorCompletingRequestException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     *
     */
    protected void put(Object o, String charsetName) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        new Putter(o, getInterpreter(), charsetName).put(getDataDest());
    }

    /**
     * Log an error when there are insufficient arguments left in the
     * interpreter argument array to satisfy the command.
     *
     * @param argArray The argument array from the interpreter
     */
    protected final void logArgArrayTooShortError(ArgArray argArray) {
        getLogger().log(Level.SEVERE, "{0} represents too few arguments to {1}", new Object[]{argArray.size(), getNameAndDescription()});
    }

    /**
     * Log an error when no as400 instance has been provided
     *
     */
    protected final void logNoAs400() {
        getLogger().log(Level.SEVERE, "No as400 instance provided to {0}", getNameAndDescription());
    }

    /**
     * Parse the arg array for system username password in that order (each and
     * any tuples or plain words) and come back with an AS400 object that has
     * not yet attempted to log in.
     *
     * @param argArray the arg array to the command where the next three
     * elements (either tuple references or plain words) represent system userid
     * password
     * @return the AS400 object or null
     * @throws PropertyVetoException
     */
    protected AS400 as400FromArgs(ArgArray argArray) throws PropertyVetoException {
        String system = argArray.nextMaybeQuotationTuplePopStringTrim();
        String username = argArray.nextMaybeQuotationTuplePopStringTrim();
        String password = argArray.nextMaybeQuotationTuplePopStringTrim();
        return AS400Factory.newAS400(getInterpreter(), system, username, password);
    }

    /**
     * Parse the arg array for system username password in that order (each and
     * any tuples or plain words) and come back with an AS400 object that has
     * not yet attempted to log in.
     *
     * @param argArray the arg array to the command where the next three
     * elements (either tuple references or plain words) represent system userid
     * password
     * @param signon_security_type if set to SIGNON_SECURITY_TYPE.SSL use ssl
     * @return the AS400 object or null
     * @throws PropertyVetoException
     */
    protected AS400 as400FromArgs(ArgArray argArray, AS400Factory.SIGNON_SECURITY_TYPE signon_security_type) throws PropertyVetoException {
        String system = argArray.nextMaybeQuotationTuplePopString();
        String username = argArray.nextMaybeQuotationTuplePopString();
        String password = argArray.nextMaybeQuotationTuplePopString();
        return AS400Factory.newAS400(getInterpreter(), system, username, password, signon_security_type);
    }

    /**
     * Set our AS400 instance from instance created by parsing the arg array.
     *
     * @param argArray the arg array to the command where the next three strings
     * are system userid password
     * @throws PropertyVetoException
     */
    protected void setAs400FromArgs(ArgArray argArray) throws PropertyVetoException {
        setAs400(as400FromArgs(argArray));
    }

    /**
     * Extract as400 from a tuple.
     *
     * @param as400Tuple Tuple nominally holding AS400 instance
     * @return the AS400 object or null
     */
    protected AS400 as400FromTuple(Tuple as400Tuple) {
        AS400 result = null;
        if (as400Tuple != null) {
            Object o = as400Tuple.getValue();
            if (o instanceof AS400) {
                result = AS400.class.cast(o);
            }
        }
        return result;
    }

    /**
     * Set our AS400 instance from tuple.
     *
     * @param as400Tuple Tuple nominally holding AS400 instance
     */
    protected void setAs400FromTuple(Tuple as400Tuple) {
        setAs400(as400FromTuple(as400Tuple));
    }

    /**
     * Set an error flag in the default case for handling dash-commands.
     *
     * @param dashCommand the unknown dash-command
     */
    protected void unknownDashCommand(String dashCommand) {
        getLogger().log(Level.SEVERE, "Unknown dash-command {0} in {1}", new Object[]{dashCommand, getNameAndDescription()});
        setHasUnknownDashCommand(true);
    }

    /**
     * Get a string from whatever data sink is the datasrc, either reading one
     * line from a file, or toString'ing a tuple, or reading a string from the
     * arg array.
     *
     * @param argArray the command's argarray
     * @return a string parsed from whatever the data src is , or null if none
     * can be parsed.
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected String getStringFromDataSrc(ArgArray argArray) throws FileNotFoundException, IOException {
        String sendString = null;
        switch (getDataSrc().getType()) {
            case FILE:
                File f = new File(getDataSrc().getName());
                if (f.exists()) {
                    FileReader fr = new FileReader(f);
                    BufferedReader br = new BufferedReader(fr);
                    sendString = br.readLine();
                }
                break;
            case STD:
                if (!argArray.isEmpty()) {
                    sendString = argArray.nextMaybeQuotationTuplePopString();
                }
                break;
            case TUPLE:
                Tuple t = getTuple(getDataSrc().getName());
                if (t != null) {
                    sendString = t.getValue().toString();
                }
                break;
        }
        return sendString;
    }

    /**
     * Get the tuple stack maintained by the interpreter
     *
     * @return the tuple stack maintained by the interpreter
     */
    protected TupleStack getTupleStack() {
        return getInterpreter().getTupleStack();
    }

    /**
     * Push to the tuple stack maintained by the interpreter
     *
     * @param t tuple to push
     */
    protected void pushTuple(Tuple t) {
        getTupleStack().push(t);
    }

    /**
     * Don't allow null strings coming from non-existent tuples. Set command
     * failure if non-existent tuple provided.
     *
     * @param argArray the rest of the args
     * @return String value of tuple or null
     */
    protected String nextStringCheckingForNonExistentTuple(ArgArray argArray) {
        String result = null;
        if (argArray.peekNonExistentTuple()) {
            setCommandResult(COMMANDRESULT.FAILURE);
            getLogger().log(Level.SEVERE, "Non-existent tuple provided to {0}", getNameAndDescription());
        } else {
            result = argArray.nextMaybeQuotationTuplePopString();
        }
        return result;
    }

    /**
     * Don't allow nulls coming from non-existent tuples. Set command failure if
     * non-existent tuple provided.
     *
     * @param argArray the rest of the args
     * @return value of tuple or null
     */
    protected Object nextTupleValueCheckingForNonExistentTuple(ArgArray argArray) {
        Object a = null;
        if (argArray.peekNonExistentTuple()) {
            setCommandResult(COMMANDRESULT.FAILURE);
            getLogger().log(Level.SEVERE, "Non-existent tuple provided to {0}", getNameAndDescription());
        } else {
            a = getTuple(argArray.next()).getValue();
        }
        return a;
    }

    /**
     * Output a message to stderr
     *
     * @param message message
     */
    public void dbugmsg(String message) {
        getInterpreter().outputerrln(message);
    }
}
