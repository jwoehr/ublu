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
import ublu.util.Tuple;
import java.util.logging.Level;

/**
 * Language SWITCH statement
 *
 * @author jwoehr
 */
public class CmdSwitch extends Command {

    {
        setNameAndDescription("SWITCH", " ~@stringselector [-case ~@${string}$ $[ block ]$ [[-case ~@${string}$ $[ block ]$] ...] [-default $[ block ]$] : language switch statement");
    }

    /**
     * Perform a switch by testing the tuple, walking the -case dashcommands and
     * running the block associated with the winning -case
     *
     * @param argArray the input arg array
     * @return remnant of the arg array
     */
    public ArgArray cmdSwitch(ArgArray argArray) {
        boolean foundCase = false;
        Tuple selectorTuple = argArray.nextTupleOrPop();
        String caseString;
        String blockString;
        if (selectorTuple == null) {
            getLogger().log(Level.SEVERE, "No selector tuple provided to {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            String selectorString = selectorTuple.getValue().toString().trim();
            while (argArray.hasDashCommand()) {
                String dashCommand = argArray.parseDashCommand();
                switch (dashCommand) {
                    case "-case":
                        caseString = argArray.nextMaybeQuotationTuplePopString().trim();
                        blockString = argArray.nextUnlessNotBlock();
                        if (!foundCase) { // because SWITCH must finish consuming -case dashcommands from the ArgArray
                            if (caseString.contentEquals(selectorString)) {
                                foundCase = true;
                                if (blockString == null) {
                                    getLogger().log(Level.SEVERE, "No block provided to case {0} in {1}", new Object[]{caseString, getNameAndDescription()});
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } else {
                                    setCommandResult(getInterpreter().executeBlock(blockString));
                                }
                            }
                        }
                        break;
                    case "--":
                    case "-default":
                        blockString = argArray.nextUnlessNotBlock();
                        if (!foundCase) {
                            foundCase = true;
                            if (blockString == null) {
                                getLogger().log(Level.SEVERE, "No block provided to default case in {1}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else {
                                setCommandResult(getInterpreter().executeBlock(blockString));
                            }
                        }
                        break;
                    default:
                        unknownDashCommand(dashCommand);
                }
                if (getCommandResult().equals(COMMANDRESULT.FAILURE)) {
                    break;
                }
            }
            if (havingUnknownDashCommand()) {
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdSwitch(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
