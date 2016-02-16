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
 * Command to open IF .. THEN .. ELSE
 *
 * @author jwoehr
 */
public class CmdIf extends Command {

    {
        setNameAndDescription("IF", "/0 @var : IF tests @var and executes THEN $[ cmd cmd .. ]$ if true, ELSE  $[ cmd cmd .. ]$ if false");
    }

    /**
     * Do the work of the IF command
     *
     * @param argArray arg array of command line
     * @return what's left of the arg array
     */
    public ArgArray doCmdIf(ArgArray argArray) {
//        if (argArray.size() < 5) { // tuple THEN $[ something ]$
//            logArgArrayTooShortError(argArray);
//            setCommandResult(COMMANDRESULT.FAILURE);
//        } else {
        Tuple t = getTuple(argArray.next());
        // Tuple t = argArray.nextTupleOrPop(); // this should be more correct
        // but have not tested it yet. 20140516
        if (t == null) {
            getLogger().log(Level.SEVERE, "Argument to IF is not a Tuple variable");
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Object o = t.getValue();
            if (o == null) { // null is an error and will throw if tested with Object.equals()
                badIfArg(o);
            } else if (o.equals(false)) { // if false procede to ELSE
                boolean safelyRemoved = removeThen(argArray);
                if (!safelyRemoved) {
                    getLogger().log(Level.SEVERE, "THEN found without a $[ block ]$");
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else if (!o.equals(true)) { // Wasn't false, it BETTAH be a true or error
                badIfArg(o);
            }
        }
//        }
        return argArray;
    }

    private void badIfArg(Object o) {
        getLogger().log(Level.SEVERE, "Tuple argument to IF must be set to true or false, but was {0}", o == null ? "null" : o + " " + o.getClass());
        setCommandResult(COMMANDRESULT.FAILURE);
    }

    private boolean removeThen(ArgArray argArray) {
        boolean result = true;
        if (argArray.isNextThen()) {
            argArray.next();
            // result = argArray.nextUnlessNotBlock() == null ? false : true;
            result = argArray.nextUnlessNotBlock() != null;
        } // Returns true if no ELSE or yes ELSE + block but false if ELSE + no block
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doCmdIf(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
