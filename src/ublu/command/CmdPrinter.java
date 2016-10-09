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
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.PrintObject;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.Printer;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Get and set printer attributes
 *
 * @author jwoehr
 */
public class CmdPrinter extends Command {

    {
        setNameAndDescription("printer",
                "/4? [-as400 @as400] [--,-printer ~@printer] [-to @var] [-get ~@{attribute}] | [[-new,-instance] | [-set ~@{attribute} ~@{value}] [-wtrjob]] ~@{printername} ~@{system} ~@{user} ~@{password} : instance as400 printer and get/set attributes");
    }

    private enum OPERATIONS {

        /**
         * Put instance of printer
         */
        INSTANCE,
        /**
         * get attrib
         */
        GET,
        /**
         * set attrib
         */
        SET,
        /**
         * get the printer job
         */
        WTRJOB,
        /**
         * do nothing
         */
        NOOP
    }

    /**
     * Get and set printer attributes
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray printer(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.INSTANCE;
        String attributeName = null;
        Tuple printerTuple = null;
        PrintParameterList ppl = new PrintParameterList();
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-get":
                    operation = OPERATIONS.GET;
                    attributeName = "ATTR_" + argArray.nextMaybeQuotationTuplePopString().toUpperCase().trim();
                    // attributeInt = attribToInt(attributeName);
                    break;
                case "-new":
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-wtrjob":
                    operation = OPERATIONS.WTRJOB;
                    break;
                case "-set":
                    operation = OPERATIONS.SET;
                    attributeName = argArray.nextMaybeQuotationTuplePopString();
                    switch (attributeName) {
                        case "CHANGES":
                            ppl.setParameter(attribToInt(attributeName), argArray.nextMaybeQuotationTuplePopString());
                            break;
                        case "DRWRSEP":
                            ppl.setParameter(attribToInt(attributeName), argArray.nextIntMaybeQuotationTuplePopString());
                            break;
                        case "FILESEP":
                            ppl.setParameter(attribToInt(attributeName), argArray.nextIntMaybeQuotationTuplePopString());
                            break;
                        case "FORMTYPE":
                            ppl.setParameter(attribToInt(attributeName), argArray.nextMaybeQuotationTuplePopString());
                            break;
                        case "OUTPUT_QUEUE":
                            ppl.setParameter(attribToInt(attributeName), argArray.nextMaybeQuotationTuplePopString());
                            break;
                        case "DESCRIPTION":
                            ppl.setParameter(attribToInt(attributeName), argArray.nextMaybeQuotationTuplePopString());
                            break;
                        default:
                            getLogger().log(Level.SEVERE, "Unknown attribute {0} in {1}", new Object[]{attributeName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
                case "--":
                case "-printer":
                    printerTuple = argArray.nextTupleOrPop();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand() || getCommandResult() == COMMANDRESULT.FAILURE) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Printer printer = null;
            String printerName = null;
            if (printerTuple != null) {
                Object o = printerTuple.getValue();
                if (o instanceof Printer) {
                    printer = Printer.class.cast(o);
                } else {
                    getLogger().log(Level.SEVERE, "Tuple value is not a Printer object in  {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else if (argArray.size() < 1) {
                logArgArrayTooShortError(argArray);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                printerName = argArray.nextMaybeQuotationTuplePopString();
            }
            if (printer == null && getAs400() == null) {
                try {
                    if (argArray.size() < 3) {
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        setAs400FromArgs(argArray);
                    }
                } catch (PropertyVetoException ex) {
                    getLogger().log(Level.SEVERE, "Couldn't instance AS400 in " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (printer == null && getAs400() != null) {
                printer = new Printer(getAs400(), printerName);
            }
            if (printer != null) {
                switch (operation) {

                    case INSTANCE:
                        try {
                            put(printer);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting Printer object in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case GET:
                        try {
                            // put(printer.getStringAttribute(attributeInt));
                            put(attrNameToValue(printer, attributeName));
                        } catch (AS400SecurityException | SQLException | ObjectDoesNotExistException | IOException | InterruptedException | RequestNotSupportedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Error getting printer attribute in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case SET:
                        try {
                            printer.setAttributes(ppl);
                        } catch (AS400SecurityException | IOException | InterruptedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Error setting printer attribute in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case WTRJOB:
                        Job pJob = null;
                        try {
                            pJob = new Job(printer.getSystem(), printer.getStringAttribute(Printer.ATTR_WTRJOBNAME), printer.getStringAttribute(PrintObject.ATTR_WTRJOBUSER), printer.getStringAttribute(PrintObject.ATTR_WTRJOBNUM));
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting printer job in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            try {
                                put(pJob);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting printer job in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case NOOP:
                        break;
                }
            }
        }
        return argArray;
    }

    private Integer attribToInt(String attrib) {
        Integer intval = null;
        switch (attrib) {
            case "CHANGES":
                intval = PrintObject.ATTR_CHANGES;
                break;
            case "DRWRSEP":
                intval = PrintObject.ATTR_DRWRSEP;
                break;
            case "FILESEP":
                intval = PrintObject.ATTR_FILESEP;
                break;
            case "FORMTYPE":
                intval = PrintObject.ATTR_FORMTYPE;
                break;
            case "OUTPUT_QUEUE":
                intval = PrintObject.ATTR_OUTPUT_QUEUE;
                break;
            case "DESCRIPTION":
                intval = PrintObject.ATTR_DESCRIPTION;
                break;
            default:
                getLogger().log(Level.SEVERE, "Unknown attribute {0} in {1}", new Object[]{attrib, getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
        }
        return intval;
    }

    private int attrNameToInt(String attrName) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
       return Printer.class.getField(attrName).getInt(Printer.class);
    }

    private Object attrNameToValue(Printer p, String attrName) {
        Object value = null;
        Integer attrInteger = null;
        try {
            attrInteger = attrNameToInt(attrName);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            getLogger().log(Level.SEVERE, "No such attribute " + attrName + " in " + getNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (attrInteger != null) {
            try {
                value = p.getSingleIntegerAttribute(attrInteger);
            } catch (IllegalArgumentException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex) {
                try {
                    value = p.getSingleFloatAttribute(attrInteger);
                } catch (IllegalArgumentException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex1) {
                    try {
                        value = p.getStringAttribute(attrInteger);
                    } catch (IllegalArgumentException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex2) {
                        getLogger().log(Level.SEVERE, "Could not get attribute value for " + attrName + " in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
            }
        }
        return value;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return printer(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
