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
import ublu.util.Generics.FunctorMap;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import ublu.util.Generics.ConstMap;

/**
 * Command to save and restore function dictionary
 *
 * @author jwoehr
 */
public class CmdSaveSys extends Command {

    {
        setNameAndDescription("savesys",
                "/0 [-to datasink] [-from datasink] [-merge] [-save | -restore] : save and restore compiled code");
    }

    enum OPERATIONS {

        SAVE, /* PEEK, */ RESTORE, NOOP
    }

    /**
     * Save/restore compiled code
     *
     * @param argArray
     * @return remnant of argArray
     */
    public ArgArray saveSys(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.NOOP; // the default
        boolean isMerging = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    break;
                case "-merge":
                    operation = OPERATIONS.RESTORE;
                    isMerging = true;
                    break;
//                case "-peek":
//                    operation = OPERATIONS.PEEK;
//                    break;
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
            switch (operation) {
//                case PEEK:
//                    switch (getDataSrc().getType()) {
//                        case STD:
//                            // getLogger().log(Level.SEVERE, "Cannot restore dictionary from standard input " + getNameAndDescription(), ex);
//                            setCommandResult(COMMANDRESULT.FAILURE);
//                            break;
//                        case TUPLE:
//                            Tuple t = getTuple(getDataSrc().getName());
//                            if (t == null) {
//                                getLogger().log(Level.SEVERE, "Tuple does not exist {0}", getNameAndDescription());
//                                setCommandResult(COMMANDRESULT.FAILURE);
//                            } else {
//                                Object o = t.getValue();
//                                if (o instanceof byte[]) {
//                                    try {
//                                        fm = FunctorMap.fromByteArray(byte[].class.cast(o));
//                                        if (fm != null) {
//                                            try {
//                                                put(fm.listFunctions());
//                                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
//                                                getLogger().log(Level.SEVERE, "Error putting peeked dictionary from tuple in " + getNameAndDescription(), ex);
//                                                setCommandResult(COMMANDRESULT.FAILURE);
//                                            }
//                                        } else {
//                                            getLogger().log(Level.SEVERE, "Tuple does not reference a saved dictionary in {0}", getNameAndDescription());
//                                            setCommandResult(COMMANDRESULT.FAILURE);
//                                        }
//                                    } catch (IOException | ClassNotFoundException ex) {
//                                        getLogger().log(Level.SEVERE, "Error opening object input stream or reading object in " + getNameAndDescription(), ex);
//                                        setCommandResult(COMMANDRESULT.FAILURE);
//                                    }
//                                } else {
//                                    getLogger().log(Level.SEVERE, "Tuple does not reference a saved dictionary in {0}", getNameAndDescription());
//                                    setCommandResult(COMMANDRESULT.FAILURE);
//                                }
//                            }
//                            break;
//                        case FILE:
//                            try {
//                                File f = new File(getDataSrc().getName());
//                                if (!f.exists()) {
//                                    getLogger().log(Level.SEVERE, "File does not exist in {0}", getNameAndDescription());
//                                    setCommandResult(COMMANDRESULT.FAILURE);
//                                } else {
//                                    fm = FunctorMap.fromFile(f);
//                                    if (fm != null) {
//                                        try {
//                                            put(fm.listFunctions());
//                                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
//                                            getLogger().log(Level.SEVERE, "Error putting peeked dictionary from file in " + getNameAndDescription(), ex);
//                                            setCommandResult(COMMANDRESULT.FAILURE);
//                                        }
//                                    } else {
//                                        getLogger().log(Level.SEVERE, "File does not contain a saved dictionary in {0}", getNameAndDescription());
//                                        setCommandResult(COMMANDRESULT.FAILURE);
//                                    }
//                                }
//                            } catch (IOException | ClassNotFoundException ex) {
//                                Logger.getLogger(CmdSaveSys.class.getName()).log(Level.SEVERE, "Couldn't reload class in " + getNameAndDescription(), ex);
//                            }
//                            break;
//                    }
//                    break;
                case SAVE:
                    switch (getDataDest().getType()) {
                        case FILE:
                            try (FileOutputStream fo = new FileOutputStream(getDataDest().getName())) {
                                writeSys(getInterpreter().getFunctorMap(), getInterpreter().getConstMap(), fo);
                            } catch (IOException ex) {
                                getLogger().log(Level.SEVERE, "Error saving system in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            break;
                        default:
                            getLogger().log(Level.SEVERE, "Unimplemented datasink type {0} for save in {1}", new Object[]{getDataDest().getType(), getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case RESTORE:
                    switch (getDataSrc().getType()) {
                        case FILE:
                            try (FileInputStream fis = new FileInputStream(getDataSrc().getName())) {
                                readSys(fis, isMerging);
                            } catch (IOException | ClassNotFoundException ex) {
                                Logger.getLogger(CmdSaveSys.class.getName()).log(Level.SEVERE, "Couldn't reload class in " + getNameAndDescription(), ex);
                            }
                            break;
                        default:
                            getLogger().log(Level.SEVERE, "Unimplemented datasink type {0} for restore in {1}", new Object[]{getDataDest().getType(), getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                    }
            }
        }
        return argArray;
    }

    private void writeSys(FunctorMap fm, ConstMap cm, OutputStream os) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(fm);
            oos.writeObject(cm);
        }
    }

    private void readSys(InputStream is, boolean merging) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            FunctorMap fm = (FunctorMap) ois.readObject();
            ConstMap cm = (ConstMap) ois.readObject();
            FunctorMap oldfm = getInterpreter().getFunctorMap();
            ConstMap oldcm = getInterpreter().getConstMap();
            if (merging) {
                oldfm.putAll(fm);
                oldcm.putAll(cm);
            } else {
                getInterpreter().setFunctorMap(fm);
                getInterpreter().setConstMap(cm);
            }
        }
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return saveSys(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
