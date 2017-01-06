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
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * FTP client
 *
 * @author jwoehr
 */
public class CmdFTPNu extends Command {

    {
        setNameAndDescription("ftp",
                "/3? [ -to datasink ] [ -tofile ~@destfile ] [ -pushrc ] -new  [ -as400 ] [ -mode ~@{act|pas} ] [ -port ~@{portnum} ] [ -type ~@{asc|bin} ] | --,-session ~@session [ -cd ~@{path} | -cmd ~@{ command string } | -connect | -disconnect | -get ~@{remotefilepath}  [-target ~@{localfilepath}] | -dir | -ls ~@{filespec} | -put ~@{localfilepath} [-target ~@{remotefilepath}] | -pwd ] ~@{system} ~@{userid} ~@{password} : FTP client with AS400-specific extensions");
    }

    /**
     * The functions we perform
     */
    protected static enum FUNCTIONS {

        /**
         * Create instance
         */
        INSTANCE,
        /**
         * Do nothing
         */
        NULL,
        /**
         * Issue command to server
         */
        CMD,
        /**
         * connect session
         */
        CONNECT,
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
        DIR,
        /**
         * ls dir with spec
         */
        LS,
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
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray ftp(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.INSTANCE;
        int portNum = 21; // default FTP port
        String cdpath = null;
        String commandString = null;
        String remoteFilename = null;
        String localFilename = null;
        String targetFilename = null;
        String lsSpec = null;
        boolean pushrc = false;
        FTP myFTP = null;
        int xferMode = FTP.ACTIVE_MODE;
        int xferType = FTP.ASCII;
        boolean typeWasSet = false;
        boolean isAs400 = false;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    isAs400 = true;
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-tofile":
                    setDataDest(DataSink.fileSinkFromTuple(argArray.nextTupleOrPop()));
                    break;
                case "--":
                case "-session":
                    myFTP = argArray.nextTupleOrPop().value(FTP.class);
                    break;
                case "-cd":
                    function = FUNCTIONS.CD;
                    cdpath = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-cmd":
                    function = FUNCTIONS.CMD;
                    commandString = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-connect":
                    function = FUNCTIONS.CONNECT;
                    break;
                case "-disconnect":
                    function = FUNCTIONS.DISCONNECT;
                    break;
                case "-get":
                    function = FUNCTIONS.GET;
                    remoteFilename = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-dir":
                    function = FUNCTIONS.DIR;
                    break;
                case "-ls":
                    function = FUNCTIONS.LS;
                    lsSpec = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-new":
                    function = FUNCTIONS.INSTANCE;
                    break;
                case "-port":
                    portNum = argArray.nextInt();
                    break;
                case "-pushrc":
                    pushrc = true;
                    break;
                case "-type":
                    typeWasSet = true;
                    String typestring = argArray.nextMaybeQuotationTuplePopStringTrim().substring(0, 3).toLowerCase();
                    switch (typestring) {
                        case "asc":
                            xferType = FTP.ASCII;
                            break;
                        case "bin":
                            xferType = FTP.BINARY;
                            break;
                        default:
                            xferType = FTP.ASCII;
                            getLogger().log(Level.WARNING, "Unknown xfer mode {0}, setting xfer type to ASCII", typestring);
                    }
                    break;
                case "-pwd":
                    function = FUNCTIONS.PWD;
                    break;
                case "-put":
                    function = FUNCTIONS.PUT;
                    localFilename = argArray.next();
                    break;
                case "-mode":
                    String modestring = argArray.nextMaybeQuotationTuplePopStringTrim().substring(0, 3);
                    switch (modestring) {
                        case "act":
                            xferMode = FTP.ACTIVE_MODE;
                            break;
                        case "pas":
                            xferMode = FTP.PASSIVE_MODE;
                            break;
                        default:
                            xferMode = FTP.ACTIVE_MODE;
                            getLogger().log(Level.WARNING, "Unknown xfer mode {0} setting xfer mode to active", modestring);
                    }
                    break;
                case "-target":
                    targetFilename = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Boolean cmdrc;
            switch (function) {
                case INSTANCE:
                    if (myFTP == null) {
                        if (argArray.size() < 3) {
                            logArgArrayTooShortError(argArray);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            String server = argArray.nextMaybeQuotationTuplePopString();
                            String userid = argArray.nextMaybeQuotationTuplePopString();
                            String password = argArray.nextMaybeQuotationTuplePopString();
                            if (isAs400) {
                                try {
                                    myFTP = new AS400FTP(AS400Factory.newAS400(getInterpreter(), server, userid, password));
                                } catch (PropertyVetoException ex) {
                                    getLogger().log(Level.SEVERE, "Error creating  AS400FTP session", ex);
                                }
                            } else {
                                myFTP = new FTP(server, userid, password);
                            }
                        }
                        if (myFTP != null) {
                            try {
                                myFTP.setPort(portNum); // only set port before 1st connect
                                myFTP.setMode(xferMode);
                                myFTP.setDataTransferType(xferType);
                            } catch (PropertyVetoException | IOException ex) {
                                getLogger().log(Level.SEVERE, "Error setting port or mode or type for FTP session", ex);
                                myFTP = null; // This will make us fall out below
                            }
                        }
                    }
                    // Here's where we fall out if no FTP instance
                    if (myFTP == null) {
                        getLogger().log(Level.SEVERE, "Unable to create FTP session");
                        setCommandResult(COMMANDRESULT.FAILURE);
                    } else {
                        try {
                            put(myFTP);
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;

                case CONNECT:
                    if (myFTP != null) {
                        try {
                            cmdrc = myFTP.connect();
                            if (pushrc) {
                                pushTuple(new Tuple(null, cmdrc));
                            }
                            put(myFTP.getLastMessage());
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "null ftp instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case CD:
                    if (myFTP != null && cdpath != null) {
                        try {
                            cmdrc = myFTP.cd(cdpath);
                            if (pushrc) {
                                pushTuple(new Tuple(null, cmdrc));
                            }
                            put(myFTP.getLastMessage());
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.WARNING, "null ftp instance or cd path in{0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case CMD:
                    if (myFTP != null) {
                        if (commandString == null) {
                            getLogger().log(Level.SEVERE, "No valid $'{' commmand string '}'$ found for -cmd in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                put(myFTP.issueCommand(commandString));
                            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "null ftp instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case DISCONNECT:
                    if (myFTP != null) {
                        try {
                            myFTP.disconnect();
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "null ftp instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case GET:
                    if (myFTP != null) {
                        if (remoteFilename != null) {
                            try {
                                if (targetFilename != null) {
                                    localFilename = targetFilename;
                                } else {
                                    localFilename = FileSystems.getDefault().getPath(remoteFilename).getFileName().toString();
                                }
                                if (typeWasSet) {
                                    myFTP.setDataTransferType(xferType);
                                }
                                cmdrc = myFTP.get(remoteFilename, localFilename);
                                if (pushrc) {
                                    pushTuple(new Tuple(null, cmdrc));
                                }
                                put(myFTP.getLastMessage());
                            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "null filename in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "null ftp instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case DIR:
                    if (myFTP != null) {
                        try {
                            StringArrayList dirListing = new StringArrayList();
                            dirListing.addAll(myFTP.dir());
                            put(dirListing);
                            getLogger().log(Level.INFO, myFTP.getLastMessage());
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "null ftp instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case LS:
                    if (myFTP != null) {
                        try {
                            StringArrayList dirListing = new StringArrayList();
                            dirListing.addAll(myFTP.dir(lsSpec));
                            put(dirListing);
                            getLogger().log(Level.INFO, myFTP.getLastMessage());
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "null ftp instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case PUT:
                    if (myFTP != null) {
                        if (localFilename != null) {
                            try {
                                if (targetFilename != null) {
                                    remoteFilename = targetFilename;
                                } else {
                                    remoteFilename = FileSystems.getDefault().getPath(localFilename).getFileName().toString();
                                }
                                if (typeWasSet) {
                                    myFTP.setDataTransferType(xferType);
                                }
                                cmdrc = myFTP.put(localFilename, remoteFilename);
                                if (pushrc) {
                                    pushTuple(new Tuple(null, cmdrc));
                                }
                                put(myFTP.getLastMessage());
                            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "null filename in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "null ftp instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case PWD:
                    if (myFTP != null) {
                        try {
                            put(myFTP.pwd());
                        } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Exception in FTP operation", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "null ftp instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }

        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return ftp(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
