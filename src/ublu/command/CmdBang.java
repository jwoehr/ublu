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
import ublu.util.History;
import ublu.util.Parser;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Shortcut to repeat execution functionality of the interpreter "history -do"
 * command.
 *
 * @author jwoehr
 */
public class CmdBang extends Command {

    {
        setNameAndDescription("!", "/1 linenumber | ! : repeat a command from the history list (short for history -do)");
    }

    /**
     * 0-arity ctor
     */
    public CmdBang() {
    }

    /**
     * Repeat execution of a numbered command in the history file.
     *
     * <p> The numbers are ones-based but the history object in memory is
     * zeroes-based.</p>
     *
     * @param args The passed-in arg array
     * @return what is left of the passed-in arg array
     */
    public ArgArray bang(ArgArray args) {
        History h = getInterpreter().getHistory();
        int doLine = -1;
        if (h == null) {
            getLogger().log(Level.SEVERE, "History is not enabled");
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (args.peekNext().trim().equals("!")) {
                args.next(); // discard
                try {
                    doLine = h.lines() - 1;
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "Exception accessing history file", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else { // user sees ones-based history list
                doLine = args.nextInt() - 1; // but history object in memory is zeroes based
            }
            if (doLine > -1) {
                try {
                    String nthLine = h.nth(doLine);
                    if (nthLine != null) {
                        while (nthLine.trim().startsWith("! !")) {
                            nthLine = h.nth(--doLine);
                        }
                        if (nthLine.matches(".*!\\s+!.*")) {
                            getLogger().log(Level.SEVERE, "Recursive ! ! in history line in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            getInterpreter().outputerrln(nthLine);
                            ArgArray newArgs = new Parser(getInterpreter(), nthLine).parseAnArgArray();
                            newArgs.addAll(args);
                            args = newArgs;
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Line {0} does not exist in history buffer", doLine + 1);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, null, ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else {
                getLogger().log(Level.SEVERE, "Line '{'0'}' does not exist in history buffer", doLine + 1);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return args;
    }

    @Override
    protected void reinit() {
        super.reinit();
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return bang(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
