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
import ublu.util.Generics.UbluProgram;
import ublu.util.Tuple;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Command to read in a file of commands and execute them.
 *
 * @author jwoehr
 */
public class CmdInclude extends Command {

    {
        setNameAndDescription("include", "/1 [-from datasink] [-s,-silent] [-if ~@tf | -!if ~@tf] ~@{filepath} : include commands from a text file or from another datasink for interpretation");
    }

    /**
     * Interpret program lines from a data source.
     */
    public CmdInclude() {
    }
    private boolean wasSetFromDataSink;

    /**
     * Include a file for interpretation
     *
     * @param args the ArgArray currently under interpretation
     * @return the remainder of the ArgArray from whence came the include
     * command
     */
    public ArgArray include(ArgArray args) {
        boolean includeIf = true;
        boolean isSilent = false;
        Path filepath;
        while (args.hasDashCommand()) {
            String dashCommand = args.parseDashCommand();
            switch (dashCommand) {
                case "-from":
                    String srcName = args.next();
                    setDataSrc(DataSink.fromSinkName(srcName));
                    wasSetFromDataSink = true;
                    break;
                case "-if":
                    includeIf = args.nextTupleOrPop().value(Boolean.class);
                    break;
                case "-!if":
                    includeIf = !args.nextTupleOrPop().value(Boolean.class);
                    break;
                case "-s":
                case "-silent":
                    isSilent = true;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (!includeIf) {
            args.nextMaybeQuotationTuplePopString(); // discard unused include name
        } else {
            String filepathspec;
            boolean wasPrompting = getInterpreter().isPrompting();
            boolean wasEchoInclude = getInterpreter().isEchoInclude();
            if (isSilent) {
                getInterpreter().setPrompting(false);
                getInterpreter().setEchoInclude(false);
            }
            if (wasSetFromDataSink) {
                switch (getDataSrc().getType()) {
                    case FILE:
                        filepathspec = getDataSrc().getName();
                        try {
                            filepath = FileSystems.getDefault().getPath(filepathspec);
                            setCommandResult(getInterpreter().include(filepath));
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Exception including " + filepathspec, ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case STD:
                        getLogger().log(Level.SEVERE, "STD not implemented in CmdInclude.");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    case TUPLE:
                        Tuple tupleWithProgramLines = getInterpreter().getTuple(getDataSrc().getName());
                        if (tupleWithProgramLines == null) {
                            getLogger().log(Level.SEVERE, "Tuple {0} does not exist.", getDataSrc().getName());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            Object value = tupleWithProgramLines.getValue();
                            if (value instanceof String) {
                                setCommandResult(getInterpreter().include(UbluProgram.newUbluProgram(String.class.cast(value))));
                            } else {
                                getLogger().log(Level.SEVERE, "Tuple {0} does not contain program lines.", tupleWithProgramLines.getKey());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case URL:
                        getLogger().log(Level.SEVERE, "URL not implemented in CmdInclude.");
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break;

                }
            } else if (args.size() < 1) {
                logArgArrayTooShortError(args);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                filepathspec = args.nextMaybeQuotationTuplePopString();
                try {
                    filepath = FileSystems.getDefault().getPath(filepathspec);
                    setCommandResult(getInterpreter().include(filepath));
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "Exception including " + filepathspec, ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (isSilent) {
                getInterpreter().setPrompting(wasPrompting);
                getInterpreter().setEchoInclude(wasEchoInclude);
            }
        }
        return args;
    }

    @Override
    public void reinit() {
        super.reinit();
        wasSetFromDataSink = false;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return include(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
