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
import ublu.util.JVMHelper;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Manipulate and report on the JVM we are running in
 *
 * @author jwoehr
 */
public class CmdJVM extends Command {

    {
        setNameAndDescription("jvm", "/0 [-to @datasink] [ -new | -gc | -set key val | -get key] : manipulate or report on the JVM on which this program is executing");
    }

    /**
     * Our operations
     */
    protected static enum OPS {

        /**
         * Put the singleton instance of the JVMHelper
         */
        GET_SINGLETON,
        /**
         * gc
         */
        GC,
        /**
         * Do nothing
         */
        NOOP,
        /**
         * set sys prop
         */
        SET,
        /**
         * get sys prop
         */
        GET
    }

    /**
     * Do the work of the jvm command
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray cmdJvm(ArgArray argArray) {
        OPS op = OPS.GET_SINGLETON;
        String key = null;
        String val = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-new":
                    op = OPS.GET_SINGLETON;
                    break;
                case "-gc":
                    op = OPS.GC;
                    break;
                case "-set":
                    op = OPS.SET;
                    key = argArray.nextMaybeQuotationTuplePopStringTrim();
                    val = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-get":
                    op = OPS.GET;
                    key = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            JVMHelper jvmh = getUblu().getJVMHelper();
            switch (op) {
                case GC:
                    System.gc();
                    break;
                case GET_SINGLETON:
                    try {
                        put(jvmh);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting JVMHelper instance in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SET:
                    System.setProperty(key, val);
                    break;
                case GET:
                    try {
                        put(System.getProperty(key));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting JVMHelper instance in " + getNameAndDescription(), ex);
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
        return cmdJvm(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
