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
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to test expression
 *
 * @author jwoehr
 */
public class CmdTest extends Command {

    static {
        setNameAndDescription("test",
                "/0 [-to @var] [[[-eq | -ne] @var @var] | [-null @var] | [-nnull @var ] | [-jcls @tuple full.javaclass.name]]: compare and return boolean result");
    }

    /**
     * Our ops
     */
    protected enum FUNCTIONS {

        /**
         * equal
         */
        EQ,
        /**
         * not equal
         */
        NE,
        /**
         * java class equality
         */
        JCLS,
        /**
         * null value
         */
        NULL,
        /**
         * not null
         */
        NNULL,
        /**
         * noop
         */
        NOOP
    }

    /**
     * Test an expression
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray test(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.NOOP;
        Object a = null;
        Object b = null;
        String classname = null;
        while (getCommandResult() != COMMANDRESULT.FAILURE && argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-eq":
                    function = FUNCTIONS.EQ;
                    a = nextStringCheckingForNonExistentTuple(argArray);
                    b = nextStringCheckingForNonExistentTuple(argArray);
                    break;
                case "-jcls":
                    function = FUNCTIONS.JCLS;
                    a = nextTupleValueCheckingForNonExistentTuple(argArray);
                    classname = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-ne":
                    function = FUNCTIONS.NE;
                    a = nextStringCheckingForNonExistentTuple(argArray);
                    b = nextStringCheckingForNonExistentTuple(argArray);
                    break;
                case "-null":
                    function = FUNCTIONS.NULL;
                    a = nextTupleValueCheckingForNonExistentTuple(argArray);
                    break;
                case "-nnull":
                    function = FUNCTIONS.NNULL;
                    a = nextTupleValueCheckingForNonExistentTuple(argArray);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }

        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            switch (function) {
                case EQ:
                    if (a instanceof String) { // tricky, "null" is not an "instanceof string"
                        a = String.class.cast(a).trim();
                    }
                    if (b instanceof String) {
                        b = String.class.cast(b).trim();
                    }

                    try {
                        if (a != null && b != null) {
                            put(a.equals(b));
                        } else {
                            put(a == b);
                        }
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Error putting result in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case JCLS:
                    if (a != null && classname != null) {
                        try {
                            Class c = Class.forName(classname);
                            put(a.getClass() == c);
                        } catch (SQLException | RequestNotSupportedException | ClassNotFoundException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Null in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NE:
                    if (a instanceof String) {
                        a = String.class.cast(a).trim();
                    }
                    if (b instanceof String) {
                        b = String.class.cast(b).trim();
                    }

                    try {
                        if (a != null && b != null) {
                            put(!a.equals(b));
                        } else {
                            put(a != b);
                        }
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Error putting result in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NULL:
                    try {
                        put(a == null);
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Error putting result in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NNULL:
                    try {
                        put(a != null);
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Error putting result in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NOOP:
                    break;
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return test(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
