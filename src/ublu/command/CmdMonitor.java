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

import ublu.Monitors;
import ublu.util.ArgArray;
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
 * Command to run System Shepherd-style monitoring.
 *
 * @author jwoehr
 */
public class CmdMonitor extends Command {

    {
        setNameAndDescription("monitor", "/3? [-as400 ~@as400] [-none|-status|-version|-all] system userid passwd : fetch system monitor data and create System Shepherd [TM API] datapoints");
    }

    /**
     * Arity-0 ctor
     */
    public CmdMonitor() {
    }

    private enum MONPOINTS {

        NONE, VERSION, STATUS, ALL
    }
    private MONPOINTS monPoint;

    private void setMonPoint(MONPOINTS monPoint) {
        this.monPoint = monPoint;
    }

    private MONPOINTS getMonPoint() {
        return monPoint;
    }

    @Override
    protected void reinit() {
        super.reinit();
        setMonPoint(MONPOINTS.NONE);
    }

    /**
     * Monitor and return data points
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray monitor(ArgArray argArray) {
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-all":
                    setMonPoint(MONPOINTS.ALL);
                    break;
                case "-none":
                    setMonPoint(MONPOINTS.NONE);
                    break;
                case "-status":
                    setMonPoint(MONPOINTS.STATUS);
                    break;
                case "-version":
                    setMonPoint(MONPOINTS.VERSION);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (getAs400() == null) {
                if (argArray.size() < 3) {
                    logArgArrayTooShortError(argArray);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    try {
                        setAs400FromArgs(argArray);
                    } catch (PropertyVetoException ex) {
                        getLogger().log(Level.SEVERE,
                                "Encountered an exception getting an AS400 instance from the supplied command arguments in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
            }
            if (getCommandResult() != COMMANDRESULT.FAILURE) {
                try {
                    Monitors oS400Monitors = new Monitors(getAs400());
                    StringBuilder sb = new StringBuilder();
                    switch (getMonPoint()) {
                        case NONE:
                            break;
                        case ALL:
                            sb.append(oS400Monitors.osVersionVRM());
                            sb.append('\n');
                            sb.append(oS400Monitors.systemStatus());
                            break;
                        case STATUS:
                            sb.append(oS400Monitors.systemStatus());
                            break;
                        case VERSION:
                            sb.append(oS400Monitors.osVersionVRM());
                            break;
                    }
                    put(sb);
                } catch (RequestNotSupportedException | SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                    getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return monitor(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
