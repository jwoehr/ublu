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
package ublu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A class to execute system functions from within Ublu
 *
 * @author jwoehr
 */
public class SystemHelper {

    /**
     * A class to hold the import result factors of a system command execution
     */
    public static class ProcessClosure {

        private final String output;
        private final int rc;

        /**
         * Get the text output returned by the command
         *
         * @return the text output returned by the command
         */
        public String getOutput() {
            return output;
        }

        /**
         * Get the return code from the command
         *
         * @return the return code from the command
         */
        public int getRc() {
            return rc;
        }

        /**
         * Instance with result factors from system command execution.
         *
         * @param output the text output of the command execution
         * @param rc the command execution return code
         */
        public ProcessClosure(String output, int rc) {
            this.output = output;
            this.rc = rc;
        }
    }

    /**
     * Execute a system command
     *
     * @param syscmd the command string
     * @return the resulting factors in a closure
     * @throws IOException
     * @throws InterruptedException
     */
    public static ProcessClosure sysCmd(String syscmd) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(syscmd);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        sb.append(line);
        line = reader.readLine();
        while (line != null) {
            sb.append("\n").append(line);
            line = reader.readLine();
        }
        return new ProcessClosure(sb.toString(), p.exitValue());
    }
}
