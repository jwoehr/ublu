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
import ublu.db.Db;
import ublu.util.DataSink;
import ublu.util.Putter;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to put an object to a {@link DataSink}.
 *
 * @author jwoehr
 */
public class CmdPut extends Command {

    {
        setNameAndDescription("put", "/1? [-to datasink] [-tofile ~@filepath] [-from datasink ] [-fromfile ~@filepath] [-append] [ -toascii ] [ -charset srccharsetname ] [-n] [-s] [ -# number | ~@{object or a string}  | a single lex ] : put data from datasink to datasink, optionally translating charset if -toascii or -charset are set");
    }

    /**
     * Arity-0 ctor
     */
    public CmdPut() {
    }
    private String charsetName;

    /**
     * Get name of charset for data before it is put
     *
     * @return charset name of data before putting
     */
    protected String getCharsetName() {
        return charsetName;
    }

    /**
     * Set name of charset for data before it is put
     *
     * @param charsetName charset name of data before putting
     */
    protected final void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    /**
     * Put an object to a {@link DataSink}
     *
     * @param argArray args to the interpreter
     * @return what's left of the args
     */
    public ArgArray performPut(ArgArray argArray) {
        boolean append = false;
        boolean newline = true;
        boolean space = false;
        Integer number = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-tofile":
                    setDataDest(DataSink.fileSinkFromTuple(argArray.nextTupleOrPop()));
                    break;
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    break;
                case "-fromfile":
                    setDataSrc(DataSink.fileSinkFromTuple(argArray.nextTupleOrPop()));
                    break;
                case "-#":
                    number = argArray.nextInt();
                    break;
                case "-append":
                    append = true;
                    break;
                case "-toascii":
                    setCharsetName(Db.cpEBCDIC);
                    break;
                case "-charset":
                    setCharsetName(argArray.next());
                    break;
                case "-n":
                    newline = false;
                    break;
                case "-s":
                    space = true;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (number != null) {
            try {
                put(number);
            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                getLogger().log(Level.SEVERE, "Could not put number " + number + " in " + getNameAndDescription(), ex);
            }
        } else {
            switch (getDataSrc().getType()) {
                case FILE:
                    try {
                        StringBuilder sb;
                        try (FileInputStream fis = new FileInputStream(new File(getDataSrc().getName()))) {
                            sb = new StringBuilder();
                            byte[] buf = new byte[32767];
                            while (fis.available() > 0) {
                                int numread = fis.read(buf);
                                sb.append(new String(buf, 0, numread));
                            }
                        }
                        put(sb.toString(), append, space, newline);
                    } catch (FileNotFoundException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (IOException | SQLException ex) {
                        getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case TUPLE:
                    Tuple tuple = getInterpreter().getTuple(getDataSrc().getName());
                    if (tuple != null) {
                        if (getDataDest().getType().equals(DataSink.SINKTYPE.LIFO)) {
                            try {
                                new Putter(tuple, getInterpreter(), getCharsetName()).put(getDataDest(), append, space, newline);
                            } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            try {
                                new Putter(tuple.getValue(), getInterpreter(), getCharsetName()).put(getDataDest(), append, space, newline);
                            } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        try {
                            new Putter(null, getInterpreter(), getCharsetName()).put(getDataDest(), append, space, newline);
                        } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case STD:
                    try {
                        new Putter(argArray.nextMaybeQuotationTuplePopString(), getInterpreter(), getCharsetName()).put(getDataDest(), append, space, newline);
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case LIFO:
                    try {
                        new Putter(getTupleStack().pop().getValue(), getInterpreter(), getCharsetName()).put(getDataDest(), append, space, newline);
                    } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                default:
                    getLogger().log(Level.SEVERE, "Unsuppoted data source in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);

            }
        }
        return argArray;
    }

    @Override
    public void reinit() {
        super.reinit();
        setCharsetName("ASCII");
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return performPut(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
