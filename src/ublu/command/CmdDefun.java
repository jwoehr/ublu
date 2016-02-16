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
import ublu.util.Functor;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to define a function from a name and a functor
 *
 * @author jwoehr
 */
public class CmdDefun extends Command {

    {
        setNameAndDescription("defun", "/2? [[-define] | [-list] | [-show name]] name tuplename | define a function from a name and a functor");
    }

    enum OPERATIONS {

        LIST, DEFINE, SHOW
    }

    /**
     * Perform the work of defining a named function from a functor and a name
     *
     * @param argArray arg array passed in
     * @return what's left of arg array
     */
    public ArgArray defun(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.DEFINE;
        String showName = "";
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-define":
                    operation = OPERATIONS.DEFINE;
                    break;
                case "-list":
                    operation = OPERATIONS.LIST;
                    break;
                case "-show":
                    operation = OPERATIONS.SHOW;
                    showName = argArray.next();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (operation) {
                case LIST:
                    try {
                        put(getInterpreter().listFunctions());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception listing functions in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DEFINE:
                    if (argArray.size() < 2) { // here's where we fall out if new ArgArray()
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        String name = argArray.next();
                        if (!argArray.isNextTupleName()) {
                            getLogger().log(Level.SEVERE, "{0} is not a Tuple name in {1}", new Object[]{argArray.next(), getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            String tupleName = argArray.next();
                            Tuple t = getTuple(tupleName);
                            if (t == null) {
                                getLogger().log(Level.SEVERE, "{0} is not a Tuple in {1}", new Object[]{argArray.next(), getNameAndDescription()});
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else {
                                Object o = t.getValue();
                                if (o instanceof Functor) {
                                    getInterpreter().addFunctor(name, Functor.class.cast(o));
                                }
                            }
                        }
                    }
                    break;
                case SHOW:
                    try {
                        put(getInterpreter().showFunction(showName));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception showing function " + showName + " in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return defun(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
