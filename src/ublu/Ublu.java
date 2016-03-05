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
package ublu;

import java.util.logging.Logger;

import ublu.server.Listener;
import ublu.util.Interpreter;
import ublu.util.InterpreterLogger;

/**
 * Main class of the interpretive application. Application controller.
 *
 * @author jwoehr
 */
public class Ublu {
    
    private Logger LOG;
    /**
     * Singleton main interpreterF
     */
    private static Interpreter mainInterpreter;
    /**
     * Singleton tcp listener
     */
    private static Listener singletonListener;

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
    private static void setMainInterpreter(Interpreter interpreter) {
        mainInterpreter = interpreter;
    }

    /**
     * Get the singleton tcpip listener
     *
     * @return the singleton tcpip listener
     */
    public static Listener getSingletonListener() {
        return singletonListener;
    }

    /**
     * Set the singleton tcpip listener
     *
     * @param singletonListener the singleton tcpip listener
     */
    private static void setSingletonListener(Listener singletonListener) {
        Ublu.singletonListener = singletonListener;
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

//    /**
//     * preset arbitrary credentials not to be repeated in command line
//     *
//     * @param key
//     * @param value
//     */
//    public void setACredential(String key, String value) {
//        getCredentials().put(key, value);
//    }
//    /**
//     * preset arbitrary credentials not to be repeated in command line
//     *
//     * @param key
//     * @return
//     */
//    public String getACredential(String key) {
//        return String.class.cast(getCredentials().get(key));
//    }
    /**
     * Get the build version
     *
     * @return String describing the build
     */
    public static String version() {
        return Version.compileDateTime;
    }

    /**
     * Create a string enumerating the open source projects used by Interpreter.
     *
     * @return a string enumerating the open source projects used by
     * Interpreter.
     */
    public static String openSourceList() {
        StringBuilder sb = new StringBuilder();
        return sb.append("JTOpen http://sourceforge.net/projects/jt400/\n")
                .append("IBM Public License 1.0\n")
                .append("Postgresql ")
                .append(org.postgresql.Driver.getVersion()).append('\n')
                .append("Copyright (c) 1997-2011, PostgreSQL Global Development Group\n")
                .append("All rights reserved http://www.postgresql.org\n")
                .append(com.softwoehr.pigiron.Version.getVersion()).append(" \n")
                .append("Copyright (c) 2008-2016 Jack J. Woehr, PO Box 51, Golden CO 80403 USA\n")
                .append("All Rights Reserved")
                .toString();
    }

    /**
     * Create a string introducing the program.
     *
     * @return a string introducing the program.
     */
    public final String startupMessage() {
        StringBuilder sb = new StringBuilder("Ublu version 1.0+");
        return sb.append(" build of ").append(version()).append("\n")
                .append("Author: Jack J. Woehr.\n")
                .append("Copyright 2014, Absolute Performance, Inc., http://www.absolute-performance.com\n")
                .append("Copyright 2016, Jack J. Woehr, http://www.softwoehr.com\n")
                .append("All Rights Reserved\n")
                .append("Ublu is open source software under the BSD 2-clause license.\n")
                .append("Ublu utilizes the following open source projects:\n")
                .append(openSourceList())
                .toString();
    }

    /**
     * Instance with args ready for the {@link #interpret} to start its first
     * {@link #loop}.
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
    private Ublu() {
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
                interpreter.outputerrln("Type help for help. Type license for license.");
            }
            interpreter.interpret();
        } else {
            interpreter.loop();
        }
        interpreter.closeHistory();
        interpreter.getErroutStream().flush();
        interpreter.getOutputStream().flush();
        stopListener();
        return interpreter.getGlobal_ret_val();
    }

    /**
     * Start the singleton tcpip listener
     *
     * @param portnum port to listen
     */
    public void newListener(int portnum) {
        Listener l = getSingletonListener();
        if (l != null) {
            l.setListening(false);
        }
        setSingletonListener(new Listener(this, portnum));
    }

    /**
     * Stop the singleton tcpip listener
     */
    public static void stopListener() {
        Listener l = getSingletonListener();
        if (l != null) {
            l.setListening(false);
            setSingletonListener(null);
        }
    }

    /**
     * Run a command or run the interpreter. This is the main() of the Main
     * Class of the system.
     *
     * <p>
     * Exits the Java virtual machine returning to the system caller the retval
     * of the last command executed, either
     * {@link ublu.command.CommandInterface.COMMANDRESULT.SUCCESS} (0) if the
     * last executed or
     * {@link ublu.command.CommandInterface.COMMANDRESULT.FAILURE} (1).</p>
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
     * {@link ublu.command.CommandInterface.COMMANDRESULT.SUCCESS} (0) if the
     * last executed normally or otherwise
     * {@link ublu.command.CommandInterface.COMMANDRESULT.FAILURE} (1).
     */
    public static int niam(String[] args) {
        Ublu api = new Ublu(args);
        return api.runMainInterpreter();
    }
}
