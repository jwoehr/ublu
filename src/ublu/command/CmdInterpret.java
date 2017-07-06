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
import ublu.util.Interpreter;
import java.util.logging.Level;

/**
 * Command to nest an instance of the interpreters
 *
 * @author jwoehr
 */
public class CmdInterpret extends Command {

    static {
        setNameAndDescription("interpret", "/0 [-block $[ block ...]$] : run the interpreter, possibly on a provided block");
    }

    /**
     * Runs the interpreter in a nested session
     */
    public CmdInterpret() {
    }

    /**
     * Run the interpreter in a nested session
     *
     * @param argArray
     * @return what's left of arguments
     */
    public ArgArray doInterpret(ArgArray argArray) {
        String block = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-block":
                    block = argArray.nextUnlessNotBlock();
                    if (block == null) {
                        getLogger().log(Level.SEVERE, "Null execution block provided to -block in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Interpreter i = new Interpreter(getInterpreter());
            if (block != null) {
                setCommandResult(i.executeBlock(block));
            } else {
                if (getUblu().isWindowing()) {
                    getLogger().log(Level.SEVERE, "Nested interpreters not supported in windowing environment.", getNameAndDescription());
                } else {
                    setCommandResult(i.interpret() == 0 ? COMMANDRESULT.SUCCESS : COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doInterpret(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
