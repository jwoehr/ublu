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
import ublu.util.ProgramCallHelper;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Invokes a host program with parameters.
 *
 * @author jwoehr
 */
public class CmdProgramCall extends Command {

    {
        setNameAndDescription("programcall", "/3? [-as400 ~@as400] [-to datasink] -program fullyqualifiedprogrampath [-in ~@tuple ~@{vartypename} [-in ..]] [-inout ~@tuple ~@{sizeout} ~@{vartypename} [-inout] ..] [-msgopt ~@{all|none|10}] [-out ~@tuple ~@{sizeout} ~@{vartypename} [-out ..]] ~@system ~@userid ~@passwd : invoke a program with parameters on the host");
    }

    /**
     * ctor/0
     */
    public CmdProgramCall() {
    }

    /**
     * Execute a program on the host.
     *
     * <p>
     * Execute parameterized program on the specified server on behalf of the
     * specified uid/passwd.</p>
     *
     * @param argArray the passed in arguments
     * @return what's left of the args
     */
    public ArgArray programcall(ArgArray argArray) {
        String programFQP = null;
        String msgOpt = null;
        ProgramCallHelper.ManagedProgramParameterList mppl = new ProgramCallHelper.ManagedProgramParameterList();
        Tuple t;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
//                case "-from":
//                    String srcName = argArray.next();
//                    setDataSrc(DataSink.fromSinkName(srcName));
//                    break;
                case "-in":
                    t = argArray.nextTupleOrPop();
                    mppl.add(ProgramCallHelper.ManagedProgramParameter.newInParam(t, argArray.nextMaybeQuotationTuplePopString().toUpperCase()));
                    break;
                case "-inout":
                    t = argArray.nextTupleOrPop();
                    mppl.add(ProgramCallHelper.ManagedProgramParameter.newInOutParam(t, argArray.nextIntMaybeQuotationTuplePopString(), argArray.nextMaybeQuotationTuplePopString().toUpperCase()));
                    break;
                case "-out":
                    t = argArray.nextTupleOrPop();
                    mppl.add(ProgramCallHelper.ManagedProgramParameter.newOutParam(t, argArray.nextIntMaybeQuotationTuplePopString(), argArray.nextMaybeQuotationTuplePopString().toUpperCase()));
                    break;
                case "-program":
                    programFQP = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-msgopt":
                    msgOpt = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (getAs400() == null && argArray.size() < 3) { // if no passed-in AS400 instance and not enough args to generate one
            logArgArrayTooShortError(argArray);
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (getAs400() == null) {
                try {
                    setAs400FromArgs(argArray);
                } catch (PropertyVetoException ex) {
                    getLogger().log(Level.SEVERE, "Could not create an AS400 instance in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (getAs400() != null) {
                // /* Debug */ getLogger().log(Level.INFO, "Command string is: {0}", commandString);
                if (programFQP == null) {
                    getLogger().log(Level.SEVERE, "Cannot execute null program fully qualified path in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    // /* DEBUG */ getLogger().log(Level.INFO, "Prog path: " + programFQP);
                    ProgramCall programCall;
                    try {
                        programCall = new ProgramCall(getAs400());
                        programCall.setProgram(programFQP);
                        /* DEBUG */ getLogger().log(Level.INFO, "ManagedProgramParameterList before runProgramCall: " + mppl.toString());
                        ProgramCallHelper pch = new ProgramCallHelper(programCall, mppl);
                        pch.addInputParameters();
                        if ((msgOpt == null) ? true : pch.setMessageOptions(msgOpt)) {
                            /* DEBUG */ getLogger().log(Level.INFO, "ProgramCallHelper before runProgramCall: " + pch.toString());
                            if (pch.runProgramCall()) {
                                pch.processOutputParameters();
                            } else {
                                getLogger().log(Level.SEVERE, "Program call failed for program {0} in {1}", new Object[]{programFQP, getNameAndDescription()});
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            // Show the messages (returned whether or not there was an error.)
                            put(pch.getMessageList());
                            /* DEBUG */ getLogger().log(Level.INFO, "ProgramCallHelper after runProgramCall: " + pch.toString());
                        } else {
                            getLogger().log(Level.SEVERE, "Invalid message option in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } catch (AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | PropertyVetoException ex) {
                        getLogger().log(Level.SEVERE, "Program " + programFQP + " failed in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (SQLException ex) {
                        getLogger().log(Level.SEVERE, null, ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return programcall(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return super.getCommandResult();
    }
}
