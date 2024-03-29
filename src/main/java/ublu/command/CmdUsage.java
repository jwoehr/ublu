/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import ublu.Ublu;
import ublu.util.ArgArray;
import ublu.util.Functor;
import ublu.util.Generics;
import java.util.Set;
import java.util.logging.Level;
import ublu.util.Utils;

/**
 * Class to deliver the usage message
 *
 * @author jwoehr
 */
public class CmdUsage extends Command {

    private static final String[] USAGE_MSG = {"Usage: java " + Ublu.class.getName() + " cmd [arg [arg ..]] [cmd [arg [arg ..]] cmd ..]",
        "\tExecutes commands left to right.",
        "\tIf no command is present, interprets input until EOF or the 'bye' command is encountered."};

    {
        setNameAndDescription("help or usage", "/0 [-to datasink] [[--,-cmd ~@{commandname}] | [-all] | [-version]] [-linelen ~@{optional_line_length}] : display usage and help message");
    }

    private int linelength;

    /**
     * 0-arity ctor
     */
    public CmdUsage() {
    }

    /**
     * Create a string describing usage of the program.
     *
     * @param cm
     * @param longmsg
     * @return a string describing usage of the program
     */
    public String usageMessage(CommandMap cm, boolean longmsg) {
        StringBuilder sb = new StringBuilder();
        for (String USAGE_MSG1 : USAGE_MSG) {
            sb.append(USAGE_MSG1).append("\n");
        }
        sb.append("\nCommands:\n");
        Generics.CommandLexicon cl = new Generics.CommandLexicon(getInterpreter(), cm);
        Set<String> keys = cl.keySet();
        for (String key : keys) {
            sb.append("\t").append(key).append("\t\t");
            if (longmsg) {
                sb.append(' ').append(Utils.breakLines(cl.get(key), linelength, 3, 2));
            }
            sb.append("\n");
        }
        sb.append("\nType \"help -cmd commandname\" for more help on any command.\n");
        sb.append("\nType \"help -all\" for help on all commands.\n");
        return sb.toString();
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        setLineLength();
        String cmdName = null;
        boolean longmsg = false;
        while (args.hasDashCommand()) {
            String dashCommand = args.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(args);
                    break;
                case "-all":
                    longmsg = true;
                    break;
                case "--":
                case "-cmd":
                    cmdName = args.nextMaybeQuotationTuplePopString();
                    break;
                case "-version":
                    cmdName = "version";
                    break;
                case "-linelen":
                    linelength = args.nextIntMaybeQuotationTuplePopString();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            try {
                if (cmdName == null) {
                    put(usageMessage(getInterpreter().getCmdMap(), longmsg));
                } else if (cmdName.equals("version")) {
                    put(Ublu.startupMessage());
                } else {
                    CommandInterface c = getInterpreter().getCmd(getInterpreter(), cmdName);
                    if (c != null) {
                        if (c instanceof Command) {
                            Command command = Command.class.cast(c);
                            put(formatSingleCommand(command));
                        }
                    } else {
                        Functor f = getInterpreter().getFunctor(cmdName);
                        if (f != null) {
                            put(Utils.breakLines(cmdName + " " + f.toString(), linelength, 0, 0));
                        } else {
                            put("No such command or functor: " + cmdName);
                        }
                    }
                }
            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                getLogger().log(Level.SEVERE, "Exception putting usage in {0} {1}", new Object[]{getNameAndDescription(), ex});
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return args;
    }

    private String formatSingleCommand(Command cmd) {
        StringBuilder sb = new StringBuilder(cmd.getCommandName());
        sb.append('\t')
                .append(Utils.breakLines(cmd.getCommandDescription(), linelength, 1, 0));
        return sb.toString();
    }

    private void setLineLength() {
        try {
            linelength = Integer.parseInt(getInterpreter().getProperty("ublu.usage.linelength", "80").trim());
        } catch (NumberFormatException ex) {
            getLogger().log(Level.WARNING, "Invalid value for property ublu.linelength");
            linelength = 80;
        }
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
