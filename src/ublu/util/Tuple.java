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
 * Tuple variable implementation.
 * <p>The Ublu interpreter keeps associative memory in the form of these
 * Tuples of string value / object pairs.</p>
 *
 * @author jwoehr
 */
public class Tuple {

    /**
     * First char in a tuplename
     */
    public static char TUPLECHAR = '@';
    /**
     * First chars in a ParamSubTuplename
     */
    public static String PARAMSUBTUPLECHARS = "@///";
    /**
     * key of the Tuple
     */
    protected String key;
    /**
     * object value of the Tuple
     */
    protected Object value;

    /**
     * get the key
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * set the key
     *
     * @param key the key
     */
    protected void setKey(String key) {
        this.key = key;
    }

    /**
     * get the object value
     *
     * @return the object value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the object value
     *
     * @param value the object value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * ctor/1 instance with only key
     *
     * @param key
     */
    public Tuple(String key) {
        this.key = key;
    }

    /**
     * ctor/2 Instance with key and value
     *
     * @param key
     * @param value
     */
    public Tuple(String key, Object value) {
        this(key);
        this.value = value;
    }

    /**
     * Get the Java class of the value object
     *
     * @return Java class of the value object
     */
    public Class valueClass() {
        return getValue().getClass();
    }

    /**
     * For the class Tuple, returns the key. In Tuple subclass ParamSubTuple,
     * returns the key of the bound tuple.
     *
     * @return key of bound tuple
     */
    public String getBoundKey() {
        return getKey();
    }

    /**
     * Get the key proposed for a possibly nonexistent tuple If it's a
     * paramsubtuple it will return the notional key If it's a "real" tuple it
     * will return its own key.
     *
     * @return
     */
    public String getProposedKey() {
        return getKey();
    }

    /**
     * For the class Tuple, returns this. In Tuple subclass ParamSubTuple,
     * returns the bound tuple.
     *
     * @return bound tuple
     */
    public Tuple getBoundTuple() {
        return this;
    }

    /**
     * Returns true if name is decorated as a tuple name
     *
     * @param name possible tuple name
     * @return true if name is decorated as a tuple name
     */
    public static boolean isTupleName(String name) {
        return name.charAt(0) == TUPLECHAR;
    }

    /**
     * Returns true if name is decorated as a paramsubtuple name
     *
     * @param name possible tuple name
     * @return true if name is decorated as a paramsubtuple name
     */
    public static boolean isParamSubTupleName(String name) {
        return name.startsWith(PARAMSUBTUPLECHARS);
    }
    
    public String toString () {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" key=\"").append(getKey()).append("\" value=\"").append(getValue()).append("\"");
        return sb.toString();
    }
}
