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

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.ArgArray;

/**
 * BREAK from enclosing FOR
 *
 * @author jwoehr
 */
public class CmdInterpreter extends Command {

    {
        setNameAndDescription("interpreter", "/0 [-all | -getlocale | -setlocale ~@{lang} ~@{country} | -getmessage ~@{key} | -args | -opts | -arg ~@{nth} | -opt ~@{nth} | -optarg ~@{nth} | -allargs] : info on and control of Ublu and the interpreter at the level this command is invoked");
    }

    enum OPS {
        ALL,
        GET_LOCALE,
        SET_LOCALE,
        GET_MESSAGE,
        OPT,
        OPTARG,
        ARG,
        ARGS,
        OPTS,
        ALLARGS
    }

    ArgArray doInterpreter(ArgArray argArray) {
        OPS op = OPS.ALL;
        String language = null;
        String country = null;
        String messagekey = null;
        Integer nth = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-all":
                    op = OPS.ALL;
                    break;
                case "":
                    break;
                case "-getlocale":
                    op = OPS.GET_LOCALE;
                    break;
                case "-setlocale":
                    op = OPS.SET_LOCALE;
                    language = argArray.nextMaybeQuotationTuplePopStringTrim();
                    country = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-getmessage":
                    op = OPS.GET_MESSAGE;
                    messagekey = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-args":
                    op = OPS.ARGS;
                    break;
                case "-opts":
                    op = OPS.OPTS;
                    break;
                case "-opt":
                    op = OPS.OPT;
                    nth = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-optarg":
                    op = OPS.OPTARG;
                    nth = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-arg":
                    op = OPS.ARG;
                    nth = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-allargs":
                    op = OPS.ALLARGS;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (op) {
                case ALL: {
                    try {
                        put("Instance depth : " + getInterpreter().getInstanceDepth());
                        put("Frame depth : " + getInterpreter().getFrameDepth());
                        put("FOR block : " + getInterpreter().isForBlock());
                        put("Break issued : " + getInterpreter().isBreakIssued());
                        put("History filename : " + getInterpreter().getHistoryFileName());
                        put("History manager : " + getInterpreter().getHistory());
                        put("Locale info : " + getLocaleHelper());

                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception putting interpreter info in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
                break;
                case GET_LOCALE:
                    try {
                        put(getLocaleHelper().getCurrentLocale());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception putting locale info in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case GET_MESSAGE:
                    try {
                        put(getLocaleHelper().getString(messagekey));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception putting locale info in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }

                    break;
                case SET_LOCALE:
                    setLocale(language, country);
                    break;
                case ARGS:
                    try {
                        put(getUbluArgs().argumentCount());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting num args in " + getNameAndDescription(), ex);
                    }
                    break;
                case OPTS:
                    try {
                        put(getUbluArgs().optionCount());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting num opts in " + getNameAndDescription(), ex);
                    }
                    break;
                case ARG:
                    try {
                        put(getUbluArgs().nthArgument(nth));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting " + nth + " argument in " + getNameAndDescription(), ex);
                    }
                    break;
                case OPT:
                    try {
                        put(getUbluArgs().nthOption(nth).option);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting " + nth + " option in " + getNameAndDescription(), ex);
                    }
                    break;
                case OPTARG:
                    try {
                        put(getUbluArgs().nthOption(nth).argument);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting " + nth + " option argument in " + getNameAndDescription(), ex);
                    }
                    break;
                case ALLARGS:
                    try {
                        put(getUbluArgs().getArgumentsAsStringArray());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting all args in " + getNameAndDescription(), ex);
                    }
                    break;
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return doInterpreter(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
