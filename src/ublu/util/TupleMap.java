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

import java.util.LinkedHashMap;
import java.util.Stack;

/**
 * Map of the associative memory in the intepreter, i.e., the tuple bag.
 *
 * @see Tuple
 * @author jwoehr
 */
public class TupleMap extends LinkedHashMap<String, Tuple> {

    private TupleMap localMap;

    /**
     * Get the local tuple map nested into this enclosing tuple map
     *
     * @return the local tuple map nested into this enclosing tuple map
     */
    public TupleMap getLocalMap() {
        return localMap;
    }

    /**
     * Set the local tuple map nested into this enclosing tuple map
     *
     * @param localMap the local tuple map nested into this enclosing tuple map
     */
    public final void setLocalMap(TupleMap localMap) {
        this.localMap = localMap;
    }

    /**
     * Instance simple
     */
    public TupleMap() {
        setLocalMap(null);
    }

    /**
     * Create a tuple map that is a deep copy of another
     *
     * @param src the tuple map to copy
     */
    public TupleMap(TupleMap src) {
        this();
        copyDeep(src);
    }

    /**
     * Deep copy of all the tuples and their values from a source tuple map
     *
     * @param src a source tuple map to copy
     */
    public final void copyDeep(TupleMap src) {
        putAll(src);
        Stack<TupleMap> tupleMapStack = new Stack<>();
        TupleMap latestNest = src.getLocalMap();
        while (latestNest != null) {
            tupleMapStack.push(latestNest);
            latestNest = (latestNest.getLocalMap());
        }
        while (!tupleMapStack.isEmpty()) {
            pushLocal();
            getLocalMap().putAll(tupleMapStack.pop());
        }
    }

    /**
     * Get a tuple from the local map by key. If not found, check the global map
     * (this).
     *
     * @param key string name of tuple
     * @return the tuple or null if not found
     */
    public Tuple getTuple(String key) {
        Tuple tuple = null;
        if (getLocalMap() != null) {
            tuple = getLocalMap().getTuple(key);
        }
        if (tuple == null) {
            if (containsKey(key)) {
                tuple = get(key);
            }
        }
        return tuple;
    }

    /**
     * Get a tuple from this map by key not checking local.
     *
     * @param key string name of tuple
     * @return the tuple or null if not found
     */
    public Tuple getTupleNoLocal(String key) {
        Tuple tuple = null;
        if (containsKey(key)) {
            tuple = get(key);
        }
        return tuple;
    }

    /**
     * Get a tuple from the local map by key. If not found, check the global map
     * (this).
     *
     * @param t the tuple
     * @return the tuple or null if not found
     */
    public Tuple deleteTuple(Tuple t) {
//        /*debug */ System.err.println("tuple to delete is " + t.getKey());
        Tuple result = null;
        if (getLocalMap() != null) {
            result = getLocalMap().deleteTuple(t);
        }
        if (result == null) {
            if (containsValue(t)) {
//                /*debug */ System.err.println("found tuple " + t.getKey());
                String found = null;
                for (String k : keySet()) {
//                    /*debug */ System.err.println("looking for tuple " + t);
//                    /*debug */ System.err.println("key is " + k);
//                    /*debug */ System.err.println("value is " + get(k));
                    if (get(k).equals(t)) {
//                        /*debug */ System.err.println("found " + k);
                        found = k;
                        break;
                    }
                }
                if (found != null) {
                    Tuple x = remove(found);
//                    /*debug */ System.err.println("removed " + x.getKey());
                }
            }
        }
        return result;
    }

    private Tuple putValueToNew(String key, Object value) {
        Tuple t = new Tuple(key, value);
        put(key, t);
        return t;
    }

    private Tuple putValueToExtant(String key, Object value) {
        Tuple t = get(key);
        t.setValue(value);
        put(key, t);
        return t;
    }

    private Tuple putValueToNewLocal(String key, Object value) {
        return getMostLocalMap().putValueToNew(key, value);
    }

    private Tuple putValueToExtantLocal(String key, Object value) {
        return getMostLocalMap().putValueToExtant(key, value);
    }

    /**
     * Put a tuple to the map
     *
     * @param t the tuple
     * @return the tuple
     */
    public Tuple putTuple(Tuple t) {
        return put(t.getKey(), t);
    }

    /**
     * Get most nested map
     *
     * @return most nested map
     */
    public TupleMap getMostLocalMap() {
        TupleMap tm = this;
        while (tm.getLocalMap() != null) {
            tm = tm.getLocalMap();
        }
        return tm;
    }

    /**
     * Put a tuple to the most local map
     *
     * @param t the tuple
     * @return the tuple
     */
    public Tuple putTupleMostLocal(Tuple t) {
        return getMostLocalMap().put(t.getKey(), t);
    }

    /**
     * Update a tuple to the local-est map in which its name already exists therein or
     * create or updated it in the global map if not found in any local map.
     *
     * @param key string name
     * @param value value object
     * @return the tuple set or created
     */
    public Tuple setTuple(String key, Object value) {
        Tuple tuple = null;
        Stack<TupleMap> tupleMapStack = new Stack<>();
        TupleMap lMap = getLocalMap();
        while (lMap != null) {
            tupleMapStack.push(lMap);
            lMap = lMap.getLocalMap();
        }
        while (!tupleMapStack.empty()) {
            lMap = tupleMapStack.pop();
            if (lMap.containsKey(key)) {
                tuple = lMap.get(key);
                tuple.setValue(value);
                break;
            }
        }
        if (tuple == null) {
            tuple = setTupleNoLocal(key, value);
        }
        return tuple;
    }

    /**
     * Create or update a tuple by putting it only to the global map
     *
     *
     * @param key string name
     * @param value value object
     * @return the tuple set or created
     */
    public Tuple setTupleNoLocal(String key, Object value) {
        Tuple tuple;
        if (containsKey(key)) {
            tuple = putValueToExtant(key, value);
        } else {
            tuple = putValueToNew(key, value);
        }
        return tuple;
    }

    /**
     * Remove a Tuple from the local map if it exists therein or from the global
     * map if it doesn't
     *
     * @param key string name
     * @return the Tuple being deleted
     */
    public Object deleteTuple(String key) {
        Object result = localDeleteTuple(key);
        if (result == null) {
            result = remove(key);
        }
        return result;
    }

    private Object localDeleteTuple(String key) {
        Object result = null;
        TupleMap lmap = getMostLocalMap();
        if (lmap != null) {
            result = lmap.deleteTuple(key);
        }
        return result;
    }

    private Tuple localSetTuple(String key, Object value) {
        Tuple tuple;
        TupleMap lmap = getMostLocalMap();
        if (lmap.containsKey(key)) {
            tuple = putValueToExtantLocal(key, value);
        } else {
            tuple = putValueToNewLocal(key, value);
        }
        return tuple;
    }

    /**
     * Display all tuple keys in the local map and global map
     *
     * @return string representation of all keys
     */
    public String keysAsDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(localKeysAsDisplayString());
        for (String key : keySet()) {
            sb.append(key).append(" ");
        }
        return sb.toString();
    }

    /**
     * Display all tuple keys in the local map
     *
     * @return string representation of all local keys or empty string.
     */
    public String localKeysAsDisplayString() {
        String result = "";
        StringBuilder sb = new StringBuilder();
        TupleMap map = getLocalMap();
        TupleMap lmap = map == null ? null : map.getLocalMap();
        if (map != null) {
            if (lmap != null) {
                sb.append(map.localKeysAsDisplayString()).append('\n');
            } else {
                for (String key : map.keySet()) {
                    sb.append(key).append(" ");
                }
            }
        }
        if (sb.length() > 0) {
            result = sb.toString();
        }

        return result;
    }

    /**
     * Push a new frame of local variables
     */
    public void pushLocal() {
        TupleMap tm = this;
        while (tm.getLocalMap() != null) {
            tm = tm.getLocalMap();
        }
        tm.setLocalMap(new TupleMap());
    }

    /**
     * Pop and discard a frame of local variables
     */
    public void popLocal() {
        TupleMap tm = this;
        while (tm.getLocalMap() != null) {
            if (tm.getLocalMap().getLocalMap() == null) {
                break;
            }
            tm = tm.getLocalMap();
        }
        tm.setLocalMap(null);
    }

    /**
     * Allows nested display of local maps
     *
     * @param n level of local map
     * @return string representing map
     */
    public String toStringWithLevel(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nLocal level ").append(n++).append(":\n")
                .append(toString());
        if (getLocalMap() != null) {
            sb.append(getLocalMap().toStringWithLevel(n));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (getLocalMap() != null) {
            sb.append(getLocalMap().toStringWithLevel(1));
        }
        return sb.toString();
    }
}
