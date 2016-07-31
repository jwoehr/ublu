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
 * Iterate between numeric limits
 *
 * @author jwoehr
 */
public class CmdDo extends Command {

    {
        setNameAndDescription("DO", "/5 [-undo] @iterator [to|TO] @limit $[ cmd .. ]$ : DO iterative from @iterator to @limit - 1 inclusive incrementing @iteratorvar");
    }

    /**
     * Parse and execute a DO block
     *
     * @param argArray args to the interpreter
     * @return what's left of the args
     */
    public ArgArray doCmdDo(ArgArray argArray) {
        boolean undo = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-undo":
                    undo = true;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Tuple startTuple = getTuple(argArray.next());
            String limitTupleName = argArray.next();
            if (limitTupleName.equalsIgnoreCase("to")) {
                limitTupleName = argArray.next();
            }
            Tuple limitTuple = getTuple(limitTupleName);
            String block = argArray.nextUnlessNotBlock();
            if (block == null) {
                getLogger().log(Level.SEVERE, "DO found without a $[ block ]$");
                setCommandResult(COMMANDRESULT.FAILURE);
            } else if (startTuple == null) {
                getLogger().log(Level.SEVERE, "Iterated tuple does not exist in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else if (limitTuple == null) {
                getLogger().log(Level.SEVERE, "Limit tuple does not exist in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                Parser p = new Parser(getInterpreter(), block);
                ArgArray aa = p.parseAnArgArray();
                getInterpreter().pushFrame();
                getInterpreter().setForBlock(true);
                if (undo) {
                    walkUnDo(startTuple, limitTuple, aa);
                } else {
                    walkDo(startTuple, limitTuple, aa);
                }
                if (getInterpreter().isBreakIssued()) {
                    // If a BREAK then the frame was already popped
                    getInterpreter().setBreakIssued(false);
                } else {
                    getInterpreter().popFrame();
                }
            }
        }
        return argArray;
    }

    private void walkDo(Tuple startTuple, Tuple limitTuple, ArgArray argArray) {
        ArgArray copy;
        int itStart = Integer.parseInt(startTuple.getValue().toString());
        int itLimit = Integer.parseInt(limitTuple.getValue().toString());
        for (; itStart < itLimit; itStart++) {
            startTuple.setValue(itStart);
            copy = new ArgArray(getInterpreter(), argArray);
            getInterpreter().setArgArray(copy);
            setCommandResult(getInterpreter().loop());
            if (getCommandResult() == COMMANDRESULT.FAILURE || getInterpreter().isBreakIssued()) {
                break;
            }
        }
    }

    private void walkUnDo(Tuple startTuple, Tuple limitTuple, ArgArray argArray) {
        ArgArray copy;
        int itStart = Integer.parseInt(startTuple.getValue().toString());
        int itLimit = Integer.parseInt(limitTuple.getValue().toString());
        for (; itStart > itLimit; itStart--) {
            startTuple.setValue(itStart);
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
        return doCmdDo(args);
    }

    @Override
    public CommandInterface.COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
