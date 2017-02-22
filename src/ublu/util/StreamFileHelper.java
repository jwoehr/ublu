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

import java.io.File;
import ublu.util.Generics.ByteArrayList;

/**
 * Manage local stream files assisting CmdStreamFile
 *
 * @author jax
 */
public class StreamFileHelper {

    private File file;

    /**
     *
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
     */
    public Object query(String q) {
        Object result = null;

        return result;
    }

    /**
     *
     * @param mode
     * @return
     */
    public Object open(String mode) {
        Object result = null;

        return result;
    }

    /**
     *
     */
    public void close() {
    }

    /**
     *
     * @param bal
     * @param length
     * @return
     */
    public Object write(ByteArrayList bal, int length) {
        Object result = null;

        return result;
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
}
