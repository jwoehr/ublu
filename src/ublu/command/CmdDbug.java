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
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to return fields of a Queued Message
 *
 * @author jwoehr
 */
public class CmdDbug extends Command {

    {
        setNameAndDescription("dbug", "/0 [--,-dbug $[ execution block ]$] [-init] [-info] [-brk ~@opname] [-clr ~@opname] [[-step]|[-go]] : dbug your program");
    }

    private enum FUNCTIONS {

        DBUG, INFO, NOP
    }

    /**
     * Do the work of operating the debugger.
     *
     * @param argArray The arguments in the interp buffer
     * @return what's left afterwards of the arguments in the interp buffer
     */
    public ArgArray doCmdDbug(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.NOP; // default op
        String block = "";
        String brkpoint;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
//                case "-to":
//                    String destName = argArray.next();
//                    setDataDest(DataSink.fromSinkName(destName));
//                    break;
                case "--":
                case "-dbug":
                    function = FUNCTIONS.DBUG;
                    block = argArray.nextUnlessNotBlock();
                    argArray.clear(); // we're off
                    break;
                case "-step":
                    getInterpreter().dbug().setStepping(true);
                    break;
                case "-go":
                    getInterpreter().dbug().setStepping(false);
                    break;
                case "-brk":
                    getInterpreter().dbug().setBreakpoint(argArray.nextMaybeQuotationTuplePopString());
                    break;
                case "-clr":
                    brkpoint = argArray.nextMaybeQuotationTuplePopString();
                    if (!getInterpreter().dbug().clearBreakpoint(brkpoint)) {
                        getLogger().log(Level.WARNING, "There was no breakpoint set for {0}", brkpoint);
                    }

                    break;
                case "-info":
                    function = FUNCTIONS.INFO;
                    break;
                case "-init":
                    getInterpreter().dbug().reinit();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (function) {
                case DBUG:
                    getInterpreter().dbug().dbugBlockExecution(block);
                    break;
                case INFO:
                    try {
                        put(getInterpreter().dbug().toString());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting dbug info", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NOP:
                    break;
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doCmdDbug(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
