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
package ublu.win;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import ublu.Ublu;
import ublu.command.CommandInterface.COMMANDRESULT;
import ublu.util.Interpreter;
import ublu.util.Parser;

/**
 *
 * @author jax
 */
public class UbluWinController {

    /**
     *
     */
    protected UbluFrame ubluFrame;

    /**
     *
     */
    protected Interpreter interpreter;

    /**
     *
     */
    protected Ublu ublu;

    /**
     *
     */
    protected TextAreaOutputStream ubluTAOS;

    /**
     *
     * @return
     */
    public TextAreaOutputStream getUbluTAOS() {
        return ubluTAOS;
    }

    /**
     *
     * @param ubluTAOS
     */
    public void setUbluTAOS(TextAreaOutputStream ubluTAOS) {
        this.ubluTAOS = ubluTAOS;
    }

    /**
     *
     */
    protected UbluWinInputStream ubluIS;

    /**
     *
     * @return
     */
    public Ublu getUblu() {
        return ublu;
    }

    /**
     *
     * @return
     */
    public UbluFrame getUbluFrame() {
        return ubluFrame;
    }

    /**
     *
     * @return
     */
    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     *
     * @return
     */
    protected UbluWinInputStream getUbluIS() {
        return ubluIS;
    }

    /**
     *
     * @param interpreter
     */
    public UbluWinController(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
     *
     * @param ublu
     */
    public UbluWinController(Ublu ublu) {
        this.ublu = ublu;
        this.interpreter = Ublu.getMainInterpreter();
    }

    /**
     *
     * @param input
     * @return
     */
    protected COMMANDRESULT interpretText(String input) {
        COMMANDRESULT result;
        input = input.trim();
        interpreter.setArgArray(new Parser(interpreter, input).parseAnArgArray());
        result = interpreter.loop();
        return result;
    }

    /**
     *
     */
    public void startup() {
        ubluFrame = new UbluFrame();
        ubluFrame.setUbluWinController(this);
        // ubluSOE = new EditorPaneOutputStream(ubluFrame.getUbluTextArea());
        ubluTAOS = ubluFrame.getUbluPanel().getjTAOS();
        ubluIS = new UbluWinInputStream();
        interpreter.setInputStream(ubluIS);
        interpreter.setInputStreamBufferedReader(new BufferedReader(new InputStreamReader(interpreter.getInputStream())));
        interpreter.setOutputStream(new PrintStream(ubluTAOS));
        interpreter.setErroutStream(new PrintStream(ubluTAOS));
        ublu.reinitLogger(new PrintStream(ubluTAOS));
        ubluFrame.runMe();
        ubluFrame.getUbluTextArea().setText(Ublu.startupMessage() + '\n');
        ubluFrame.getUbluPanel().getUbluTextField().requestFocusInWindow();
    }
}
