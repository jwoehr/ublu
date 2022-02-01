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

/**
 * Parser to take an input line and lex it into an {@link ArgArray}.
 *
 * @author jwoehr
 */
public class Parser {

    private Interpreter myInterpreter;
    private String input;

    /**
     * Get the string given us as input to parse.
     *
     * @return the string given us as input to parse
     */
    public String getInput() {
        return input;
    }

    /**
     * Set the string given us as input to parse.
     *
     * @param input the string given us as input to parse
     */
    public final void setInput(String input) {
        this.input = input;
    }

    /**
     * Instance with a string to parse.
     *
     * @param i associated Interpreter
     * @param input a string to parse
     */
    public Parser(Interpreter i, String input) {
        setInput(input);
        myInterpreter = i;
    }

    /**
     * Parse a string which is a line of input into a string array of lexes.
     *
     * @return a string array of lexes
     */
    public String[] parseALine() {
        String[] line = null;
        if (getInput() != null) {
            setInput(getInput().trim());
            line = getInput().split("\\p{Space}+");
        }
        return line;
    }

    /**
     * Parse our line of input into an ArgArray for use in the interpreter.
     *
     * @return an ArgArray for use in the interpreter
     */
    public ArgArray parseAnArgArray() {
        return new ArgArray(myInterpreter, parseALine());
    }
}
