/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.swing.JOptionPane;

/**
 * Ask the user and get a response
 *
 * @author jwoehr
 */
public class CmdAsk extends Command {

    {
        setNameAndDescription("ask",
                "/0 [-to datasink] [-from datasink] [-say ~@{prompt string}]  : get input from user");
    }

    /**
     * Cmd action to ask user and get a response
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray cmdAsk(ArgArray argArray) {
        String prompt = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    break;
                case "-say":
                    prompt = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            String s;
            if (getDataSrc().getType().equals(DataSink.SINKTYPE.TUPLE)) {
                prompt = getTuple(getDataSrc().getName()).getValue().toString();
            } else if (getDataSrc().getType().equals(DataSink.SINKTYPE.FILE)) {
                try {
                    prompt = getPromptFromFile();
                } catch (FileNotFoundException ex) {
                    getLogger().log(Level.SEVERE, "Could not find file for prompt in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "Could not read file for prompt in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (getInterpreter().isConsole()) {
                /* debug */ // getInterpreter().outputerrln("Is console.");
                if (prompt != null) {
                    s = System.console().readLine("%s : ", prompt.trim());
                } else {
                    s = System.console().readLine();
                }
                try {
                    put(s);
                } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                    getLogger().log(Level.SEVERE, "Could not put response \"" + s + "\"" + inNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else { // Not console
                if (getInterpreter().isWindowing()) {
                    try {
                        put(JOptionPane.showInputDialog(null, prompt));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Could not either get or put response with prompt " + prompt + " windowing in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                } else {
                    if (prompt != null) {
                        prompt = prompt.trim() + (isGoubluing() ? " : \n" : " : ");
                        getInterpreter().output(prompt);
                    }
                    try {
                        put(getInterpreter().getInputStreamBufferedReader().readLine());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Could not either get or put response with prompt " + prompt + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
            }
        }
        return argArray;
    }

    private String getPromptFromFile() throws FileNotFoundException, IOException {
        String filepath = getDataSrc().getName();
        FileReader fileReader;
        fileReader = new FileReader(new File(filepath));
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        return bufferedReader.readLine();
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdAsk(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
