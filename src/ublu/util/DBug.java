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
import ublu.util.Generics.StringArrayList;

/**
 * Class to issue debug messages that can be turned on and off by setting
 * properties.
 *
 * @author jwoehr
 */
public class DBug {

    static private boolean isDBug(Interpreter interpreter, String dbugkey) {
        return interpreter.isDBugBooleanProperty(dbugkey);
    }

    /**
     * Display debug info on a tuple
     *
     * @param interpreter
     * @param t the Tuple
     */
    static protected void dbugTuple(Interpreter interpreter, Tuple t) {
        if (isDBug(interpreter, "tuple")) {
            StringBuilder sb = new StringBuilder("Tuple ").append(t);
            interpreter.getErroutStream().println(sb);
        }
    }

    /**
     * Display debug info on a tuple map
     *
     * @param interpreter
     * @param map the tuple map
     */
    static protected void dbugTupleMap(Interpreter interpreter, TupleMap map) {
        if (isDBug(interpreter, "tuple.map")) {
            StringBuilder sb = new StringBuilder("TupleMap ")
                    .append(map)
                    .append("\n")
                    .append("Keys: ").append(map.keysAsDisplayString())
                    .append("\n")
                    .append("Local Keys: ").append(map.localKeysAsDisplayString());
            interpreter.getErroutStream().println(sb);
        }
    }

    /**
     * Display debug info on a tuple with a prepended message
     *
     * @param message prepended message
     * @param interpreter
     * @param t the tuple
     */
    static protected void dbugTuple(String message, Interpreter interpreter, Tuple t) {
        if (isDBug(interpreter, "tuple")) {
            StringBuilder sb = new StringBuilder(message);
            sb.append("Tuple ").append(t);
            interpreter.getErroutStream().println(sb);
        }
    }

    /**
     * Display debug info on a tuple map with a prepended message
     *
     * @param message prepended message
     * @param interpreter
     * @param map the tuple map
     */
    static protected void dbugTupleMap(String message, Interpreter interpreter, TupleMap map) {
        if (isDBug(interpreter, "tuple.map")) {
            StringBuilder sb = new StringBuilder(message);
            sb.append("TupleMap ")
                    .append(map)
                    .append("\n")
                    .append("Keys: ").append(map.keysAsDisplayString())
                    .append("\n")
                    .append("Local Keys: ").append(map.localKeysAsDisplayString());
            interpreter.getErroutStream().println(sb);
        }
    }
    private Interpreter myInterpreter;
    private DBugInterpreter myDBugInterpreter;
    private BrkInterpreter myBrkInterpreter;
    private boolean stepping;
    private boolean onBrk;

    /**
     * Get our hosting interpreter instance
     *
     * @return hosting interpreter instance
     */
    protected Interpreter getHostInterpreter() {
        return myInterpreter;
    }

    /**
     *
     * @return
     */
    protected boolean isStepping() {
        return stepping;
    }

    /**
     *
     * @param stepping
     */
    public void setStepping(boolean stepping) {
        this.stepping = stepping;
    }

    /**
     *
     * @return
     */
    protected boolean isOnBrk() {
        return onBrk;
    }

    /**
     *
     * @param onBrk
     */
    public void setOnBrk(boolean onBrk) {
        this.onBrk = onBrk;
    }

    /**
     * ctor/0
     */
    protected DBug() {
        myBrkInterpreter = new BrkInterpreter(this);
    }

    /**
     * ctor/1 open on interpreter
     *
     * @param myInterpreter
     */
    public DBug(Interpreter myInterpreter) {
        this();
        this.myInterpreter = myInterpreter;
    }
    private StringArrayList breakpoints;

    /**
     * Reinitialize to defaults
     */
    public void reinit() {
        if (breakpoints != null) {
            breakpoints.clear();
        }
        setStepping(false);
        setOnBrk(false);
    }

    /**
     *
     * @param name
     * @return
     */
    protected boolean isBreakpoint(String name) {
        boolean result = false;
        if (breakpoints != null) {
            result = breakpoints.contains(name.trim());
        }
        return result;
    }

    /**
     *
     * @param name
     */
    public void setBreakpoint(String name) {
        if (breakpoints == null) {
            breakpoints = new StringArrayList();
        }
        name = name.trim();
        if (!breakpoints.contains(name)) {
            breakpoints.add(name);
        }
    }

    /**
     *
     * @param name
     * @return
     */
    public boolean clearBreakpoint(String name) {
        boolean result = false;
        if (breakpoints != null) {
            result = breakpoints.remove(name.trim());
        }
        return result;
    }

    /**
     * Execute a block in the dbug interpreter
     *
     * @param block to execute
     * @return the command result
     */
    public CommandInterface.COMMANDRESULT dbugBlockExecution(String block) {
        myDBugInterpreter = new DBugInterpreter(myInterpreter);
        CommandInterface.COMMANDRESULT cmdr = myDBugInterpreter.executeBlock(block);
        // myDBugInterpreter = null;
        return cmdr;
    }

    /**
     * Set a quick quit for the dbug interpreter
     */
    public void quit() {
        if (myDBugInterpreter != null) {
            myDBugInterpreter.setArgArray(new ArgArray(myDBugInterpreter));
            myDBugInterpreter.setQuickQuit(true);
        }
    }

    /**
     * Refer output of s string to the attached regular Interpreter instance
     *
     * @param s
     */
    public void output(String s) {
        myInterpreter.output(s);
    }

    /**
     * Refer output of s string with a newline to the attached regular
 Interpreter instance
     *
     * @param s
     */
    public void outputln(String s) {
        myInterpreter.outputln(s);
    }

    /**
     * Interpret lite during a breakpoint
     *
     * @param commandName the current command which was the breakpoint
     * @param argArray the rest of the arg array
     */
    public void breakInterpret(String commandName, ArgArray argArray) {
        // setStepping(true);
        setOnBrk(true);
        setStepping(true);
        myBrkInterpreter.brkInterpret(commandName, argArray);
    }

    /**
     * Get a tuple from the debug interp instance
     *
     * @param tuplename name
     * @return the tuple
     */
    public Tuple getTuple(String tuplename) {
        return myDBugInterpreter.getTuple(tuplename);
    }

    /**
     * Display debug info on a tuple map .IFF. the host interpreter's properties
     * are set so.
     *
     * @param message
     * @param map the tuple map
     */
    public void dbugTupleMap(String message, TupleMap map) {
        dbugTupleMap(message, myInterpreter, map);
    }

    /**
     * Display debug info on a tuple with a prepended message .IFF. the host
     * interpreter's properties are set so.
     *
     * @param message prepended message
     * @param t the tuple
     */
    public void dbugTuple(String message, Tuple t) {
        dbugTuple(message, myInterpreter, t);
    }

    /**
     * Display a message .IFF. the host interpreter's properties are set so.
     *
     * @param message message to display
     */
    public void dbugMessage(String message) {
        if (isDBug(myInterpreter, "msgs")) {
            String s = "dbug.msgs : " + message;
            myInterpreter.getErroutStream().println(s);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString())
                .append('\n')
                .append("stepping : ").append(isStepping()).append('\n')
                .append("breaking : ").append(isOnBrk()).append('\n')
                .append("breakpoints:\n")
                .append("------------\n");
        if (breakpoints != null) {
            for (String s : breakpoints) {
                sb.append(" ").append(s).append('\n');
            }
        } else {
            sb.append("(none)\n");
        }
        return sb.toString();
    }
}
