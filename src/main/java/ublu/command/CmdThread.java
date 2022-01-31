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
package ublu.command;

import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.InterpreterThread;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to instance and manage an interpreter thread
 *
 * @author jwoehr
 */
public class CmdThread extends Command {

    {
        setNameAndDescription("thread", "/0 [-from datasink] [-to datasink ] [--,-thread ~@thread] -new,-instance | -start | -stop : interpret in a background thread");
    }

    /**
     * Create and manage a thread of our interpreter
     */
    public CmdThread() {
    }

    /**
     * Our thread functions
     */
    protected enum FUNCTIONS {

        /**
         * Get thread instance
         */
        INSTANCE,
        /**
         * start thread
         */
        START,
        /**
         * stop thread
         */
        STOP
    }

    /**
     * Interpret in a thread in the background
     *
     * @param argArray the ArgArray currently under interpretation
     * @return the remainder of the ArgArray
     */
    public ArgArray cmdInterpreterThread(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.INSTANCE; // default
        InterpreterThread interpreterThread = null;
        Tuple interpreterThreadTuple = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-from":
                    String srcName = argArray.next();
                    setDataSrc(DataSink.fromSinkName(srcName));
                    break;
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "--":
                case "-thread":
                    interpreterThreadTuple = argArray.nextTupleOrPop();
                    break;
                case "-new":
                case "-instance":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-start":
                    function = FUNCTIONS.START;
                    break;
                case "-stop":
                    function = FUNCTIONS.STOP;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {

            if (interpreterThreadTuple == null) {
                // Create the interpreter thread
                switch (getDataSrc().getType()) {
                    case FILE:
                        getLogger().log(Level.SEVERE, "FILE not implemented in {0}.", getCommandDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                    case STD:
                        getLogger().log(Level.SEVERE, "STD not implemented in {0}.", getCommandDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    case TUPLE:
                        Tuple tupleWithProgramLines = getInterpreter().getTuple(getDataSrc().getName());
                        if (tupleWithProgramLines == null) {
                            getLogger().log(Level.SEVERE, "Tuple {0} does not exist.", getDataSrc().getName());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            Object value = tupleWithProgramLines.getValue();
                            if (value instanceof String) {
                                interpreterThread = new InterpreterThread(getInterpreter(), String.class.cast(value));
                            } else {
                                getLogger().log(Level.SEVERE, "Tuple {0} does not contain program lines in " + getNameAndDescription(), tupleWithProgramLines.getKey());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case URL:
                        getLogger().log(Level.SEVERE, "URL not implemented in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;
                }
            } else { // Thread was provided in tuple
                Object o = interpreterThreadTuple.getValue();
                if (o instanceof InterpreterThread) {
                    interpreterThread = InterpreterThread.class.cast(o);
                } else {
                    getLogger().log(Level.SEVERE, "Tuple value is not a thread in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (interpreterThread != null) {
                switch (function) {
                    case INSTANCE:
                        try {
                            put(interpreterThread);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting thread instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case START:
                        interpreterThread.start();
                        break;
                    case STOP:
                        interpreterThread.stop();
                        break;
                }
            }
        }
        return argArray;
    }

    @Override
    public void reinit() {
        super.reinit();
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdInterpreterThread(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
