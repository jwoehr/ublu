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
 * Shortcut to repeat execution functionality of the interpreter "history -do"
 * command.
 *
 * @author jwoehr
 */
public class CmdSleep extends Command {

    {
        setNameAndDescription("sleep", "/0 [-m ~@{milliseconds}] [-n ~@{nanoseconds}] Sleep milliseconds (default 0) plus nanoseconds (default 0)");
    }

    /**
     * 0-arity ctor
     */
    public CmdSleep() {
    }

    /**
     * Sleep milliseconds and nanoseconds
     *
     * @param argArray The passed-in arg array
     * @return what is left of the passed-in arg array
     */
    public ArgArray sleep(ArgArray argArray) {
        int millis = 0;
        int nanos = 0;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(newDataSink(argArray));
                    break;
                case "-m":
                    millis = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-n":
                    nanos = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            try {
                Thread.sleep(millis, nanos);
            } catch (InterruptedException ex) {
                getLogger().log(Level.SEVERE, "Exception sleeping in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return sleep(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
