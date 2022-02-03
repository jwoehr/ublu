/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
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
package ublu.util;

import org.tn5250j.My5250;
import org.tn5250j.framework.common.SessionManager;
import org.tn5250j.tools.LangTool;
import ublu.util.Generics.Session5250ArrayList;
import ublu.util.Generics.StringArrayList;

/**
 * A class to encapsulate tn5250j
 *
 * @author jwoehr
 */
public class TN5250Helper extends Thread {

    private My5250 my5250;

    /**
     * Return subapp instance
     *
     * @return subapp instance
     */
    public My5250 getMy5250() {
        return my5250;
    }

    /**
     * Get subapp instance SessionManage
     *
     * @return subapp instance SessionManage
     */
    public SessionManager getSessionManager() {
        return my5250.getSessionManager();
    }
    //////////////////////////////
    // Stuff for connect
    //////////////////////////////
    private String system;
//    private String user;
//    private String password;
    private Generics.StringArrayList args;

    /**
     * Args to the subapp invocation
     *
     * @return Args to the subapp invocation
     */
    public StringArrayList getArgs() {
        return args;
    }

    /**
     * Set Args to the subapp invocation
     *
     * @param args Args to the subapp invocation
     */
    public void setArgs(StringArrayList args) {
        this.args = args;
    }

    /**
     * Instance with an empty array of args
     */
    public TN5250Helper() {
        args = new Generics.StringArrayList();
    }

    /**
     * Instance with a system name and an array of args
     *
     * @param system system name or ip
     * @param args array of args for subapp invocation
     */
    public TN5250Helper(String system, StringArrayList args) {
        this();
        this.system = system;
        this.args = args;
    }

    /**
     *
     * @param system
     */
    public TN5250Helper(String system) {
        this();
        this.system = system;
    }

    @Override
    public void run() {
        StringArrayList sal = new StringArrayList(system);
        sal.addAll(args);
        my5250 = new My5250();
        /* Debug */ System.out.println("args to tn5250 : " + args);
        LangTool.init();
        my5250.startNewUbluSession(sal.toStringArray());
    }

    /**
     * Return list of Sessions in tn5250
     *
     * @return list of Sessions in tn5250
     */
    public Session5250ArrayList getSessionList() {
        return new Session5250ArrayList(getSessionManager().getSessions().getSessionsList());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString()).append(" ");
        sb.append("System: ").append(system).append(" ");
        sb.append("Args : ").append(args).append(" ");
        return sb.toString();
    }

    /**
     * Thread which can run an independent regular TN5250J instance
     *
     * @author jwoehr
     */
    public static class My5250Thread extends Thread {

        /**
         * Create an instance of the full TN5250J emulator. Calls System.exit()
         * when it closes!
         *
         * @param name Name of thread
         * @param args Args to the emulator
         */
        public My5250Thread(String name, String[] args) {
            super(name);
            this.args = args;
        }
        private String[] args = new String[0];

        @Override
        public void run() {
            My5250.main(args);
        }
    }
}
