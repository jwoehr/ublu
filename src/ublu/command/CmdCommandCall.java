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
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Calls a host command on OS400.
 *
 * @author jwoehr
 */
public class CmdCommandCall extends Command {

    {
        setNameAndDescription("commandcall", "/4? [-as400 ~@as400] [-to datasink] ~@{system} ~@{userid} ~@{passwd} ~@{commandstring} : execute a CL command");
    }

    /**
     * ctor/0
     */
    public CmdCommandCall() {
    }

    /**
     * Execute a command on the host.
     *
     * <p>
     * Looks for its {@code ${ quoted string }$} argument and executes on the
     * specified server on behalf of the specified uid/passwd.</p>
     *
     * @param argArray the passed in arguments
     * @return what's left of the args
     */
    public ArgArray commandcall(ArgArray argArray) {
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    // /* debug */ getLogger().log(Level.INFO, "my AS400 == {0}", getAs400());
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
//                case "-from":
//                    String srcName = argArray.next();
//                    setDataSrc(DataSink.fromSinkName(srcName));
//                    break;
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
                    getLogger().log(Level.SEVERE, "commandcall/6+? could not create an AS400 instance", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (getAs400() != null) {
                String commandString = argArray.nextMaybeQuotationTuplePopString();
                // /* Debug */ getLogger().log(Level.INFO, "Command string is: {0}", commandString);
                if (commandString == null) {
                    getLogger().log(Level.SEVERE, "Cannot execute null command string in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    // /* DEBUG */ getLogger().log(Level.INFO, "Command string is: " + commandString);
                    StringBuilder sb = new StringBuilder();
                    CommandCall command;
                    try {
                        command = new CommandCall(getAs400());
                        // /* Debug */ getLogger().log(Level.INFO, "Command string is: {0}", commandString);
                        if (command.run(commandString) != true) {
                            getLogger().log(Level.WARNING, "commandcall failed");
                        }
                        // Show the messages (returned whether or not there was an error.)
                        AS400Message[] messagelist = command.getMessageList();
                        for (AS400Message message : messagelist) {
                            sb.append(message.getText());
                            put(sb.toString());
                        }
                    } catch (AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | PropertyVetoException ex) {
                        getLogger().log(Level.SEVERE, "Command " + commandString + " failed in commandcall", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (SQLException ex) {
                        getLogger().log(Level.SEVERE, null, ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } finally {
                        getAs400().disconnectService(AS400.COMMAND);
                    }
                }
            }
        }
        return argArray;
    }

//    private String getCommandString(ArgArray argArray) {
//        String commandString = null;
//        DataSink ds = getDataSrc();
//        switch (ds.getType()) {
//            case TUPLE:
//                Tuple t = getTuple(ds.getName());
//                if (!(t == null)) {
//                    Object o = t.getValue();
//                    if (o instanceof String) {
//                        commandString = String.class.cast(o);
//                    }
//                }
//                break;
//            case FILE:
//                try (FileInputStream fis = new FileInputStream(new File(getDataSrc().getName()))) {
//                    StringBuilder sb = new StringBuilder();
//                    byte[] buf = new byte[32767];
//                    while (fis.available() > 0) {
//                        int numread = fis.read(buf);
//                        sb.append(new String(buf, 0, numread));
//                    }
//                    commandString = sb.toString();
//                } catch (FileNotFoundException ex) {
//                    getLogger().log(Level.SEVERE, "Couldn't find file specified as data source in " + getNameAndDescription(), ex);
//                    setCommandResult(COMMANDRESULT.FAILURE);
//                } catch (IOException ex) {
//                    getLogger().log(Level.SEVERE, "Couldn't input file specified as data source in " + getNameAndDescription(), ex);
//                    setCommandResult(COMMANDRESULT.FAILURE);
//                }
//                break;
//            case STD:
//                if (!argArray.isOpenQuoteNext()) {
//                    getLogger().log(Level.SEVERE, "commandcall/6+? (system userid passwd ${ commandstring }$ must contain a ${ quoted string }$)");
//                    setCommandResult(COMMANDRESULT.FAILURE);
//                } else {
//                    argArray.assimilateFullQuotation();
//                    commandString = argArray.next();
//                }
//                break;
//        }
//        return commandString;
//    }
    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return commandcall(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return super.getCommandResult();
    }
}
