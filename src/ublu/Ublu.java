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
package ublu;

import java.io.PrintStream;
import java.util.logging.Logger;
import org.sblim.cimclient.internal.cim.CIMVersion;
import ublu.util.Generics.StringArrayList;
import ublu.util.GetArgs;
import ublu.util.Interpreter;
import ublu.util.InterpreterLogger;
import ublu.util.JVMHelper;

/**
 * Main class of the interpretive application. Application controller.
 *
 * @author jwoehr
 */
public class Ublu {

    private GetArgs myGetArgs;
    private StringArrayList originalArgs;
    private JVMHelper jVMHelper = null;
    private boolean goubluing = false;
    private boolean windowing = false;

    /**
     * True if command-line switch told us we're running under Goublu.
     *
     * @return True if command-line switch told us we're running under Goublu
     */
    public boolean isGoubluing() {
        return goubluing;
    }

    /**
     * True if we're running as UbluWin.
     *
     * @return True if we're running as UbluWin.
     */
    public boolean isWindowing() {
        return windowing;
    }

    /**
     * True if we're running in as UbluWin.
     *
     * @param windowing True if we're running as UbluWin.
     */
    public void setWindowing(boolean windowing) {
        this.windowing = windowing;
    }

    /**
     * Return singleton JVMHelper
     *
     * @return singleton
     */
    public final JVMHelper getJVMHelper() {
        if (jVMHelper == null) {
            jVMHelper = new JVMHelper();
        }
        return jVMHelper;
    }

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
    public final static Interpreter getMainInterpreter() {
        return mainInterpreter;
    }

    /**
     * Set main interpreter instance
     *
     * @param interpreter main interpreter instance
     */
    protected final static void setMainInterpreter(Interpreter interpreter) {
        mainInterpreter = interpreter;
    }

    /**
     * Get the user
     *
     * @return the user.name from System.properties
     */
    public final static String getUser() {
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
     * Get the Ublu version
     *
     * @return String describing the Ublu version
     */
    public final static String ubluVersion() {
        return Version.ubluVersion;
    }

    /**
     * Get the compile date and time
     *
     * @return String describing the build
     */
    public final static String compileDateTime() {
        return Version.compileDateTime;
    }

    static String HELPLINE
            = "Type help for help. Type license for license. Type bye to exit.";

    /**
     * Create a string enumerating the open source projects used by Interpreter.
     *
     * @return a string enumerating the open source projects used by
     * Interpreter.
     */
    public final static String openSourceList() {
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
                .append(CIMVersion.getProductName()).append(' ')
                .append(CIMVersion.getVersion()).append(' ')
                .append(CIMVersion.getBuildDate()).append(' ')
                .append(CIMVersion.getBuildTime()).append('\n')
                .append("http://sblim.cvs.sourceforge.net/viewvc/sblim/jsr48-client/").append('\n')
                .append(CIMVersion.getCopyright()).append('\n')
                .append("Eclipse Public License https://opensource.org/licenses/eclipse-1.0.php")
                .append("\n---\n")
                .append(com.softwoehr.pigiron.Version.getVersion())
                .append(" http://pigiron.sourceforge.net\n")
                .append("Copyright (c) 2008-2016 Jack J. Woehr, PO Box 51, Golden CO 80402 USA\n")
                .append("All Rights Reserved")
                .append("\n---\n")
                .append("org.json\n")
                .append("Copyright (c) 2002 JSON.org")
                .toString();
    }

    /**
     * Create a string introducing the program.
     *
     * @return a string introducing the program.
     */
    public final static String startupMessage() {
        StringBuilder sb = new StringBuilder("Ublu ");
        return sb.append(ubluVersion())
                .append(" build of ").append(compileDateTime()).append("\n")
                .append("Author: Jack J. Woehr.\n")
                .append("Copyright 2015, Absolute Performance, Inc., http://www.absolute-performance.com\n")
                .append("Copyright 2017, Jack J. Woehr, http://www.softwoehr.com\n")
                .append("All Rights Reserved\n")
                .append("Ublu is Open Source Software under the BSD 2-clause license.\n")
                .append("THERE IS NO WARRANTY and NO GUARANTEE OF CORRECTNESS NOR APPLICABILITY.\n")
                .append("***\n")
                .append("Ublu utilizes the following open source projects:")
                .append(openSourceList())
                .append("\n***")
                .toString();
    }

    /**
     * String describing invocation usage
     *
     * @return String describing invocation usage
     */
    public final static String invocationHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ublu invocation: java [ java options .. ] -jar ublu.jar [ ublu options .. ] [ ublu commands .. ]\n");
        sb.append("Ublu options:\n");
        sb.append("  -i filename [-i filename ..]\tinclude all indicated source files\n");
        sb.append("  -s\t\t\t\tif including, include silently, otherwise startup interpreter silently\n");
        sb.append("  -t [filename, --]\t\topen history file filename or default if --\n");
        sb.append("  -h\t\t\t\tdisplay this help and then exit\n");
        return sb.toString();
    }

    /**
     * Reinit logger e.g., after creating windowing
     * @param out logging stream
     */
    public void reinitLogger(PrintStream out) {
        LOG = new InterpreterLogger("UbluInterpreter." + Thread.currentThread().toString(), Logger.getLogger(Ublu.class.getName()), out);
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
        args = processArgs(args);
        // /* Debug */ System.err.println("Args processed are " + new StringArrayList(args));
        // /* Debug */ System.err.println("GetArgs processed are " + myGetArgs);
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
        if (myGetArgs.containsOpt("-h")) {
            interpreter.outputerrln(startupMessage());
            interpreter.outputerrln(invocationHelp());
        } else {
            if (myGetArgs.containsOpt("-g")) {
                goubluing = true;
            }
            if (myGetArgs.containsOpt("-t")) { // Instance history (with filename, if provided)
                StringArrayList sal = myGetArgs.getAllIdenticalOptionArguments("-t");
                if (!sal.isEmpty()) {
                    String hfilename = sal.get(0);
                    if (hfilename != null) {
                        interpreter.setHistoryFileName(hfilename);
                    }
                }
                interpreter.instanceHistory();
            }
            if (myGetArgs.containsOpt("-i")) {
                for (String i : myGetArgs.getAllIdenticalOptionArguments("-i")) {
                    interpreter.getArgArray().add(0, "include");
                    interpreter.getArgArray().add(1, i);
                    if (myGetArgs.containsOpt("-s")) {
                        interpreter.getArgArray().add(1, "-s");
                    }
                }
                interpreter.loop();
                interpreter.interpret();
            } else if (interpreter.getArgArray().isEmpty()) {
                if (!myGetArgs.containsOpt("-s")) {
                    if (interpreter.isConsole() || isGoubluing()) {
                        interpreter.outputerrln(startupMessage());
                        interpreter.outputerrln(HELPLINE);
                    }
                }
                interpreter.interpret();
            } else {
                interpreter.loop();
            }
            interpreter.closeHistory();
            interpreter.getErroutStream().flush();
            interpreter.getOutputStream().flush();
        }
        return interpreter.getGlobal_ret_val();
    }
    //    /**
    //     * Run the singleton main interpreter
    //     *
    //     * @return the global return value
    //     */
    //    public int oldRunMainInterpreter() {
    //        Interpreter interpreter = getMainInterpreter();
    //        if (interpreter.getArgArray().isEmpty()) {
    //            if (!getSwitches().contains("-s")) {
    //                if (interpreter.isConsole()) {
    //                    interpreter.outputerrln(startupMessage());
    //                    interpreter.outputerrln(HELPLINE);
    //                }
    //            }
    //            interpreter.interpret();
    //        } else if (getSwitches().contains("-include")
    //                || getSwitches().contains("-i")) {
    //            interpreter.getArgArray().add(0, "include");
    //            if (getSwitches().contains("-s")) {
    //                interpreter.getArgArray().add(1, "-s");
    //            }
    //            interpreter.loop();
    //            interpreter.interpret();
    //        } else {
    //            interpreter.loop();
    //        }
    //        interpreter.closeHistory();
    //        interpreter.getErroutStream().flush();
    //        interpreter.getOutputStream().flush();
    //        return interpreter.getGlobal_ret_val();
    //    }

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
     * @param args commands to program in the program's syntax. Any strings
     * starting with a dash <code>-</code> are taken to be switches to the ublu
     * invocation itself and are processed and removed from the args passed to
     * the interpreter.
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
     * @param args commands to program in the program's syntax. Any strings
     * starting with a dash <code>-</code> are taken to be switches to the ublu
     * invocation itself and are processed and removed from the args passed to
     * the interpreter.
     * @return retval of the last command executed, either
     * {@link ublu.command.CommandInterface.COMMANDRESULT#SUCCESS} (0) if the
     * last executed normally or otherwise
     * {@link ublu.command.CommandInterface.COMMANDRESULT#FAILURE} (1).
     */
    public static int niam(String[] args) {
        Ublu api = new Ublu(args);
        return api.runMainInterpreter();
    }

    /**
     * Get the value of originalArgs
     *
     * @return the value of originalArgs
     */
    public StringArrayList getOriginalArgs() {
        return originalArgs;
    }

    private StringArrayList switches = new StringArrayList();

    /**
     * Get the value of switches
     *
     * @return the value of switches
     */
    public StringArrayList getSwitches() {
        return switches;
    }

    /**
     * Set the value of switches
     *
     * @param switches new value of switches
     */
    public void setSwitches(StringArrayList switches) {
        this.switches = switches;
    }

    /**
     * Set the value of originalArgs
     *
     * @param originalArgs new value of originalArgs
     */
    public void setOriginalArgs(StringArrayList originalArgs) {
        this.originalArgs = originalArgs;
    }

//    private String[] oldProcessArgs(String[] args) {
//        StringArrayList remainderArgs = new StringArrayList(args);
//        setOriginalArgs(new StringArrayList(args));
//        for (String s : getOriginalArgs()) {
//            if (s.startsWith("-")) {
//                getSwitches().add(s);
//            } else {
//                break;
//            }
//        }
//        for (String switche : getSwitches()) {
//            remainderArgs.remove(0);
//        }
//        return remainderArgs.toStringArray();
//    }
    private String[] processArgs(String[] args) {
        // /* Debug */ System.err.println("args are " + Arrays.toString(args));
        myGetArgs = new GetArgs(args);
        // /* Debug */ System.err.println("myGetArgs is " + myGetArgs);
        return myGetArgs.getArgumentsAsStringArray();
    }

}
