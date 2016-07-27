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
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to parse text into specific numeric class
 *
 * @author jwoehr
 */
public class CmdNumber extends Command {

    {
        setNameAndDescription("num",
                "/1 [-to (@)datasink] [-int] [-short] [-double] [-long] [-float] [-radix ~@{radix}] ~@{numstring} : convert string to number class instance");
    }

    /**
     * Conversions we do (to wrapper classes)
     */
    protected enum CONVERSION {

        /**
         * D'oh
         */
        INT,
        /**
         * D'oh
         */
        SHORT,
        /**
         * D'oh
         */
        DOUBLE,
        /**
         * D'oh
         */
        LONG,
        /**
         * D'oh
         */
        FLOAT
    }

    /**
     * Convert string to specific java number class
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray number(ArgArray argArray) {
        CONVERSION conversion = CONVERSION.INT;
        int radix = 10;
        while (getCommandResult() != COMMANDRESULT.FAILURE && argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "-int":
                    conversion = CONVERSION.INT;
                    break;
                case "-short":
                    conversion = CONVERSION.SHORT;
                    break;
                case "-double":
                    conversion = CONVERSION.DOUBLE;
                    break;
                case "-long":
                    conversion = CONVERSION.LONG;
                    break;
                case "-float":
                    conversion = CONVERSION.FLOAT;
                    break;
                case "-radix":
                    radix = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }

        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            String theNumber = argArray.nextMaybeQuotationTuplePopString();
            switch (conversion) {
                case INT:
                    try {
                        put(Integer.parseInt(theNumber, radix));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception converting or putting number " + theNumber + " in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SHORT:
                    try {
                        put(Short.parseShort(theNumber, radix));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception converting or putting number " + theNumber + " in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DOUBLE:
                    try {
                        put(Double.parseDouble(theNumber));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception converting or putting number " + theNumber + " in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case LONG:
                    try {
                        put(Long.parseLong(theNumber, radix));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception converting or putting number " + theNumber + " in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case FLOAT:
                    try {
                        put(Float.parseFloat(theNumber));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception converting or putting number " + theNumber + " in " + getNameAndDescription(), ex);
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
        return number(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
