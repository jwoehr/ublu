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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Class to represent string constants
 *
 * @author jax
 */
public class Const implements Serializable {

    private final String name;
    private final String value;

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void readObjectNoData()
            throws ObjectStreamException {
    }

    /**
     *
     */
    public static final String CONSTNAMECHAR = "*";

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getValue() {
        return value;
    }

    /**
     * ctor/2 creates new const with name and value
     *
     * @param name name
     * @param value value
     */
    public Const(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Checks if a name is a const name
     *
     * @param s the name
     * @return true if a legal const name
     */
    public static boolean isConstName(String s) {
        return s.startsWith(CONSTNAMECHAR);
    }
}
