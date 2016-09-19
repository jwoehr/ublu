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
package ublu.util;

import java.io.PrintStream;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A special logger for our interpreter
 *
 * @author jwoehr
 */
public class InterpreterLogger extends Logger {

    /**
     * The stream we log to.
     */
    protected PrintStream errOutStream = System.err;

    /**
     * Get the stream we log to.
     *
     * @return The stream we log to.
     */
    public PrintStream getErrOutStream() {
        return errOutStream;
    }

    /**
     * Set the stream we log to.
     *
     * @param errOutStream The stream we log to.
     */
    public void setErrOutStream(PrintStream errOutStream) {
        this.errOutStream = errOutStream;
    }

    /**
     * Create with a parent and with an err stream
     *
     * @param name
     * @param parent
     * @param errOutStream
     */
    public InterpreterLogger(String name, Logger parent, PrintStream errOutStream) {
        super(name, null);
        setParent(parent);
    }

    @Override
    public void log(LogRecord record) {
        StringBuilder sb = new StringBuilder("Ublu:");
        sb.append(record.getThreadID()).append(":");
        sb.append(record.getLoggerName()).append(":");
        sb.append(record.getLevel()).append(":");
        sb.append(record.getSourceClassName()).append(".");
        sb.append(record.getSourceMethodName()).append("():");
        String message = record.getMessage();
        Object[] params = record.getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    String s = params[i].toString();
                    if (!s.isEmpty()) {
                        while (message.contains("{" + i + "}")) {
                            message = message.replace("{" + i + "}", s);
                        }
                    }
                }
            }
        }
        sb.append(message).append("\n");
        getErrOutStream().print(sb.toString());
        Throwable t = record.getThrown();
        if (t != null) {
            getErrOutStream().println(t.getLocalizedMessage());
            t.printStackTrace(getErrOutStream());
        }
    }
}
