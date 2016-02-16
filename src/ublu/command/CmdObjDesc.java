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
import ublu.util.Generics;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ObjectDescription;
import com.ibm.as400.access.ObjectLockListEntry;
import com.ibm.as400.access.QSYSObjectPathName;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Manipulates an OS400 Object Description
 *
 * @author jwoehr
 */
public class CmdObjDesc extends Command {

    {
        setNameAndDescription("objdesc", "/0 [-as400 @as400] [-to datasink] [--,-objdesc ~@objdesc] [-path ~@{ifspath}] [-instance] | [-refresh}] | [-query exists | library | name | path | type] | [-valuestring ~@{attribute}] | -refresh | -locks] : examine an object description");

    }

    /**
     * the operations we know
     */
    protected enum OPS {

        /**
         * Create the Object Description
         */
        /**
         * Create the Object Description
         */
        INSTANCE,
        /**
         * Query various aspects
         */
        QUERY,
        /**
         * Get attribute value
         */
        VALUE_STRING,
        /**
         * Get the lock list
         */
        LOCKLIST,
        /**
         * Refresh all info
         */
        REFRESH

    }

    /**
     * Arity-0 ctor
     */
    public CmdObjDesc() {
    }

    /**
     * retrieve a (filtered) list of OS400 Objects on the system
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray objDesc(ArgArray argArray) {
        OPS op = OPS.INSTANCE;
        String ifspath = null;
        String queryString = null;
        String attributeName = null;
        Tuple objDescTuple = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    // /* Debug */ getLogger().log(Level.INFO, "my AS400 == {0}", getAs400());
                    break;
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "--":
                case "-objdesc":
                    objDescTuple = argArray.nextTupleOrPop();
                    break;
                case "-path":
                    ifspath = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-query":
                    op = OPS.QUERY;
                    queryString = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-valuestring":
                    op = OPS.VALUE_STRING;
                    attributeName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-locks":
                    op = OPS.LOCKLIST;
                    break;
                case "-instance":
                    op = OPS.INSTANCE;
                    break;
                case "-refresh":
                    op = OPS.REFRESH;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            ObjectDescription objDesc = null;
            switch (op) {
                case INSTANCE:
                    if (getAs400() == null) {
                        getLogger().log(Level.WARNING, "No as400 instance provided in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        if (ifspath != null) {
                            objDesc = new ObjectDescription(getAs400(), new QSYSObjectPathName(ifspath));
                            try {
                                put(objDesc);
                            } catch (AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.INFO, "Exception putting ObjectDescription in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } catch (SQLException ex) {
                                getLogger().log(Level.SEVERE, "SQL Exception putting ObjectDescription in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.WARNING, "No ifspath provided to instance object description in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case LOCKLIST:
                    objDesc = getObjDescfromTuple(objDescTuple);
                    if (objDesc == null) {
                        getLogger().log(Level.WARNING, "No objdesc instance provided in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            ObjectLockListEntry[] objectLockListEntries = objDesc.getObjectLockList();
                            put(new Generics.ObjectLockListEntryArrayList(objectLockListEntries));
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Exception getting or putting lock list from ObjectDescription in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case QUERY:
                    objDesc = getObjDescfromTuple(objDescTuple);
                    if (objDesc == null) {
                        getLogger().log(Level.WARNING, "No objdesc instance provided in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            Object o = doQuery(objDesc, queryString);
                            put(o);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Exception getting or putting query in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case REFRESH:
                    objDesc = getObjDescfromTuple(objDescTuple);
                    if (objDesc != null) {
                        try {
                            objDesc.refresh();
                            put(objDesc);
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting ObjectDescription in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.WARNING, "Could not instance ObjectDescription provided to -refresh in {0}", getNameAndDescription());
                    }
                    break;
                case VALUE_STRING:
                    objDesc = getObjDescfromTuple(objDescTuple);
                    if (objDesc == null) {
                        getLogger().log(Level.WARNING, "No objdesc instance provided in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        int attributeInt = 0;
                        boolean converted = true;
                        try {
                            try {
                                attributeInt = Integer.parseInt(attributeName);
                            } catch (NumberFormatException ex) {
                                converted = false;
                            }
                            String stringValueOfAttribute = null;
                            if (converted) {
                                stringValueOfAttribute = objDesc.getValueAsString(attributeInt);
                            } else {
                                attributeInt = getAttributeInt(attributeName);
                                if (getCommandResult() != COMMANDRESULT.FAILURE) {
                                    stringValueOfAttribute = objDesc.getValueAsString(attributeInt);
                                }
                            }
                            if (getCommandResult() != COMMANDRESULT.FAILURE) {
                                put(stringValueOfAttribute);
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Exception getting or putting query in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                default:
                    getLogger().log(Level.WARNING, "Unknown operation in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private ObjectDescription getObjDescfromTuple(Tuple objDescTuple) {
        ObjectDescription objDesc = null;
        if (objDescTuple != null) {
            Object tupleValue = objDescTuple.getValue();
            if (tupleValue instanceof ObjectDescription) {
                objDesc = ObjectDescription.class.cast(tupleValue);
            } else {
                getLogger().log(Level.WARNING, "Valued tuple which is not an ObjectDescription tuple provided in {0}", getNameAndDescription());
            }
        }
        return objDesc;
    }

    private int getAttributeInt(String attributeName) {
        int attributeInt = 0;
        switch (attributeName.toUpperCase().trim()) {
            case "ALLOW_CHANGE_BY_PROGRAM":
                attributeInt = ObjectDescription.ALLOW_CHANGE_BY_PROGRAM;
                break;
            case "AUDITING ":
                attributeInt = ObjectDescription.AUDITING;
                break;
            case "CHANGE_DATE":
                attributeInt = ObjectDescription.CHANGE_DATE;
                break;
            case "CHANGED_BY_PROGRAM":
                attributeInt = ObjectDescription.CHANGED_BY_PROGRAM;
                break;
            case "COMPILER":
                attributeInt = ObjectDescription.COMPILER;
                break;
            case "COMPRESSION":
                attributeInt = ObjectDescription.COMPRESSION;
                break;
            case "CREATION_DATE":
                attributeInt = ObjectDescription.CREATION_DATE;
                break;
            case "CREATOR_SYSTEM":
                attributeInt = ObjectDescription.CREATOR_SYSTEM;
                break;
            case "CREATOR_USER_PROFILE":
                attributeInt = ObjectDescription.CREATOR_USER_PROFILE;
                break;
            case "DAYS_USED":
                attributeInt = ObjectDescription.DAYS_USED;
                break;
            case "DIGITALLY_SIGNED":
                attributeInt = ObjectDescription.DIGITALLY_SIGNED;
                break;
            case "DIGITALLY_SIGNED_MULTIPLE":
                attributeInt = ObjectDescription.DIGITALLY_SIGNED_MULTIPLE;
                break;
            case "DIGITALLY_SIGNED_TRUSTED":
                attributeInt = ObjectDescription.DIGITALLY_SIGNED_TRUSTED;
                break;
            case "DOMAIN":
                attributeInt = ObjectDescription.DOMAIN;
                break;
            case "EXTENDED_ATTRIBUTE":
                attributeInt = ObjectDescription.EXTENDED_ATTRIBUTE;
                break;
            case "JOURNAL":
                attributeInt = ObjectDescription.JOURNAL;
                break;
            case "JOURNAL_IMAGES":
                attributeInt = ObjectDescription.JOURNAL_IMAGES;
                break;
            case "JOURNAL_OMITTED_ENTRIES":
                attributeInt = ObjectDescription.JOURNAL_OMITTED_ENTRIES;
                break;
            case "JOURNAL_START_DATE":
                attributeInt = ObjectDescription.JOURNAL_START_DATE;
                break;
            case "JOURNAL_STATUS":
                attributeInt = ObjectDescription.JOURNAL_STATUS;
                break;
            case "LAST_USED_DATE":
                attributeInt = ObjectDescription.LAST_USED_DATE;
                break;
            case "LIBRARY":
                attributeInt = ObjectDescription.LIBRARY;
                break;
            case "LIBRARY_ASP_DEVICE_NAME":
                attributeInt = ObjectDescription.LIBRARY_ASP_DEVICE_NAME;
                break;
            case "LIBRARY_ASP_NUMBER":
                attributeInt = ObjectDescription.LIBRARY_ASP_NUMBER;
                break;
            case "LICENSED_PROGRAM":
                attributeInt = ObjectDescription.LICENSED_PROGRAM;
                break;
            case "NAME":
                attributeInt = ObjectDescription.NAME;
                break;
            case "OBJECT_ASP_DEVICE_NAME":
                attributeInt = ObjectDescription.OBJECT_ASP_DEVICE_NAME;
                break;
            case "OBJECT_ASP_NUMBER":
                attributeInt = ObjectDescription.OBJECT_ASP_NUMBER;
                break;
            case "OBJECT_LEVEL":
                attributeInt = ObjectDescription.OBJECT_LEVEL;
                break;
            case "OBJECT_SIZE":
                attributeInt = ObjectDescription.OBJECT_SIZE;
                break;
            case "ORDER_IN_LIBRARY_LIST":
                attributeInt = ObjectDescription.ORDER_IN_LIBRARY_LIST;
                break;
            case "OVERFLOWED_ASP":
                attributeInt = ObjectDescription.OVERFLOWED_ASP;
                break;
            case "OWNER":
                attributeInt = ObjectDescription.OWNER;
                break;
            case "PRIMARY_GROUP":
                attributeInt = ObjectDescription.PRIMARY_GROUP;
                break;
            case "PTF":
                attributeInt = ObjectDescription.PTF;
                break;
            case "RESET_DATE":
                attributeInt = ObjectDescription.RESET_DATE;
                break;
            case "RESTORE_DATE":
                attributeInt = ObjectDescription.RESTORE_DATE;
                break;
            case "SAVE_ACTIVE_DATE":
                attributeInt = ObjectDescription.SAVE_ACTIVE_DATE;
                break;
            case "SAVE_COMMAND":
                attributeInt = ObjectDescription.SAVE_COMMAND;
                break;
            case "SAVE_DATE":
                attributeInt = ObjectDescription.SAVE_DATE;
                break;
            case "SAVE_DEVICE":
                attributeInt = ObjectDescription.SAVE_DEVICE;
                break;
            case "SAVE_FILE":
                attributeInt = ObjectDescription.SAVE_FILE;
                break;
            case "SAVE_LABEL":
                attributeInt = ObjectDescription.SAVE_LABEL;
                break;
            case "SAVE_SEQUENCE_NUMBER":
                attributeInt = ObjectDescription.SAVE_SEQUENCE_NUMBER;
                break;
            case "SAVE_SIZE":
                attributeInt = ObjectDescription.SAVE_SIZE;
                break;
            case "SAVE_VOLUME_ID":
                attributeInt = ObjectDescription.SAVE_VOLUME_ID;
                break;
            case "SOURCE_FILE":
                attributeInt = ObjectDescription.SOURCE_FILE;
                break;
            case "SOURCE_FILE_UPDATED_DATE":
                attributeInt = ObjectDescription.SOURCE_FILE_UPDATED_DATE;
                break;
            case "STORAGE_STATUS":
                attributeInt = ObjectDescription.STORAGE_STATUS;
                break;
            case "SYSTEM_LEVEL":
                attributeInt = ObjectDescription.SYSTEM_LEVEL;
                break;
            case "TEXT_DESCRIPTION":
                attributeInt = ObjectDescription.TEXT_DESCRIPTION;
                break;
            case "TYPE":
                attributeInt = ObjectDescription.TYPE;
                break;
            case "USAGE_INFO_UPDATED":
                attributeInt = ObjectDescription.USAGE_INFO_UPDATED;
                break;
            case "USER_CHANGED":
                attributeInt = ObjectDescription.USER_CHANGED;
                break;
            case "USER_DEFINED_ATTRIBUTE":
                attributeInt = ObjectDescription.USER_DEFINED_ATTRIBUTE;
                break;
            default:
                getLogger().log(Level.WARNING, "Unknown attribute provided in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
        }
        return attributeInt;
    }

    private Object doQuery(ObjectDescription description, String queryString) throws AS400SecurityException, ErrorCompletingRequestException, AS400Exception, InterruptedException, IOException, ObjectDoesNotExistException {
        Object o;
        switch (queryString.toLowerCase().trim()) {
            case "exists":
                o = description.exists();
                break;
            case "library":
                o = description.getLibrary();
                break;
            case "name":
                o = description.getName();
                break;
            case "path":
                o = description.getPath();
                break;
            case "type":
                o = description.getType();
                break;
            default:
                o = "unknown object description query \"" + queryString + "\"";
        }
        return o;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return objDesc(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
