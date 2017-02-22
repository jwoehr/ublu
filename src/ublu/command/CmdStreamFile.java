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
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.StreamFileHelper;

/**
 * Handle local file system stream files mostly for character stream files to
 * record file activities.
 *
 * @author jax
 */
public class CmdStreamFile extends Command {

    {
        setNameAndDescription("streamf", "/0 [-to datasink] [-from datasink] [--,-streamf @streamfileinstance] [ -new ~@{fqp} | -open ~@{mode} | -close | -read ~@{offset} ~@{length} | -write ~@{offset} ~@{length} | -query ~@{qstring} : manipulate stream files");

    }

    /**
     * the operations we know
     */
    protected enum OPS {

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
        NEW
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
        while (argArray.hasDashCommand() && getCommandResult() != COMMANDRESULT.FAILURE) {
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
                case "-open":
                    op = OPS.OPEN;
                    break;
                case "-close":
                    op = OPS.CLOSE;
                    break;
                case "-read":
                    op = OPS.READ;
                    break;
                case "-write":
                    op = OPS.WRITE;
                    break;
                case "-query":
                    op = OPS.QUERY;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
            if (havingUnknownDashCommand()) {
                setCommandResult(COMMANDRESULT.FAILURE);
            }
            if (getCommandResult() != COMMANDRESULT.FAILURE) {
                switch (op) {
                    case CLOSE:
                        break;
                    case NEW:
                        if (fqp == null) {
                            getLogger().log(Level.SEVERE, "No path for -new in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            streamFileHelper = new StreamFileHelper(fqp);
                            try {
                                put(streamFileHelper);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting StreamFileHelper in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case OPEN:
                        if (streamFileHelper == null) {
                            noInstance();
                        } else {
                        }
                        break;
                    case QUERY:
                        if (streamFileHelper == null) {
                            noInstance();
                        } else {
                        }
                        break;
                    case READ:
                        if (streamFileHelper == null) {
                            noInstance();
                        } else {
                        }
                        break;
                    case WRITE:
                        if (streamFileHelper == null) {
                            noInstance();
                        } else {
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Not supported yet.");

                }
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
