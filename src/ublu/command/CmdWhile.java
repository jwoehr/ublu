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
import ublu.util.Parser;
import ublu.util.Tuple;
import java.util.logging.Level;

/**
 *
 * Iterate while true
 *
 * @author jwoehr
 */
public class CmdWhile extends Command {

    static {
        setNameAndDescription("WHILE", "/4 ~@whiletrue $[ cmd .. ]$ : WHILE @whiletrue iterate over block");
    }

    /* Parse and execute a WHILE block
     *
     * @param argArray args to the interpreter
     * @return what's left of the args
     */
    /**
     * Do the work of a FOR cmd
     *
     * @param argArray passed-in arg array
     * @return remnant of the arg array
     */
    public ArgArray doWhile(ArgArray argArray) {
        Tuple whileTuple;
        if (!argArray.isNextTupleNameOrPop()) {
            getLogger().log(Level.SEVERE, "No WHILE tuple provided", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            whileTuple = argArray.nextTupleOrPop();
            String block = argArray.nextUnlessNotBlock();
            if (block == null) {
                getLogger().log(Level.SEVERE, "WHILE found without a $[ block ]$");
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                if (whileTuple == null) {
                    getLogger().log(Level.SEVERE, "WHILE tuple does not exist in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    Parser p = new Parser(getInterpreter(), block);
                    ArgArray aa = p.parseAnArgArray();
                    getInterpreter().pushFrame();
                    getInterpreter().setForBlock(true);
                    walkWhile(whileTuple, aa);
                    if (getInterpreter().isBreakIssued()) {
                        // If a BREAK then the frame was already popped
                        getInterpreter().setBreakIssued(false);
                    } else {
                        getInterpreter().popFrame();
                    }
                }
            }
        }
        return argArray;
    }

    private void walkWhile(Tuple whileTuple, ArgArray argArray) {
        ArgArray copy;
        while (whileTuple.getValue().equals(true)) {
            copy = new ArgArray(getInterpreter(), argArray);
            getInterpreter().setArgArray(copy);
            setCommandResult(getInterpreter().loop());
            if (getCommandResult() == COMMANDRESULT.FAILURE || getInterpreter().isBreakIssued()) {
                break;
            }
        }
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doWhile(args);
    }

    @Override
    public CommandInterface.COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
