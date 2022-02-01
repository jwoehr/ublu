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
import ublu.util.SysShepHelper;
import ublu.util.SysShepHelper.ALERTCOMPARATOR;
import ublu.util.SysShepHelper.MetricName;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to manipulate jobs
 *
 * @author jwoehr
 */
public class CmdDPoint extends Command {

    {
        setNameAndDescription("dpoint", "/0? [--,-dpoint @dpoint] [-to datasink] [[-dup] | [-dkey ~@{keytext}] [-addkey ~@{keytext}] [-type ~@{int|long|float} [-value ~@{value}] [-alertlevel ~@{alertlevel}] [-compare ~@{gt|gte|lt|lte|info|warn|crit}] [-msg ~@{msg}]]  : create and manipulate monitoring datapoints");
    }

    private enum VALTYPE {

        INT, FLOAT, LONG
    }

    /**
     * Creating a SystemShepherd datapoint and manipulating it.
     *
     * @param argArray the input arg array
     * @return what's left of the arg array
     */
    public ArgArray dpoint(ArgArray argArray) {
        Tuple helperTuple = null;
        String dkey = null;
        Boolean dup = false;
        String keyToAdd = null;
        String valuetypename = null;
        String value = null;
        String message = null;
        String alertlevel = null;
        String comparison = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
//                case "-from":
//                    String srcName = argArray.next();
//                    setDataSrc(DataSink.fromSinkName(srcName));
//                    break;
                case "--":
                case "-dpoint":
                    helperTuple = argArray.nextTupleOrPop();
                    break;
                case "-dkey":
                    dkey = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-dup":
                    dup = true;
                    break;
                case "-addkey":
                    keyToAdd = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-type":
                    valuetypename = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-alertlevel":
                    alertlevel = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-compare":
                    comparison = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-msg":
                    message = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-value":
                    value = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            SysShepHelper myHelper;
            MetricName mn;
            VALTYPE valuetype = null;
            if (helperTuple != null) {
                myHelper = helperTuple.value(SysShepHelper.class);
                if (myHelper == null) {
                    getLogger().log(Level.SEVERE, "Valued tuple which is not a dpoint tuple in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else {
                myHelper = new SysShepHelper();
            }
            if (myHelper != null) {
                if (dup) {
                    if (helperTuple != null) {
                        try {
                            put(new SysShepHelper(myHelper));
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting duplicate dpoint tuple in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No datapoint tuple provided to duplicate in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                } else {
                    if (dkey != null) {
                        myHelper.setMetric(new MetricName(dkey));
                    }
                    if (keyToAdd != null) {
                        mn = myHelper.getMetric();
                        if (mn != null) {
                            mn.append(keyToAdd);
                        } else {
                            mn = new MetricName(keyToAdd);
                            myHelper.setMetric(mn);
                        }
                    }
                    if (valuetypename != null) {
                        valuetype = getvaltype(valuetypename);
                        if (valuetype == null) {
                            getLogger().log(Level.WARNING, "Unknown value type {0} provided to -type in {1}", new Object[]{valuetypename, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    if (getCommandResult() != COMMANDRESULT.FAILURE) {
                        if (comparison != null) {
                            ALERTCOMPARATOR a = getCompareType(comparison);
                            if (a == null) {
                                getLogger().log(Level.WARNING, "Invalid comparator {0} provided to -compare in {1}", new Object[]{comparison, getNameAndDescription()});
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else {
                                myHelper.setAlertcomparator(a);
                            }
                        }
                        if (getCommandResult() != COMMANDRESULT.FAILURE) {
                            if (value != null) {
                                myHelper.setValue(getNumberByType(value, valuetype));
                            }
                            if (alertlevel != null) {
                                myHelper.setAlertlevel(getNumberByType(alertlevel, valuetype));
                            }
                            if (message != null) {
                                myHelper.setMessage(message);
                            }
                            try {
                                put(myHelper);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting job query in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                }
            }
        }
        return argArray;
    }

    private VALTYPE getvaltype(String valuetype) {
        VALTYPE valtype = null;
        switch (valuetype) {
            case "int":
                valtype = VALTYPE.INT;
                break;
            case "float":
                valtype = VALTYPE.FLOAT;
                break;
            case "long":
                valtype = VALTYPE.LONG;
                break;
        }
        return valtype;
    }

    private ALERTCOMPARATOR getCompareType(String comparison) {
        ALERTCOMPARATOR a = null;
        switch (comparison) {
            case "gt":
                a = ALERTCOMPARATOR.GT;
                break;
            case "gte":
                a = ALERTCOMPARATOR.GTE;
                break;
            case "lt":
                a = ALERTCOMPARATOR.LT;
                break;
            case "lte":
                a = ALERTCOMPARATOR.LTE;
                break;
            case "info":
                a = ALERTCOMPARATOR.INFO;
                break;
            case "warn":
                a = ALERTCOMPARATOR.WARN;
                break;
            case "crit":
                a = ALERTCOMPARATOR.CRIT;
                break;
        }
        return a;
    }

    private Number getNumberByType(String number, VALTYPE valtype) {
        Number n = null;
        switch (valtype == null ? VALTYPE.INT : valtype) {
            case FLOAT:
                n = Float.parseFloat(number);
                break;
            case INT:
                n = Integer.parseInt(number);
                break;
            case LONG:
                n = Long.parseLong(number);
                break;
        }
        return n;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return dpoint(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
