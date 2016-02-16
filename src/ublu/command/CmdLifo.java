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
import ublu.util.Generics.TupleStack;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command for tuple stack operations
 *
 * @author jwoehr
 */
public class CmdLifo extends Command {

    {
        setNameAndDescription("lifo", "/0 [-to datasink] -push @tuplevar | -pop | -popval  | -dup | -swap | -over | -pick 0index | -rot | -depth | -clear | -drop | -show : operate on the tuple stack");
    }

    enum OPERATIONS {

        PUSH, POP, POPVAL, DUP, SWAP, OVER, PICK, ROT, DEPTH, CLEAR, DROP, SHOW
    }

    /**
     * Perform operations on tuple stack
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray lifo(ArgArray argArray) {
        OPERATIONS operation = null;
        String operandName = "";
        Tuple operand = null;
        int picknum = 0;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(newDataSink(argArray));
                    break;
                case "-push":
                    operation = OPERATIONS.PUSH;
                    operandName = argArray.peekNext();
                    operand = argArray.nextTuple();
                    break;
                case "-pop":
                    operation = OPERATIONS.POP;
                    break;
                case "-popval":
                    operation = OPERATIONS.POPVAL;
                    break;
                case "-dup":
                    operation = OPERATIONS.DUP;
                    break;
                case "-swap":
                    operation = OPERATIONS.SWAP;
                    break;
                case "-over":
                    operation = OPERATIONS.OVER;
                    break;
                case "-pick":
                    operation = OPERATIONS.PICK;
                    picknum = argArray.nextInt();
                    break;
                case "-rot":
                    operation = OPERATIONS.ROT;
                    break;
                case "-depth":
                    operation = OPERATIONS.DEPTH;
                    break;
                case "-clear":
                    operation = OPERATIONS.CLEAR;
                    break;
                case "-drop":
                    operation = OPERATIONS.DROP;
                    break;
                case "-show":
                    operation = OPERATIONS.SHOW;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            try {
                TupleStack ts = getTupleStack();
                Tuple a;
                Tuple b;
                switch (operation) {
                    case PUSH:
                        if (operand == null) {
                            getLogger().log(Level.SEVERE, " Tuple {0} does not exist for push in {1}", new Object[]{operandName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            ts.push(operand);
                        }
                        break;
                    case POP:
                        put(ts.pop());
                        break;
                    case POPVAL:
                        put(ts.pop().getValue());
                        break;
                    case DUP:
                        ts.push(ts.peek());
                        break;
                    case SWAP:
                        a = ts.pop();
                        b = ts.pop();
                        ts.push(a);
                        ts.push(b);
                        break;
                    case OVER:
                        a = ts.elementAt(ts.size() - 3);
                        ts.push(a);
                        break;
                    case PICK:
                        a = ts.elementAt(ts.size() - (picknum + 1));
                        ts.push(a);
                        break;
                    case ROT:
                        a = ts.remove(ts.size() - 3);
                        ts.push(a);
                        break;
                    case DEPTH:
                        put(ts.size());
                        break;
                    case CLEAR:
                        ts.clear();
                        break;
                    case DROP:
                        ts.remove(ts.size() - 1);
                        break;
                    case SHOW:
                        StringBuilder sb = new StringBuilder();
                        if (ts.empty()) {
                            sb.append("(empty)");
                        } else {
                            for (Tuple t : ts) {
                                sb.insert(0, " ");
                                sb.insert(0, t.getKey());
                            }
                            sb.insert(0, "top <== ");
                        }
                        put(sb.toString().trim());
                        break;
                }
            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                getLogger().log(Level.SEVERE, "Error putting popped tuple in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return lifo(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
