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

import ublu.AS400Factory;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics.StringArrayList;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400FTP;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.FTP;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * FTP client
 *
 * @author jwoehr
 */
public class CmdFTP extends Command {

    {
        setNameAndDescription("ftp",
                "/3? -cd path | -cmd ${ command string }$ | -disconnect | -get filepath | -list | -put filepath | -pwd | -session @session [ -as400 ] [ -from datasink ] [ -to datasink ]  [ -mode act/pas ] [ -port portnum ] [ -type asc/bin ] [ -tofile destfile ] ~@{system} ~@{userid} ~@{password} : FTP client");
    }

    /**
     * The functions we perform
     */
    protected static enum FUNCTIONS {

        /**
         * Do nothing
         */
        NULL,
        /**
         * Issue command to server
         */
        CMD,
        /**
         * Disconnect and null the tuple
         */
        DISCONNECT,
        /**
         * Changedir
         */
        CD,
        /**
         * Get a file
         */
        GET,
        /**
         * ls dir
         */
        LIST,
        /**
         * Get present working dir
         */
        PWD,
        /**
         * PUt a file
         */
        PUT
    }

    /**
     * Carry out a variety of ftp client commands
     *
     * @param args the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray ftp(ArgArray args) {
        FUNCTIONS function = FUNCTIONS.NULL;
        int portNum = 21; // default FTP port
        String cdpath = null;
        String commandString = null;
        String remoteFilename = null;
        String localFilename = null;
        String sessionTupleName = null;
        Tuple sessionTuple = null;
        int xferMode = FTP.ACTIVE_MODE;
        int xferType = FTP.ASCII;
        boolean modeWasSet = false;
        boolean typeWasSet = false;
        boolean isAs400 = false;
        while (args.hasDashCommand()) {
            String dashCommand = args.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    isAs400 = true;
                    break;
                case "-to":
                    setDataDestfromArgArray(args);
                    break;
                case "-from":
                    setDataSrcfromArgArray(args);
                    break;
                case "-cd":
                    function = FUNCTIONS.CD;
                    cdpath = args.next();
                    break;
                case "-cmd":
                    function = FUNCTIONS.CMD;
                    commandString = args.nextMaybeQuotation();
                    break;
                case "-disconnect":
                    function = FUNCTIONS.DISCONNECT;
                    break;
                case "-get":
                    function = FUNCTIONS.GET;
                    remoteFilename = args.next();
                    break;
                case "-list":
                    function = FUNCTIONS.LIST;
                    break;
                case "-port":
                    portNum = args.nextInt();
                    break;
                case "-type":
                    String typestring = args.next().substring(0, 3);
                    switch (typestring) {
                        case "asc":
                            xferType = FTP.ASCII;
                            typeWasSet = true;
                            break;
                        case "bin":
                            xferType = FTP.BINARY;
                            typeWasSet = true;
                            break;
                        default:
                            xferType = FTP.ASCII;
                            typeWasSet = true;
                            getLogger().log(Level.WARNING, "Unknown xfer mode {0}, setting xfer type to ASCII", typestring);
                    }
                    break;
                case "-pwd":
                    function = FUNCTIONS.PWD;
                    break;
                case "-put":
                    function = FUNCTIONS.PUT;
                    localFilename = args.next();
                    break;
                case "-session":
                    sessionTupleName = args.next();
                    sessionTuple = getTuple(sessionTupleName);
                    break;
                case "-mode":
                    String modestring = args.next().substring(0, 3);
                    switch (modestring) {
                        case "act":
                            xferMode = FTP.ACTIVE_MODE;
                            typeWasSet = true;
                            break;
                        case "pas":
                            xferMode = FTP.PASSIVE_MODE;
                            typeWasSet = true;
                            break;
                        default:
                            xferMode = FTP.ACTIVE_MODE;
                            modeWasSet = true;
                            getLogger().log(Level.WARNING, "Unknown xfer mode {0}setting xfer mode to active", modestring);
                    }
                    break;
                case "-tofile":
                    remoteFilename = args.next();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            FTP myFTP = null;
            if (sessionTuple != null) {
                if (FTP.class.isInstance(sessionTuple.getValue())) {
                    myFTP = FTP.class.cast(sessionTuple.getValue());
                } else {
                    getLogger().log(Level.SEVERE, "Session tuple {0} does not reference FTP.", sessionTupleName);
                }
            } else // no session tuple yet established
            if (args.size() < 3) {
                logArgArrayTooShortError(args);
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                String server = args.nextMaybeQuotationTuplePopString();
                String userid = args.nextMaybeQuotationTuplePopString();
                String password = args.nextMaybeQuotationTuplePopString();
                if (isAs400) {
                    try {
                        myFTP = new AS400FTP(AS400Factory.newAS400(getInterpreter(), server, userid, password));
                    } catch (PropertyVetoException ex) {
                        getLogger().log(Level.SEVERE, "Error creating  AS400FTP session", ex);
                    }
                } else {
                    myFTP = new FTP(server, userid, password);
                }
                try {
                    if (myFTP != null) {
                        myFTP.setPort(portNum); // only set port before 1st connect
                        // If we got an FTP and user provided a session tuple
                        if (sessionTupleName != null) { // BUG allows illegal tuple name
                            setTuple(sessionTupleName, myFTP);
                        }
                    }
                } catch (PropertyVetoException ex) {
                    getLogger().log(Level.SEVERE, "Error setting port for FTP session", ex);
                    myFTP = null; // This will make us fall out below
                }
            }
            if (myFTP == null) { // Here's where we fall out if no FTP instance
                getLogger().log(Level.SEVERE, "Unable to create FTP session");
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                if (modeWasSet) {
                    myFTP.setMode(xferMode);
                }
                try {
                    if (typeWasSet) {
                        myFTP.setDataTransferType(xferType);
                    }
                    switch (function) {
                        case CD:
                            myFTP.cd(cdpath);
                            getLogger().log(Level.INFO, myFTP.getLastMessage());
                            break;
                        case CMD:
                            if (commandString == null) {
                                getLogger().log(Level.SEVERE, "No valid ${ commmand string }$ found for -cmd");
                                setCommandResult(COMMANDRESULT.FAILURE);
                            } else {
                                put(myFTP.issueCommand(commandString));
                            }
                            break;
                        case DISCONNECT:
                            myFTP.disconnect();
                            if (sessionTuple != null) {
                                getInterpreter().deleteTuple(sessionTuple);
                            }
                            break;
                        case GET:
                            boolean getrc;
                            if (getDataDest().getType() == DataSink.SINKTYPE.FILE) {
                                localFilename = getDataDest().getName();
                            }
                            if (localFilename != null) {
                                getrc = myFTP.get(remoteFilename, localFilename);
                            } else {
                                getrc = myFTP.get(remoteFilename, remoteFilename);
                            }
                            getLogger().log(Level.INFO, myFTP.getLastMessage());
                            setCommandResult(getrc ? COMMANDRESULT.SUCCESS : COMMANDRESULT.FAILURE);
                            break;
                        case LIST:
                            StringArrayList dirListing = new StringArrayList();
                            dirListing.addAll(myFTP.dir());
                            put(dirListing);
                            getLogger().log(Level.INFO, myFTP.getLastMessage());
                            break;
                        case PUT:
                            boolean putrc;
                            if (remoteFilename != null) { // User provided -tofile
                                putrc = myFTP.put(localFilename, remoteFilename);
                            } else { // No -tofile, did user -to a file datadest?
                                if (getDataDest().getType() == DataSink.SINKTYPE.FILE) {
                                    remoteFilename = getDataDest().getName();
                                } else { // No, just make dest name same as local
                                    remoteFilename = localFilename;
                                }
                                putrc = myFTP.put(localFilename, remoteFilename);
                            }
                            getLogger().log(Level.INFO, myFTP.getLastMessage());
                            setCommandResult(putrc ? COMMANDRESULT.SUCCESS : COMMANDRESULT.FAILURE);
                            break;
                        case PWD:
                            put(myFTP.pwd());
                            break;
                    }
                } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                    getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return args;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return ftp(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
