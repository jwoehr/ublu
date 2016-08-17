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

import ublu.util.Interpreter;
import java.util.LinkedHashMap;
import java.util.logging.Level;

/**
 * Defines a dictionary of commands for the interpreter in
 * {@link ublu.util.Interpreter}.
 *
 * @author jwoehr
 */
public class CommandMap extends LinkedHashMap<String, Class<? extends CommandInterface>> {

    /**
     * Instance a command map of all commands available
     *
     */
    public CommandMap() {
        put("as400", CmdAS400.class);
        put("ask", CmdAsk.class);
        put("BREAK", CmdBreak.class);
        put("bye", CmdBye.class);
        put("CALL", CmdCall.class);
        put("calljava", CmdCallJava.class);
        put("collection", CmdCollection.class);
        put("commandcall", CmdCommandCall.class);
        put("const", CmdConst.class);
        put("db", CmdDb.class);
        put("dbug", CmdDbug.class);
        put("defun", CmdDefun.class);
        put("dict", CmdDict.class);
        put("DO", CmdDo.class);
        put("dpoint", CmdDPoint.class);
        put("dq", CmdDq.class);
        put("ELSE", CmdElse.class);
        put("eval", CmdEval.class);
        put("exit", CmdExit.class);
        put("FOR", CmdFor.class);
        put("ftp", CmdFTP.class);
        put("FUN", CmdFun.class);
        put("FUNC", CmdFunc.class);
        put("gensh", CmdGenSh.class);
        put("help", CmdUsage.class);
        put("histlog", CmdHistoryLog.class);
        put("h", CmdHistory.class);
        put("host", CmdHost.class);
        put("history", CmdHistory.class);
        put("IF", CmdIf.class);
        put("ifs", CmdIFS.class);
        put("include", CmdInclude.class);
        put("interpret", CmdInterpret.class);
        put("jmx", CmdJMX.class);
        put("job", CmdJob.class);
        put("joblist", CmdJobList.class);
        put("jrnl", CmdJournal.class);
        put("jvm", CmdJVM.class);
        put("LOCAL", CmdLocal.class);
        put("license", CmdLicense.class);
        put("lifo", CmdLifo.class);
        put("list", CmdList.class);
        put("monitor", CmdMonitor.class);
        put("msg", CmdMsg.class);
        put("msgq", CmdMsgQ.class);
        put("num", CmdNumber.class);
        put("objlist", CmdObjList.class);
        put("objdesc", CmdObjDesc.class);
        put("outq", CmdOutQ.class);
        put("ppl", CmdPpl.class);
        put("printer", CmdPrinter.class);
        put("programcall", CmdProgramCall.class);
        put("props", CmdProps.class);
        put("put", CmdPut.class);
        put("RETURN", CmdReturn.class);
        put("rs", CmdRs.class);
        put("savf", CmdSavF.class);
        put("server", CmdServer.class);
        put("sess", CmdSession.class);
        put("session", CmdSession.class);
        put("sleep", CmdSleep.class);
        put("smapi", CmdSmapi.class);
        put("spoolf", CmdSpoolF.class);
        put("spoolflist", CmdSpoolFList.class);
        put("string", CmdString.class);
        put("subsys", CmdSubSystem.class);
        put("SWITCH", CmdSwitch.class);
        put("system", CmdSystem.class);
        put("sysval", CmdSysVal.class);
        put("TASK", CmdTask.class);
        put("test", CmdTest.class);
        put("thread", CmdThread.class);
        put("THEN", CmdThen.class);
        put("THROW", CmdThrow.class);
        put("tn5250", CmdTN5250.class);
        put("trace", CmdTrace.class);
        put("TRY", CmdTry.class);
        put("tuple", CmdTuple.class);
        put("usage", CmdUsage.class);
        put("user", CmdUser.class);
        put("userlist", CmdUserList.class);
        put("WHILE", CmdWhile.class);
        put("!", CmdBang.class);
        put("#", CmdComment.class);
        put("#!", CmdComment.class);
        put("\\\\", CmdCommentQuote.class);
    }

    /**
     * Instance a command class by name
     *
     * @param i the controlling Interpreter
     * @param name the class name
     * @return instance of the class or null
     */
    public CommandInterface getCmd(Interpreter i, String name) {
        Class<? extends CommandInterface> c = get(name);
        /* Debug */ // System.out.println("Command interface object is " + c);
        Command command = null;
        if (c != null) {
            try {
                command = Command.class.cast(c.newInstance());
                command.setInterpreter(i);
            } catch (IllegalArgumentException | InstantiationException | IllegalAccessException | SecurityException ex) {
                i.getLogger().log(Level.SEVERE, "Exception instancing command", ex);
            }
        }
        return command;
    }
}
