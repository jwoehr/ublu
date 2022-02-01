/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics.ByteArrayList;
import ublu.util.StreamFileHelper;
import ublu.util.Tuple;

/**
 * Handle local file system stream files mostly for character stream files to
 * record file activities.
 *
 * @author jax
 */
public class CmdStreamFile extends Command {

    {
        setNameAndDescription("streamf", "/0 [-to datasink] [-from datasink] [--,-streamf @streamfileinstance] [ -list | -new ~@{fqp} | -open ~@{mode RB|RC|W} | -close | -create | -delete | -file | -rename ~@streamf | -mkdirs | -rball | -rcall | -rline | -read ~@{offset} ~@{length} | -write ~@{data} ~@{offset} ~@{length} | -q,-query ~@{qstring [af|ap|c|d|e|f|length|n|p|r|w|x]}] : manipulate stream files");
    }

    /**
     * the operations we know
     */
    protected enum OPS {

        /**
         *
         */
        CREATE,
        /**
         *
         */
        DELETE,
        /**
         *
         */
        OPEN,
        /**
         *
         */
        CLOSE,
        /**
         *
         */
        LIST,
        /**
         *
         */
        MKDIRS,
        /**
         *
         */
        QUERY,
        /**
         *
         */
        READ,
        /**
         *
         */
        WRITE,
        /**
         *
         */
        RBALL,
        /**
         *
         */
        RCALL,
        /**
         *
         */
        RLINE,
        /**
         *
         */
        NEW,
        /**
         *
         */
        RENAME,
        /**
         *
         */
        FILE
    }

    /**
     * Perform local stream file operations
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray doStreamF(ArgArray argArray) {
        OPS op = OPS.NEW;
        StreamFileHelper streamFileHelper = null;
        String fqp = null;
        String openMode = null;
        Tuple dataToWriteTuple = null;
        Integer offset = null;
        Integer length = null;
        String queryString = null;
        StreamFileHelper renTarg = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    break;
                case "--":
                case "-streamf":
                    streamFileHelper = argArray.nextTupleOrPop().value(StreamFileHelper.class);
                    break;
                case "-new":
                    op = OPS.NEW;
                    if (getDataSrc().getType().equals(DataSink.SINKTYPE.FILE)) {
                        fqp = getDataSrc().getName();
                    } else {
                        fqp = argArray.nextMaybeQuotationTuplePopStringTrim();
                    }
                    break;
                case "-create":
                    op = OPS.CREATE;
                    break;
                case "-delete":
                    op = OPS.DELETE;
                    break;
                case "-rename":
                    op = OPS.RENAME;
                    renTarg = argArray.nextTupleOrPop().value(StreamFileHelper.class);
                    break;
                case "-list":
                    op = OPS.LIST;
                    break;
                case "-mkdirs":
                    op = OPS.MKDIRS;
                    break;
                case "-open":
                    op = OPS.OPEN;
                    openMode = argArray.nextMaybeQuotationTuplePopStringTrim().toUpperCase();
                    break;
                case "-close":
                    op = OPS.CLOSE;
                    break;
                case "-read":
                    op = OPS.READ;
                    length = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-write":
                    op = OPS.WRITE;
                    dataToWriteTuple = argArray.nextTupleOrPop();
                    offset = argArray.nextIntMaybeQuotationTuplePopString();
                    length = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-rball":
                    op = OPS.RBALL;
                    break;
                case "-rcall":
                    op = OPS.RCALL;
                    break;
                case "-rline":
                    op = OPS.RLINE;
                    break;
                case "-q":
                case "-query":
                    op = OPS.QUERY;
                    queryString = argArray.nextMaybeQuotationTuplePopStringTrim().toLowerCase();
                    break;
                case "-file":
                    op = OPS.FILE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            switch (op) {
                case CREATE:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            streamFileHelper.create();
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Error creating " + streamFileHelper.getFile() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case DELETE:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.delete());
                        } catch (IOException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error deleting " + streamFileHelper.getFile() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case RENAME:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.rename(renTarg));
                        } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error renaming " + streamFileHelper.getFile() + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case CLOSE:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            streamFileHelper.close();
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Error closing in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case LIST:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.list());
                        } catch (SQLException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error listing dir in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case MKDIRS:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.mkdirs());
                        } catch (SQLException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error making dirs in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case NEW:
                    if (fqp == null) {
                        fqp = argArray.nextMaybeQuotationTuplePopStringTrim();
                    }
                    streamFileHelper = new StreamFileHelper(fqp);
                    try {
                        put(streamFileHelper);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error putting StreamFileHelper in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case OPEN:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            streamFileHelper.open(StreamFileHelper.MODE.valueOf(openMode));
                        } catch (FileNotFoundException ex) {
                            getLogger().log(Level.SEVERE, "Error opening " + openMode + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case QUERY:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.query(queryString));
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error querying " + queryString + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case READ:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.read(0, length));
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error -read in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case WRITE:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        if (dataToWriteTuple != null) {
                            Object o = dataToWriteTuple.getValue();
                            try {
                                if (o instanceof String) {
                                    streamFileHelper.write(String.class.cast(o).getBytes(), offset, length);
                                } else if (o instanceof ByteArrayList) {
                                    streamFileHelper.write(ByteArrayList.class.cast(o), offset, length);
                                } else if (o instanceof byte[]) {
                                    streamFileHelper.write((byte[]) o, offset, length);
                                } else {
                                    getLogger().log(Level.SEVERE, "Unsupported object for write in {0}", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            } catch (IOException ex) {
                                getLogger().log(Level.SEVERE, "Exception writing in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "Null object for write in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case RBALL:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.readAllBytes());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error -rball in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case RCALL:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.readAllLines());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error -rcall in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case RLINE:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.readLine());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error -rball in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case FILE:
                    if (streamFileHelper == null) {
                        noInstance();
                    } else {
                        try {
                            put(streamFileHelper.getFile());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE, "Error -file in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Not supported yet.");

            }
        }
        return argArray;
    }

    private void noInstance() {
        getLogger().log(Level.SEVERE, "No stream file instance in {0}", getNameAndDescription());
        setCommandResult(COMMANDRESULT.FAILURE);
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return doStreamF(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
