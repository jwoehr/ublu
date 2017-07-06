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
import ublu.util.DataSink;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.QueuedMessage;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to return fields of a Queued Message
 *
 * @author jwoehr
 */
public class CmdMsg extends Command {

    static {
        setNameAndDescription("msg", "/0 --,-msg ~@message [-to datasink] [-sender | -user | -key | -fromjob | -fromjobnumber | -fromprogram | -message | -queue] : examine queued messages");
    }

    private enum FUNCTIONS {

        SENDER, USER, KEY, FROMJOB, FROMJOBNUMBER, FROMPROGRAM, MESSAGE, QUEUE
    }

    /**
     * Do the work of returning fields of a Queued Message
     *
     * @param argArray The arguments in the interp buffer
     * @return what's left afterwards of the arguments in the interp buffer
     */
    public ArgArray doCmdMsg(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.MESSAGE; // default op
        QueuedMessage qm = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "--":
                case "-msg":
                    qm = argArray.nextTupleOrPop().value(QueuedMessage.class);
                    break;
                case "-sender":
                    function = FUNCTIONS.SENDER;
                    break;
                case "-user":
                    function = FUNCTIONS.USER;
                    break;
                case "-key":
                    function = FUNCTIONS.KEY;
                    break;
                case "-fromjob":
                    function = FUNCTIONS.FROMJOB;
                    break;
                case "-fromjobnumber":
                    function = FUNCTIONS.FROMJOBNUMBER;
                    break;
                case "-fromprogram":
                    function = FUNCTIONS.FROMPROGRAM;
                    break;
                case "-message":
                    function = FUNCTIONS.MESSAGE;
                    break;
                case "-queue":
                    function = FUNCTIONS.QUEUE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (qm == null) {
            getLogger().log(Level.SEVERE, "No message tuple in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Object result = null;
            switch (function) {
                case SENDER:
                    result = qm.getSendingUserProfile();
                    break;
                case USER:
                    result = qm.getUser();
                    break;
                case KEY:
                    result = qm.getKey();
                    break;
                case FROMJOB:
                    result = qm.getFromJobName();
                    break;
                case FROMJOBNUMBER:
                    result = qm.getFromJobNumber();
                    break;
                case FROMPROGRAM:
                    result = qm.getFromProgram();
                    break;
                case MESSAGE:
                    result = qm.toString();
                    break;
                case QUEUE:
                    result = qm.getQueue();
                    break;
            }
            try {
                put(result);
            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                getLogger().log(Level.SEVERE, "Error putting message content in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doCmdMsg(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
