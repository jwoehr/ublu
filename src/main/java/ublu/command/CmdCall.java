/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 51, Golden CO 80402-0051 http://www.softwoehr.com
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
import ublu.util.Functor;
import ublu.util.Generics;
import ublu.util.Generics.TupleNameList;
import ublu.util.Tuple;
import java.util.logging.Level;

/**
 * A command to call a Functor
 *
 * @author jwoehr
 */
public class CmdCall extends Command {

    {
        setNameAndDescription("CALL", "/? ~@tuple ( [@parm] .. ) : Call a functor");
    }

    /**
     * Do the work of CALL
     *
     * @param argArray input args
     * @return what's left of the args
     */
    public ArgArray doCall(ArgArray argArray) {
        Tuple functorTuple = argArray.nextTupleOrPop();
        if (functorTuple == null) {
            getLogger().log(Level.SEVERE, "No functor tuple or pop in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Functor f = functorTuple.value(Functor.class);
            if (f == null) {
                getLogger().log(Level.SEVERE, "Can't get FUNctor from tuple {0} in {1}", new Object[]{functorTuple.getKey(), getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                if (argArray.size() < 2) {
                    logArgArrayTooShortError(argArray);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    if (!argArray.peekNext().equals("(")) {
                        getLogger().log(Level.SEVERE, "Need a ( parameter list ) in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        argArray.next(); // discard "("
                        TupleNameList tnl = new Generics.TupleNameList();
                        while (!argArray.peekNext().equals(")")) {
                            tnl.add(argArray.next());
                        }
                        argArray.next(); // discard ")"
                        setCommandResult(getInterpreter().executeFunctor(f, tnl));
                    }
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return doCall(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
