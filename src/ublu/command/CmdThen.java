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
import java.util.logging.Level;

/**
 * Command to perform a THEN block
 *
 * @author jwoehr
 */
public class CmdThen extends Command {

    {
        setNameAndDescription("THEN", "/3.. $[ an execution block possibly spanning lines ]$ : the THEN block for an IF");
    }

    /**
     * Arity-0 ctor
     */
    public CmdThen() {
    }

    /**
     * Parse and execute a THEN block
     *
     * @param argArray args to the interpreter
     * @return what's left of the args
     */
    public ArgArray doThen(ArgArray argArray) {
        String block = argArray.nextUnlessNotBlock();
        if (block == null) {
            getLogger().log(Level.SEVERE, "THEN found without a $[ block ]$");
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            setCommandResult(getInterpreter().executeBlock(block));
            if (!argArray.isEmpty()) {
                boolean safelyRemoved = removeElse(argArray);
                if (!safelyRemoved) {
                    getLogger().log(Level.SEVERE, "ELSE found without a $[ block ]$");
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    // Returns true if ELSE + block (discarding the block, if one is returned).
    // Returns true if there was no ELSE waiting (without removing anything from arg array)
    // Rerturn false if ELSE without a block (discarding the ELSE but not whatever follows in arg array)
    private boolean removeElse(ArgArray argArray) {
        boolean result = true;
        if (argArray.isNextElse()) { // Returns true if no ELSE
            argArray.next();
            result = argArray.nextUnlessNotBlock() != null;
        }
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doThen(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
