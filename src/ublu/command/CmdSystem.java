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
import ublu.util.SystemHelper;
import ublu.util.SystemHelper.ProcessClosure;
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

/**
 * Class to execute a system command from within ublu
 *
 * @author jwoehr
 */
public class CmdSystem extends Command {

    {
        setNameAndDescription("system", "/1 [-to datasink] -from datasink | ~@{ system command } : execute a system command");
    }

    /**
     * 0-arity ctor
     */
    public CmdSystem() {
    }

    /**
     * Get one line of a file and assume it is a valid system command.
     *
     * @param filename the file to read one line from to get a system command
     * @return a system command
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected String readCommandFromFile(String filename) throws FileNotFoundException, IOException {
        StringBuilder sb = new StringBuilder();
        File f = new File(filename);
        FileReader fr = new FileReader(f);
        BufferedReader b = new BufferedReader(fr);
        if (b.ready()) {
            sb.append(b.readLine());
        }
        return sb.toString();
    }

    /**
     * Execute a system command and set results appropriately
     *
     * @param command the command string
     */
    protected void executeCommand(String command) {
        try {
            ProcessClosure pc = SystemHelper.sysCmd(command);
            if (getDataDest().getType() == DataSink.SINKTYPE.TUPLE) {
                put(pc.getOutput());
                getLogger().log(Level.INFO, "return code: {0}", pc.getRc());
            } else {
                put(pc);
            }
        } catch (IOException | RequestNotSupportedException | InterruptedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | ObjectDoesNotExistException ex) {
            getLogger().log(Level.SEVERE, "Error executing system command", ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    /**
     * Do the work of the "system" command
     *
     * @param argArray the argument array from the interpreter
     * @return what's left of the argument array
     */
    public ArgArray system(ArgArray argArray) {
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-from":
                    String srcName = argArray.next();
                    setDataSrc(DataSink.fromSinkName(srcName));
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            String command;
            switch (getDataSrc().getType()) {
                case TUPLE:
                    command = getTuple(getDataSrc().getName()).getValue().toString();
                    executeCommand(command);
                    break;
                case FILE:
                    try {
                        command = readCommandFromFile(getDataSrc().getName());
                        executeCommand(command);
                    } catch (IOException ex) {
                        getLogger().log(Level.SEVERE, "Error reading command line from file {0} in {1}", new Object[]{getDataSrc().getName(), getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case STD:
                    command = argArray.nextMaybeQuotationTuplePopString();
                    executeCommand(command);
                    break;
                case LIFO:
                    command = getTupleStack().pop().getValue().toString();
                    executeCommand(command);
                    break;
                case NUL:
                case URL:
                default:
                    getLogger().log(Level.SEVERE, "Unsupported data source {0} in {1}", new Object[]{getDataSrc().getType(), getNameAndDescription()});
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return system(args);

    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
