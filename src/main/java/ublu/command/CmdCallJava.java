/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 82, Beulah CO 81023-0082 http://www.softwoehr.com
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
package ublu.command;

import ublu.util.ArgArray;
import ublu.util.JavaCallHelper;
import ublu.util.JavaCallHelper.MethodArgPair;
import ublu.util.JavaCallHelper.MethodArgPairList;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Call a Java method
 *
 * @author jwoehr
 */
public class CmdCallJava extends Command {

    {
        setNameAndDescription("calljava", "/0 [-to @datasink] -forname ~@{classname} | -class ~@{classname} [-field ~@{fieldName} | -method ~@{methodname} [-arg ~@argobj [-arg ..]] [-primarg ~@argobj [-primarg ..]] [-castarg ~@argobj ~@{classname} [-castarg ..]] | -new ~@{classname} [-arg ~@argobj [-arg ..]] [-primarg ~@argobj [-primarg ..]] [-castarg ~@argobj ~@{classname} [-castarg ..]] | --,-obj ~@object [-field ~@{fieldName} | -method ~@{methodname} [-arg ~@argobj [-arg ..]] [-primarg ~@argobj [-primarg ..]] : call Java methods and fields");
    }

    /**
     * Our operations
     */
    protected static enum OPERATIONS {
        /**
         * field
         */
        FIELD,
        /**
         * return Class object
         */
        FORNAME,
        /**
         * method
         */
        METHOD,
        /**
         * ctor
         */
        NEW
    }

    /**
     * Do the work of the calljava command
     *
     * @param argArray
     * @return the remainder of the arg array
     */
    public ArgArray cmdCallJava(ArgArray argArray) {
        OPERATIONS op = OPERATIONS.METHOD;
        Object object = null;
        MethodArgPair marg = null;
        MethodArgPairList margs = new MethodArgPairList();
        String methodName = null;
        String newClassName = null;
        String fieldName = null;
        dashcommand:
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                case "-obj":
                    object = argArray.nextTupleOrPop().getValue();
                    break;
                case "-forname":
                    op = OPERATIONS.FORNAME; // falls thru into -class
                case "-class":
                    object = forName(argArray.nextMaybeQuotationTuplePopStringTrim());
                    break;
                case "-field":
                    op = OPERATIONS.FIELD;
                    fieldName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-method":
                    methodName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-arg":
                    marg = new MethodArgPair(argArray.nextTupleOrPop().getValue());
                    margs.add(marg);
                    break;
                case "-primarg":
                    marg = new MethodArgPair(argArray.nextTupleOrPop().getValue());
                    marg.primitize();
                    margs.add(marg);
                    break;
                case "-castarg":
                    Object o = argArray.nextTupleOrPop().getValue();
                    String classname = argArray.nextMaybeQuotationTuplePopStringTrim();
                    try {
                        marg = new MethodArgPair(o, Class.forName(classname));
                    } catch (ClassNotFoundException ex) {
                        getLogger().log(Level.SEVERE, "Class not found: " + classname, ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                        break dashcommand;
                    }
                    margs.add(marg);
                    break;
                case "-new":
                    op = OPERATIONS.NEW;
                    newClassName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (getCommandResult() != COMMANDRESULT.FAILURE) {
            JavaCallHelper jch = null;
            Object callResult = null;
            switch (op) {
                case FIELD:
                    if (object != null) {
                        Class c = object instanceof Class ? (Class) object : object.getClass();
                        try {
                            put(c.getField(fieldName));
                        } catch (SQLException | NoSuchFieldException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error getting or putting Field in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No object provided for field in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case FORNAME:
                    try {
                        put(object);
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE, "Error getting or putting Class object in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case METHOD:
                    try {
                        jch = new JavaCallHelper(object, methodName, margs);
                    } catch (NoSuchMethodException ex) {
                        getLogger().log(Level.SEVERE, "Error looking up method in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    if (jch != null) {
                        /* Debug */ // System.err.println("void? : " + jch.isVoid());
                        /* Debug */ // System.err.println("return type : " + jch.getReturnType());
                        try {
                            callResult = jch.callJava();
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            getLogger().log(Level.SEVERE, "Error invoking method in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        if (!jch.isVoid()) {
                            try {
                                put(callResult);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting result of Java call in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    }
                    break;

                case NEW:
                    try {
                        jch = new JavaCallHelper(Class.forName(newClassName), margs);
                    } catch (NoSuchMethodException | ClassNotFoundException ex) {
                        getLogger().log(Level.SEVERE, "Error looking up class name " + newClassName + " for Java new in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    if (jch != null) {
                        try {
                            callResult = jch.newInstance();
                        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            getLogger().log(Level.SEVERE, "Error invoking new in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        try {
                            put(callResult);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting result of Java new in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
            }
        }
        return argArray;
    }

    Class forName(String className
    ) {
        Class c = null;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            getLogger().log(Level.SEVERE, "No such class " + className + inNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        return c;
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return cmdCallJava(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
