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
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.Generics.ByteArrayList;
import ublu.util.Generics.StringArrayList;
import ublu.util.Tuple;

/**
 * Command for string operations
 *
 * @author jwoehr
 */
public class CmdString extends Command {

    {
        setNameAndDescription("string",
                "/0 [-to datasink] [--,-string ~@{lopr}] [-uchar ~@{ 0x????  0x???? ...} | -bl ~@{string} | -bls ~@{string} n | -cat ~@{string1} ~@{string2} | -eq ~@{string1} ~@{string2} | -frombytes ~@byte_array | -len ~@{string}  | -new | -nl ~@{string} -repl ~@{string} ~@{target} ~@{replacement} | -repl1 ~@{string} ~@{target} ~@{replacement} | -replregx ~@{string} ~@{regex} ~@{replacement} | -startswith ~@{string} ~@{substr} | -substr ~@{string} ~@intoffset ~@intlen | -tobytes ~@{string} | -toas400 ~@as400 ~@{string} ~@{ccsid} | -toascii ~@as400 ~@bytes ~@{ccsid} |-trim ~@{string}] : string operations");
    }

    enum OPERATIONS {

        UCHAR, BL, BLS, CAT, EQ, FROMBYTES, LEN, NEW, NL, REPL, REPL1, REPLREGX, TOBYTES, TRIM, STARTSWITH, SUBSTR, NOOP, TOASCII, TOAS400
    }

    /**
     * command for String operations
     *
     * @param argArray passed-in arg array
     * @return rest of arg array
     */
    public ArgArray cmdString(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.NOOP;
        String lopr = null;
        String ropr = "";
        String regex = "";
        String target = "";
        String replacement = "";
        int beginindex = 0;
        int endindex = 0;
        int fillcount = 0;
        int ccsid = -1;
        Tuple fromBytesTuple = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "--":
                case "-string":
                    lopr = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-uchar":
                    operation = OPERATIONS.UCHAR;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    break;
                case "-bl":
                    operation = OPERATIONS.BL;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    break;
                case "-bls":
                    operation = OPERATIONS.BLS;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    fillcount = argArray.nextIntMaybeTupleString();
                    break;
                case "-cat":
                    operation = OPERATIONS.CAT;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    ropr = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-eq":
                    operation = OPERATIONS.EQ;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    ropr = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-frombytes":
                    operation = OPERATIONS.FROMBYTES;
                    fromBytesTuple = argArray.nextTupleOrPop();
                    break;
                case "-len":
                    operation = OPERATIONS.LEN;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    break;
                case "-new":
                    operation = OPERATIONS.NEW;
                    break;
                case "-nl":
                    operation = OPERATIONS.NL;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    break;
                case "-repl":
                    operation = OPERATIONS.REPL;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    target = argArray.nextMaybeQuotationTuplePopString();
                    replacement = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-repl1":
                    operation = OPERATIONS.REPL1;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    target = argArray.nextMaybeQuotationTuplePopString();
                    replacement = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-replregx":
                    operation = OPERATIONS.REPLREGX;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    regex = argArray.nextMaybeQuotationTuplePopString();
                    replacement = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-startswith":
                    operation = OPERATIONS.STARTSWITH;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    ropr = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-substr":
                    operation = OPERATIONS.SUBSTR;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    beginindex = argArray.nextInt();
                    endindex = argArray.nextInt();
                    break;
                case "-tobytes":
                    operation = OPERATIONS.TOBYTES;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    break;
                case "-trim":
                    operation = OPERATIONS.TRIM;
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    break;
                case "-toascii":
                    operation = OPERATIONS.TOASCII;
                    setAs400FromTuple(argArray.nextTupleOrPop());
                    fromBytesTuple = argArray.nextTupleOrPop();
                    ccsid = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-toas400":
                    operation = OPERATIONS.TOAS400;
                    setAs400FromTuple(argArray.nextTupleOrPop());
                    lopr = lopr == null ? argArray.nextMaybeQuotationTuplePopString() : lopr;
                    ccsid = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Object opresult = null;
            StringBuilder sb;
            switch (operation) {
                case UCHAR:
                    StringArrayList sal = new StringArrayList(lopr);
                    sb = new StringBuilder();
                    for (String s : sal) {
                        sb.append(Character.toString((char) Integer.decode(s).intValue()));
                    }
                    opresult = sb.toString();
                    break;
                case BL:
                    opresult = lopr + ' ';
                    break;
                case BLS:
                    fillcount = Math.max(0, fillcount);
                    sb = new StringBuilder(lopr);
                    for (int i = 0; i < fillcount; i++) {
                        sb.append(' ');
                    }
                    opresult = sb.toString();
                    break;
                case CAT:
                    opresult = lopr + ropr;
                    break;
                case EQ:
                    opresult = lopr.equals(ropr);
                    break;
                case FROMBYTES:
                    if (fromBytesTuple != null) {
                        Object o = fromBytesTuple.getValue();
                        if (o instanceof ByteArrayList) {
                            opresult = new String(ByteArrayList.class.cast(o).byteArray());
                        } else if (o instanceof byte[]) {
                            opresult = new String((byte[]) o);
                        }
                    }
                    break;
                case LEN:
                    opresult = lopr.length();
                    break;
                case NEW:
                    opresult = "";
                    break;
                case NL:
                    opresult = lopr + '\n';
                    break;
                case STARTSWITH:
                    opresult = lopr.startsWith(ropr);
                    break;
                case SUBSTR:
                    opresult = lopr.substring(beginindex, endindex);
                    break;
                case REPL:
                    opresult = lopr.replace(target, replacement);
                    break;
                case REPL1:
                    opresult = lopr.replaceFirst(target, replacement);
                    break;
                case REPLREGX:
                    opresult = lopr.replaceAll(regex, replacement);
                    break;
                case TOBYTES:
                    opresult = lopr.getBytes();
                    break;
                case TRIM:
                    opresult = lopr.trim();
                    break;
                case TOASCII:
                    if (ccsid == -1) {
                        ccsid = getAs400().getCcsid();
                    }
                    byte[] b = null;
                    if (fromBytesTuple != null) {
                        Object o = fromBytesTuple.getValue();
                        if (o instanceof ByteArrayList) {
                            b = ByteArrayList.class.cast(o).byteArray();
                        } else if (o instanceof byte[]) {
                            b = (byte[]) o;
                        }
                        if (b != null) {
                            opresult = new AS400Text(b.length, ccsid, getAs400()).toObject(b).toString();
                        }
                    }
                    break;
                case TOAS400:
                    if (ccsid == -1) {
                        ccsid = getAs400().getCcsid();
                    }
                    opresult = new AS400Text(lopr.length(), ccsid, getAs400()).toBytes(lopr);
                    break;
            }
            try {
                put(opresult);
            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                getLogger().log(Level.SEVERE, "Error putting result in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }

        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdString(args);
    }

    @Override
    public CommandInterface.COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
