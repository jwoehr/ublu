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

import ublu.AS400Factory;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics.QueuedMessageKey;
import ublu.util.Generics.QueuedMessageList;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IllegalPathNameException;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.QueuedMessage;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * Command to get messages from a message queue on the host.
 *
 * @author jwoehr
 */
public class CmdMsgQ extends Command {

    {
        setNameAndDescription("msgq", "/4? [[-as400 @as400] [--,-msgq ~@messagequeue]] [-to datasink] [-instance | -query | -remove messagekey | -removeall | -sendinfo ~@{message text ...} | -sendinquiry ~@{message text} ~@replyqueueIFSpath | -sendreply messagekey ~@{reply text} | -sendreplybinkey ~@bytearraykey ~@{reply text}] [[-all ] | [[-none] [-reply] [-noreply] [-copyreply]]] ~@system ~@fullyqualifiedifspath ~@userid ~@passwd : send, retrieve, remove or reply messages");
    }

    /**
     * The ops we know
     */
    protected enum FUNCTIONS {

        /**
         * Instance representation of the queue
         */
        INSTANCE,
        /**
         * Query the queue
         */
        QUERY,
        /**
         * remove next message
         */
        REMOVE,
        /**
         * remove all messages
         */
        REMOVEALL,
        /**
         * Send an informational message
         */
        SENDINFO,
        /**
         * Send an inquiry message
         */
        SENDINQ,
        /**
         * Reply a message expressing key in text
         */
        SENDREPLY,
        /**
         * Reply a message passing key as byte []
         */
        SENDREPLYBINKEY
    }

    /**
     * Arity-0 ctor
     */
    public CmdMsgQ() {
    }

    /**
     * Fetch the contents of a message queue.
     *
     * @param argArray the args to the interpreter
     * @return what's left of the args
     */
    public ArgArray msgq(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.QUERY;
        boolean all = true;
        boolean reply = false;
        boolean noreply = false;
        boolean copyreply = true;
        String messageKey = null;
        String sendreply = null;
        String sendText = null;
        String replyQueue = null;
        Tuple mqTuple = null;
        Tuple binkeyTuple = null;

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
                case "--":
                case "-msgq":
                    mqTuple = argArray.nextTupleOrPop();
                    break;
                case "-all":
                    all = true;
                    break;
                case "-none":
                    all = false;
                    reply = false;
                    noreply = false;
                    copyreply = false;
                    break;
                case "-query":
                    function = FUNCTIONS.QUERY;
                    break;
                case "-reply":
                    all = false;
                    reply = true;
                    break;
                case "-noreply":
                    all = false;
                    noreply = true;
                    break;
                case "-copyreply":
                    all = false;
                    copyreply = true;
                    break;
                case "-instance":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-remove":
                    function = FUNCTIONS.REMOVE;
                    messageKey = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-removeall":
                    function = FUNCTIONS.REMOVEALL;
                    break;
                case "-sendinfo":
                    function = FUNCTIONS.SENDINFO;
                    sendText = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-sendinquiry":
                    function = FUNCTIONS.SENDINQ;
                    sendText = argArray.nextMaybeQuotationTuplePopString();
                    replyQueue = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-sendreply":
                    function = FUNCTIONS.SENDREPLY;
                    messageKey = argArray.nextMaybeQuotationTuplePopString();
                    sendreply = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-sendreplybinkey":
                    function = FUNCTIONS.SENDREPLYBINKEY;
                    binkeyTuple = argArray.nextTupleOrPop();
                    sendreply = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            String system = null;
            String ifsPath = null;
            String userid = null;
            String passwd = null;
            MessageQueue mq = null;
            if (mqTuple != null) {
                Object tupleValue = mqTuple.getValue();
                if (tupleValue instanceof MessageQueue) {
                    mq = MessageQueue.class.cast(tupleValue);
                } else {
                    getLogger().log(Level.WARNING, "Valued tuple which is not a Job tuple provided to -job in {0}", getNameAndDescription());
                }
            }
            if (mq == null) {
                if (getAs400() == null) {
                    if (argArray.size() < 4) {
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        system = argArray.nextMaybeQuotationTuplePopString();
                        ifsPath = argArray.nextMaybeQuotationTuplePopString();
                        userid = argArray.nextMaybeQuotationTuplePopString();
                        passwd = argArray.nextMaybeQuotationTuplePopString();
                    }
                } else {
                    ifsPath = argArray.nextMaybeQuotationTuplePopString();
                }
                if (getAs400() == null) {
                    try {
                        setAs400(AS400Factory.newAS400(getInterpreter(), system, userid, passwd));
                    } catch (PropertyVetoException ex) {
                        getLogger().log(Level.SEVERE, "Error in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
            }
            if (getCommandResult() != COMMANDRESULT.FAILURE) {
                Object o;
                switch (function) {
                    case INSTANCE:
                        if (getAs400() != null) {
                            mq = new MessageQueue(getAs400(), ifsPath);
                            try {
                                put(mq);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Could not instance message queue in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No AS400 instance to instance message queue in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case QUERY:
                        if (mq != null) {
                            QueuedMessageList qml = null;
                            try {
                                qml = readMq(mq, all, reply, noreply, copyreply);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Could not read message queue in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            if (getCommandResult().equals(COMMANDRESULT.SUCCESS)) {
                                try {
                                    put(qml);
                                } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                    getLogger().log(Level.SEVERE, "Could not put queued message list in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                        } else if (getAs400() != null && ifsPath != null) {
                            readMessageQueue(ifsPath, all, reply, noreply, copyreply);
                        } else {
                            getLogger().log(Level.SEVERE, "Neither MessageQueue nor AS400/IFS pair instanced in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }

                        break;
                    case REMOVE:
                        if (mq != null) {
                            try {
                                remove(mq, messageKey);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Error removing message " + messageKey + " in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "MessageQueue not instanced for remove in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case REMOVEALL:
                        if (mq != null) {
                            try {
                                mq.remove();
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Error removing message " + messageKey + " in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "MessageQueue not instanced for remove all in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case SENDINFO:
                        if (mq != null) {
                            try {
                                sendInfo(mq, sendText);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Could not send informational in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "MessageQueue not instanced in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case SENDINQ:
                        if (mq != null) {
                            try {
                                mq.sendInquiry(sendText, replyQueue);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Could not send informational in {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "MessageQueue not instanced for sendinq in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case SENDREPLY:
                        if (mq != null) {
                            try {
                                sendReply(mq, messageKey, sendreply);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception replying to a message in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "MessageQueue not instanced for sendreply in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        // sendReply(ifsPath, messageKey, sendreply);
                        break;
                    case SENDREPLYBINKEY:
                        if (mq != null) {
                            byte[] binkey = null;
                            if (binkeyTuple != null) {
                                o = binkeyTuple.getValue();
                                if (o instanceof byte[]) {
                                    binkey = (byte[]) o;
                                }
                            }
                            try {
                                sendReply(mq, binkey, sendreply);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception replying to a message in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "MessageQueue not instanced for sendreplybinkey in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                }
            }
        }

        return argArray;
    }

    private static void setMq(MessageQueue mq, boolean reply, boolean noreply, boolean copyreply) {
        mq.setSelectMessagesNeedReply(reply);
        mq.setSelectMessagesNoNeedReply(noreply);
        mq.setSelectSendersCopyMessagesNeedReply(copyreply);
    }

    private QueuedMessageList readMq(MessageQueue mq, boolean all, boolean reply, boolean noreply, boolean copyreply) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        QueuedMessageList qml = new QueuedMessageList();
        if (!all) {
            setMq(mq, reply, noreply, copyreply);
        }
        mq.load();
        int mqLength = mq.getLength();
        for (int i = 0; i < mqLength; i += 1000) { // MessageQueue.getMessages() retrieves in blocks of 1000 (com/ibm/as400/access/MessageQueue.html)
            Enumeration e = mq.getMessages();
            while (e.hasMoreElements()) {
                qml.add(QueuedMessage.class.cast(e.nextElement()));
            }
        }
        return qml;
    }

    private void sendInfo(MessageQueue mq, String msgtxt) throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        mq.sendInformational(msgtxt);
    }

    private void readMessageQueue(String ifsPath, boolean all, boolean reply, boolean noreply, boolean copyreply) {
        try {
            QueuedMessageList queuedMessageList = new QueuedMessageList();
            MessageQueue mq = new MessageQueue(getAs400(), ifsPath);
            if (!all) {
                setMq(mq, reply, noreply, copyreply);
            }
            mq.load();
            int mqLength = mq.getLength();
            for (int i = 0; i < mqLength; i += 1000) { // MessageQueue.getMessages() retrieves in blocks of 1000 (com/ibm/as400/access/MessageQueue.html)
                Enumeration e = mq.getMessages();
                while (e.hasMoreElements()) {
                    queuedMessageList.add(QueuedMessage.class.cast(e.nextElement()));
                }
                put(queuedMessageList);
            }
            mq.close();
        } catch (AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | IllegalPathNameException ex) {
            getLogger().log(Level.SEVERE, "Error in " + getNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "SQL exception in command msgq", ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

//    private void sendReply(String ifsPath, String key, String reply) {
//        QueuedMessageKey qmk = new QueuedMessageKey(key);
//        MessageQueue mq = new MessageQueue(getAs400(), ifsPath);
//        try {
//            mq.reply(qmk.toMessageKey(), reply);
//        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
//            getLogger().log(Level.SEVERE, "Exception replying to a message in command msgq", ex);
//            setCommandResult(COMMANDRESULT.FAILURE);
//        }
//    }
    private void sendReply(MessageQueue mq, String key, String reply) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        QueuedMessageKey qmk = new QueuedMessageKey(key);
        mq.reply(qmk.toMessageKey(), reply);
    }

    private void sendReply(MessageQueue mq, byte[] key, String reply) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        mq.reply(key, reply);
    }

    private void remove(MessageQueue mq, String key) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        QueuedMessageKey qmk = new QueuedMessageKey(key);
        mq.remove(qmk.toMessageKey());
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return msgq(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
