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
import ublu.util.DataSink;
import ublu.util.Functor;
import ublu.util.Generics.FunctorParamList;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to create a functor
 *
 * @author jwoehr
 */
public class CmdFun extends Command {

    {
        setNameAndDescription("FUN", "/6.. [-to @tuplename] ( parameter name list )  $[ an execution block possibly spanning lines ]$ : create a functor");
    }

    /**
     * Command to create a functor
     *
     * @param argArray input arg array
     * @return what's left of input
     */
    public ArgArray fun(ArgArray argArray) {
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            FunctorParamList fpl = new FunctorParamList();
            if (!argArray.peekNext().equals("(")) {
                getLogger().log(Level.SEVERE, "No parameter list found for {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                argArray.next(); // discard "(" 
                while (!argArray.peekNext().equals(")")) {
                    fpl.add(argArray.next());
                }
                argArray.next(); // discard ")" 
                String block = argArray.nextUnlessNotBlock();
                if (block == null) {
                    getLogger().log(Level.SEVERE, "No block found for {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    try {
                        put(new Functor(block, fpl));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting functor in " + getNameAndDescription(), ex);
                    }
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return fun(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
