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

import ublu.util.Interpreter;
import ublu.util.ArgArray;

/**
 * Interface implemented by all Command instances
 *
 * @author jwoehr
 */
public interface CommandInterface {

    /**
     * Result from command execution
     */
    public enum COMMANDRESULT {

        /**
         * D'oh
         */
        SUCCESS,
        /**
         * D'oh
         */
        FAILURE
    }

    /**
     * Execute the work of the command
     *
     * @param args Argument array passed in by the interpreter
     * @return What is left of the argument array after the command has consumed
     * its share
     */
    ArgArray cmd(ArgArray args);

    /**
     * Get the result of execution
     *
     * @return the result of execution
     */
    COMMANDRESULT getResult();

    /**
     * Set the interpreter instance for the command
     *
     * @param interpreter
     */
    void setInterpreter(Interpreter interpreter);

    /**
     * Get command name
     *
     * @see CommandMap
     * @return command name
     */
    String getCommandName();

    /**
     * Get command description
     *
     * @see CommandMap
     * @return command description
     */
    String getCommandDescription();
}
