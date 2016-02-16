/*
 * Copyright (c) 2015, Jack J. Woehr jax@well.com po box 51 golden co 80402-0051
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

import ublu.smapi.Host;
import ublu.smapi.SmapiHelper;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.softwoehr.pigiron.access.VSMException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * VSMAPI functionality
 *
 * @author jax
 */
public class CmdSmapi extends Command {

    {
        setNameAndDescription("smapi",
                "/2? [-to @var] ~@host ~@{funcname} [~@{parm} [~{@parm} ..]] : make a smapi call to a host");
    }

    /**
     * Do the SMAPI call
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray callSmapi(ArgArray argArray) {
        // OPERATIONS operation = OPERATIONS.NOOP;
        Host host;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Tuple hostTuple = argArray.nextTupleOrPop();
            if (hostTuple == null) {
                getLogger().log(Level.SEVERE, "Null host tuple in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                Object o = hostTuple.getValue();
                if (!(o instanceof Host)) {
                    getLogger().log(Level.SEVERE, "Host tuple is not an instance of Host in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    host = Host.class.cast(o);
                    String funcname = argArray.nextMaybeQuotationTuplePopString();
                    SmapiHelper sh = new SmapiHelper(host, funcname, this, argArray);
                    try {
                        put(sh.doIt());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | VSMException ex) {
                        getLogger().log(Level.SEVERE, "Exception executing SMAPI function in " + getNameAndDescription(), ex);
                    }
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return callSmapi(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }

}
