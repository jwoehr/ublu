/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Make Java method calls
 *
 * @author jwoehr
 */
public class JavaCallHelper {

    Object zObject;
    Object[] zArgs;
    MethodArgPairList margs;
    Method zMethod;

    private JavaCallHelper() {
    }

    /**
     * Instance simply
     *
     * @param zObject
     * @param zMethod
     * @param zArgs
     */
    protected JavaCallHelper(Object zObject, Method zMethod, Object[] zArgs) {
        this.zObject = zObject;
        this.zArgs = zArgs;
        this.zMethod = zMethod;
    }

    /**
     * Instance keeping the passed-in MethodArgPairList around for debugging
     *
     * @param o
     * @param methodName
     * @param margs
     * @throws NoSuchMethodException
     */
    public JavaCallHelper(Object o, String methodName, MethodArgPairList margs) throws NoSuchMethodException {
        this(o, o.getClass().getDeclaredMethod(methodName, margs.toClassArray()), margs.toArgList());
        this.margs = margs;
    }

    /**
     * Instance for Constructor.newInstance() call
     *
     * @param o
     * @param margs
     * @throws NoSuchMethodException
     */
    public JavaCallHelper(Object o, MethodArgPairList margs) throws NoSuchMethodException {
        zObject = o;
        zArgs = margs.toArgList();
        this.margs = margs;
    }

    /**
     * Make the call
     *
     * @return result of call
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public Object callJava() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return zMethod.invoke(zObject, zArgs);
    }

    /**
     * Do a java 'new'
     *
     * @return the new object
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public Object newInstance() throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Constructor c = Class.class.cast(zObject).getConstructor(margs.toClassArray());
        return c.newInstance(zArgs);
    }

    /**
     * Class to hold arg and type so we can substitute prim types for wrapper
     * args
     */
    public static class MethodArgPair {

        private Class classType;
        private Object argObject;

        /**
         *
         * @return the class for the method sig
         */
        public Class getClassType() {
            return classType;
        }

        /**
         *
         * @param classType the class for the method sig
         */
        public void setClassType(Class classType) {
            this.classType = classType;
        }

        /**
         *
         * @return arg object
         */
        public Object getArgObject() {
            return argObject;
        }

        /**
         *
         * @param argObject arg object for method call
         */
        public void setArgObject(Object argObject) {
            this.argObject = argObject;
        }

        /**
         * Instance simply, instances class automagically
         *
         * @param argObject arg object for method call
         */
        public MethodArgPair(Object argObject) {
            this.argObject = argObject;
            this.classType = argObject.getClass();
        }

        /**
         * Indicate the class type as primitive of a wrapper object, e.g., int
         * for Integer, to resolve the method signature for the call.
         */
        public void primitize() {
            if (classType == Integer.class) {
                classType = Integer.TYPE;
            } else if (classType == Short.class) {
                classType = Short.TYPE;
            } else if (classType == Long.class) {
                classType = Long.TYPE;
            } else if (classType == Double.class) {
                classType = Double.class;
            } else if (classType == Float.class) {
                classType = Float.TYPE;
            } else if (classType == Byte.class) {
                classType = Byte.TYPE;
            } else if (classType == Boolean.class) {
                classType = Boolean.TYPE;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            sb.append("Object:\t").append(argObject.toString()).append('\n');
            sb.append("Real Class:\t").append(argObject.getClass()).append('\n');
            sb.append("Nominal Class:\t").append(classType).append('\n');
            return sb.toString();
        }
    }

    /**
     * List of pairs that represent a method arg and the arg's signature class
     */
    public static class MethodArgPairList extends ArrayList<MethodArgPair> {

        /**
         * Convert the list to an obj list of the method call args
         *
         * @return obj list of the method call args
         */
        public Object[] toArgList() {
            ArrayList al = new ArrayList();
            for (MethodArgPair marg : this) {
                al.add(marg.argObject);
            }
            return al.toArray();
        }

        /**
         * Return array of class types for method signature.
         *
         * @return array of class types for method signature.
         */
        public Class[] toClassArray() {
            ArrayList<Class> cl = new ArrayList<>();
            for (MethodArgPair marg : this) {
                cl.add(marg.classType);
            }
            Class[] classes = new Class[cl.size()];
            classes = cl.toArray(classes);
            return classes;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            sb.append('\n');
            for (MethodArgPair marg : this) {
                sb.append(marg.toString()).append('\n');
            }
            return sb.toString();
        }
    }
}
