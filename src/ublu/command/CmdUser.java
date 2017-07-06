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
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.User;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to manipulate user profile on the host
 *
 * @author jwoehr
 */
public class CmdUser extends Command {

    static {
        setNameAndDescription("user",
                "/3? [-as400 @as400] [--,-user ~@user] [-to datasink]  [-userprofile ~@{username}] [-enable | -disable | -new,-instance | -query ~@{property} | -refresh ] system userid password : manipulate user profile");
    }

    private enum OPERATIONS {

        NOOP, ENABLE, DISABLE, INSTANCE, QUERY, REFRESH
    }

    /**
     * Do the work of fetching the user list and putting it.
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray cmdUser(ArgArray argArray) {
        String userprofile = "";
        String querystring = "";
        Tuple userTuple = null;
        OPERATIONS operation = OPERATIONS.INSTANCE;

        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
//                case "-from":
//                    setDataSrc(newDataSink(argArray));
//                    break;
                case "--":
                case "-user":
                    userTuple = argArray.nextTupleOrPop();
                    break;
                case "-userprofile":
                    userprofile = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-new":
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-disable":
                    operation = OPERATIONS.DISABLE;
                    break;
                case "-enable":
                    operation = OPERATIONS.ENABLE;
                    break;
                case "-query":
                    operation = OPERATIONS.QUERY;
                    querystring = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            try {
                User user = null;
                if (userTuple != null) {
                    Object o = userTuple.getValue();
                    if (o instanceof User) {
                        user = User.class.cast(o);
                    } else {
                        getLogger().log(Level.SEVERE, "Tuple does represent a User in {0}", new Object[]{getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }

                if (user == null) {
                    if (getAs400() == null) {
                        if (argArray.size() < 3) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            setAs400FromArgs(argArray);
                        }
                    }
                    if (getAs400() != null) {
                        if (userprofile.equals("")) {
                            getLogger().log(Level.SEVERE, "No user profile specified and no user tuple specified in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            user = new User(getAs400(), userprofile);
                        }
                    }
                }
                if (user != null) {
                    switch (operation) {
                        case DISABLE:
                            user.setStatus("*DISABLED");
                            break;
                        case ENABLE:
                            user.setStatus("*ENABLED");
                            break;
                        case INSTANCE:
                            put(user);
                            break;
                        case QUERY:
                            switch (querystring) {
                                case "curlib":
                                    put(user.getCurrentLibraryName());
                                    break;
                                case "desc":
                                    put(user.getDescription());
                                    break;
                                case "direntry":
                                    put(user.getDirectoryEntry());
                                    break;
                                case "exists":
                                    put(user.exists());
                                    break;
                                case "homedir":
                                    put(user.getHomeDirectory());
                                    break;
                                case "msgq":
                                    put(user.getMessageQueue());
                                    break;
                                case "name":
                                    put(user.getName());
                                    break;
                                case "status":
                                    put(user.getStatus());
                                    break;
                                case "storage":
                                    put(user.getStorageUsed());
                                    break;
                                case "supplementalgroups":
                                    put(user.getSupplementalGroups());
                                    break;
                                case "supplementalgroupsnum":
                                    put(user.getSupplementalGroupsNumber());
                                    break;
                                default:
                                    getLogger().log(Level.SEVERE, "Unknown query string: {0} in {1}", new Object[]{querystring, getNameAndDescription()});
                                    setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            break;
                        case REFRESH:
                            user.refresh();
                            break;
                    }
                }
            } catch (PropertyVetoException ex) {
                getLogger().log(Level.SEVERE, "Unable to get AS400 instance in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException | SQLException | RequestNotSupportedException ex) {
                getLogger().log(Level.SEVERE, "Unable to get (or perhaps put) User List in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdUser(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
