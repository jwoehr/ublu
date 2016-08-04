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

import com.ibm.as400.access.AS400Message;
import ublu.util.Generics.AS400MessageList;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import com.ibm.as400.access.Trace;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Encapsulate making program calls via JTOpen
 *
 * @author jwoehr
 */
public class ProgramCallHelper {

    private ProgramCall programCall;
    private ManagedProgramParameterList managedProgramParameterList;

    /**
     * ctor/2
     *
     * @param programCall
     * @param managedProgramParameterList
     */
    public ProgramCallHelper(ProgramCall programCall, ManagedProgramParameterList managedProgramParameterList) {
        this.programCall = programCall;
        this.managedProgramParameterList = managedProgramParameterList;
    }

    private ProgramCallHelper() {
    }

    /**
     * Add more parameters to this call encapsulation
     *
     * @throws PropertyVetoException
     */
    public void addInputParameters() throws PropertyVetoException {
        Iterator<ManagedProgramParameter> it = managedProgramParameterList.iterator();
        while (it.hasNext()) {
            ManagedProgramParameter mpp = it.next();
            programCall.addParameter(mpp.getProgramParameter());
        }
    }

    /**
     * run the call
     *
     * @return true if call succeeds
     *
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     */
    public boolean runProgramCall() throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        // /* DEBUG */ programCall.setMessageOption(AS400Message.MESSAGE_OPTION_NONE);
        boolean result = programCall.run();
        return result;
    }

    /**
     * Get list of messages associated with run of the call
     *
     * @return list of messages
     */
    public AS400MessageList getMessageList() {
        return new Generics.AS400MessageList(programCall.getMessageList());
    }

    /**
     *
     */
    public void processOutputParameters() {
        Iterator<ManagedProgramParameter> it = managedProgramParameterList.iterator();
        while (it.hasNext()) {
            ManagedProgramParameter mpp = it.next();
            if (mpp.isOutput()) {
                mpp.instanceOutput();
            }
        }
    }

    /**
     * Parameters for the called programs
     */
    public static class ManagedProgramParameter {

        /**
         * These represent OS400 CL parameter types
         */
        public static enum VARTYPE {

            /**
             *
             */
            DEC,
            /**
             *
             */
            CHAR,
            /**
             *
             */
            LGL,
            /**
             *
             */
            INT,
            /**
             *
             */
            UINT,
            /**
             *
             */
            PTR
        }
        private ProgramParameter programParameter;
        private Tuple tuple;
        private VARTYPE vartype;

        /**
         *
         * @return
         */
        public VARTYPE getVartype() {
            return vartype;
        }

        /**
         *
         * @param vartype
         */
        public final void setVarType(VARTYPE vartype) {
            this.vartype = vartype;
        }

        /**
         *
         * @param s
         */
        public final void setVarType(String s) {
            this.vartype = VARTYPE.valueOf(s);
        }

        /**
         *
         * @param t
         * @return
         */
        public static ManagedProgramParameter newInParam(Tuple t) {
            ProgramParameter pp;
            Object o = t.getValue();
            if (o instanceof byte[]) {
                pp = new ProgramParameter(byte[].class.cast(o));
            } else {
                String s = o.toString();
                AS400Text a = new AS400Text(s.length());
                pp = new ProgramParameter(a.toBytes(s));
            }
            return new ManagedProgramParameter(pp, t);
        }

        /**
         *
         * @param t
         * @param vartype
         * @return
         */
        public static ManagedProgramParameter newInParam(Tuple t, String vartype) {
            ProgramParameter pp;
            Object o = t.getValue();
            if (o instanceof byte[]) {
                pp = new ProgramParameter(byte[].class.cast(o));
            } else {
                String s = o.toString();
                AS400Text a = new AS400Text(s.length());
                pp = new ProgramParameter(a.toBytes(s));
            }
            return new ManagedProgramParameter(pp, t, vartype);
        }

        /**
         *
         * @param t
         * @param length
         * @return
         */
        public static ManagedProgramParameter newOutParam(Tuple t, int length) {
            return new ManagedProgramParameter(new ProgramParameter(length), t);
        }

        /**
         *
         * @param t
         * @param length
         * @param vartype
         * @return
         */
        public static ManagedProgramParameter newOutParam(Tuple t, int length, String vartype) {
            return new ManagedProgramParameter(new ProgramParameter(length), t, vartype);
        }

        /**
         *
         * @param t
         * @param length
         * @return
         */
        public static ManagedProgramParameter newInOutParam(Tuple t, int length) {
            ProgramParameter pp;
            Object o = t.getValue();
            if (o instanceof byte[]) {
                pp = new ProgramParameter(byte[].class.cast(o), length);
            } else {
                String s = o.toString();
                AS400Text a = new AS400Text(s.length());
                pp = new ProgramParameter(a.toBytes(s), length);
            }
            return new ManagedProgramParameter(pp, t);
        }

        /**
         *
         * @param t
         * @param length
         * @param vartype
         * @return
         */
        public static ManagedProgramParameter newInOutParam(Tuple t, int length, String vartype) {
            ProgramParameter pp;
            Object o = t.getValue();
            if (o instanceof byte[]) {
                pp = new ProgramParameter(byte[].class.cast(o), length);
            } else {
                String s = o.toString();
                AS400Text a = new AS400Text(s.length());
                pp = new ProgramParameter(a.toBytes(s), length);
            }
            return new ManagedProgramParameter(pp, t, vartype);
        }

        /**
         *
         * @param programParameter
         * @param tuple
         */
        public ManagedProgramParameter(ProgramParameter programParameter, Tuple tuple) {
            this.programParameter = programParameter;
            this.tuple = tuple;
        }

        /**
         *
         * @param programParameter
         * @param tuple
         * @param vartype
         */
        public ManagedProgramParameter(ProgramParameter programParameter, Tuple tuple, VARTYPE vartype) {
            this(programParameter, tuple);
            setVarType(vartype);
        }

        /**
         *
         * @param programParameter
         * @param tuple
         * @param vartype
         */
        public ManagedProgramParameter(ProgramParameter programParameter, Tuple tuple, String vartype) {
            this(programParameter, tuple);
            setVarType(vartype);
        }

        /**
         *
         */
        public void instanceOutput() {
            byte[] output = this.programParameter.getOutputData();
            AS400Text text = new AS400Text(getProgramParameter().getOutputDataLength());
            Object o = text.toObject(output);
            switch (getVartype()) {
                case CHAR:
                    this.tuple.setValue(o.toString());
                    break;
                case INT:
                    // /* Debug */ System.err.println("output to string is " + o.toString());
//                    ByteBuffer bb = ByteBuffer.wrap(output);
//                    this.tuple.setValue(bb.getInt());
                    this.tuple.setValue(Integer.decode(o.toString().trim()));
                    break;
                case DEC:
                case LGL:
                case PTR:
                case UINT:
                default:
                    this.tuple.setValue(o.toString());
            }
        }

        /**
         *
         * @return
         */
        public boolean isOutput() {
            return this.programParameter.getOutputDataLength() != 0;
        }

        /**
         *
         * @return
         */
        public boolean isInput() {
            return this.programParameter.getInputData() != null;
        }

        /**
         *
         * @return
         */
        public Tuple getTuple() {
            return this.tuple;
        }

        /**
         *
         * @return
         */
        public ProgramParameter getProgramParameter() {
            return programParameter;
        }
    }

    /**
     * Typedef
     */
    public static class ManagedProgramParameterList extends ArrayList<ManagedProgramParameter> {
    }
}
