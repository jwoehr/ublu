/*
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

import java.io.*;
import javax.swing.JEditorPane;

/**
 * A class to encapsulate writing to a JEditorPane as an an OutputStream.
 *
 * @author jwoehr *
 */
public class EditorPaneOutputStream extends OutputStream {

    /**
     * The text area we stream for
     */
    private final JEditorPane jEPane;

    /**
     * Arity/0 ctor.
     *
     * @param t TextArea we open on.
     */
    public EditorPaneOutputStream(JEditorPane t) {
        jEPane = t;
    }

    /**
     * Write a byte to the output stream.
     *
     * @param b byte to write
     */
    @Override
    public void write(int b) {
        // /* Debug */ System.err.println(this + ".write " + b);
        jEPane.setText(jEPane.getText() + (char) b);
    }

    /**
     * Write a byte array to the output stream.
     *
     * @param b byte array to write
     */
    @Override
    public void write(byte b[]) {
        jEPane.setText(jEPane.getText() + new String(b));
    }
}
