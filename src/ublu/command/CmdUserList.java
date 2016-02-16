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
import ublu.util.Generics.UserArrayList;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.UserList;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Command to fetch a list of users on the host
 *
 * @author jwoehr
 */
public class CmdUserList extends Command {

    {
        setNameAndDescription("userlist",
                "/3? [-as400 @as400] [-to datasink] [-userinfo ~@{ALL|USER|GROUP|MEMBER}] [-groupinfo ~@{NONE|NOGROUP|profilename}] [-userprofile ~@{username|*ALL}] ~@{system} ~@{userid} ~@{password} : return a list of users");
    }

    /**
     * Do the work of fetching the user list and putting it.
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray cmdUserList(ArgArray argArray) {
        String userinfo = "ALL";
        String groupinfo = "NONE";
        String userprofile = "*ALL";

        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
                case "-to":
                    setDataDest(newDataSink(argArray));
                    break;
//                case "-from":
//                    setDataSrc(newDataSink(argArray));
//                    break;
                case "-userinfo":
                    userinfo = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-groupinfo":
                    groupinfo = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-userprofile":
                    userprofile = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            try {
                if (getAs400() == null) {
                    if (argArray.size() < 3) {
                        logArgArrayTooShortError(argArray);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        setAs400FromArgs(argArray);
                    }
                }
                if (getAs400() != null) {
                    UserList ul = new UserList(getAs400(), toUserInfoConstant(userinfo), toGroupInfoConstant(groupinfo), userprofile);
                    ul.load();
                    put(new UserArrayList(ul));
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

    private String toUserInfoConstant(String input) {
        String result = null;
        switch (input) {
            case "ALL":
                result = UserList.ALL;
                break;
            case "USER":
                result = UserList.USER;
                break;
            case "GROUP":
                result = UserList.GROUP;
                break;
            case "MEMBER":
                result = UserList.MEMBER;
                break;
        }
        return result;
    }

    private String toGroupInfoConstant(String input) {
        String result;
        switch (input) {
            case "NONE":
                result = UserList.NONE;
                break;
            case "NOGROUP":
                result = UserList.NOGROUP;
                break;
            default:
                result = input;
        }
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdUserList(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
