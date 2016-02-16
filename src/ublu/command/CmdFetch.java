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
import ublu.ReportFetcher;
import ublu.util.DataSink;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to fetch a spooled file as text.
 *
 * @author jwoehr
 */
public class CmdFetch extends Command {

    {
        setNameAndDescription("fetch", "/8? [-as400 @as400] [-to datasink] ~@{system} ~@{userid} ~@{passwd} ~@{spoolFileName} ~@{spoolNumber} ~@{jobName} ~@{jobUser} ~@{jobNumber} : fetch a spooled file");
    }

    /**
     * Fetch a tidied spool file
     */
    public CmdFetch() {
    }

    /**
     * fetch a file from the spool
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray fetch(ArgArray argArray) {
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
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
        } else if (getAs400() == null && argArray.size() < 5) {
            logArgArrayTooShortError(argArray);
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (getAs400() == null) {
                try {
                    setAs400FromArgs(argArray);
                } catch (PropertyVetoException ex) {
                    getLogger().log(Level.SEVERE, "The fetch command could not instance a server.", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (getAs400() == null) {
                getLogger().log(Level.SEVERE, "The fetch command could not instance a server.");
                setCommandResult(COMMANDRESULT.FAILURE);
            } else if (argArray.size() < 5) {
                logArgArrayTooShortError(argArray);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                String spoolFileName = argArray.nextMaybeQuotationTuplePopString();
                int spoolNumber = argArray.nextIntMaybeQuotationTuplePopString();
                String jobName = argArray.nextMaybeQuotationTuplePopString();
                String jobUser = argArray.nextMaybeQuotationTuplePopString();
                String jobNumber = argArray.nextMaybeQuotationTuplePopString();
                try {
                    ReportFetcher fetcher = new ReportFetcher(getAs400());
                    String s = fetcher.fetchTidied(spoolFileName, spoolNumber, jobName, jobUser, jobNumber);
                    put(s);
                } catch (PropertyVetoException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException | SQLException | ObjectDoesNotExistException ex) {
                    getLogger().log(Level.SEVERE, "in command fetch", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return fetch(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
