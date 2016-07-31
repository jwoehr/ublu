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

/**
 * RETURN from immediately from a FUNC
 *
 * @author jwoehr
 */
public class CmdReturn extends Command {

    {
        setNameAndDescription("RETURN", "/0 : return immediately from a FUNC");
    }

    ArgArray performReturn(ArgArray argArray) {
        while (getInterpreter().frameDepth() > 0 && getInterpreter().isForBlock()) {
            getInterpreter().popFrame();
            getInterpreter().setBreakIssued(true);
            /* Debug */ getInterpreter().outputerrln("popping in RETURN");
        }
        /* Debug */ getInterpreter().outputerrln("doing RETURN");
        getInterpreter().popFrame();
        getInterpreter().setBreakIssued(true);
        return new ArgArray(getInterpreter());
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return performReturn(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
