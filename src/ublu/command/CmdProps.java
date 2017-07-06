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
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manage preferences
 *
 * @author jwoehr
 */
public class CmdProps extends Command {

    {
        setNameAndDescription("props", "/0 [-to datasink] -set ~@${ name }$ ~@${ value }$ | -get ~@${ name }$ | -list | -read ~@${filepath}$ | -write ~@${filepath}$ ~@${comment}$ : manage properties");
    }

    /**
     * The ops we know
     */
    protected enum OPERATIONS {

        /**
         * Set a prpperty
         */
        SET,
        /**
         * List properties
         */
        LIST,
        /**
         * Get a property
         */
        GET,
        /**
         * Read in a properties file
         */
        READ,
        /**
         * Write out a properties file
         */
        WRITE,
        /**
         * Nada
         */
        NOOP
    }

    /**
     * Operate on properties
     *
     * @param argArray args to the interpreter
     * @return what's left of the args
     */
    public ArgArray props(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.NOOP;
        String propfilepath = null;
        String comment = null;
        String propname = null;
        String value = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-get":
                    operation = OPERATIONS.GET;
                    propname = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-set":
                    operation = OPERATIONS.SET;
                    propname = argArray.nextMaybeQuotationTuplePopString();
                    value = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-list":
                    operation = OPERATIONS.LIST;
                    break;
                case "-read":
                    operation = OPERATIONS.READ;
                    propfilepath = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-write":
                    operation = OPERATIONS.WRITE;
                    propfilepath = argArray.nextMaybeQuotationTuplePopString();
                    comment = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (operation) {
                case GET:
                    try {
                        put(getInterpreter().getProperty(propname));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting propery in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SET:
                    getInterpreter().setProperty(propname, value);
                    break;
                case LIST:
                    StringBuilder sb = new StringBuilder();
                    Set set = getInterpreter().propertyKeys();
                    for (Object o : set) {
                        sb.append(o).append('=').append(getInterpreter().getProperty(o.toString())).append('\n');
                    }
                    if (sb.length() > 0) { // remove last linefeed
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    try {
                        put(sb.toString());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting properies list in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case READ:
                    try {
                        getInterpreter().readProps(propfilepath);
                    } catch (FileNotFoundException ex) {
                        getLogger().log(Level.SEVERE, "Error reading properties file in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (IOException ex) {
                        getLogger().log(Level.SEVERE, "Error reading properties file in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case WRITE:
                    try {
                        getInterpreter().writeProps(propfilepath, comment);
                    } catch (FileNotFoundException ex) {
                        getLogger().log(Level.SEVERE, "Error writing properties file in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (IOException ex) {
                        getLogger().log(Level.SEVERE, "Error writing properties file in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unhandled operation {0} in {1}", new Object[]{operation.name(), getNameAndDescription()});
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return props(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
