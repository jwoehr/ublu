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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * A pushable frame of Interpreter factors.
 *
 * @author jwoehr
 */
public class InterpreterFrame {

    private InputStream inputStream;
    private PrintStream outputStream;
    private PrintStream erroutStream;
    private boolean parsingString;
    private int parsingBlock = 0;
    private boolean including;
    private BufferedReader includeFileBufferedReader;
    private BufferedReader inputStreamBufferedReader;
    private ArgArray argArray;
    private boolean forBlock;
    private Path includePath;

    /**
     * ctor/0
     */
    public InterpreterFrame() {
    }

    /**
     * Instance an interpreter frame from its individual factors
     *
     * @param inputStream
     * @param outputStream
     * @param erroutStream
     * @param parsingString
     * @param parsingBlock
     * @param including
     * @param includeFileBufferedReader
     * @param inputStreamBufferedReader
     * @param argArray
     * @param includePath
     */
    public InterpreterFrame(InputStream inputStream, PrintStream outputStream, PrintStream erroutStream, boolean parsingString, int parsingBlock, boolean including, BufferedReader includeFileBufferedReader, BufferedReader inputStreamBufferedReader, ArgArray argArray, Path includePath) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.erroutStream = erroutStream;
        this.parsingString = parsingString;
        this.parsingBlock = parsingBlock;
        this.including = including;
        this.includeFileBufferedReader = includeFileBufferedReader;
        this.inputStreamBufferedReader = inputStreamBufferedReader;
        this.argArray = argArray;
        this.includePath = includePath;
        // this.forBlock = forBlock;
    }

    /**
     * Instance an interpreter frame from another of the same
     *
     * @param iFrame
     */
    public InterpreterFrame(InterpreterFrame iFrame) {
        this(iFrame.inputStream,
                iFrame.outputStream,
                iFrame.erroutStream,
                iFrame.parsingString,
                iFrame.parsingBlock,
                iFrame.including,
                iFrame.includeFileBufferedReader,
                iFrame.inputStreamBufferedReader,
                iFrame.argArray, // , iFrame.forBlock,
                iFrame.includePath);
    }

    /**
     *
     * @return the input stream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     *
     * @param inputStream the input stream
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     *
     * @return outputStream
     */
    public PrintStream getOutputStream() {
        return outputStream;
    }

    /**
     *
     * @param outputStream
     */
    public void setOutputStream(PrintStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     *
     * @return erroutStream
     */
    public PrintStream getErroutStream() {
        return erroutStream;
    }

    /**
     *
     * @param erroutStream
     */
    public void setErroutStream(PrintStream erroutStream) {
        this.erroutStream = erroutStream;
    }

    /**
     *
     * @return true if parsing String
     */
    public boolean isParsingString() {
        return parsingString;
    }

    /**
     *
     * @param parsingString
     */
    public void setParsingString(boolean parsingString) {
        this.parsingString = parsingString;
    }

    /**
     * Return true if parsing block depth &gt; 0
     *
     * @return true if parsing block depth &gt; 0
     */
    public boolean isParsingBlock() {
        return parsingBlock > 0;
    }

    /**
     * Return parsing block depth
     *
     * @return parsing block depth
     */
    public int getParsingBlockDepth() {
        return parsingBlock;
    }

    /**
     *
     * @param isParsingBlock
     */
    public void setParsingBlock(boolean isParsingBlock) {
        if (isParsingBlock) {
            ++this.parsingBlock;
        } else {
            this.parsingBlock = Math.max(0, this.parsingBlock - 1);
        }

    }

    /**
     *
     * @return true if including
     */
    public boolean isIncluding() {
        return including;
    }

    /**
     *
     * @param including
     */
    public void setIncluding(boolean including) {
        this.including = including;
    }

    /**
     * Get marker that this is a FOR block
     *
     * @return true if FOR block
     */
    public boolean isForBlock() {
        return forBlock;
    }

    /**
     * Set marker that this is a FOR block
     *
     * @param forBlock true if FOR block
     */
    public void setForBlock(boolean forBlock) {
        this.forBlock = forBlock;
    }

    /**
     *
     * @return includeFileBufferedReader
     */
    public BufferedReader getIncludeFileBufferedReader() {
        return includeFileBufferedReader;
    }

    /**
     *
     * @param includeFileBufferedReader
     */
    public void setIncludeFileBufferedReader(BufferedReader includeFileBufferedReader) {
        this.includeFileBufferedReader = includeFileBufferedReader;
    }

    /**
     *
     * @return inputStreamBufferedReader
     */
    public BufferedReader getInputStreamBufferedReader() {
        return inputStreamBufferedReader;
    }

    /**
     *
     * @param inputStreamBufferedReader
     */
    public void setInputStreamBufferedReader(BufferedReader inputStreamBufferedReader) {
        this.inputStreamBufferedReader = inputStreamBufferedReader;
    }

    /**
     *
     * @return argArray
     */
    public ArgArray getArgArray() {
        return argArray;
    }

    /**
     *
     * @param argArray
     */
    public void setArgArray(ArgArray argArray) {
        this.argArray = argArray;
    }

    /**
     *
     * @return includePath
     */
    public Path getIncludePath() {
        return includePath;
    }

    /**
     *
     * @param includePath
     */
    public void setIncludePath(Path includePath) {
        this.includePath = includePath;
    }
}
