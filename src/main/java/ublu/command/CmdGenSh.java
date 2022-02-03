/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 82, Beulah CO 81023-0082 http://www.softwoehr.com
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
import ublu.util.GenSh;
import ublu.util.GenSh.Opt;
import ublu.util.GenSh.OptMulti;
import ublu.util.GenSh.Option;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.GenSh.OptScriptOnly;

/**
 * Class to generate a launcher shell scripts on command
 *
 * @author jwoehr
 */
public class CmdGenSh extends Command {

    {
        setNameAndDescription("gensh",
                "/5+ [-to datasink] [-strictPosix] [ [-path ~@{fullyqualifiedjarpath}] [-includepath ~@{searchpath}] [-opt optchar assignment_name tuplename ${ description }$ ..] [-optr optchar assignment_name tuplename ${ description }$ ..] [-opts optchar assignment_name ${ description }$ ..] [-optx optchar multiple_assignment_name tuplename ${ description }$ ..] [-prelude ~@{prelude command string ..] ] ~@{scriptname} ~@{includename} ~@{ functionCall ( @a @b ... ) } : generate launcher shell script");
    }

    private static final String RESERVED = "[DXh]";

    /**
     * Command to generate a launcher shell script
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray cmdGenSh(ArgArray argArray) {
        GenSh genSh = new GenSh();
        Option o;
        boolean strictPosix = false;
        while (argArray.hasDashCommand() && getCommandResult() != COMMANDRESULT.FAILURE) {
            String dashCommand = argArray.parseDashCommand();
            genSh.accumulateCommand(dashCommand);
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    genSh.accumulateCommand(getDataDest().getName());
                    break;
                case "-opt":
                    if (!validateOptChar(argArray)) {
                        break;
                    }
                    o = new Opt(argArray.next().charAt(0), argArray.next(), argArray.next(), argArray.nextMaybeQuotation());
                    genSh.accumulateOption(o);
                    genSh.addOption(o);
                    break;
                case "-optr":
                    if (!validateOptChar(argArray)) {
                        break;
                    }
                    o = new Opt(argArray.next().charAt(0), argArray.next(), argArray.next(), argArray.nextMaybeQuotation(), Opt.REQUIRED);
                    genSh.accumulateOption(o);
                    genSh.addOption(o);
                    break;
                case "-opts":
                    if (!validateOptChar(argArray)) {
                        break;
                    }
                    o = new OptScriptOnly(argArray.next().charAt(0), argArray.next(), argArray.nextMaybeQuotation());
                    genSh.accumulateOption(o);
                    genSh.addOption(o);
                    break;
                case "-optx":
                    if (!validateOptChar(argArray)) {
                        break;
                    }
                    o = new OptMulti(argArray.next().charAt(0), argArray.next(), argArray.next(), argArray.nextMaybeQuotation());
                    genSh.accumulateOption(o);
                    genSh.addOption(o);
                    break;
                case "-strictPosix":
                    strictPosix = true;
                    break;
                case "-path":
                    genSh.setFqJarPath(argArray.nextMaybeQuotationTuplePopStringTrim());
                    genSh.accumulateCommand(genSh.getFqJarPath());
                    break;
                case "-includepath":
                    genSh.setIncludePath(argArray.nextMaybeQuotationTuplePopStringTrim());
                    genSh.accumulateCommand(genSh.getIncludePath());
                    break;
                case "-prelude":
                    genSh.addPreludeCommand(argArray.nextMaybeQuotationTuplePopStringTrim());
                    genSh.accumulateCommandQuoted(genSh.getPreludeCommandList().get(genSh.getPreludeCommandList().size() - 1));
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (getCommandResult() != COMMANDRESULT.FAILURE) {
            if (argArray.size() < 3) {
                logArgArrayTooShortError(argArray);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                genSh.setStrictPosix(strictPosix);
                String scriptname = argArray.nextMaybeQuotationTuplePopString();
                String includename = argArray.nextMaybeQuotationTuplePopString();
                String functionInvocation = argArray.nextMaybeQuotationTuplePopString();
                genSh.setScriptName(scriptname);
                genSh.accumulateCommandQuoted(scriptname);
                genSh.setIncludeName(includename);
                genSh.accumulateCommand(includename);
                genSh.setFunctionInvocation(functionInvocation);
                genSh.accumulateCommandQuoted(functionInvocation);
                try {
                    put(genSh.genSh());
                } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                    getLogger().log(Level.SEVERE, "Could not put script in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    private boolean validOptChar(String c) {
        return !c.matches(RESERVED);
    }

    private void reservedError(String c) {
        getLogger().log(Level.SEVERE, "Reserved option character {0} in {1}", new Object[]{c, getNameAndDescription()});
        setCommandResult(COMMANDRESULT.FAILURE);
    }

    private boolean validateOptChar(ArgArray a) {
        String c = a.peekNext().substring(0, 1);
        boolean valid = validOptChar(c);
        if (!valid) {
            reservedError(c);
        }
        return valid;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdGenSh(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
