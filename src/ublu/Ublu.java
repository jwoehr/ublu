/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
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
package ublu;

import java.util.logging.Logger;

import ublu.util.Interpreter;
import ublu.util.InterpreterLogger;

/**
 * Main class of the interpretive application. Application controller.
 *
 * @author jwoehr
 */
public class Ublu {

    /**
     * Our special logging instance that no, does NOT conform to Java design
     * recommendations.
     */
    protected Logger LOG;
    /**
     * Singleton main interpreter
     */
    private static Interpreter mainInterpreter;

    /**
     * Get main interpreter instance
     *
     * @return main interpreter instance
     */
    public static Interpreter getMainInterpreter() {
        return mainInterpreter;
    }

    /**
     * Set main interpreter instance
     *
     * @param interpreter main interpreter instance
     */
    protected static void setMainInterpreter(Interpreter interpreter) {
        mainInterpreter = interpreter;
    }

    /**
     * Get the user
     *
     * @return the user.name from System.properties
     */
    public static String getUser() {
        return System.getProperty("user.name");
    }

    /**
     * Get the logger for the Interpreter instance.
     *
     * @return the logger for the Interpreter instance.
     */
    public Logger getLogger() {
        return LOG;
    }

    /**
     * Get the build version
     *
     * @return String describing the build
     */
    public static String version() {
        return Version.compileDateTime;
    }

    static String EXITLINE
            = "Type help for help. Type license for license. Type bye to exit.";

    /**
     * Create a string enumerating the open source projects used by Interpreter.
     *
     * @return a string enumerating the open source projects used by
     * Interpreter.
     */
    public static String openSourceList() {
        StringBuilder sb = new StringBuilder();
        return sb.append(utilities.AboutToolbox.getVersionDescription())
                .append("\n---\n")
                .append("Postgresql ")
                .append(org.postgresql.Driver.getVersion()).append('\n')
                .append("Copyright (c) 1997-2011, PostgreSQL Global Development Group\n")
                .append("All rights reserved http://www.postgresql.org")
                .append("\n---\n")
                .append("tn5250j http://tn5250j.sourceforge.net/\n")
                .append("NO WARRANTY (GPL) see the file tn5250_LICENSE")
                .append("\n---\n")
                .append(com.softwoehr.pigiron.Version.getVersion())
                .append(" http://pigiron.sourceforge.net\n")
                .append("Copyright (c) 2008-2016 Jack J. Woehr, PO Box 51, Golden CO 80402 USA\n")
                .append("All Rights Reserved")
                .toString();
    }

    /**
     * Create a string introducing the program.
     *
     * @return a string introducing the program.
     */
    public final String startupMessage() {
        StringBuilder sb = new StringBuilder("Ublu version 1.1.2+");
        return sb.append(" build of ").append(version()).append("\n")
                .append("Author: Jack J. Woehr.\n")
                .append("Copyright 2015, Absolute Performance, Inc., http://www.absolute-performance.com\n")
                .append("Copyright 2016, Jack J. Woehr, http://www.softwoehr.com\n")
                .append("All Rights Reserved\n")
                .append("Ublu is Open Source Software under the BSD 2-clause license.\n")
                .append("***\n")
                .append("Ublu utilizes the following open source projects ")
                .append(openSourceList())
                .append("\n***")
                .toString();
    }

    /**
     * Instance with args ready for {@link ublu.util.Interpreter} to start its
     * first {@link ublu.util.Interpreter#loop()}.
     *
     * @param args arguments at invocation, effectively just another command
     * line
     */
    public Ublu(String[] args) {
        this();
        setMainInterpreter(new Interpreter(args, this));
        LOG = new InterpreterLogger("UbluInterpreter." + Thread.currentThread().toString(), Logger.getLogger(Ublu.class.getName()), getMainInterpreter().getErroutStream());
    }

    /**
     * Not really used
     */
    protected Ublu() {
    }

    /**
     * Run the singleton main interpreter
     *
     * @return the global return value
     */
    public int runMainInterpreter() {
        Interpreter interpreter = getMainInterpreter();
        if (interpreter.getArgArray().isEmpty()) {
            if (interpreter.isConsole()) {
                interpreter.outputerrln(startupMessage());
                interpreter.outputerrln(EXITLINE);
            }
            interpreter.interpret();
        } else {
            interpreter.loop();
        }
        interpreter.closeHistory();
        interpreter.getErroutStream().flush();
        interpreter.getOutputStream().flush();
        return interpreter.getGlobal_ret_val();
    }

    /**
     * Run a command or run the interpreter. This is the main() of the Main
     * Class of the system.
     *
     * <p>
     * Exits the Java virtual machine returning to the system caller the retval
     * of the last command executed, either
     * {@link ublu.command.CommandInterface.COMMANDRESULT#SUCCESS} (0) if the
     * last executed or
     * {@link ublu.command.CommandInterface.COMMANDRESULT#FAILURE} (1).</p>
     *
     * @param args commands to program in the program's syntax
     */
    public static void main(String[] args) {
        System.exit(niam(args));
    }

    /**
     * Run a command or run the interpreter. This is a factor of the Main Class
     * main() of the system. This method does not System.exit, merely returning
     * the retval of the last command. Useful for calling Ublu from another Java
     * program.
     *
     * @param args commands to program in the program's syntax
     * @return retval of the last command executed, either
     * {@link ublu.command.CommandInterface.COMMANDRESULT#SUCCESS} (0) if the
     * last executed normally or otherwise
     * {@link ublu.command.CommandInterface.COMMANDRESULT#FAILURE} (1).
     */
    public static int niam(String[] args) {
        Ublu api = new Ublu(args);
        return api.runMainInterpreter();
    }
}
