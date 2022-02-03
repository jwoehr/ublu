/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 82, Beulah CO 81023-0082 http://www.softwoehr.com
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
import ublu.util.Generics.StringArrayList;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectAlreadyExistsException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SaveFile;
import com.ibm.as400.access.SaveFileEntry;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to operate on OS400 savefiles.
 *
 * @author jwoehr
 */
public class CmdSaveF extends Command {

    {
        setNameAndDescription("savef",
                "/2? [-as400 ~@as400] [-to datasink] [--,-savef ~@savef] [ -lib ~@libname ] [ -obj ~@objectname [ -obj ~@objname ...]] [ -path ~@pathname [ -path ~@pathname ...]] [-tolib ~@{libname}] [-create | -delete | -exists | -list | -new | -restore | -save ] ~@{libraryname} ~@{savefilename} : instance and perform various savefile operations");
    }

    /**
     * The functions we perform
     */
    protected static enum FUNCTIONS {

        /**
         * Instance savefile object
         */
        NEW, /**
         * Create the savefile
         */
        CREATE,
        /**
         * Delete the savefile
         */
        DELETE,
        /**
         * Test if the savefile exists
         */
        EXISTS,
        /**
         * List objects within savefile
         */
        LIST,
        /**
         * Save objects to a savefile
         */
        SAVE,
        /**
         * Restore objects from a savefile
         */
        RESTORE
    }

    /**
     * Do the work of the savef command
     *
     * @param args arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray savef(ArgArray args) {
        FUNCTIONS function = FUNCTIONS.NEW;
        StringArrayList pathList = new StringArrayList();
        String savedLibName = null;
        StringArrayList objectList = new StringArrayList();
        String toLibName = null;
        SaveFile saveFile = null;
        while (args.hasDashCommand()) {
            String dashCommand = args.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    this.setDataDestfromArgArray(args);
                    break;
                case "--":
                case "-savef":
                    saveFile = args.nextTupleOrPop().value(SaveFile.class);
                    if (saveFile == null) {
                        getLogger().log(Level.SEVERE, "No savefile provided for {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case "-as400":
                    setAs400fromTupleOrPop(args);
                    break;
                case "-new":
                    function = FUNCTIONS.NEW;
                    break;
                case "-create":
                    function = FUNCTIONS.CREATE;
                    break;
                case "-delete":
                    function = FUNCTIONS.DELETE;
                    break;
                case "-exists":
                    function = FUNCTIONS.EXISTS;
                    break;
                case "-lib":
                    savedLibName = args.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-list":
                    function = FUNCTIONS.LIST;
                    break;
                case "-obj":
                    objectList.add(args.nextMaybeQuotationTuplePopStringTrim());
                    break;
                case "-path":
                    pathList.add(args.nextMaybeQuotationTuplePopStringTrim());
                    break;
                case "-restore":
                    function = FUNCTIONS.RESTORE;
                    break;
                case "-save":
                    function = FUNCTIONS.SAVE;
                    break;
                case "-tolib":
                    toLibName = args.nextMaybeQuotationTuplePopStringTrim();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            if (saveFile == null) {
                if (args.size() < 2) {
                    logArgArrayTooShortError(args);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else if (getAs400() == null) {
                    logNoAs400();
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    String libraryname = args.nextMaybeQuotationTuplePopStringTrim();
                    String savefilename = args.nextMaybeQuotationTuplePopStringTrim();
                    saveFile = new SaveFile(getAs400(), libraryname, savefilename);
                }
            }
            if (saveFile != null) {
                switch (function) {
                    case NEW:
                        try {
                            put(saveFile);
                        } catch (AS400SecurityException | ObjectDoesNotExistException | IOException | InterruptedException | ErrorCompletingRequestException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting instance of savefile in " + getNameAndDescription(), ex);
                        }
                        break;
                    case CREATE:
                        try {
                            saveFile.create();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | ObjectAlreadyExistsException ex) {
                            getLogger().log(Level.SEVERE, "Exception encountered creating savefile", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case DELETE:
                        try {
                            saveFile.delete();
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Exception encountered deleting savefile", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | IOException | InterruptedException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Exception encountered deleting savefile", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case EXISTS:
                        try {
                            put(saveFile.exists());
                        } catch (AS400Exception | RequestNotSupportedException | AS400SecurityException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception encountered testing for existence of savefile", ex);
                        } catch (ErrorCompletingRequestException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Exception encountered testing for existence of savefile", ex);
                        }
                        break;
                    case LIST:
                        try {
                            SaveFileEntry[] saveFileEntries = saveFile.listEntries();
                            put(saveFileEntries);
                        } catch (AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | InterruptedException | SQLException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error encountered listing savefile", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case RESTORE:
                        try {
                            if (savedLibName == null) {
                                if (pathList.isEmpty()) {
                                    getLogger().log(Level.SEVERE, "No library selected for restore nor are any pathnames set");
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } else {
                                    try {
                                        throw new UnsupportedOperationException("Savefile restore to pathname not implemented in JT400.");
                                    } catch (UnsupportedOperationException ex) {
                                        getLogger().log(Level.SEVERE, "Error in restore to pathname(s)");
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    }
                                }
                            } else // we have a library name
                            {
                                if (objectList.isEmpty()) // save lib if no obj list
                                {
                                    saveFile.restore(savedLibName);
                                } else { // We have an obj list
                                    if (toLibName == null) {
                                        toLibName = savedLibName;
                                    }
                                    // System.out.println("saveFile.restore(" + savedLibName + ", " + objectList.toStringArray() + "," + toLibName + ")");
                                    saveFile.restore(savedLibName, objectList.toStringArray(), toLibName);
                                }
                            }
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Error encountered saving to savefile", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | InterruptedException | IOException | ObjectDoesNotExistException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Error encountered saving to savefile", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case SAVE:
                        try {
                            if (savedLibName == null) {
                                if (pathList.isEmpty()) {
                                    getLogger().log(Level.SEVERE, "No library selected for save nor are any pathnames set");
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                } else {
                                    saveFile.save(pathList.toStringArray());
                                }
                            } else // we have a library name
                            {
                                if (objectList.isEmpty()) // save lib if no obj list
                                {
                                    saveFile.save(savedLibName);
                                } else { // We have an obj list
                                    saveFile.save(savedLibName, objectList.toStringArray());
                                }
                            }
                        } catch (AS400Exception ex) {
                            getLogger().log(Level.SEVERE, "Error encountered saving to savefile", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (AS400SecurityException | InterruptedException | IOException | ObjectDoesNotExistException | ErrorCompletingRequestException ex) {
                            getLogger().log(Level.SEVERE, "Error encountered saving to savefile", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                }

            }
        }
        return args;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return savef(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
