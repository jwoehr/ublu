/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
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
 * Implementing a TRY ... CATCH
 *
 * @author jwoehr
 */
public class CmdTry extends Command {

    {
        setNameAndDescription("TRY", "/3 $[ try block ]$ CATCH $[ catch block ]$ : TRY and CATCH on error or THROW");
    }

    public ArgArray doTry(ArgArray argArray) {
        String tryBlock;
        String catchBlock;
        tryBlock = argArray.nextUnlessNotBlock();
        if (tryBlock == null) {
            getLogger().log(Level.SEVERE, "No TRY block found in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if ("CATCH".equals(argArray.next())) {
            catchBlock = argArray.nextUnlessNotBlock();
            if (catchBlock == null) {
                getLogger().log(Level.SEVERE, "No CATCH block found in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                if (getInterpreter().executeBlock(tryBlock) == COMMANDRESULT.FAILURE) {
                    setCommandResult(getInterpreter().executeBlock(catchBlock));
                }
            }
        } else {
            getLogger().log(Level.SEVERE, "TRY without CATCH found in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doTry(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }

}
