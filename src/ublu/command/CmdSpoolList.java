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
import ublu.AS400Factory;
import ublu.SpooledFileLister;
import ublu.util.DataSink;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * Command to get a list of spooled files.
 *
 * @author jwoehr
 */
public class CmdSpoolList extends Command {

    {
        setNameAndDescription("spoollist", "/4 [-to datasrc] system userid passwd spoolfileowner : fetch a list of the given user's spooled files");
    }

    /**
     *
     */
    public CmdSpoolList() {
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return spoollist(args);
    }

    /**
     * Return spooled file list for a given userid
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray spoollist(ArgArray argArray) {
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
            if (argArray.size() < 4) {
                logArgArrayTooShortError(argArray);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                String systemName = argArray.next();
                String userId = argArray.next();
                String password = argArray.next();
                String spoolfileowner = argArray.next();
                try {
                    AS400 as400 = AS400Factory.newAS400(getInterpreter(), systemName, userId, password);
                    SpooledFileLister lister = new SpooledFileLister(as400);
                    lister.setUserFilter(spoolfileowner);
                    Enumeration e = lister.getSynchronously();
                    StringBuilder sb = new StringBuilder();
                    while (e.hasMoreElements()) {
                        SpooledFile spf = SpooledFile.class.cast(e.nextElement());
                        sb.append(spf.getName()).append(" ").append(spf.getNumber()).append(" ").append(spf.getJobName()).append(" ").append(spf.getJobUser()).append(" ").append(spf.getJobNumber()).append(" ").append(spf.getCreateDate()).append(" ").append(spf.getCreateTime()).append("\n");
                    }
                    put(sb.toString());
                } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | PropertyVetoException | RequestNotSupportedException | SQLException | ObjectDoesNotExistException ex) {
                    getLogger().log(Level.SEVERE, "Exception fetching list of spooled files", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    @Override
    public void reinit() {
        super.reinit();
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
