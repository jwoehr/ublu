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
import ublu.util.Generics.FunctorParamList;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to create a named function
 *
 * @author jwoehr
 */
public class CmdFunc extends Command {

    static {
        setNameAndDescription("FUNC", "/7?.. [-to datasink] [[-delete name] | [-list] | [-show name]] name ( parameter name list )  $[ an execution block possibly spanning lines ]$ : define a named function");
    }

    enum OPERATIONS {

        LIST, DEFINE, DELETE, SHOW
    }

    /**
     * Command to create a named function
     *
     * @param argArray input arg array
     * @return what's left of input
     */
    public ArgArray func(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.DEFINE;
        String deleteName = "";
        String showName = "";
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-list":
                    operation = OPERATIONS.LIST;
                    break;
                case "-delete":
                    operation = OPERATIONS.DELETE;
                    deleteName = argArray.next();
                    break;
                case "-show":
                    operation = OPERATIONS.SHOW;
                    showName = argArray.next();
                    break;
                case "-define":
                    operation = OPERATIONS.DEFINE;
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
                case SHOW:
                    try {
                        put(getInterpreter().showFunction(showName));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception showing function " + showName + " in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DELETE:
                    if (!getInterpreter().deleteFunction(deleteName)) {
                        getLogger().log(Level.WARNING, "Function {0} not found to delete in {1}", new Object[]{deleteName, getNameAndDescription()});
                    }
                    break;
                case DEFINE:
                    if (argArray.size() < 2) { // here's where we fall out if new ArgArray()
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        String funcName = argArray.next();
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
                                getInterpreter().addFunctor(funcName, new Functor(block, fpl));
                            }
                        }
                    }
                    break;
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return func(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
