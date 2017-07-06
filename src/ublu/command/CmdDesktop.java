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
import ublu.util.ArgArray;
// import ublu.util.DataSink;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Perform desktop operations
 *
 * @author jwoehr
 */
public class CmdDesktop extends Command {

    static {
        setNameAndDescription("desktop",
                "/0 [-browse ~@{uri} | -mail | -mailto ~@{uri} | -supported] : desktop browser or mail");
    }

    enum OPS {
        BROWSE,
        MAIL,
        MAILTO,
        SUPPORTED
    }

    /**
     * Cmd action to perform desktop operations
     *
     * @param argArray arguments in interpreter buffer
     * @return what's left of arguments
     */
    public ArgArray cmdDesktop(ArgArray argArray) {
        String uri = null;
        OPS op = OPS.SUPPORTED;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
//                case "-to":
//                    setDataDestfromArgArray(argArray);
//                    break;
//                case "-from":
//                    setDataSrcfromArgArray(argArray);
//                    break;
                case "-browse":
                    op = OPS.BROWSE;
                    uri = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-mail":
                    op = OPS.MAIL;
                    break;
                case "-mailto":
                    op = OPS.MAILTO;
                    uri = argArray.nextMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            boolean supported = Desktop.isDesktopSupported();
            if (op.equals(OPS.SUPPORTED)) {
                try {
                    put(supported);
                } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                    getLogger().log(Level.SEVERE, "Exception putting supported in " + getNameAndDescription(), ex);
                }
            } else if (!supported) {
                getLogger().log(Level.SEVERE, "Desktop not supported in {0}", getNameAndDescription());
            } else {
                switch (op) {
                    case BROWSE:
                        try {
                            Desktop.getDesktop().browse(new URI(uri));
                        } catch (URISyntaxException | IOException ex) {
                            getLogger().log(Level.SEVERE, "Exception launching browser on URI " + uri + inNameAndDescription(), ex);
                        }
                        break;
                    case MAIL:
                        try {
                            Desktop.getDesktop().mail();
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Exception launching mailer in " + getNameAndDescription(), ex);
                        }
                        break;
                    case MAILTO:
                        try {
                            Desktop.getDesktop().mail(new URI(uri));
                        } catch (URISyntaxException | IOException ex) {
                            getLogger().log(Level.SEVERE, "Exception launching mailer on URI " + uri + inNameAndDescription(), ex);
                        }
                        break;
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdDesktop(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
