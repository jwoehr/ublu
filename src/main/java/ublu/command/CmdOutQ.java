/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
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

import ublu.AS400Factory;
import ublu.util.ArgArray;
import ublu.util.Tuple;
import ublu.util.Utils;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.OutputQueue;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to manipulate output queues
 *
 * @author jwoehr
 */
public class CmdOutQ extends Command {

    {
        setNameAndDescription("outq",
                "/4? [-as400 @as400] [--,-outq ~@outqueue] [-to @var] [-from @qnamevar] [[-clear [[user jobuser] | [form formtype] | all]] | [-get ~@{attributename}] | [-getfloat ~@{attr_int}] | [-getint ~@{attr_int}] | [-getstring ~@{attr_int}] | [-hold] | [-new,-instance] | [-noop] | [-release]] outputqueuename system user password : operate on output queues");
    }

    /**
     * What we do to the Q
     */
    public enum FUNCTIONS {

        /**
         * Clear the Q
         */
        CLEAR,
        /**
         * Get any attribute
         */
        GET,
        /**
         * Get float attribute
         */
        GETFLOAT,
        /**
         * Get int attribute
         */
        GETINT,
        /**
         * Get string attribute
         */
        GETSTRING,
        /**
         * Hold the Q
         */
        HOLD,
        /**
         * Put the Q instance
         */
        INSTANCE,
        /**
         * Release the Q
         */
        RELEASE,
        /**
         * do nothing
         */
        NOOP
    }

    /**
     * Operate on OutputQueues
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray outqueue(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.INSTANCE;
        OutputQueue outQ = null;
        String clearOptName = "";
        String clearOptValue = "";
        String attributeName = null;
        Integer attr_int = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    break;
                case "-clear":
                    function = FUNCTIONS.CLEAR;
                    clearOptName = argArray.nextMaybeTupleString();
                    if (!clearOptName.equals("all")) {
                        clearOptValue = argArray.nextMaybeTupleString();
                    }
                    break;
                case "-noop":
                    function = FUNCTIONS.NOOP;
                    break;
                case "--":
                case "-outq":
                    outQ = argArray.nextTupleOrPop().value(OutputQueue.class);
                    break;
                case "-get":
                    function = FUNCTIONS.GET;
                    attributeName = "ATTR_" + argArray.nextMaybeQuotationTuplePopString().toUpperCase().trim();
                    break;
                case "-getfloat":
                    function = FUNCTIONS.GETFLOAT;
                    attr_int = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-getint":
                    function = FUNCTIONS.GETINT;
                    attr_int = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-getstring":
                    function = FUNCTIONS.GETSTRING;
                    attr_int = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-hold":
                    function = FUNCTIONS.HOLD;
                    break;
                case "-new":
                case "-instance":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-release":
                    function = FUNCTIONS.RELEASE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (outQ == null) {
            switch (getDataSrc().getType()) {
                case TUPLE:
                    String tuplename = getDataSrc().getName();
                    Tuple t = getTuple(tuplename);
                    if (!(t == null)) {
                        Object o = t.getValue();
                        if (o instanceof String) {
                            outQ = new OutputQueue(getAs400(), o.toString());
                        } else {
                            getLogger().log(Level.SEVERE, "Tuple was not a string for OutputQueue name in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "Tuple {0} does not exist to specify OutputQueue name in {1}", new Object[]{tuplename, getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case FILE:
                    String filename = getDataSrc().getName();
                    try {
                        FileReader fr = new FileReader(filename);
                        BufferedReader br = new BufferedReader(fr);
                        String outQName = br.readLine();
                        outQ = new OutputQueue(getAs400(), outQName);
                    } catch (FileNotFoundException ex) {
                        getLogger().log(Level.SEVERE, "Couldn't get OutputQueue from file " + filename, ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (IOException ex) {
                        getLogger().log(Level.SEVERE, "Couldn't get OutputQueue from file " + filename, ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case STD:
                    if (argArray.size() < 1) {
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else { // get the Job factors
                        String outQName = argArray.next();
                        if (getAs400() == null) { // no AS400 instance
                            if (argArray.size() < 3) {
                                logArgArrayTooShortError(argArray);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else { // Get the AS400 instance
                                String system = argArray.next();
                                String userid = argArray.next();
                                String password = argArray.next();
                                try {
                                    setAs400(AS400Factory.newAS400(getInterpreter(), system, userid, password));
                                } catch (PropertyVetoException ex) {
                                    getLogger().log(Level.SEVERE, "Couldn't create AS400 instance in " + getNameAndDescription(), ex);
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                        }
                        if (getAs400() != null) {
                            outQ = new OutputQueue(getAs400(), outQName);
                        } else {
                            getLogger().log(Level.SEVERE, "No AS400, couldn''t instance OutputQueue from STD: in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
            }
        }
        if (outQ != null) {
            switch (function) {
                case CLEAR:
                    PrintParameterList ppl = new PrintParameterList();
                    switch (clearOptName) {
                        case "all":
                            ppl = null;
                            break;
                        case "user":
                            ppl.setParameter(OutputQueue.ATTR_JOBUSER, clearOptValue);
                            break;
                        case "form":
                            ppl.setParameter(OutputQueue.ATTR_FORMTYPE, clearOptValue);
                    }
                    try {
                        outQ.clear(ppl);
                    } catch (AS400Exception | AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception clearing outq " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (ErrorCompletingRequestException ex) {
                        getLogger().log(Level.SEVERE, "Exception clearing outq " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case GET:
                    Object attrValue = Utils.attrNameToValue(outQ, attributeName, this);
                    if (attrValue != null) {
                        try {
                            put(attrValue);
                        } catch (AS400SecurityException | SQLException | ObjectDoesNotExistException | IOException | InterruptedException | RequestNotSupportedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Error putting attribute in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case GETFLOAT:
                    try {
                        put(outQ.getFloatAttribute(attr_int));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception getting float attribute of " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case GETINT:
                    try {
                        put(outQ.getIntegerAttribute(attr_int));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception getting integer attribute of " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case GETSTRING:
                    try {
                        put(outQ.getStringAttribute(attr_int));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception getting string attribute of " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case HOLD:
                    try {
                        outQ.hold();
                    } catch (AS400Exception | AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception holding outq " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (ErrorCompletingRequestException ex) {
                        getLogger().log(Level.SEVERE, "Exception holding outq " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case INSTANCE:
                    try {
                        put(outQ);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception holding putting " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NOOP:
                    break;
                case RELEASE:
                    try {
                        outQ.release();
                    } catch (AS400Exception | AS400SecurityException | IOException | InterruptedException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception releasing outq " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (ErrorCompletingRequestException ex) {
                        getLogger().log(Level.SEVERE, "Exception releasing outq " + outQ + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return outqueue(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
