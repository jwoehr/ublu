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
import ublu.util.Generics.ByteArrayList;
import ublu.util.Generics.FunctorMap;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command to save and restore function dictionary
 *
 * @author jwoehr
 */
public class CmdDict extends Command {

    {
        setNameAndDescription("dict",
                "/0 [-to datasink] [-from datasink] [-list | -save | -restore | -merge | -peek] : save and restore function dictionary");
    }

    enum OPERATIONS {

        LIST, SAVE, PEEK, RESTORE, NOOP
    }

    /**
     *
     * @param argArray
     * @return
     */
    public ArgArray dict(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.LIST; // the default
        boolean isMerging = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "-from":
                    setDataSrc(DataSink.fromSinkName(argArray.next()));
                    break;
                case "-list":
                    operation = OPERATIONS.LIST;
                    break;
                case "-merge":
                    operation = OPERATIONS.RESTORE;
                    isMerging = true;
                    break;
                case "-peek":
                    operation = OPERATIONS.PEEK;
                    break;
                case "-save":
                    operation = OPERATIONS.SAVE;
                    break;
                case "-restore":
                    operation = OPERATIONS.RESTORE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            FunctorMap fm;
            switch (operation) {
                case LIST:
                    try {
                        put(getInterpreter().listFunctions());
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Exception listing functions in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case NOOP:
                    break;
                case PEEK:
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
                                        fm = functorMapFromByteArray(byte[].class.cast(o));
                                        if (fm != null) {
                                            try {
                                                put(fm.listFunctions());
                                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                                getLogger().log(Level.SEVERE, "Error putting peeked dictionary from tuple in " + getNameAndDescription(), ex);
                                                setCommandResult(COMMANDRESULT.FAILURE);
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
                                    getLogger().log(Level.SEVERE, "File does not exist {0}", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } else {
                                    fm = functorMapFromFile(f);
                                    if (fm != null) {
                                        try {
                                            put(fm.listFunctions());
                                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                            getLogger().log(Level.SEVERE, "Error putting peeked dictionary from file in " + getNameAndDescription(), ex);
                                            setCommandResult(COMMANDRESULT.FAILURE);
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
                    break;
                case SAVE:
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        fm = getInterpreter().getFunctorMap();
                        oos.writeObject(fm);
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
                                        fm = functorMapFromByteArray(byte[].class.cast(o));
                                        if (fm != null) {
                                            if (isMerging) {
                                                getInterpreter().getFunctorMap().putAll(fm);
                                            } else {
                                                getInterpreter().setFunctorMap(fm);
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
                                    getLogger().log(Level.SEVERE, "File does not exist {0}", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } else {
                                    fm = functorMapFromFile(f);
                                    if (fm != null) {
                                        if (isMerging) {
                                            getInterpreter().getFunctorMap().putAll(fm);
                                        } else {
                                            getInterpreter().setFunctorMap(fm);
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

    private FunctorMap functorMapFromFile(File f) throws FileNotFoundException, IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ByteArrayList bal = new ByteArrayList();
        byte[] buff = new byte[1024];
        int numread;
        while (bis.available() > 0) {
            numread = bis.read(buff);
            for (int i = 0; i < numread; i++) {
                bal.add(new Byte(buff[i]));
            }
        }
        return functorMapFromByteArray(bal.byteArray());
    }

    private FunctorMap functorMapFromByteArray(byte[] byteArray) throws IOException, ClassNotFoundException {
        FunctorMap fm = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object restoredObject = ois.readObject();
        if (restoredObject instanceof FunctorMap) {
            fm = FunctorMap.class.cast(restoredObject);
        }
        return fm;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return dict(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}