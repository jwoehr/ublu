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
import ublu.util.TupleMap;
import java.util.logging.Level;

/**
 * Declare tuples local to a block
 *
 * @author jwoehr
 */
public class CmdLocal extends Command {

    {
        setNameAndDescription("LOCAL", "/1 @localvar : declare local variable");
    }

    /**
     * Do the work of declaring tuples local to a block
     *
     * @param argArray args passed in from interpreter
     * @return remnants of the args
     */
    public ArgArray doCmdLocal(ArgArray argArray) {
        String tupleName = argArray.next();
        if (!Tuple.isTupleName(tupleName)) {
            getLogger().log(Level.SEVERE, "{0} is not a tuple name in {1}", new Object[]{tupleName, getNameAndDescription()});
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            TupleMap tm = getInterpreter().getTupleMap().getLocalMap();
            if (tm == null) {
                getLogger().log(Level.SEVERE, "No local context exists in {0}", new Object[]{tupleName, getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                tm = tm.getMostLocalMap();
                tm.setTuple(tupleName, null);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doCmdLocal(args);
    }

    @Override
    public CommandInterface.COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
