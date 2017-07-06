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
import ublu.SpooledFileLister;
import ublu.util.Generics.SpooledFileArrayList;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to get a list of spooled files and treat them as objects.
 *
 * @author jwoehr
 */
public class CmdSpoolFList extends Command {

    static {
        setNameAndDescription("spoolflist", "/4? [-as400 ~@as400] [-to datasink] ~@{system} ~@{userid} ~@{passwd} ~@{spoolfileowner} : fetch a list of the given user's spooled files as objects");
    }

    /**
     * ctor /0
     */
    public CmdSpoolFList() {
    }

    /**
     * Return spooled file list as objects for a given userid
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray spoolflist(ArgArray argArray) {
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (argArray.size() < (getAs400() == null ? 4 : 1)) {
                logArgArrayTooShortError(argArray);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                try {
                    SpooledFileLister lister = getSpooledFileListerFromArgs(argArray);
                    if (lister != null) {
                        SpooledFileArrayList spfal = lister.getSpooledFileListSynchronously();
                        put(spfal);
                    } else {
                        getLogger().log(Level.SEVERE, "Could not create list of spooled files in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | PropertyVetoException | RequestNotSupportedException | SQLException | ObjectDoesNotExistException ex) {
                    getLogger().log(Level.SEVERE, "Exception fetching list of spooled files in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    private SpooledFileLister getSpooledFileListerFromArgs(ArgArray argArray) throws PropertyVetoException, AS400SecurityException {
        SpooledFileLister splflister = null;
        AS400 a = getAs400();
        if (a == null) {
            try {
                setAs400(as400FromArgs(argArray));
            } catch (PropertyVetoException ex) {
                getLogger().log(Level.SEVERE, "Could not instance AS400 system from provided arguments in " + getNameAndDescription(), ex);
            }
        }
        if (a != null) {
            String spoolowner = argArray.nextMaybeQuotationTuplePopStringTrim();
            splflister = new SpooledFileLister(a);
            splflister.setUserFilter(spoolowner);
        }
        return splflister;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return spoolflist(args);
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
