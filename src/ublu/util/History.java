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

import ublu.util.Generics.StringArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * Create and maintain a history file
 *
 * @author jwoehr
 */
public class History implements Closeable {

    private static final Logger LOG = Logger.getLogger(History.class.getName());
    /**
     * default History File Name
     */
    public static final String DEFAULT_HISTORY_FILENAME = "history.ublu";
    /**
     * current history File Name
     */
    protected String historyFileName = DEFAULT_HISTORY_FILENAME;
    /**
     * Writes the lines
     */
    protected BufferedWriter writer;
    
    private Interpreter myInterpreter;

    /**
     * Get the writer of lines
     *
     * @return writer of lines or null
     */
    public BufferedWriter getWriter() {
        return writer;
    }

    /**
     * Set the writer of lines
     *
     * @param writer the writer of lines or null
     */
    protected final void setWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    /**
     * Get history file name
     *
     * @return history file name
     */
    public String getHistoryFileName() {
        return historyFileName;
    }

    /**
     * Set history file name
     *
     * @param historyFileName history file name
     */
    protected final void setHistoryFileName(String historyFileName) {
        this.historyFileName = historyFileName;
    }

    /**
     * Instance with the default history file name
     *
     * @throws IOException
     */
    public History(Interpreter interpreter) throws IOException {
        this.myInterpreter=interpreter;
        setHistoryFileName(DEFAULT_HISTORY_FILENAME);
        fetchWriter();
    }

    /**
     * Instance with a filename
     *
     * @param filename name of history file
     * @throws IOException
     */
    public History(Interpreter interpreter, String filename) throws IOException {
        this.myInterpreter=interpreter;
        setHistoryFileName(filename);
        fetchWriter();
    }

    /**
     * close the file and discard the writer
     *
     * @throws IOException
     */
    protected final void closeWriter() throws IOException {
        if (getWriter() != null) {
            getWriter().flush();
            getWriter().close();
            setWriter(null);
        }
    }

    /**
     * instance the writer from the history filename
     *
     * @throws IOException
     */
    protected final void fetchWriter() throws IOException {
        closeWriter();
        setWriter(Files.newBufferedWriter(FileSystems.getDefault().getPath(historyFileName), StandardCharsets.US_ASCII, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
    }

    /**
     * Write a line of history to the history file
     *
     * @param line line of history
     * @throws IOException
     */
    public void writeLine(String line) throws IOException {
        if (getWriter() != null) {
            getWriter().write(line + "\n");
            writer.flush();
        }
    }

    /**
     * Close old writer and start again with a new filename
     *
     * @param newfilename new filename
     * @throws IOException
     */
    public void reinstance(String newfilename) throws IOException {
        closeWriter();
        setHistoryFileName(newfilename);
        fetchWriter();
    }

    @Override
    public void close() throws IOException {
        closeWriter();
    }

    /**
     * Read the history file into a string array list
     *
     * @return list of history lines unnumbered
     * @throws IOException
     */
    protected StringArrayList readHistory() throws IOException {
        StringArrayList sal = new StringArrayList();
        BufferedReader br = Files.newBufferedReader(FileSystems.getDefault().getPath(historyFileName), StandardCharsets.US_ASCII);
        while (br.ready()) {
            sal.add(br.readLine());
        }
        br.close();
        return sal;
    }

    /**
     * Build a string containing the first n lines of history
     *
     * @param numlines number of history lines starting at top including first
     * @param numbered true if lines should appear numbered
     * @return String representing the history lines
     * @throws IOException
     */
    public String head(int numlines, boolean numbered) throws IOException {
        StringArrayList sal = readHistory();
        int numActualLines = Math.min(numlines, sal.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numActualLines; i++) {
            if (numbered) {
                sb.append(i + 1).append(" ");
            }
            sb.append(sal.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Build a string containing the last n lines of history
     *
     * @param numlines number of history lines above last including last
     * @param numbered true if lines should appear numbered
     * @return String representing the history lines
     * @throws IOException
     */
    public String tail(int numlines, boolean numbered) throws IOException {
        StringArrayList sal = readHistory();
        int numActualLines = Math.min(numlines, sal.size());
        StringBuilder sb = new StringBuilder();
        for (int i = sal.size() - numActualLines; i < sal.size(); i++) {
            if (numbered) {
                sb.append(i + 1).append(" ");
            }
            sb.append(sal.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Build a string containing the lines of all history
     *
     * @param numbered true if lines should appear numbered
     * @return String representing the history lines
     * @throws IOException
     */
    public String show(boolean numbered) throws IOException {
        StringArrayList sal = readHistory();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sal.size(); i++) {
            if (numbered) {
                sb.append(i + 1).append(" ");
            }
            sb.append(sal.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Get a particular line of history
     *
     * @param n the zero-based line number
     * @return the string representing the line of history
     * @throws IOException
     */
    public String nth(int n) throws IOException {
        String result = null;
        StringArrayList sal = readHistory();
        if (sal.size() > n) {
            result = sal.get(n);
        }
        return result;
    }

    /**
     * Get number of history lines.
     *
     * @return number of history lines
     * @throws IOException
     */
    public int lines() throws IOException {
        StringArrayList sal = readHistory();
        return sal.size();
    }

    /**
     * Return a range of history as a String of lines. The lines are all lines
     * between first and last, inclusive.
     *
     * @param first zero-based index of first line
     * @param last zero-based index of last line
     * @param numbered true if lines should appear numbered
     * @return range of history as a String of lines
     * @throws IOException
     */
    public String range(int first, int last, boolean numbered) throws IOException {
        StringArrayList sal = readHistory();
        StringBuilder sb = new StringBuilder();
        if (first >= 0 && last >= 0 && first <= last) {
            for (int i = first; i <= last; i++) {
                if (numbered) {
                    sb.append(i + 1).append(" ");
                }
                sb.append(sal.get(i)).append("\n");
            }
        }
        return sb.toString();
    }
}
