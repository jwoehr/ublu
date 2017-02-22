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
package ublu.util;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400FTP;
import com.ibm.as400.access.AS400File;
import com.ibm.as400.access.DataArea;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.FTP;
import com.ibm.as400.access.HistoryLog;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.JobList;
import com.ibm.as400.access.JobLog;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDescription;
import com.ibm.as400.access.ObjectList;
import com.ibm.as400.access.OutputQueue;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.Printer;
import com.ibm.as400.access.QueuedMessage;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.SaveFile;
import com.ibm.as400.access.SecureAS400;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.SpooledFileList;
import com.ibm.as400.access.Subsystem;
import com.ibm.as400.access.User;
import com.ibm.as400.access.UserList;
import java.net.Socket;
import java.sql.CallableStatement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import ublu.AS400Extender;
import ublu.SecureAS400Extender;
import ublu.db.Db;
import ublu.db.ResultSetClosure;
import ublu.server.Listener;
import ublu.smapi.Host;
import ublu.util.Generics.ThingArrayList;

/**
 * Class to provide autonomization of tuple variables, that is, providing Ublu
 * command strings relevant to the class type of the variable value so variables
 * can auto-execute.
 *
 * @author jax
 */
public class Autonome {

    /**
     * The map of class types and their relevant Ublu commands
     */
    public static final LinkedHashMap<Class, String> AUTONOMY;

    static {
        AUTONOMY = new LinkedHashMap<>();
        AUTONOMY.put(AS400.class, "as400");
        AUTONOMY.put(SecureAS400.class, "as400");
        AUTONOMY.put(AS400Extender.class, "as400");
        AUTONOMY.put(SecureAS400Extender.class, "as400");
        AUTONOMY.put(CallableStatement.class, "cs");
        AUTONOMY.put(CimUbluHelper.class, "cim");
        AUTONOMY.put(Db.class, "db");
        AUTONOMY.put(SysShepHelper.class, "dpoint");
        AUTONOMY.put(DataQueue.class, "dq");
        AUTONOMY.put(DataArea.class, "dta");
        AUTONOMY.put(AS400File.class, "file");
        AUTONOMY.put(FTP.class, "ftp");
        AUTONOMY.put(AS400FTP.class, "ftp");
        AUTONOMY.put(HistoryLog.class, "histlog");
        AUTONOMY.put(Host.class, "host");
        AUTONOMY.put(IFSFile.class, "ifs");
        AUTONOMY.put(JMXHelper.class, "jmx");
        AUTONOMY.put(Job.class, "job");
        AUTONOMY.put(JobList.class, "joblist");
        AUTONOMY.put(JobLog.class, "joblog");
        AUTONOMY.put(JournalHelper.class, "jrnl");
        AUTONOMY.put(JSONObject.class, "json");
        AUTONOMY.put(JSONArray.class, "json");
        AUTONOMY.put(ThingArrayList.class, "list");
        AUTONOMY.put(QueuedMessage.class, "msg");
        AUTONOMY.put(MessageQueue.class, "msgq");
        AUTONOMY.put(ObjectList.class, "objlist");
        AUTONOMY.put(ObjectDescription.class, "objdesc");
        AUTONOMY.put(OutputQueue.class, "outq");
        AUTONOMY.put(PrintParameterList.class, "ppl");
        AUTONOMY.put(Printer.class, "printer");
        AUTONOMY.put(Record.class, "record");
        AUTONOMY.put(ResultSetClosure.class, "rs");
        AUTONOMY.put(SaveFile.class, "savef");
        AUTONOMY.put(Listener.class, "server");
        AUTONOMY.put(Sess5250.class, "sess");
        AUTONOMY.put(Socket.class, "sock");
        AUTONOMY.put(SpooledFile.class, "spoolf");
        AUTONOMY.put(SpooledFileList.class, "spoolflist");
        AUTONOMY.put(StreamFileHelper.class, "streamf");
        AUTONOMY.put(String.class, "string");
        AUTONOMY.put(Subsystem.class, "subsys");
        AUTONOMY.put(SysValHelper.class, "sysval");
        AUTONOMY.put(InterpreterThread.class, "thread");
        AUTONOMY.put(TN5250Helper.class, "tn5250");
        AUTONOMY.put(User.class, "user");
        AUTONOMY.put(UserList.class, "userlist");
    }

    /**
     * Get an ublu command string from the class type
     *
     * @param c the class type
     * @return the applicable ublu command string
     */
    public static String get(Class c) {
        return AUTONOMY.get(c);
    }

    /**
     * Create a string of all the autonomes
     *
     * @return a string of all the autonomes
     */
    public static String displayAll() {
        StringBuilder sb = new StringBuilder();
        Set s = AUTONOMY.keySet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            Class c = Class.class.cast(i.next());
            sb.append(get(c))
                    .append(" : ")
                    .append(c.toString())
                    .append('\n');
        }
        return sb.toString();
    }

    /**
     * Return description indicating command if the class of the object is
     * autonomic
     *
     * @param o object whose class type to test
     * @return class name and command it invokes, null command if not autonomic
     */
    public static String autonomeDescription(Object o) {
        String autonomic = null;
        if (o != null) {
            autonomic = o.getClass().toString() + " : " + get(o.getClass());
        }
        return autonomic;
    }

    /**
     * True if the class of the object is autonomic
     *
     * @param o object whose class type to test
     * @return class name and command it invokes
     */
    public static boolean isAutonomic(Object o) {
        boolean autonomic = false;
        if (o != null) {
            autonomic = null != get(o.getClass());
        }
        return autonomic;
    }

    /**
     * Prepend in the arg array the applicable Ublu command for the class type.
     *
     * @param t The autonomic tuple
     * @param aa The arg array
     * @return True if was autonomized, false if non-autonomizable
     */
    public static boolean autonomize(Tuple t, ArgArray aa) {
        boolean result = false;
        if (t != null) {
            Object o = t.getValue();
            if (o != null) {
                Class c = o.getClass();
                String s = AUTONOMY.get(c);
                if (s == null) {
                    for (Class sup : AUTONOMY.keySet()) {
                        if (sup.isAssignableFrom(c)) {
                            s = AUTONOMY.get(sup);
                        }
                    }
                }
                if (s != null) {
                    aa.add(0, "--");
                    aa.add(0, s);
                    result = true;
                }
            }
        }
        return result;
    }
}
