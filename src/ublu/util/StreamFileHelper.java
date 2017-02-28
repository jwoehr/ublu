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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import ublu.util.Generics.ByteArrayList;
import ublu.util.Generics.StringArrayList;

/**
 * Manage local stream files assisting CmdStreamFile
 *
 * @author jax
 */
public class StreamFileHelper {

    /**
     *
     */
    public enum MODE {

        /**
         *
         */
        RB,
        /**
         *
         */
        RC,
        /**
         *
         */
        W,
        /**
         *
         */
//        WB,
        /**
         *
         */
//        WC
    }

    private File file;
    private FileInputStream fileInputStream;
    private BufferedInputStream bufferedInputStream;
    private FileReader fileReader;
    private BufferedReader bufferedReader;
    private FileOutputStream fileOutputStream;

    /**
     *
     * @return
     */
    public File getFile() {
        return file;
    }

    /**
     *
     * @return @throws IOException
     */
    public Boolean create() throws IOException {
        return file.createNewFile();
    }

    /**
     *
     * @throws FileNotFoundException
     */
    public void setUpToReadBinary() throws FileNotFoundException {
        fileInputStream = new FileInputStream(file);
        bufferedInputStream = new BufferedInputStream(fileInputStream);
    }

    /**
     *
     * @throws FileNotFoundException
     */
    public void setUpToReadCharacter() throws FileNotFoundException {
        fileReader = new FileReader(file);
        bufferedReader = new BufferedReader(fileReader);
    }

    /**
     *
     * @throws FileNotFoundException
     */
    public void setUpToWrite() throws FileNotFoundException {
        fileOutputStream = new FileOutputStream(file);
    }

    /**
     * Ctor/0
     */
    public StreamFileHelper() {
    }

    /**
     *
     * @param file
     */
    public StreamFileHelper(File file) {
        this();
        this.file = file;
    }

    /**
     *
     * @param fqp
     */
    public StreamFileHelper(String fqp) {
        this();
        file = new File(fqp);
    }

    /**
     *
     * @param q
     * @return
     * @throws java.io.IOException
     */
    public Object query(String q) throws IOException {
        Object result = null;
        switch (q) {
            case "af":
                result = file.getAbsoluteFile();
                break;
            case "ap":
                result = file.getAbsolutePath();
                break;
            case "c":
                result = file.getCanonicalPath();
                break;
            case "d":
                result = file.isDirectory();
                break;
            case "e":
                result = file.exists();
                break;
            case "f":
                result = file.isFile();
                break;
            case "length":
                result = file.length();
                break;
            case "n":
                result = file.getName();
                break;
            case "p":
                result = file.getPath();
                break;
            case "r":
                result = file.canRead();
                break;
            case "w":
                result = file.canWrite();
                break;
            case "x":
                result = file.canExecute();
                break;
        }
        return result;
    }

    /**
     *
     * @return
     */
    public boolean mkdir() {
        return file.mkdir();
    }

    /**
     *
     * @return
     */
    public boolean mkdirs() {
        return file.mkdirs();
    }

    /**
     *
     * @param mode
     * @throws java.io.FileNotFoundException
     */
    public void open(MODE mode) throws FileNotFoundException {
        switch (mode) {
            case RB:
                setUpToReadBinary();
                break;
            case RC:
                setUpToReadCharacter();
                break;
            case W:
                setUpToWrite();
                break;
//            case WB:
//                break;
//            case WC:
//                break;
        }
    }

    /**
     *
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        if (bufferedInputStream != null) {
            bufferedInputStream.close();
            bufferedInputStream = null;
        }
        if (fileInputStream != null) {
            fileInputStream.close();
            fileInputStream = null;
        }
        if (bufferedReader != null) {
            bufferedReader.close();
            bufferedReader = null;
        }
        if (fileReader != null) {
            fileReader.close();
            fileReader = null;
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
            fileOutputStream = null;
        }
    }

    /**
     *
     * @param bal
     * @param offset
     * @param length
     * @throws java.io.IOException
     */
    public void write(ByteArrayList bal, int offset, int length) throws IOException {
        fileOutputStream.write(bal.byteArray(), offset, length);
    }

    /**
     *
     * @param b
     * @param offset
     * @param length
     * @throws java.io.IOException
     */
    public void write(byte[] b, int offset, int length) throws IOException {
        fileOutputStream.write(b, offset, length);
    }

    /**
     *
     * @param offset
     * @param length
     * @return
     */
    public ByteArrayList read(int offset, int length) {
        ByteArrayList bal = null;

        return bal;
    }

    /**
     *
     * @return @throws IOException
     */
    public String readLine() throws IOException {
        String result = bufferedReader.readLine();
        return result;
    }

    /**
     *
     * @return @throws IOException
     */
    public StringArrayList readAllLines() throws IOException {
        return new StringArrayList(Files.readAllLines(file.toPath()));
    }

    /**
     *
     * @return @throws IOException
     */
    public ByteArrayList readAllBytes() throws IOException {
        return new ByteArrayList(Files.readAllBytes(file.toPath()));
    }

    /**
     *
     * @throws IOException
     */
    public void reset() throws IOException {
        bufferedReader.reset();
    }

    /**
     *
     * @param readAheadLimit
     * @throws IOException
     */
    public void mark(int readAheadLimit) throws IOException {
        bufferedReader.mark(readAheadLimit);
    }

    /**
     *
     * @param n
     * @throws IOException
     */
    public void skip(long n) throws IOException {
        bufferedReader.skip(n);
    }

}
