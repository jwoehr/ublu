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
 * Represents textually the data source or destination used by
 * {@link ublu.command.Command} extenders.
 *
 * @author jwoehr
 */
public class DataSink {

    /**
     * The type of the data source or destination used by a
     * {@link ublu.command.Command} extender.
     * <p> The types of data sink are:
     * <ul>
     * <li>file - a text file </li>
     * <li>url - a hyperlink (not currently used)</li>
     * <li>tuple - an object variable - see {@link ublu.util.Tuple}</li>
     * <li>stdio</li>
     * </ul>
     * </p>
     */
    public static enum SINKTYPE {

        /**
         * A file
         */
        FILE,
        /**
         * A URL
         */
        URL,
        /**
         * A Tuple
         */
        TUPLE,
        /**
         * STDIO
         */
        STD,
        /**
         * null device i.e., nowhere
         */
        NUL
    }
    private SINKTYPE type;
    private String name;

    /**
     *
     * @param name
     * @return
     */
    public static SINKTYPE sinkNameType(String name) {
        SINKTYPE sinkType;
        if (name.startsWith("@")) {
            sinkType = SINKTYPE.TUPLE;
        } else if (name.startsWith("http://")) {
            sinkType = SINKTYPE.URL;
        } else if (name.equals("NULL:")) {
            sinkType = SINKTYPE.NUL;
        } else if (name.equals("STD:")) {
            sinkType = SINKTYPE.STD;
        } else {
            sinkType = SINKTYPE.FILE;
        }
        return sinkType;
    }

    /**
     * Factory method creating an instance from the name by inferring the type
     * from the name.
     * <p>
     * The name implies the type:
     * <ul>
     * <li>{@code @name} is a tuple name</li>
     * <li>{@code name} is a file name</li>
     * <li>{@code http://name} is a URL</li>
     * </ul>
     * </p>
     *
     * @param name name of the sink, i.e., file name, tuple name, url
     * @return the new DataSink
     */
    public static DataSink fromSinkName(String name) {
        return new DataSink(sinkNameType(name), name);
    }

    /**
     * Return a file data sink named by the string value of a Tuple's value.
     *
     * @param t the tuple
     * @return the data sink
     */
    public static DataSink fileSinkFromTuple(Tuple t) {
        return new DataSink(DataSink.SINKTYPE.FILE, t.getValue().toString());
    }

    /**
     * Get the sink type of the instance
     *
     * @return the sink type of the instance
     */
    public SINKTYPE getType() {
        return type;
    }

    /**
     * set the sink type of the instance
     *
     * @param type the sink type of the instance
     */
    public final void setType(SINKTYPE type) {
        this.type = type;
    }

    /**
     * Get the name of the instance
     *
     * @return the name of the instance public String getName() { return name; }
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the instance
     *
     * @param name the name of the instance
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * Instance from a name and type
     *
     * @param type type of sink
     * @param name name of sink
     */
    public DataSink(SINKTYPE type, String name) {
        setType(type);
        setName(name);
    }
}
