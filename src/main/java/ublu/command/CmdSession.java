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
import ublu.util.Sess5250;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.awt.Point;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to create and manage sess5250 sessions and data streams.
 *
 * @author jwoehr
 */
public class CmdSession extends Command {

    {
        setNameAndDescription("session or sess",
                "/0 --,-sess @sess [-to datasink] [-from datasink] [-nt] [[-? ~@${question}$] | [-close] | [-disconnect] | [-dump] | [-getcursor] | [-setcursor x y] | [-send [~@${ send string including [tab] metakeys etc. }$] ]  : interact with a tn5250 session");
    }

    enum OPERATIONS {

        NOOP, CLOSE, DISCONNECT, DUMP, QUESTION, SEND, SETCURSOR, GETCURSOR
    }

    /**
     * Create and manage sess5250 instances and data streams.
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray sess5250(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.NOOP; // the default
        String sess5250TupleName = null;
        Tuple sess5250HelperTuple = null;
        String sendString = null;
        String questionString = null;
        Point cursorPoint = null;
        boolean isTrim = true;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    break;
                case "-close":
                    operation = OPERATIONS.CLOSE;
                    break;
                case "-disconnect":
                    operation = OPERATIONS.DISCONNECT;
                    break;
                case "-dump":
                    operation = OPERATIONS.DUMP;
                    break;
                case "-getcursor":
                    operation = OPERATIONS.GETCURSOR;
                    break;
                case "-setcursor":
                    operation = OPERATIONS.SETCURSOR;
                    int x = argArray.nextIntMaybeTupleString();
                    int y = argArray.nextIntMaybeTupleString();
                    cursorPoint = new Point(x, y);
                    break;
                case "-?":
                    operation = OPERATIONS.QUESTION;
                    questionString = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-nt":
                    isTrim = false;
                    break;
                case "--":
                case "-sess":
                    sess5250TupleName = argArray.next();
                    sess5250HelperTuple = getTuple(sess5250TupleName);
                    break;
                case "-send":
                    operation = OPERATIONS.SEND;
                    try {
                        sendString = getStringFromDataSrc(argArray);
                    } catch (IOException ex) {
                        getLogger().log(Level.SEVERE, "Error reading send string from file " + getDataSrc().getName() + inNameAndDescription(), ex);
                    }
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Sess5250 sess;
            switch (operation) {
                case SEND:
                    if (sendString == null) {
                        getLogger().log(Level.SEVERE, "Could not instance send string in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        sess = instanceSess5250FromTuple(sess5250TupleName, sess5250HelperTuple);
                        if (sess == null) {
                            getLogger().log(Level.SEVERE, "Could not instance session for send in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            sess.sendKeys(isTrim ? sendString.trim() : sendString);
                        }
                    }
                    break;
                case CLOSE:
                    sess = instanceSess5250FromTuple(sess5250TupleName, sess5250HelperTuple);
                    if (sess == null) {
                        getLogger().log(Level.SEVERE, "Could not instance session for close in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        sess.close();
                    }
                    break;
                case GETCURSOR:
                    sess = instanceSess5250FromTuple(sess5250TupleName, sess5250HelperTuple);
                    if (sess == null) {
                        getLogger().log(Level.SEVERE, "Could not instance session for close in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(sess.getCursor());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "error putting cursor of session instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case SETCURSOR:
                    sess = instanceSess5250FromTuple(sess5250TupleName, sess5250HelperTuple);
                    if (sess == null) {
                        getLogger().log(Level.SEVERE, "Could not instance session for close in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        sess.setCursor(cursorPoint);
                    }
                    break;
                case DISCONNECT:
                    sess = instanceSess5250FromTuple(sess5250TupleName, sess5250HelperTuple);
                    if (sess == null) {
                        getLogger().log(Level.SEVERE, "Could not instance session for disconnect in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        sess.disconnect();
                    }
                    break;
                case DUMP:
                    sess = instanceSess5250FromTuple(sess5250TupleName, sess5250HelperTuple);
                    if (sess == null) {
                        getLogger().log(Level.SEVERE, "Could not instance session for send in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else if (sess.isConnected()) {
                        try {
                            put(sess.screenDump());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "error putting dump  of session instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "session instance is not connected for scrdump in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case QUESTION:
                    sess = instanceSess5250FromTuple(sess5250TupleName, sess5250HelperTuple);
                    if (sess == null) {
                        getLogger().log(Level.SEVERE, "Could not instance session for question in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        Object answer = null;
                        switch (questionString) {
                            case "connected":
                                answer = sess.isConnected();
                                break;
                            default:
                                getLogger().log(Level.SEVERE, "Don't know answer for question in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        try {
                            put(answer);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting answer for question in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
            }
        }
        return argArray;
    }

    private Sess5250 instanceSess5250FromTuple(String tupleName, Tuple t) {
        Sess5250 sess = null;
        if (t == null) {
            getLogger().log(Level.SEVERE, "Tuple {0} is null", tupleName);
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Object o = t.getValue();
            if (o instanceof Sess5250) {
                sess = Sess5250.class.cast(o);
            } else {
                getLogger().log(Level.SEVERE, "Tuple {0} does not represent a Sess5250", tupleName);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return sess;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return sess5250(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
