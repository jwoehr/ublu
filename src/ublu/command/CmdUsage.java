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

import ublu.Ublu;
import ublu.util.ArgArray;
import ublu.util.Functor;
import ublu.util.Generics;
import java.util.Set;
import utilities.AboutToolbox;

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
        setNameAndDescription("help or usage", "/0 [[-cmd commandname] | [-all] | [-version]] : display usage and help message");
    }

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
        for (int i = 0; i < USAGE_MSG.length; i++) {
            sb.append(USAGE_MSG[i]).append("\n");
        }
        sb.append("\nCommands:\n");
        Generics.CommandLexicon cl = new Generics.CommandLexicon(getInterpreter(), cm);
        Set<String> keys = cl.keySet();
        for (String key : keys) {
            sb.append("\t").append(key);
            if (longmsg) {
                sb.append(' ').append(cl.get(key));
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
        String cmdName = null;
        boolean longmsg = false;
        while (args.hasDashCommand()) {
            String dashCommand = args.parseDashCommand();
            switch (dashCommand) {
                case "-all":
                    longmsg = true;
                    break;
                case "-cmd":
                    cmdName = args.next();
                    break;
                case "-version":
                    cmdName = "version";
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (cmdName == null) {
                getInterpreter().outputerrln(getInterpreter().getMyUblu().startupMessage());
                getInterpreter().outputerrln("");
                getInterpreter().outputerrln(usageMessage(getInterpreter().getCmdMap(), longmsg));
            } else {
                if (cmdName.equals("version")) {
                    getInterpreter().outputerrln(getInterpreter().getMyUblu().startupMessage());
                    getInterpreter().outputerrln(AboutToolbox.getVersionDescription().trim());
                    getInterpreter().outputerrln("Postgresql version: " + org.postgresql.Driver.getVersion());                    
                } else {
                    CommandInterface c = getInterpreter().getCmd(getInterpreter(), cmdName);
                    if (c != null) {
                        if (c instanceof Command) {
                            Command command = Command.class.cast(c);
                            getInterpreter().outputerrln(command.getNameAndDescription());
                        }
                    } else {
                        Functor f = getInterpreter().getFunctor(cmdName);
                        if (f != null) {
                            getInterpreter().outputerrln(cmdName + " " + f.toString());
                        } else {
                            getInterpreter().outputerrln("No such command or functor: " + cmdName);
                        }
                    }
                }
            }
        }
        return args;
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
