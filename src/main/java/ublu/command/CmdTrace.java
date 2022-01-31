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
import com.ibm.as400.access.Trace;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Ask the user and get a response
 *
 * @author jwoehr
 */
public class CmdTrace extends Command {

    {
        setNameAndDescription("trace",
                "/0   [-tofile ~@{filename}] [-on] [-off] [-set ~@{all|conversion|datastream|diagnostic|error|info|jdbc|pcml|proxy|thread|warning} ~@{on|off}]: set JTOpen tracing");
    }
    private boolean onOff;
    private boolean okay;

    /**
     * Cmd action to ask user and get a response
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray cmdTrace(ArgArray argArray) {
        String mode;
        String state;
        String filename;
        okay = true;
        while (okay && argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
//                case "-to":
//                    setDataDest(newDataSink(argArray));
//                    break;
//                case "-from":
//                    setDataSrc(newDataSink(argArray));
//                    break;
                case "-tofile":
                    filename = argArray.nextMaybeQuotationTuplePopString();
                     {
                        try {
                            Trace.setFileName(filename);
                        } catch (IOException ex) {
                            okay = false;
                            getLogger().log(Level.SEVERE, "Couldn't set filename " + filename + "in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                case "-set":
                    mode = argArray.nextMaybeQuotationTuplePopString();
                    state = argArray.nextMaybeQuotationTuplePopString();
                    if (setOnOff(state)) {
                        traceMode(mode, onOff);
                    }
                    break;
                case "-off":
                    Trace.setTraceOn(false);
                    break;
                case "-on":
                    Trace.setTraceOn(true);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }

        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            /**/
        }
        return argArray;
    }

    private void errorOnOff() {
        getLogger().log(Level.SEVERE, "Flag {0}is neither \"on\" nor \"off\" in {1}", new Object[]{onOff, getNameAndDescription()});
        setCommandResult(COMMANDRESULT.FAILURE);
    }

    private boolean setOnOff(String flag) {
        okay = true;
        flag = flag.toLowerCase().trim();
        switch (flag) {
            case "on":
                onOff = true;
                break;
            case "off":
                onOff = false;
                break;
            default:
                errorOnOff();
                okay = false;
                break;
        }
        return okay;
    }

    private void traceMode(String mode, boolean flag) {
        switch (mode.toLowerCase().trim()) {
            case "all":
                Trace.setTraceAllOn(flag);
                break;
            case "conversion":
                Trace.setTraceConversionOn(flag);
                break;
            case "datastream":
                Trace.setTraceDatastreamOn(flag);
                break;
            case "diagnostic":
                Trace.setTraceDiagnosticOn(flag);
                break;
            case "error":
                Trace.setTraceErrorOn(flag);
                break;
            case "info":
                Trace.setTraceInformationOn(flag);
                break;
            case "jdbc":
                Trace.setTraceJDBCOn(flag);
                break;
            case "pcml":
                Trace.setTracePCMLOn(flag);
                break;
            case "proxy":
                Trace.setTraceProxyOn(flag);
                break;
            case "thread":
                Trace.setTraceThreadOn(flag);
                break;
            case "warning":
                Trace.setTraceWarningOn(flag);
                break;
            default:
                okay = false;
                getLogger().log(Level.SEVERE, "Unknown trace mode {0} in {1}", new Object[]{mode, getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
                break;
        }
    }

    @Override

    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdTrace(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
