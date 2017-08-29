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
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Command to test expression
 *
 * @author jwoehr
 */
public class CmdEval extends Command {

    {
        setNameAndDescription("eval",
                "/2/3 [-to @var] ~@[inc dec max min + - * / % << >> ! & | ^ && || == > < <= >= != pct] ~@operand [~@operand] : arithmetic ");
    }

    /**
     * Eval an expression
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray eval(ArgArray argArray) {

        while (argArray.hasDashCommand() && !argArray.get(0).trim().equals("-")) {
            // don't let minus be confused with a dash command
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (argArray.size() < 2) { // at least operator and one operand
                logArgArrayTooShortError(argArray);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                String opr = argArray.nextMaybeQuotationTuplePopStringTrim();
                Long lopr;
                Long ropr;

                switch (opr) {
                    case "inc":
                        if (argArray.size() < 1) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(++lopr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "dec":
                        if (argArray.size() < 1) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(--lopr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "+":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr + ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "-":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr - ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "*":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr * ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "/":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr / ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "%":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr % ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "!":
                        if (argArray.size() < 1) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            String logical = argArray.nextMaybeTupleString();
                            try {

                                boolean not = !logical.equals("true");
                                put(not);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "&":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr & ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "<<":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr << ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case ">>":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr >> ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "|":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr | ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "^":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr ^ ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "&&":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            boolean llog = argArray.nextMaybeQuotationTuplePopStringTrim().equals("true");
                            boolean rlog = argArray.nextMaybeQuotationTuplePopStringTrim().equals("true");
                            try {
                                put(llog && rlog);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "||":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            boolean llog = argArray.nextMaybeQuotationTuplePopStringTrim().equals("true");
                            boolean rlog = argArray.nextMaybeQuotationTuplePopStringTrim().equals("true");
                            try {
                                put(llog || rlog);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "==":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(Objects.equals(lopr, ropr));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "<":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr < ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case ">":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr > ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "<=":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr <= ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case ">=":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(lopr >= ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "!=":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(!Objects.equals(lopr, ropr));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "pct":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put((lopr * 100) / ropr);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "max":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(java.lang.Math.max(lopr, ropr));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case "min":
                        if (argArray.size() < 2) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            lopr = argArray.nextLongMaybeQuotationTuplePopString();
                            ropr = argArray.nextLongMaybeQuotationTuplePopString();
                            try {
                                put(java.lang.Math.min(lopr, ropr));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    default:
                        getLogger().log(Level.SEVERE, "Unknown operator {0} in {1}", new Object[]{opr, getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return eval(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
