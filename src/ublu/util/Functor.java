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

import ublu.util.Generics.FunctorParamList;
import ublu.util.Generics.TupleNameList;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Class for defining executable parameterized functors.
 *
 * @author jwoehr
 */
public class Functor implements Serializable {

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

    private Functor() {
        super();
    }

    /**
     * Instance from an execution block and a list of parameter names
     *
     * @param block execution block
     * @param fpl list of parameter names (without decoration)
     */
    public Functor(String block, FunctorParamList fpl) {
        setBlock(block);
        setFunctorParamList(fpl);
    }
    private String block;

    /**
     * Return the execution block
     *
     * @return the execution block
     */
    public String getBlock() {
        return block;
    }

    /**
     * Set the execution block
     *
     * @param block the execution block
     */
    public final void setBlock(String block) {
        this.block = block;
    }
    FunctorParamList functorParamList;

    /**
     * Set the parameter name list
     *
     * @param functorParamList the parameter name list
     */
    public final void setFunctorParamList(FunctorParamList functorParamList) {
        this.functorParamList = functorParamList;
    }

    /**
     * Add a param name to the param list
     *
     * @param paramName param name to add
     * @return the functor (this)
     */
    public Functor addParam(String paramName) {
        functorParamList.add(paramName);
        return this;
    }

    /**
     * Get the parameter name list
     *
     * @return the parameter name list
     */
    public FunctorParamList getFunctorParamList() {
        return functorParamList;
    }

    /**
     * Resolve all decorated occurrences of decorated param names in the block
     * to tuple names from the substitution list
     *
     * @param tnl substitution list of tuple names
     * @return the bound block
     */
    public String bind(TupleNameList tnl) {
        String s = getBlock();
        String y;
        for (int i = 0; i < getFunctorParamList().size(); i++) {
            String rgx = "@@" + getFunctorParamList().get(i);
            y = s.replaceAll(rgx, tnl.get(i));
            s = y;
            // /* Debug */ System.err.println("block after replacement " + s);
        }
        return s;
    }

    /**
     * Resolve all decorated occurrences of decorated param names in the block
     * to ParamSubstitutionTuple names from the substitution list
     *
     * @param interpreter
     * @param tnl substitution list of tuple names
     * @return the bound block
     */
    public String bindWithSubstitutes(Interpreter interpreter, TupleNameList tnl) {
        tnl = subTupleNameList(interpreter, tnl);
        // /* debug */ System.err.println("Tuple name list is " + tnl);
        FunctorParamList fpl = getFunctorParamList();
        String s = getBlock();
        String y;
        for (int i = 0; i < fpl.size(); i++) {
            String rgx = "@@" + fpl.get(i) + "\\s";
            y = s.replaceAll(rgx, tnl.get(i) + " ");
            s = y;
            // /* Debug */ System.err.println("block after ParamSubstitutionTuple replacement " + s);
        }
        return s;
    }

    /**
     * Replace the tuple names in the tuple name list passed to function with
     * substitute temporary tuples
     *
     * @param interpreter
     * @param tnl
     * @return the list with all tuple names replaced with the names of the
     * substitute temporaries
     */
    public TupleNameList subTupleNameList(Interpreter interpreter, TupleNameList tnl) {
        String tupleName;
        Tuple substituteTuple;
        for (int i = 0; i < tnl.size(); i++) {
            tupleName = tnl.get(i);
            if (Tuple.isTupleName(tupleName)) {
                substituteTuple = createSubstitute(interpreter, tupleName, interpreter.getParamSubIndex());
                tnl.set(i, substituteTuple.getKey());
            }
        }
        return tnl;
    }

    /**
     * Create a substitute tuple so that func block LOCALs can't hide the params
     * to a function
     *
     * @param interpreter
     * @param tupleName the real tuple name
     * @param index
     * @return a new tuple in the local map created by
     * Interpreter.executeFunctor
     */
    public Tuple createSubstitute(Interpreter interpreter, String tupleName, long index) {
        Tuple t = interpreter.getTuple(tupleName);
        interpreter.dbug().dbugTuple("FUNCTOR create sub for: ", t);
        if (t == null) {
            t = interpreter.setTuple(tupleName, null);
            interpreter.dbug().dbugTuple("FUNCTOR Tuple sprang into existence: ", t);
        }
        ParamSubTuple pst = new ParamSubTuple("@///" + index, t, tupleName);
        interpreter.dbug().dbugTuple("FUNCTOR New PST: ", pst);
        interpreter.getTupleMap().putTupleMostLocal(pst);
        interpreter.dbug().dbugTupleMap("FUNCTOR Map after new PST: ", interpreter.getTupleMap());
        return pst;
    }

    /**
     * Get number of params in list
     *
     * @return number of params in list
     */
    public int numParams() {
        return getFunctorParamList().size();
    }

    /**
     * toString() using the superclass method, gives the Java noise whereas the
     * class toString() gives the functor body.
     *
     * @return superclass's toString() result
     */
    public String superString() {
        return super.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" ( ");
        for (String s : getFunctorParamList()) {
            sb.append(s).append(" ");
        }
        sb.append(") ");
        sb.append("$[ ").append(getBlock()).append(" ]$");
        return sb.toString();
    }
}
