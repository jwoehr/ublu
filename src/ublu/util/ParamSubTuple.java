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
 * A tuple that references another tuple for func param binding
 *
 * @author jwoehr
 */
public class ParamSubTuple extends Tuple {

    private Tuple boundTuple;
    private final String proposedKey;

    @Override
    public Object getValue() {
        return boundTuple.getValue();
    }

    @Override
    public void setValue(Object value) {
        if (boundTuple != null) {
            boundTuple.setValue(value);
        } else {
           // /* debug */ System.err.println("ParamSubTuple " + getKey() + " ready to be instanced with key " + getProposedKey());
            boundTuple = new Tuple(proposedKey, value);
           // /* debug */ System.err.println("ParamSubTuple " + getKey() + " instanced with key " + getProposedKey() + " to value " + getValue());
        }
    }

    @Override
    public String getBoundKey() {
        Tuple t = boundTuple;
        while (t instanceof ParamSubTuple) {
            t = t.getBoundTuple();
        }
        return t.getKey();
    }

    @Override
    public Tuple getBoundTuple() {
        return boundTuple;
    }

    /**
     * Get the key of the possibly nonexistent bound tuple
     *
     * @return key that was passed in to indicate name of tuple if has to be
     * created
     */
    @Override
    public String getProposedKey() {
        return proposedKey;
    }

    /**
     * Create a tuple that references another
     *
     * @param key key of new tuple
     * @param boundTuple the tuple it references
     * @param proposedKey
     */
    public ParamSubTuple(String key, Tuple boundTuple, String proposedKey) {
        super(key);
        Tuple x = boundTuple;
        while (x instanceof ParamSubTuple) {
            x = x.getBoundTuple();
        }
        this.boundTuple = x;
        this.proposedKey = proposedKey;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" bound key=\"")
                .append(getBoundKey())
                .append("\" bound tuple=[")
                .append(getBoundTuple()).append("]");
        return sb.toString();
    }
}