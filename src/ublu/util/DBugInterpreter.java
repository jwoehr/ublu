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

import ublu.Ublu;
import ublu.command.CommandInterface;
import java.io.IOException;
import java.util.logging.Level;

/**
 * A version of the interpreter slightly modified for single-step debugging.
 *
 * @author jwoehr
 */
public class DBugInterpreter extends Interpreter {

    private Interpreter myHostInterpreter;
    private boolean quickQuit = false;

    /**
     * Flag a quick quit
     *
     * @param quickQuit true if quick quit
     */
    public void setQuickQuit(boolean quickQuit) {
        this.quickQuit = quickQuit;
    }

    @Override
    public DBug dbug() {
        return myHostInterpreter.dbug();
    }

    private DBugInterpreter() {
    }

    private DBugInterpreter(String args, Ublu ublu) {
        super(args, ublu);
    }

    private DBugInterpreter(Ublu ublu) {
        super(ublu);
    }

    /**
     * Interpret stopping at breakpoints and invoking the brk interpreter
     *
     * @param i the host interpreter
     */
    public DBugInterpreter(Interpreter i) {
        super(i);
        myHostInterpreter = i;
        if (getProperty("dbug.usehostenv", "false").equalsIgnoreCase("true")) {
            setTupleMap(myHostInterpreter.getTupleMap());
            setFunctorMap(myHostInterpreter.getFunctorMap());
        }
    }

    /**
     * The debug processing loop, processes all the input for a line until
     * exhausted or until a command returns a command result indicating failure
     * breaking at breakpoints to step.
     *
     * @return the last command result indicating success or failure.
     */
    @Override
    public CommandInterface.COMMANDRESULT loop() {
//        dbug().dbugMessage("in DBugInterpreter.loop()");
        CommandInterface.COMMANDRESULT lastCommandResult = CommandInterface.COMMANDRESULT.SUCCESS;
        String initialCommandLine = getArgArray().toHistoryLine();
        while (!getArgArray().isEmpty() && !isGoodBye() && !isBreakIssued()) {
            String commandName = getArgArray().next().trim();
//            dbug()
//                    .dbugMessage(
//                    "command name is " + commandName
//                    + " stepping is " + dbug().isStepping()
//                    + " is breakpoint? " + dbug().isBreakpoint(commandName));
            if (dbug().isStepping() || dbug().isBreakpoint(commandName)) {
                dbug().breakInterpret(commandName, getArgArray());
            }
            if (quickQuit) {
                break;
            }
            if (commandName.equals("")) {
                continue; // cr or some sort of whitespace got parsed, skip to next
            }
            if (getCmdMap().containsKey(commandName)) {
                CommandInterface command = getCmd(this, commandName);
                try {
                    setArgArray(command.cmd(getArgArray()));
                    lastCommandResult = command.getResult();
                    if (lastCommandResult == CommandInterface.COMMANDRESULT.FAILURE) {
                        break; // we exit the loop on error
                    }
                } catch (IllegalArgumentException ex) {
                    getLogger().log(Level.SEVERE, "Command \"" + commandName + "\" threw exception", ex);
                    lastCommandResult = CommandInterface.COMMANDRESULT.FAILURE;
                    break;
                } catch (java.lang.RuntimeException ex) {
                    /* java.net.UnknownHostException lands here, as well as  */
                    /* com.ibm.as400.access.ExtendedIllegalArgumentException */
                    getLogger().log(Level.SEVERE, "Command \"" + commandName + "\" threw exception", ex);
                    lastCommandResult = CommandInterface.COMMANDRESULT.FAILURE;
                    break;
                }
            } else if (getFunctorMap().containsKey(commandName)) {
                try {
                    Generics.TupleNameList tnl = parseTupleNameList();
                    if (tnl != null) {
                        lastCommandResult = executeFunctor(getFunctor(commandName), tnl);
                        if (lastCommandResult == CommandInterface.COMMANDRESULT.FAILURE) {
                            break;
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Found function {0} but could not execute it", commandName);
                        lastCommandResult = CommandInterface.COMMANDRESULT.FAILURE;
                        break;
                    }
                } catch (java.lang.RuntimeException ex) {
                    getLogger().log(Level.SEVERE, "Function \"" + commandName + "\" threw exception", ex);
                    lastCommandResult = CommandInterface.COMMANDRESULT.FAILURE;
                    break;
                }
            } else {
                getLogger().log(Level.SEVERE, "Command \"{0}\" not found.", commandName);
                lastCommandResult = CommandInterface.COMMANDRESULT.FAILURE;
                break;
            }
        }
        if (!initialCommandLine.isEmpty()) {
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
}
