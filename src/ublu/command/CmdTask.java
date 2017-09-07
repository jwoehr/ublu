/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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

import ublu.util.ArgArray;
import static ublu.util.DataSink.SINKTYPE.FILE;
import static ublu.util.DataSink.SINKTYPE.STD;
import static ublu.util.DataSink.SINKTYPE.TUPLE;
import static ublu.util.DataSink.SINKTYPE.URL;
import ublu.util.InterpreterThread;
import ublu.util.Tuple;
import ublu.util.Utils;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.TupleMap;

/**
 * Command to launch a block in a background interpreter thread
 *
 * @author jwoehr
 */
public class CmdTask extends Command {

    {
        setNameAndDescription("TASK", "/1? [-from ~@datasink] [-to ~@datasink] [-local @tuplename ~@tuple [-local ..]] [-start] $[ BLOCK TO EXECUTE ]$ : create a background thread to execute a block, putting the thread and starting the thread if specified");
    }

    /**
     * Command to launch a block in a background interpreter thread
     */
    public CmdTask() {
    }

    TupleMap tm = new TupleMap();

    /**
     * Interpret a block in a thread in the background
     *
     * @param argArray the ArgArray currently under interpretation
     * @return the remainder of the ArgArray
     */
    public ArgArray cmdTask(ArgArray argArray) {
        boolean wasSetDataSource = false;
        boolean startNow = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    wasSetDataSource = true;
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-local":
                    tm.setTuple(argArray.next(), argArray.nextTupleOrPop().getValue());
                    break;
                case "-start":
                    startNow = true;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            String block = null;
            if (wasSetDataSource) {
                switch (getDataSrc().getType()) {
                    case FILE:
                        String filepath = getDataSrc().getName();
                        try {
                            block = Utils.getFileAsString(filepath);
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Exception including " + filepath, ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case STD:
                        getLogger().log(Level.SEVERE, "STD not implemented in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    case TUPLE:
                        Tuple tupleWithProgramLines = getInterpreter().getTuple(getDataSrc().getName());
                        if (tupleWithProgramLines == null) {
                            getLogger().log(Level.SEVERE, "Tuple {0} does not exist.", getDataSrc().getName());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            Object value = tupleWithProgramLines.getValue();
                            if (value instanceof String) {
                                block = String.class.cast(value);
                            } else {
                                getLogger().log(Level.SEVERE, "Tuple {0} does not contain program lines.", tupleWithProgramLines.getKey());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case URL:
                        getLogger().log(Level.SEVERE, "URL not implemented in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;

                }
            } else {
                block = argArray.nextUnlessNotBlock();
            }
            if (block == null) {
                getLogger().log(Level.SEVERE, "TASK found with neither a $[ block ]$ nor a data source containing a program in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                InterpreterThread interpreterThread = new InterpreterThread(getInterpreter(), block);
                for (String k : tm.keySet()) {
                    interpreterThread.getInterpreter().setTuple(k, tm.getTuple(k).getValue());
                }
                if (startNow) {
                    interpreterThread.start();
                }
                try {
                    put(interpreterThread);
                } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                    getLogger().log(Level.SEVERE, "Error putting InterpreterThread in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
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
        return cmdTask(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
