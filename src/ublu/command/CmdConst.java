/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
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

import java.util.logging.Level;
import ublu.util.ArgArray;
import ublu.util.Const;

/**
 * Command to create a named constant with a string value.
 *
 * @author jwoehr
 */
public class CmdConst extends Command {

    {
        setNameAndDescription("const",
                "/2 *name ~@{value} : create a constant value");
    }

    /**
     * Create a named constant with a string value.
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray doConst(ArgArray argArray) {
        while (getCommandResult() != COMMANDRESULT.FAILURE && argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
//                case "-to":
//                    String destName = argArray.next();
//                    setDataDest(DataSink.fromSinkName(destName));
//                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            if (argArray.size() < 2) {
                logArgArrayTooShortError(argArray);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                String name = argArray.next();
                String value = argArray.nextMaybeQuotationTuplePopString();
                if (Const.isConstName(name)) {
                    if (getInterpreter().getConst(name) != null) {
                        getLogger().log(Level.SEVERE, "\"{0}\" already exists as a const with value \"{1}\" in {2}", new Object[]{name, getInterpreter().getConst(name), getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (!getInterpreter().setConst(name, value)) {
                        getLogger().log(Level.SEVERE, "Attempt to set a const with null value in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                } else {
                    getLogger().log(Level.SEVERE, "{0}" + " is not a const name starting with \"" + Const.CONSTNAMECHAR + "\" in {1}", new Object[]{name, getNameAndDescription()});
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doConst(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
