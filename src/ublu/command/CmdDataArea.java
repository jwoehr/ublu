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

import ublu.util.ArgArray;
import com.ibm.as400.access.DataArea;
import java.util.logging.Level;

/**
 * Manipulates an OS400 Object Description
 *
 * @author jwoehr
 */
public class CmdDataArea extends Command {

    {
        setNameAndDescription("dataarea", "/0 [-as400 ~@as400] [-to datasink] [--,-dataarea ~@dataarea] [-path ~@{ifspath}] [-new,-instance CHAR|DEC|LOC|LOG | -create | -refresh | -query  name| system|length | -write ~@{data}] | -read | -clear] : create and use data areas");

    }

    /**
     * the operations we know
     */
    protected enum OPS {
        /**
         * Create the Object Description
         */
        INSTANCE,
        /**
         * Create the area
         */
        CREATE,
        /**
         * Query various aspects
         */
        QUERY,
        /**
         * Read data
         */
        READ,
        /**
         * Write data
         */
        WRITE,
        /**
         * Refresh all info
         */
        REFRESH,
        /**
         * Clear area
         */
        CLEAR
    }

    /**
     * Arity-0 ctor
     */
    public CmdDataArea() {
    }

    /**
     * retrieve a (filtered) list of OS400 Objects on the system
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray dta(ArgArray argArray) {
        OPS op = OPS.INSTANCE;
        String ifspath = null;
        String queryString = null;
//        String attributeName = null;
        DataArea da = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    // /* Debug */ getLogger().log(Level.INFO, "my AS400 == {0}", getAs400());
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                case "-dataarea":
                    da = argArray.nextTupleOrPop().value(DataArea.class);
                    break;
                case "-path":
                    ifspath = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-query":
                    op = OPS.QUERY;
                    queryString = argArray.nextMaybeQuotationTuplePopString().toUpperCase().trim();
                    break;
                case "-read":
                    op = OPS.READ;
                    break;
                case "-clear":
                    op = OPS.CLEAR;
                    break;
                case "-new":
                case "-instance":
                    op = OPS.INSTANCE;
                    break;
                case "-create":
                    op = OPS.CREATE;
                    break;
                case "-refresh":
                    op = OPS.REFRESH;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (op) {
                case CLEAR:
                    break;
                case CREATE:
                    break;
                case INSTANCE:
                    break;
                case QUERY:
                    break;
                case READ:
                    break;
                case REFRESH:
                    break;
                case WRITE:
                    break;

                default:
                    getLogger().log(Level.WARNING, "Unknown operation in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return dta(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
