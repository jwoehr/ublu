/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
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
package ublu.util;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A socket server for access to an Ublu interpreter thread
 *
 * @author jwoehr
 */
public class InterpreterThread extends Thread {

    private Interpreter interpreter;

    /**
     * Get private Interpreter instance for thread
     *
     * @return private Interpreter instance
     */
    public final Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     * Get private Interpreter instance for thread
     *
     * @param interpreter the private Interpreter instance for thread
     */
    public final void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * Associated logger
     *
     * @return Associated logger
     */
    protected Logger getLogger() {
        return getInterpreter().getMyUblu().getLogger();
    }

    /**
     * Arity\0 ctor sets a thread name
     */
    protected InterpreterThread() {
        super("Ublu Interpreter Thread");
    }

    /**
     * ctor/2 from source interpreter which launched this one plus args. Copies
     * in deep the tuple map from the source.
     *
     * @param srcInterpreter private Interpreter instance for thread
     * @param args program for the thread to interpret
     */
    public InterpreterThread(Interpreter srcInterpreter, String args) {
        this();
        setInterpreter(new Interpreter(srcInterpreter, args));
        getInterpreter().pushFrame().pushLocal();
    }

    /**
     * loop through code until exhausted
     *
     * @throws IOException
     */
    protected void threadedInterpret() throws IOException {
        getInterpreter().loop();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass()).append(" || ");
        sb.append("Name: ").append(getName()).append(" || ");
        sb.append("ID: ").append(getId()).append(" || ");
        sb.append("Priority: ").append(getPriority()).append(" || ");
        sb.append("State: ").append(getState()).append(" || ");
        sb.append("ThreadGroup: ").append(this.getThreadGroup()).append(" || ");
        sb.append("Alive: ").append(this.isAlive()).append(" || ");
        sb.append("Daemon: ").append(this.isDaemon()).append(" || ");
        sb.append("Interrupted: ").append(this.isInterrupted()).append(" ||");
        return sb.toString();
    }

    @Override
    public void run() {
        try {
            threadedInterpret();
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error in threaded interpret.", ex);
        }
    }
}
