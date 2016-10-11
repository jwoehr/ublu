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

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ublu.util.ArgArray;
import ublu.util.Const;
import ublu.util.DataSink;
import ublu.util.Generics;
import ublu.util.Generics.ConstMap;
import ublu.util.Tuple;

/**
 * Command to create a named constant with a string value.
 *
 * @author jwoehr
 */
public class CmdConst extends Command {

    {
        setNameAndDescription("const",
                "/2? [-to datasink] [-list | -create | -save | -restore | -merge ] *name ~@{value} : create a constant value");
    }

    enum OPS {
        CREATE, LIST, SAVE, RESTORE
    }

    /**
     * Create a named constant with a string value.
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray doConst(ArgArray argArray) {
        OPS op = OPS.CREATE; // default
        boolean isMerging = false;
        while (getCommandResult() != COMMANDRESULT.FAILURE && argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "-from":
                    setDataSrc(DataSink.fromSinkName(argArray.next()));
                    break;
                case "-list":
                    op = OPS.LIST;
                    break;
                case "-create":
                    op = OPS.CREATE;
                    break;
                case "-save":
                    op = OPS.SAVE;
                    break;
                case "-restore":
                    op = OPS.RESTORE;
                    break;
                case "-merge":
                    op = OPS.RESTORE;
                    isMerging = true;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            ConstMap cm;
            switch (op) {
                case CREATE:
                    if (argArray.size() < 2) {
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    String name = argArray.next();
                    String value = argArray.nextMaybeQuotationTuplePopString();
                    if (Const.isConstName(name)) {
                        if (getInterpreter().getConst(name) != null) {
                            getLogger().log(Level.SEVERE, "\"{0}\" already exists as a const with value \"{1}\" in {2}", new Object[]{name, getInterpreter().getConst(name), getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else if (!getInterpreter().setConst(name, value)) {
                            getLogger().log(Level.SEVERE, "Attempt to set a const with null value in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "{0}" + " is not a const name starting with \"" + Const.CONSTNAMECHAR + "\" in {1}", new Object[]{name, getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case LIST: {
                    try {
                        put(getInterpreter().getConstMap().listConsts());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception putting const list in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
                break;
                case SAVE:
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        cm = getInterpreter().getConstMap();
                        oos.writeObject(cm);
                        switch (getDataDest().getType()) {
                            case STD:
                                put(baos.toByteArray());
                                break;
                            case TUPLE:
                                put(baos.toByteArray());
                                break;
                            case FILE:
                                FileOutputStream fo = new FileOutputStream(getDataDest().getName());
                                fo.write(baos.toByteArray());
                                break;
                        }
                    } catch (IOException | RequestNotSupportedException | ObjectDoesNotExistException | SQLException | InterruptedException | AS400SecurityException | ErrorCompletingRequestException ex) {
                        getLogger().log(Level.SEVERE, "Error opening object output stream or writing object in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case RESTORE:
                    switch (getDataSrc().getType()) {
                        case STD:
                            // getLogger().log(Level.SEVERE, "Cannot restore dictionary from standard input " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                            break;
                        case TUPLE:
                            Tuple t = getTuple(getDataSrc().getName());
                            if (t == null) {
                                getLogger().log(Level.SEVERE, "Tuple does not exist {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else {
                                Object o = t.getValue();
                                if (o instanceof byte[]) {
                                    try {
                                        cm = Generics.ConstMap.fromByteArray(byte[].class.cast(o));
                                        if (cm != null) {
                                            if (isMerging) {
                                                getInterpreter().getConstMap().putAll(cm);
                                            } else {
                                                getInterpreter().setConstMap(cm);
                                            }
                                        } else {
                                            getLogger().log(Level.SEVERE, "Tuple does not reference a saved dictionary in {0}", getNameAndDescription());
                                            setCommandResult(COMMANDRESULT.FAILURE);
                                        }
                                    } catch (IOException | ClassNotFoundException ex) {
                                        getLogger().log(Level.SEVERE, "Error opening object input stream or reading object in " + getNameAndDescription(), ex);
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    }
                                } else {
                                    getLogger().log(Level.SEVERE, "Tuple does not reference a saved dictionary in {0}", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            }
                            break;
                        case FILE:
                            try {
                                File f = new File(getDataSrc().getName());
                                if (!f.exists()) {
                                    getLogger().log(Level.SEVERE, "File does not exist in {0}", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } else {
                                    cm = Generics.ConstMap.fromFile(f);
                                    if (cm != null) {
                                        if (isMerging) {
                                            getInterpreter().getConstMap().putAll(cm);
                                        } else {
                                            getInterpreter().setConstMap(cm);
                                        }
                                    } else {
                                        getLogger().log(Level.SEVERE, "File does not contain a saved dictionary in {0}", getNameAndDescription());
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    }
                                }
                            } catch (IOException | ClassNotFoundException ex) {
                                Logger.getLogger(CmdDict.class.getName()).log(Level.SEVERE, "Couldn't reload class in " + getNameAndDescription(), ex);
                            }
                            break;
                    }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return doConst(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
