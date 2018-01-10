/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2018, Jack J. Woehr jwoehr@softwoehr.com 
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

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics.StringArrayList;
import ublu.util.WatsonHelper;

/**
 *
 * @author jax
 */
public class CmdWatson extends Command {

    {
        setNameAndDescription("watson",
                "/0 [-to datasink] [-h ~@{host}] -s ~@{service-url-part} [-p ~@{watson-parameter [p ~@{watson-parameter ..]] : invoke IBM Watson microservice");
    }

    public ArgArray cmdWatson(ArgArray argArray) {
        String host = WatsonHelper.BLUEMIX_HOST;
        String usrv = null;
        StringArrayList parms = new StringArrayList();
        while (getCommandResult() != COMMANDRESULT.FAILURE && argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-h":
                    host = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-s":
                    usrv = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-p":
                    parms.add(argArray.nextMaybeQuotationTuplePopString());
                    break;
                default:
                    unknownDashCommand(dashCommand);

            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }

        if (getCommandResult() != COMMANDRESULT.FAILURE) {

            try {
                put(WatsonHelper.watson(host, usrv, parms.toStringArray()));
            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                getLogger().log(Level.SEVERE, "Error in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }

        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return cmdWatson(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }

}
