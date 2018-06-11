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

import com.ibm.as400.access.AS400Message;
import ublu.util.Generics.AS400MessageList;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append('\n')
                .append(this.programCall).append('\n')
                .append(this.managedProgramParameterList != null
                        ? this.managedProgramParameterList.toString()
                        : "[missing ManagedProgramParameterList]")
                .append('\n');
        return sb.toString();

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
     * Set return message options
     *
     * @param msgOpt
     * @return true if success
     */
    public boolean setMessageOptions(String msgOpt) {
        boolean result = true;
        switch (msgOpt.toLowerCase()) {
            case "all":
                programCall.setMessageOption(AS400Message.MESSAGE_OPTION_ALL);
                break;
            case "none":
                programCall.setMessageOption(AS400Message.MESSAGE_OPTION_NONE);
                break;
            case "10":
                programCall.setMessageOption(AS400Message.MESSAGE_OPTION_UP_TO_10);
                break;
            default:
                result = false; // Unknown option
        }
        return result;
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
        /* DEBUG */ System.err.println("before running the program call");
        boolean result = programCall.run();
        /* DEBUG */ System.err.println("after running the program call");
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
     * Operate on parameter list and decode output parameters into their tuples.
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            sb.append('\n')
                    .append(this.programParameter != null ? this.programParameter.toString() : "no program parameter")
                    .append('\n')
                    .append(this.tuple != null ? this.tuple.toString() : "no tuple")
                    .append('\n')
                    .append(this.vartype != null ? this.vartype.name() : "no vartype")
                    .append('\n');
            return sb.toString();
        }

        /**
         * return the program var type
         *
         * @return the program var type
         */
        public VARTYPE getVartype() {
            return vartype;
        }

        /**
         * set the program var type
         *
         * @param vartype vartype
         */
        public final void setVarType(VARTYPE vartype) {
            this.vartype = vartype;
        }

        /**
         * set the program var type from string
         *
         * @param s string representing typeF
         */
        public final void setVarType(String s) {
            this.vartype = VARTYPE.valueOf(s);
        }

        /**
         * Create new in-param
         *
         * @param t tuple with param value
         * @return the param
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
         * Create new in-param
         *
         * @param t tuple with param value
         * @param vartype string describing type
         * @return the param
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
         * Create new out-param
         *
         * @param t tuple naming param
         * @param length length
         * @return param
         */
        public static ManagedProgramParameter newOutParam(Tuple t, int length) {
            return new ManagedProgramParameter(new ProgramParameter(length), t);
        }

        /**
         * Create new out-param
         *
         * @param t tuple naming param
         * @param length length
         * @param vartype string describing type
         * @return param
         */
        public static ManagedProgramParameter newOutParam(Tuple t, int length, String vartype) {
            return new ManagedProgramParameter(new ProgramParameter(length), t, vartype);
        }

        /**
         * Create new inout-param
         *
         * @param t tuple naming param
         * @param length length
         * @return param
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
         * Create new inout-param
         *
         * @param t tuple naming param
         * @param length length
         * @param vartype string describing type
         * @return param
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
         * Create managed param from param
         *
         * @param programParameter extant
         * @param tuple holds param
         */
        public ManagedProgramParameter(ProgramParameter programParameter, Tuple tuple) {
            this.programParameter = programParameter;
            this.tuple = tuple;
        }

        /**
         * Create managed param from param
         *
         * @param programParameter extant
         * @param tuple holds param
         * @param vartype type enum
         */
        public ManagedProgramParameter(ProgramParameter programParameter, Tuple tuple, VARTYPE vartype) {
            this(programParameter, tuple);
            setVarType(vartype);
        }

        /**
         * Create managed param from param
         *
         * @param programParameter extant
         * @param tuple holds param
         * @param vartype string name of type enum
         */
        public ManagedProgramParameter(ProgramParameter programParameter, Tuple tuple, String vartype) {
            this(programParameter, tuple);
            setVarType(vartype);
        }

        /**
         * decode output into tuple
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
         * return true if has output
         *
         * @return true if has output
         */
        public boolean isOutput() {
            return this.programParameter.getOutputDataLength() != 0;
        }

        /**
         * return true if has input
         *
         * @return true if has input
         */
        public boolean isInput() {
            return this.programParameter.getInputData() != null;
        }

        /**
         * return tuple holding results
         *
         * @return tuple holding results
         */
        public Tuple getTuple() {
            return this.tuple;
        }

        /**
         * get raw parameter
         *
         * @return raw parameter
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
