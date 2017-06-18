/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
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
package ublu.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import ublu.Ublu;
import ublu.command.CommandInterface.COMMANDRESULT;
import ublu.command.CommandInterface;
import ublu.command.CommandMap;
import ublu.util.Generics.ConstMap;
import ublu.util.Generics.FunctorMap;
import ublu.util.Generics.InterpreterFrameStack;
import ublu.util.Generics.TupleNameList;
import ublu.util.Generics.TupleStack;
import ublu.util.Generics.UbluProgram;

/**
 * command interface object
 *
 * @author jwoehr
 */
public class Interpreter {

    private Ublu myUblu;
    private DBug myDBug;
    private History history;
    private String historyFileName;
    private ConstMap constMap;
    private TupleMap tupleMap;
    private CommandMap cmdMap;
    private FunctorMap functorMap;
    private boolean good_bye;
    private int global_ret_val;
    private InterpreterFrame interpreterFrame;
    private InterpreterFrameStack interpreterFrameStack;
    private TupleStack tupleStack;
    private boolean break_issued;
    private Props props;
    private long paramSubIndex = 0;
    private int instanceDepth = 0;
    private LocaleHelper localeHelper;

    /**
     *
     * @return
     */
    public final LocaleHelper getLocaleHelper() {
        return localeHelper;
    }

    /**
     *
     * @param localeHelper
     */
    public final void setLocaleHelper(LocaleHelper localeHelper) {
        this.localeHelper = localeHelper;
    }

    /**
     * Get the const map
     *
     * @return the const map
     */
    public ConstMap getConstMap() {
        return constMap;
    }

    /**
     * Set the const map
     *
     * @param constMap the const map
     */
    public void setConstMap(ConstMap constMap) {
        this.constMap = constMap;
    }

    /**
     * Get constant value from the map. Return null if not found.
     *
     * @param name name of const
     * @return the value of const
     */
    public String getConst(String name) {
        String result = null;
        Const theConst = constMap.get(name);
        if (theConst != null) {
            result = theConst.getValue();
        }
        return result;
    }

    /**
     * Set constant value in the map. Won't set a Const with a null value. Does
     * not prevent a Const in the map from being overwritten, CmdConst checks
     * first to make sure no Const of that name exists.
     *
     * @param name name of const
     * @param value value of const
     * @return true if const was created and stored to the ConstMap, false if a
     * null value was passed in and no Const was created nor stored to the
     * ConstMap.
     * @see ublu.util.Const
     * @see ublu.command.CmdConst
     */
    public boolean setConst(String name, String value) {
        boolean result = false;
        if (Const.isConstName(name) && value != null) {
            constMap.put(name, new Const(name, value));
            result = true;
        }
        return result;
    }

    /**
     * Get nested interpreter depth
     *
     * @return nested interpreter depth
     */
    public int getInstanceDepth() {
        return instanceDepth;
    }

    /**
     * Get properties
     *
     * @return properties
     */
    public Props getProps() {
        return props;
    }

    /**
     * Set properties
     *
     * @param props properties
     */
    protected final void setProps(Props props) {
        this.props = props;
    }

    /**
     * Test for a property's being set to string <code>true</code>
     *
     * @param key
     * @return true if set to string <code>true</code>
     */
    public boolean isBooleanProperty(String key) {
        return getProperty(key, "false").equalsIgnoreCase("true");
    }

    /**
     * Test for a debug subkey's being set to string <code>true</code>. The
     * string <code>dbug.</code> is prepended to the subkey.
     *
     * @param subkey
     * @return true if set to string <code>true</code>
     */
    public boolean isDBugBooleanProperty(String subkey) {
        return isBooleanProperty("dbug." + subkey);
    }

    /**
     * Return autoincrementing parameter substitute naming deus ex machina
     *
     * @return the value before postincrementation
     */
    protected long getParamSubIndex() {
        return paramSubIndex++;
    }

    /**
     * BREAK command just popped a frame
     *
     * @return true if BREAK command just popped a frame
     */
    public boolean isBreakIssued() {
        return break_issued;
    }

    /**
     * Indicate BREAK command just popped a frame
     *
     * @param break_issued true if BREAK command just popped a frame
     */
    public final void setBreakIssued(boolean break_issued) {
        this.break_issued = break_issued;
    }

    /**
     * Return the application instance
     *
     * @return the application instance
     */
    public final Ublu getMyUblu() {
        return myUblu;
    }

    /**
     * Set the application instance
     *
     * @param myUblu the application instance
     */
    public final void setMyUblu(Ublu myUblu) {
        this.myUblu = myUblu;
    }

    /**
     * True if command-line switch told us we're running under Goublu.
     *
     * @return True if command-line switch told us we're running under Goublu
     */
    public boolean isGoubluing() {
        return getMyUblu().isGoubluing();
    }

    /**
     * Return the logger for this interpreter
     *
     * @return the logger for this interpreter
     */
    public final Logger getLogger() {
        return getMyUblu().getLogger();
    }

    /**
     * Get the current input stream to the interpret
     *
     * @return my input stream
     */
    public final InputStream getInputStream() {
        return interpreterFrame.getInputStream();
    }

    /**
     * Set the current input stream to the interpret
     *
     * @param inputStream my input stream
     */
    public final void setInputStream(InputStream inputStream) {
        interpreterFrame.setInputStream(inputStream);
    }

    /**
     * Get output stream for the interpret
     *
     * @return output stream for the interpret
     */
    public PrintStream getOutputStream() {
        return interpreterFrame.getOutputStream();
    }

    /**
     * Set output stream for the interpret
     *
     * @param outputStream output stream for the interpret
     */
    public final void setOutputStream(PrintStream outputStream) {
        interpreterFrame.setOutputStream(outputStream);
    }

    /**
     * Get error stream for the interpret
     *
     * @return error stream for the interpret
     */
    public final PrintStream getErroutStream() {
        return interpreterFrame.getErroutStream();
    }

    /**
     * Set error stream for the interpret
     *
     * @param erroutStream error stream for the interpret
     */
    public final void setErroutStream(PrintStream erroutStream) {
        interpreterFrame.setErroutStream(erroutStream);
    }

    /**
     * Are we currently parsing a quoted string?
     *
     * @return true if currently parsing a "${" quoted string "}$"
     */
    public boolean isParsingString() {
        return interpreterFrame.isParsingString();
    }

    /**
     * True if we are currently parsing a quoted string.
     *
     * @param parsingString set true if currently parsing a "${" quoted string
     * "}$"
     */
    public final void setParsingString(boolean parsingString) {
        interpreterFrame.setParsingString(parsingString);
    }

    /**
     * Are we currently parsing an execution block?
     *
     * @return true if currently parsing an "$[" execution block "]$"
     */
    public boolean isParsingBlock() {
        return interpreterFrame.isParsingBlock();
    }

    /**
     * Return parsing block depth
     *
     * @return parsing block depth
     */
    public int getParsingBlockDepth() {
        return interpreterFrame.getParsingBlockDepth();
    }

    /**
     * True if we are currently parsing an execution block.
     *
     * @param parsingBlock set true if currently parsing an "$[" execution block
     * "]$"
     */
    public void setParsingBlock(boolean parsingBlock) {
        interpreterFrame.setParsingBlock(parsingBlock);
    }

    /**
     * Are we currently including a file of commands?
     *
     * @return true if currently including a file of commands
     */
    public boolean isIncluding() {
        return interpreterFrame.isIncluding();
    }

    /**
     * Echo lines of include file?
     *
     * @return true if we should be echoing include lines
     */
    public boolean isEchoInclude() {
        return getProperty("includes.echo", "true").equalsIgnoreCase("true");
    }

    /**
     * Echo lines of include file?
     *
     * @param tf
     */
    public void setEchoInclude(Boolean tf) {
        setProperty("includes.echo", tf.toString());
    }

    /**
     * Should we be prompting?
     *
     * @return true if we should be prompting
     */
    public boolean isPrompting() {
        return getProperty("prompting", "true").equalsIgnoreCase("true");
    }

    /**
     * Should we be prompting?
     *
     * @param tf
     */
    public void setPrompting(Boolean tf) {
        setProperty("prompting", tf.toString());
    }

    /**
     * Set the status of whether we are currently including a file of commands.
     *
     * @param including true if currently including a file of commands
     */
    public final void setIncluding(boolean including) {
        interpreterFrame.setIncluding(including);
    }

    /**
     * Get the basepath of the include file which is including
     *
     * @return basepath of the include file which is including
     */
    public Path getIncludePath() {
        return interpreterFrame.getIncludePath();
    }

    /**
     * Set the basepath of the include file which is including
     *
     * @param includePath basepath of the include file which is including
     */
    public void setIncludePath(Path includePath) {
        interpreterFrame.setIncludePath(includePath);
    }

    /**
     * Is the current block a FOR block or not?
     *
     * @return true iff the current block a FOR block
     */
    public final boolean isForBlock() {
        return interpreterFrame.isForBlock();
    }

    /**
     * Set the current block a FOR block or not.
     *
     * @param forBlock true if a FOR block
     * @return this
     */
    public final Interpreter setForBlock(boolean forBlock) {
        interpreterFrame.setForBlock(forBlock);
        return this;
    }

    /**
     * Get the buffered reader we are using to include a file of commands.
     *
     * @return the buffered reader we are using to include a file of commands
     */
    public BufferedReader getIncludeFileBufferedReader() {
        return interpreterFrame.getIncludeFileBufferedReader();
    }

    /**
     * Set the buffered reader we are using to include a file of commands.
     *
     * @param includeFileBufferedReader the buffered reader we are using to
     * include a file of commands
     */
    public final void setIncludeFileBufferedReader(BufferedReader includeFileBufferedReader) {
        interpreterFrame.setIncludeFileBufferedReader(includeFileBufferedReader);
    }

    /**
     * Get the buffered reader for the input stream when we don't have console
     *
     * @return the buffered reader for the input stream when we don't have
     * console
     */
    public BufferedReader getInputStreamBufferedReader() {
        return interpreterFrame.getInputStreamBufferedReader();
    }

    /**
     * Set the buffered reader for the input stream when we don't have console
     *
     * @param inputStreamBufferedReader the buffered reader for the input stream
     * when we don't have console
     */
    public final void setInputStreamBufferedReader(BufferedReader inputStreamBufferedReader) {
        interpreterFrame.setInputStreamBufferedReader(inputStreamBufferedReader);
    }

    /**
     * Get the history file name that will be used next time the history manager
     * is instanced.
     *
     * @return history file name
     */
    public final String getHistoryFileName() {
        return historyFileName;
    }

    /**
     * Set the history file name that will be used next time the history manager
     * is instanced without instancing history file
     *
     * @param historyFileName history file name
     */
    public final void setHistoryFileName(String historyFileName) {
        this.historyFileName = historyFileName;
    }

    /**
     * Get the history manager
     *
     * @return the history manager
     */
    public History getHistory() {
        return history;
    }

    /**
     * Set the history manager
     *
     * @param history the history manager
     */
    public final void setHistory(History history) {
        this.history = history;
    }

    /**
     * Close the history file.
     */
    public final void closeHistory() {
        try {
            if (getHistory() != null) {
                getHistory().close();
                setHistory(null);
            }
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Error closing history file", ex);
        }
    }

    /**
     * Close any old history file and open a new one with the name we have set
     * via {@link #setHistoryFileName(String)}.
     *
     */
    public final void instanceHistory() {
        closeHistory();
        try {
            setHistory(new History(this, getHistoryFileName()));
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Could not open history file", ex);
        }
    }

    /**
     * A return value for the interpret, not currently used.
     *
     * @return the global return value
     */
    public int getGlobal_ret_val() {
        return global_ret_val;
    }

    /**
     * A return value for the interpret, not currently used.
     *
     * @param global_ret_val the global return value
     */
    public final void setGlobal_ret_val(int global_ret_val) {
        this.global_ret_val = global_ret_val;
    }

    /**
     * Signals the interpret to exit
     *
     * @return true is we're done
     */
    public boolean isGoodBye() {
        return good_bye;
    }

    /**
     * Signals the interpret to exit
     *
     * @param good_bye true if we're done
     */
    public final void setGoodBye(boolean good_bye) {
        this.good_bye = good_bye;
    }

    /**
     * The argument array of lexes from input
     *
     * @return my arg array
     */
    public final ArgArray getArgArray() {
        return interpreterFrame.getArgArray();
    }

    /**
     * The argument array of lexes from input
     *
     * @param argArray my arg array
     */
    public final void setArgArray(ArgArray argArray) {
        interpreterFrame.setArgArray(argArray);
    }

    /**
     * The map of key-value pairs used as tuple variables in Ublu
     *
     * @return The map of key-value pairs
     */
    public final TupleMap getTupleMap() {
        return tupleMap;
    }

    /**
     * The map of key-value pairs used as tuple variables in Ublu
     *
     * @param tupleMap The map of key-value pairs
     */
    protected final void setTupleMap(TupleMap tupleMap) {
        this.tupleMap = tupleMap;
    }

    /**
     * Get the tuple var assigned to a key
     *
     * @param key
     * @return the tuple var assigned to a key
     */
    public Tuple getTuple(String key) {
        return tupleMap.getTuple(key);
    }

    /**
     * Get the tuple var assigned to a key but don't check local map
     *
     * @param key
     * @return the tuple var assigned to a key
     */
    public Tuple getTupleNoLocal(String key) {
        return tupleMap.getTupleNoLocal(key);
    }

    /**
     * Put a tuple to the most local map
     *
     * @param t the tuple
     * @return the tuple
     */
    public Tuple putTupleMostLocal(Tuple t) {
        return tupleMap.putTupleMostLocal(t);
    }

    /**
     * Set a tuple var from a key and a value
     *
     * @param key tuple key
     * @param value tuple object value
     * @return the tuple which was created or set
     */
    public Tuple setTuple(String key, Object value) {
        return tupleMap.setTuple(key, value);
    }

    /**
     * Set a tuple var from a key and a value but only in global map
     *
     * @param key tuple key
     * @param value tuple object value
     * @return the tuple which was created or set
     */
    public Tuple setTupleNoLocal(String key, Object value) {
        return tupleMap.setTupleNoLocal(key, value);
    }

    /**
     * Remove a tuple from the tuple map
     *
     * @param t the tuple to remove
     * @return the tuple's object value
     */
    public Object deleteTuple(Tuple t) {
        tupleMap.deleteTuple(t);
        return t.getValue();
    }

    /* Tuples we start with already instanced */
    private void defaultTuples() {
        setTuple("@true", true);
        setTuple("@false", false);
    }

    /**
     * Get the map of commands
     *
     * @return the map of commands
     */
    public final CommandMap getCmdMap() {
        return cmdMap;
    }

    /**
     * Set the map of commands
     *
     * @param cmdMap the map of commands
     */
    protected final void setCmdMap(CommandMap cmdMap) {
        this.cmdMap = cmdMap;
    }

    /**
     * Get the command object matching the name
     *
     * @param i
     * @param cmdName name of command
     * @return the command object corresponding to the name
     */
    public CommandInterface getCmd(Interpreter i, String cmdName) {
        return getCmdMap().getCmd(i, cmdName);
    }

    /**
     * Local function dictionary
     *
     * @return Local function dictionary
     */
    public FunctorMap getFunctorMap() {
        return functorMap;
    }

    /**
     * Local function dictionary
     *
     * @param functorMap Local function dictionary
     */
    public final void setFunctorMap(FunctorMap functorMap) {
        this.functorMap = functorMap;
    }

    /**
     * Add named function to dictionary
     *
     * @param name name of function
     * @param f functor
     */
    public void addFunctor(String name, Functor f) {
        getFunctorMap().put(name, f);
    }

    /**
     * Get named functor from dictionary
     *
     * @param name name of function
     * @return the functor
     */
    public Functor getFunctor(String name) {
        return getFunctorMap().get(name);
    }

    /**
     * True if name is in dictionary
     *
     * @param name name of function
     * @return true iff name is in dictionary
     */
    public boolean hasFunctor(String name) {
        return getFunctorMap().containsKey(name);
    }

    /**
     * Get the tuple stack
     *
     * @return the tuple stack
     */
    public TupleStack getTupleStack() {
        return tupleStack;
    }

    /**
     * Set the tuple stack
     *
     * @param tupleStack the tuple stack
     */
    public final void setTupleStack(TupleStack tupleStack) {
        this.tupleStack = tupleStack;
    }

    /**
     * Do we have a console (or are we reading from a stream)?
     *
     * @return true if console active
     */
    public boolean isConsole() {
        return System.console() != null && getInputStream() == System.in;
    }

    /**
     * Instance with args ready for the {@link #interpret} to start its first
     * {@link #loop}.
     *
     * @param args arguments at invocation, effectively just another command
     * line
     * @param ublu the associated instance
     */
    public Interpreter(String[] args, Ublu ublu) {
        this(ublu);
        setArgArray(new ArgArray(this, args));
    }

    /**
     * Instance with the application controller and passed-in args (commands)
     * set.
     *
     * @param args passed-in args (commands)
     * @param ublu the application controller
     */
    public Interpreter(String args, Ublu ublu) {
        this(ublu);
        setArgArray(new Parser(this, args).parseAnArgArray());

    }

    /**
     * Instance with only the application controller set
     *
     * @param ublu the application controller
     */
    public Interpreter(Ublu ublu) {
        this();
        setMyUblu(ublu);
    }

    /**
     * Copy ctor creates Interpreter with same maps i/o.
     *
     * @param i interpreter to be copied
     */
    public Interpreter(Interpreter i) {
        this();
        instanceDepth = i.instanceDepth + 1;
        setInputStream(i.getInputStream());
        setInputStreamBufferedReader(i.getInputStreamBufferedReader());
        setErroutStream(i.getErroutStream());
        setOutputStream(i.getOutputStream());
        setTupleMap(i.getTupleMap());
        setCmdMap(i.getCmdMap());
        setFunctorMap(i.getFunctorMap());
        setHistoryFileName(i.getHistoryFileName());
        setMyUblu(i.getMyUblu());
        setProps(i.getProps());
        constMap = new ConstMap(i.constMap);
        setLocaleHelper(new LocaleHelper(i.localeHelper));
    }

    /**
     * Copy ctor New instance spawned from another instance with args passed in
     *
     * @param i Instance to spawn from
     * @param args the args
     */
    public Interpreter(Interpreter i, String args) {
        this(i);
        setArgArray(new Parser(this, args).parseAnArgArray());
    }

    /**
     * Initialize internals such as tuple map to store variables.
     */
    protected Interpreter() {
        interpreterFrame = new InterpreterFrame();
        interpreterFrameStack = new InterpreterFrameStack();
        setTupleStack(new TupleStack());
        setInputStream(System.in);
        setInputStreamBufferedReader(new BufferedReader(new InputStreamReader(getInputStream())));
        setErroutStream(System.err);
        setOutputStream(System.out);
        setTupleMap(new TupleMap());
        defaultTuples();
        setCmdMap(new CommandMap());
        setFunctorMap(new FunctorMap());
        setParsingString(false);
        setForBlock(false);
        setBreakIssued(false);
        setIncluding(false);
        setIncludeFileBufferedReader(null);
        setHistoryFileName(History.DEFAULT_HISTORY_FILENAME);
        setGlobal_ret_val(0);
        setGoodBye(false);
        props = new Props();
        myDBug = new DBug(this);
        constMap = new ConstMap();
        setLocaleHelper(new LocaleHelper(null, null, "ublu.resource.MessageBundle"));
    }

    /**
     * Get dbug instance
     *
     * @return the DBug instance
     */
    public DBug dbug() {
        return myDBug;
    }

    /**
     *
     * @param filepath
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void readProps(String filepath) throws FileNotFoundException, IOException {
        props.readIn(filepath);
    }

    /**
     *
     * @param filepath
     * @param comment
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void writeProps(String filepath, String comment) throws FileNotFoundException, IOException {
        props.writeOut(filepath, comment);
    }

    /**
     * get a property
     *
     * @param key
     * @return the property
     */
    public String getProperty(String key) {
        return props.get(key);
    }

    /**
     * get Property return a default value if non
     *
     * @param key sought
     * @param defaultValue default
     * @return value
     */
    public final String getProperty(String key, String defaultValue) {
        return props.get(key, defaultValue);
    }

    /**
     * Set key to value
     *
     * @param key key
     * @param value value
     */
    public void setProperty(String key, String value) {
        props.set(key, value);
    }

    /**
     * Get set of keys
     *
     * @return set of keys
     */
    public Set propertyKeys() {
        return props.keySet();
    }

    /**
     * Push a new local tuple map
     */
    public void pushLocal() {
        getTupleMap().pushLocal();
    }

    /**
     * Pop the local tuple map
     */
    public void popLocal() {
        getTupleMap().popLocal();
    }

    /**
     * Push an interpreter frame
     *
     * @return this
     */
    public Interpreter pushFrame() {
        InterpreterFrame newFrame = new InterpreterFrame(interpreterFrame);
        interpreterFrameStack.push(interpreterFrame);
        getTupleMap().pushLocal();
        interpreterFrame = newFrame;
        return this;
    }

    /**
     * Pop interpreter frame
     *
     * @return this
     */
    public Interpreter popFrame() {
        interpreterFrame = interpreterFrameStack.pop();
        getTupleMap().popLocal();
//        if (getTupleMap().getLocalMap() != null) {
//            getTupleMap().setLocalMap(null);
//        }
        return this;
    }

    /**
     * How many frames have been pushed?
     *
     * @return How many frames have been pushed
     */
    public int getFrameDepth() {
        return interpreterFrameStack.size();
    }

    /**
     * Print a string to out
     *
     * @param s to print
     */
    public void output(String s) {
        getOutputStream().print(s);
    }

    /**
     * Print a string and a newline to out
     *
     * @param s to print
     */
    public void outputln(String s) {
        getOutputStream().println(s);
    }

    /**
     * Print a string to err
     *
     * @param s to print
     */
    public void outputerr(String s) {
        getErroutStream().print(s);
    }

    /**
     * Print a string and a newline to err
     *
     * @param s to print
     */
    public void outputerrln(String s) {
        getErroutStream().println(s);
    }

    /**
     * Execute a block
     *
     * @param block the block to execute
     * @return command result
     */
    public COMMANDRESULT executeBlock(String block) {
        COMMANDRESULT rc;
        pushFrame();
        int deep = getFrameDepth();
        Parser p = new Parser(this, block);
        setArgArray(p.parseAnArgArray());
        rc = loop();
        if (deep <= getFrameDepth()) {
            // /* debug */ outputerrln("about to pop frame in executeBlock");
            popFrame();
            // /* debug */ outputerrln("popped frame in executeBlock");
        }

        // }
        return rc;
    }

    /**
     * Create a tuple name list from the param array to a function
     *
     * @return tuple name list from the param array to a function
     */
    public TupleNameList parseTupleNameList() {
        TupleNameList tnl = null;
        if (!getArgArray().peekNext().equals("(")) {
            getLogger().log(Level.SEVERE, "Missing ( parameter list ) function call.");
        } else {
            getArgArray().next(); // discard "("
            tnl = new TupleNameList();
            while (!getArgArray().peekNext().equals(")")) {
                tnl.add(getArgArray().nextAssimilableElement());
            }
            getArgArray().next(); // discard ")"
        }
        return tnl;
    }

    /**
     * Execute a functor
     *
     * @param f the functor
     * @param tupleNames list of names to sub for params
     * @return command result
     */
    public COMMANDRESULT executeFunctor(Functor f, TupleNameList tupleNames) {
        COMMANDRESULT rc;
        if (tupleNames.size() != f.numParams()) {
            getLogger().log(Level.SEVERE, "Unable to execute functor {0}\n which needs {1} params but received {2}", new Object[]{f, f.numParams(), tupleNames.toString()});
            rc = COMMANDRESULT.FAILURE;
        } else {
            // rc = executeBlock(f.bind(tupleNames.delifoize(this)));
            // getTupleMap().pushLocal();

            pushFrame();
            String block = f.bindWithSubstitutes(this, tupleNames/*.delifoize(this)*/);
            rc = executeBlock(block);
            // getTupleMap().popLocal();
            // /* debug */ outputerrln("about to pop frame in executeFunctor");
            // /* debug */ outputerrln("Frame depth in executeFunctor : " + frameDepth());
            // if (frameDepth() > 0) {
            popFrame();
            //  }
            // /* debug */ outputerrln("popped frame in executeFunctor");
        }
        return rc;
    }

    /**
     * Show one function in a code re-usable form
     *
     * @param funcName function to show
     * @return the function in a code re-usable form
     */
    public String showFunction(String funcName) {
        return getFunctorMap().showFunction(funcName);
    }

    /**
     * List all named functions
     *
     * @return String describing all known functions
     */
    public String listFunctions() {
        return getFunctorMap().listFunctions();
    }

    /**
     * Remove a function from the dictionary
     *
     * @param name function name
     * @return boolean if was found (and removed)
     */
    public boolean deleteFunction(String name) {
        return getFunctorMap().deleteFunction(name);
    }

    /**
     * The processing loop, processes all the input for a line until exhausted
     * or until a command returns a command result indicating failure.
     *
     * @return the last command result indicating success or failure.
     */
    public COMMANDRESULT loop() {
        COMMANDRESULT lastCommandResult = COMMANDRESULT.SUCCESS;
        String initialCommandLine = getArgArray().toHistoryLine();
        while (!getArgArray().isEmpty() && !good_bye && !isBreakIssued()) {
            // /* Debug */ System.err.println(" arg array is " + getArgArray());
            if (getArgArray().isNextTupleNameOrPop()) {
                Tuple t = getArgArray().peekNextTupleOrPop();
                if (Autonome.autonomize(t, getArgArray())) {
                    continue;
                } else {
                    getLogger().log(Level.SEVERE, "non-autonomized tuple or pop : {0}", getArgArray().next());
                    lastCommandResult = COMMANDRESULT.FAILURE;
                    break;
                }
            }
            String commandName = getArgArray().next().trim();
            if (commandName.equals("")) {
                continue; // cr or some sort of whitespace got parsed, skip to next
            }
            if (getCmdMap().containsKey(commandName)) {
                CommandInterface command = getCmd(this, commandName);
                try {
                    setArgArray(command.cmd(getArgArray()));
                    lastCommandResult = command.getResult();
                    if (lastCommandResult == COMMANDRESULT.FAILURE) {
                        break; // we exit the loop on error
                    }
                } catch (IllegalArgumentException ex) {
                    getLogger().log(Level.SEVERE, "Command \"" + commandName + "\" threw exception", ex);
                    lastCommandResult = COMMANDRESULT.FAILURE;
                    break;
                } catch (java.lang.RuntimeException ex) {
                    /* java.net.UnknownHostException lands here, as well as  */
 /* com.ibm.as400.access.ExtendedIllegalArgumentException */
                    getLogger().log(Level.SEVERE, "Command \"" + commandName + "\" threw exception", ex);
                    lastCommandResult = COMMANDRESULT.FAILURE;
                    break;
                }
            } else if (getFunctorMap().containsKey(commandName)) {
                try {
                    TupleNameList tnl = parseTupleNameList();
                    if (tnl != null) {
                        lastCommandResult = executeFunctor(getFunctor(commandName), tnl);
                        if (lastCommandResult == COMMANDRESULT.FAILURE) {
                            break;
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Found function {0} but could not execute it", commandName);
                        lastCommandResult = COMMANDRESULT.FAILURE;
                        break;
                    }
                } catch (java.lang.RuntimeException ex) {
                    getLogger().log(Level.SEVERE, "Function \"" + commandName + "\" threw exception", ex);
                    lastCommandResult = COMMANDRESULT.FAILURE;
                    break;
                }
            } else {
                getLogger().log(Level.SEVERE, "Command \"{0}\" not found.", commandName);
                lastCommandResult = COMMANDRESULT.FAILURE;
                break;
            }
        }
        if (!isIncluding() && !initialCommandLine.isEmpty()) {
            if (getHistory() != null) {
                try {
                    getHistory().writeLine(initialCommandLine);
                } catch (IOException ex) {
                    getLogger().log(Level.WARNING, "Couldn't write to history file " + getHistory().getHistoryFileName(), ex);
                }
            }
        }
        setGlobal_ret_val(lastCommandResult.ordinal());
        return lastCommandResult;
    }

    /**
     * Include a program already parsed into lines
     *
     * @param api400prog an instance of a an array of lines to be treated as a
     * program
     * @return the command result
     */
    public COMMANDRESULT include(UbluProgram api400prog) {
        setIncluding(true);
        COMMANDRESULT commandResult = COMMANDRESULT.SUCCESS;
        for (String line : api400prog) {
            if (isEchoInclude()) {
                getErroutStream().println(":: " + line);
            }
            if (!line.isEmpty()) {
                ArgArray aa = new Parser(this, line).parseAnArgArray();
                setArgArray(aa);
                commandResult = loop();
                if (commandResult == COMMANDRESULT.FAILURE) {
                    break;
                }
            }
        }
        setIncluding(false);
        return commandResult;
    }

    /**
     * Read in a text file and execute as commands, searching ublu.includepath
     * for relative paths.
     *
     * @param filepath Path to the file of commands
     * @return last command result
     * @throws FileNotFoundException
     * @throws IOException
     */
    public COMMANDRESULT include(Path filepath) throws FileNotFoundException, IOException {
        pushFrame();

        boolean foundPath = false;

        // First order of business is to search the search paths
        if (!filepath.isAbsolute()) {
            for (String searchPart : getProperty("ublu.includepath").split(":")) {
                Path searchPath = FileSystems.getDefault().getPath(searchPart).resolve(filepath);
                if (searchPath.toFile().exists()) {
                    foundPath = true;
                    filepath = searchPath.normalize();
                    break;
                }
            }
        }
        // If the path wasn't found or is absolute, try the current path as is
        // (allowing failure to raise an exception)
        if (!foundPath) {
            Path currentIncludePath = getIncludePath();
            if (currentIncludePath != null) {
                filepath = currentIncludePath.resolve(filepath.normalize());
            }
        }
        setIncludePath(filepath.getParent());
        setIncluding(true);
        COMMANDRESULT commandResult = COMMANDRESULT.SUCCESS;
        File file = filepath.normalize().toFile();
        try (FileReader fileReader = new FileReader(file); BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            setIncludeFileBufferedReader(bufferedReader);
            while (getIncludeFileBufferedReader().ready()) {
                String input = getIncludeFileBufferedReader().readLine();
                if (isEchoInclude()) {
                    getErroutStream().println(":: " + input);
                }
                if (!input.isEmpty()) {
                    ArgArray aa = new Parser(this, input).parseAnArgArray();
                    setArgArray(aa);
                    commandResult = loop();
                    if (commandResult == COMMANDRESULT.FAILURE) {
                        getLogger().log(Level.SEVERE, "Error in include : {0}", input);
                        break;
                    }
                }
            }
        }
        setIncludeFileBufferedReader(null);
        setIncluding(false);
        popFrame();
        return commandResult;
    }

    /**
     * Read a line, parse its whitespace-separated lexes into an
     * {@link ublu.util.ArgArray}.
     *
     * @return the {@link ublu.util.ArgArray} thus parsed
     */
    public ArgArray readAndParse() {
        String input = null;
        ArgArray aa = new ArgArray(this);
        if (isIncluding()) {
            try {
                if (getIncludeFileBufferedReader() != null) {
                    if (getIncludeFileBufferedReader().ready()) {
                        input = getIncludeFileBufferedReader().readLine();
                        if (isParsingString() || isParsingBlock()) {
                            if (isPrompting()) {
                                outputerrln(input);
                            }
                        }
                    }
                } else {
                    setIncluding(false);
                    input = ""; // so we won't exit on file failure because input == null;
                }
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "IO error reading included file", ex);
            }
        } else if (isConsole()) {
            input = System.console().readLine();
        } else { // we're reading from the input stream
            try {
                input = getInputStreamBufferedReader().readLine();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Error reading interpreter input from input stream", ex);
            }
        }
        if (input != null) {
            input = input.trim();
            aa = new Parser(this, input).parseAnArgArray();
        } else {
            setGoodBye(true);
        }
        setArgArray(aa);
        return getArgArray();
    }

    /**
     * interactive loop until bye
     *
     * @return a global return value from the interpret loop
     */
    public int interpret() {
        while (!isGoodBye()) {
            prompt();
            readAndParse();
            loop();
            if (isGoodBye() && (isConsole() || isGoubluing()) && instanceDepth == 0) {
                outputerrln("Goodbye!");
            }
        }
        return getGlobal_ret_val();
    }

    /**
     * Prompt the user for input. Usually just a <tt>&gt;</tt> sign, but when
     * parsing a multiline <tt>${ quoted string }$</tt> an intermediate
     * continuation prompt of <tt>${</tt>
     */
    public void prompt() {
        if (isPrompting()) {
            StringBuilder sb = new StringBuilder(instanceDepth == 0 ? "" : Integer.toString(instanceDepth)).append("> ");
            String thePrompt = sb.toString();
            if (isParsingString()) {
                thePrompt = "(${) ";
            }
            if (isParsingBlock()) {
                sb = new StringBuilder().append('(');
                for (int i = 0; i < getParsingBlockDepth(); i++) {
                    sb.append("$[");
                }
                sb.append(") ");
                thePrompt = sb.toString();
            }
            if (isConsole()) {
                outputerr(thePrompt);
            } else if (isGoubluing()) {
                outputerrln(thePrompt);
            }
        }
    }
}
