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
import ublu.util.DataSink;
import ublu.util.History;
import ublu.util.Parser;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Functionality of the interpreter "history" command.
 * <p>
 * Manages history file creation, reading, writing, and repeat execution.</p>
 *
 *
 * @author jwoehr
 */
public class CmdHistory extends Command {

    {
        setNameAndDescription("history or h", "/0 [-on | -off | -onfile filename | -to datasink | -do linenum [-change expr1 expr2] | -show [-to datasink] | -head numlines | -tail numlines | -name | -range firstline lastline] : turns history file recording on to default, off, on to a filename, or does a numbered line again, shows history, head or tail numlines or a range or set history filename");
    }

    /**
     * 0-arity ctor
     */
    public CmdHistory() {
    }

    /**
     * The functions History can perform
     */
    protected static enum FUNCTIONS {

        /**
         * Turn history off
         */
        OFF,
        /**
         * Turn history on
         */
        ON,
        /**
         * Turn history on changing history file commandName
         */
        ONFILE,
        /**
         * Show all history
         */
        SHOW,
        /**
         * Show n lines from head of history
         */
        HEAD,
        /**
         * Show n lines from tail of history
         */
        TAIL,
        /**
         * Display current history filename
         */
        NAME,
        /**
         * Execute again nth (one's-based) history line
         */
        DO,
        /**
         * Return a range of lines.
         */
        RANGE
    }
    private FUNCTIONS function;

    private FUNCTIONS getFunction() {
        return function;
    }

    private void setFunction(FUNCTIONS function) {
        this.function = function;
    }

    /**
     * Manage history file
     *
     * @param args the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray history(ArgArray args) {
        String historyFileName = History.DEFAULT_HISTORY_FILENAME;
        int numLines = 0;
        int doLine = 0;
        int firstLine = -1;
        int lastLine = -1;
        String change_from = null;
        String change_to = null;
        while (args.hasDashCommand()) {
            String dashCommand = args.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(args);
                    break;
                case "-off":
                    setFunction(FUNCTIONS.OFF);
                    break;
                case "-on":
                    setFunction(FUNCTIONS.ON);
                    break;
                case "-onfile":
                    setFunction(FUNCTIONS.ONFILE);
                    historyFileName = args.next();
                    break;
                case "-range":
                    setFunction(FUNCTIONS.RANGE);
                    firstLine = args.nextInt();
                    lastLine = args.nextInt();
                    break;
                case "-show":
                    setFunction(FUNCTIONS.SHOW);
                    break;
                case "-head":
                    setFunction(FUNCTIONS.HEAD);
                    numLines = args.nextInt();
                    break;
                case "-tail":
                    setFunction(FUNCTIONS.TAIL);
                    numLines = args.nextInt();
                    break;
                case "-name":
                    setFunction(FUNCTIONS.NAME);
                    break;
                case "-do":
                    setFunction(FUNCTIONS.DO);
                    doLine = args.nextInt() - 1; // from one's-based history
                    // to 0-based index into history array
                    break;
                case "-change": // not a function, just a suboption to DO
                    change_from = args.nextMaybeQuotation();
                    change_to = args.nextMaybeQuotation();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            History h = getInterpreter().getHistory();
            boolean withLineNumbers = getDataDest().getType() == DataSink.SINKTYPE.STD;
            switch (getFunction()) {
                case OFF:
                    getInterpreter().closeHistory();
                    break;
                case ON:
                    getInterpreter().instanceHistory();
                    break;
                case ONFILE:
                    getInterpreter().setHistoryFileName(historyFileName);
                    getInterpreter().instanceHistory();
                    break;
                case SHOW:
                    if (h != null) {
                        try {
                            put(h.show(withLineNumbers));
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put history", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "History is not enabled (try history -on)");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case HEAD:
                    if (h != null) {
                        try {
                            put(h.head(numLines, withLineNumbers));
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put history", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "History is not enabled (try history -on)");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case TAIL:
                    if (h != null) {
                        try {
                            put(h.tail(numLines, withLineNumbers));
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put history", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "History is not enabled (try history -on)");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NAME:
                    if (h != null) {
                        try {
                            put(h.getHistoryFileName());
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put history name", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "History is not enabled (try history -on)");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case DO:
                    if (h != null) {
                        if (doLine >= 0) {
                            try {
                                String nthLine = h.nth(doLine);
                                if (nthLine != null) {
                                    if (change_from != null && change_to != null) {
                                        nthLine = nthLine.replace(change_from, change_to);
                                    }
                                    System.out.println(nthLine);
                                    ArgArray newArgs = new Parser(getInterpreter(), nthLine).parseAnArgArray();
                                    newArgs.addAll(args);
                                    args = newArgs;
                                } else {
                                    getLogger().log(Level.SEVERE, "Line {0} does not exist in history buffer", doLine + 1);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            } catch (IOException ex) {
                                getLogger().log(Level.SEVERE, null, ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "Line '{'0'}' does not exist in history buffer", doLine + 1);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "History is not enabled (try history -on)");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case RANGE:
                    if (h != null) {
                        try {
                            put(h.range(firstLine - 1, lastLine - 1, withLineNumbers));
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Couldn't put history range", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "History is not enabled (try history -on)");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }
        return args;
    }

    @Override
    protected void reinit() {
        super.reinit();
        setFunction(FUNCTIONS.SHOW);
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return history(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
