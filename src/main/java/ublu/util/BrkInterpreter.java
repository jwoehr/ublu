/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
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
package ublu.util;

import java.io.IOException;
import ublu.util.Generics.StringArrayList;

/**
 * A lite interpreter for short commands issued at a dbug breakpoint.
 *
 * @author jwoehr
 */
public class BrkInterpreter {

    private DBug myDBug;

    private BrkInterpreter() {
    }

    /**
     * Instance a break interpreter on the calling DBug instance
     *
     * @param myDBug the calling DBug instance
     */
    public BrkInterpreter(DBug myDBug) {
        this();
        this.myDBug = myDBug;
    }

    private Interpreter getHostInterpreter() {
        return myDBug.getHostInterpreter();
    }

    private void output(String s) {
        myDBug.output(s);
    }

    private void outputln(String s) {
        myDBug.outputln(s);
    }

    private String brkPrompt(String commandName, ArgArray argArray) {
        String s = "at: " + commandName + ' ' + argArray + "\nbrk>";
        if (getHostInterpreter().isGoubluing()) s = s+ "\n";
        return s;
    }

    private void prompt(String commandName, ArgArray argArray) {
        output(brkPrompt(commandName, argArray));
    }

    private StringArrayList getBrkLine() {
        StringArrayList sal = null;
        if (getHostInterpreter().isConsole()) {
            sal = new StringArrayList(System.console().readLine().trim());
        } else {
            try {
                sal = new StringArrayList(getHostInterpreter().getInputStreamBufferedReader().readLine().trim());
            } catch (IOException ex) {
                outputln("Exception getting break input: " + ex);
            }
        }
        return sal;
    }

    /**
     * Interpret dbug commands while at a breakpoint
     *
     * @param commandName current command
     * @param argArray rest of the arg array
     */
    public void brkInterpret(String commandName, ArgArray argArray) {
        while (myDBug.isOnBrk()) {
            outputln("");
            prompt(commandName, argArray);
            processBrkLine(getBrkLine());
        }
    }

    /**
     * Process break commands and their arguments during a dbug brk point
     *
     * @param sal the string array of the brk command line
     */
    public void processBrkLine(StringArrayList sal) {
//        /* debug */ System.err.println(sal);
        if (sal.size() >= 1 && sal.get(0).length() > 0) {
            String command = sal.get(0);
            switch (command) {
                case "b":
                    process_brk_b(sal);
                    break;
                case "g":
                    process_brk_g(sal);
                    break;
                case "i":
                    process_brk_i(sal);
                    break;
                case "m":
                    process_brk_m(sal);
                    break;
                case "t":
                    process_brk_t(sal);
                    break;
                case "q":
                    process_brk_q(sal);
                    break;
                case "x":
                    process_brk_x(sal);
                    break;
                default:
                    outputln("Unknown dbug brk command: " + command);
            }
        } else {
            myDBug.setOnBrk(false);
        }
    }

    private String badBrkCmd(StringArrayList sal) {
        StringBuilder sb = new StringBuilder("Bad brk command: ");
        for (String s : sal) {
            sb.append(s).append(' ');
        }
        return sb.toString();
    }

    private void process_brk_b(StringArrayList sal) {
        String brkpt = "";
        if (sal.size() >= 2) {
            brkpt = sal.get(1);
        }
        if (sal.size() == 2) {
            myDBug.setBreakpoint(brkpt);
            outputln("Brk set on " + brkpt);
        } else if (sal.size() == 3) {
            String toggle = sal.get(2);
            switch (toggle) {
                case "on":
                    myDBug.setBreakpoint(brkpt);
                    outputln("Brk set on " + brkpt);
                    break;
                case "off":
                    myDBug.clearBreakpoint(brkpt);
                    outputln("Brk cleared for " + brkpt);
                    break;
                default:
                    outputln(badBrkCmd(sal));
            }
        }
    }

    private void process_brk_g(StringArrayList sal) {
        myDBug.setStepping(false);
        myDBug.setOnBrk(false);
    }

    private void process_brk_i(StringArrayList sal) {
        outputln(myDBug.toString());
    }

    private void process_brk_m(StringArrayList sal) {
        outputln(myDBug.getHostInterpreter().getTupleMap().toString());
    }

    private void process_brk_t(StringArrayList sal) {
        if (sal.size() >= 2) {
            String tuplename = sal.get(1);
            Tuple t = myDBug.getTuple(tuplename);
            if (t != null) {
                outputln(t.toString());
            } else {
                outputln("tuple " + tuplename + " not found");

            }
        } else {
            outputln("no tuple name specified to brk t");
        }
    }

    private void process_brk_q(StringArrayList sal) {
        myDBug.quit();
        myDBug.setOnBrk(false);
    }

    private void process_brk_x(StringArrayList sal) {
        Interpreter i = new Interpreter(getHostInterpreter());
        sal.remove(0);
        i.executeBlock(sal.toBlock());
    }
}
