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
package ublu.command;

import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics;
import ublu.util.Generics.ObjectInstanceHashSet;
import ublu.util.Generics.ObjectNameHashSet;
import ublu.util.Generics.StringArrayList;
import ublu.util.JMXHelper;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;

/**
 * Command to perform JMX access to a JVM
 *
 * @author jwoehr
 */
public class CmdJMX extends Command {

    {
        setNameAndDescription("jmx",
                "/0 [-from datasink] [-to datasink] [--,-jmx @jmx_instance] [-obj @obj_instance] [-protocol ~@rmi|iop|?] [-host ~@hostname|hostip] [-port ~@portnum] [-url ~@/remainder_of_url] [-role ~@${ rolename }$ ] [-password ~@${ password }$] [-connect | -close | -new,-instance | -get ~@${}domain ~@${}type ~@${}name | -attrib ~@${ attribute }$ | -attribs ~@${ attrib attrib ... }$ | -cdi ~@attribute | -datakey ~@attribute ~@key | -mbeaninfo |-query [ names | mbeans | class classname]] : perform JMX access to a JVM");
    }

    /**
     * The operations this command knows
     */
    protected static enum OPERATIONS {

        /**
         * JMXHelper instance
         */
        INSTANCE,
        /**
         * Connect instance to JMX server
         */
        CONNECT,
        /**
         * Disconnect instance to JMX server
         */
        CLOSE,
        /**
         * Get a JMX ObjectInstance
         */
        GET,
        /**
         * Get the composite data instance itself, helps in examining
         * implementation
         */
        CDI,
        /**
         * Get an attribute from a JMX ObjectInstance
         */
        ATTRIB,
        /**
         * Get multiple attributes from a JMX ObjectInstance
         */
        ATTRIBS,
        /**
         * Get specific data item from a composite data member of an attribute
         */
        DATAKEY,
        /**
         * Get info on a JMX ObjectInstance
         */
        MBEANINFO,
        /**
         * Get info from MBean Server
         */
        QUERY,
        /**
         * nada
         */
        NOOP
    }

    /**
     * ctor/0
     */
    public CmdJMX() {
    }

    /**
     * Do the work of JMX access
     *
     * @param argArray
     * @return the remainder of original argArray
     */
    public ArgArray jmx(ArgArray argArray) {
        OPERATIONS operation = OPERATIONS.INSTANCE;
        String protocol = "rmi";
        String hostname = null;
        Integer portnum = null;
        String urlPath = "/jmxrmi";
        Tuple jmxhTuple = null;
        String queryString = null;
        String objInstDomain = null;
        String objInstName = null;
        String objInstType = null;
        String objTupleName = null;
        String attribName = null;
        String attribNames = null;
        String queryTypeName = null;
        String role = null;
        String password = null;
        String datakey = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-from":
                    String srcName = argArray.next();
                    setDataSrc(DataSink.fromSinkName(srcName));
                    break;
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
                case "--":
                case "-jmx":
                    jmxhTuple = argArray.nextTupleOrPop();
                    break;
                case "-obj":
                    objTupleName = argArray.next();
                    break;
                case "-host":
                    hostname = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-protocol":
                    protocol = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-port":
                    portnum = argArray.nextIntMaybeTupleString();
                    break;
                case "-role":
                    role = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-password":
                    password = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-url":
                    urlPath = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-new":
                case "-instance":
                    operation = OPERATIONS.INSTANCE;
                    break;
                case "-connect":
                    operation = OPERATIONS.CONNECT;
                    break;
                case "-close":
                    operation = OPERATIONS.CLOSE;
                    break;
                case "-get":
                    operation = OPERATIONS.GET;
                    objInstDomain = argArray.nextMaybeQuotationTuplePopString().trim();
                    objInstType = argArray.nextMaybeQuotationTuplePopString().trim();
                    objInstName = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-attrib":
                    operation = OPERATIONS.ATTRIB;
                    attribName = argArray.nextMaybeQuotationTuplePopString().trim();
                    // /* Debug */ System.out.println("Switch -attrib: " + attribName);
                    break;
                case "-attribs":
                    operation = OPERATIONS.ATTRIBS;
                    attribNames = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-cdi":
                    operation = OPERATIONS.CDI;
                    attribName = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-datakey":
                    operation = OPERATIONS.DATAKEY;
                    attribName = argArray.nextMaybeQuotationTuplePopString().trim();
                    datakey = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-mbeaninfo":
                    operation = OPERATIONS.MBEANINFO;
                    break;
                case "-query":
                    operation = OPERATIONS.QUERY;
                    queryString = argArray.nextMaybeQuotationTuplePopString().trim();
                    if (queryString.equals("class")) {
                        queryTypeName = argArray.nextMaybeQuotationTuplePopString().trim();
                    }
                    break;
                case "-noop":
                    operation = OPERATIONS.NOOP;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            JMXHelper myJmxh = null;
            if (jmxhTuple != null) {
                Object tupleValue = jmxhTuple.getValue();
                if (tupleValue instanceof JMXHelper) {
                    myJmxh = JMXHelper.class.cast(tupleValue);
                } else {
                    getLogger().log(Level.SEVERE, "Valued tuple which is not a JMX tuple provided to -jmx in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (myJmxh == null) {
                myJmxh = instanceJMXHelper(protocol, hostname, portnum, urlPath);
            }
            if (myJmxh == null) {
                getLogger().log(Level.SEVERE, "Could not instance JMX {0}:{1} in {2}", new Object[]{hostname, portnum, getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE); // should be redundant
            } else {
                ObjectInstance oi;
                AttributeList al;
                StringArrayList sal;
                MBeanInfo mBeanInfo;
                switch (operation) {
                    case INSTANCE:
                        try {
                            put(myJmxh);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting JMX instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case CONNECT:
                        try {
                            if (role == null) {
                                myJmxh.connect();
                            } else {
                                myJmxh.connect(role, password);
                            }
                        } catch (MalformedURLException ex) {
                            getLogger().log(Level.SEVERE, "Bad URL portion of JMX specifier while connecting in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Error connecting to JMX instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case CLOSE:
                        try {
                            myJmxh.close();
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Error closing JMX instance in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case GET:
                        try {
                            oi = get(myJmxh, objInstDomain, objInstType, objInstName);
                            try {
                                put(oi);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting JMX object instance " + objInstName + " in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } catch (MalformedObjectNameException | InstanceNotFoundException | IOException ex) {
                            getLogger().log(Level.SEVERE, "Error getting JMX object instance " + objInstName + " in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case ATTRIB:
                        oi = getObjectInstanceFromTupleName(objTupleName);
                        if (oi == null) {
                            getLogger().log(Level.SEVERE, "No object instance retrievable from tuple {0} provided for JMX object instance in {1}", new Object[]{objTupleName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else if (attribName == null) {
                            getLogger().log(Level.SEVERE, "No attribute name (or null name) provided to -attrib dash-command  in {1}", new Object[]{objTupleName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            attribName = attribName.trim();
                            // /* Debug */ System.out.println(" attribName: " + attribName + "***");
                            try {
                                Object attributeObj = myJmxh.getAttribute(oi.getObjectName(), attribName);
                                put(attributeObj);
                            } catch (AS400SecurityException | MBeanException | AttributeNotFoundException | ErrorCompletingRequestException | IOException | InstanceNotFoundException | InterruptedException | ObjectDoesNotExistException | ReflectionException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Error getting or putting attributes in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case ATTRIBS:
                        oi = getObjectInstanceFromTupleName(objTupleName);
                        if (oi == null) {
                            getLogger().log(Level.SEVERE, "No object instance retrievable from tuple {0} provided for JMX object instance in {1}", new Object[]{objTupleName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            sal = new StringArrayList(attribNames).trimAll();
                            // /* Debug */ System.out.println(" attribNames: " + attribNames + "***");
                            // /* Debug */ System.out.println(" attribNameList: " + sal + "***");
                            try {
                                al = myJmxh.getAttributes(oi.getObjectName(), sal.toStringArray());
                                put(al);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InstanceNotFoundException | InterruptedException | ObjectDoesNotExistException | ReflectionException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Error getting or putting attributes in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case CDI:
                        oi = getObjectInstanceFromTupleName(objTupleName);
                        if (oi == null) {
                            getLogger().log(Level.SEVERE, "No object instance retrievable from tuple {0} provided for JMX object instance in {1}", new Object[]{objTupleName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else if (attribName == null) {
                            getLogger().log(Level.SEVERE, "No attrib (or null name) provided to -cdi dash-command  in {1}", new Object[]{objTupleName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                CompositeDataSupport cdi = myJmxh.attributeCompositeDataInstanceFromObjectInstance(oi, attribName);
                                put(cdi);
                            } catch (AS400SecurityException | MBeanException | AttributeNotFoundException | ErrorCompletingRequestException | IOException | InstanceNotFoundException | InterruptedException | ObjectDoesNotExistException | ReflectionException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Error getting or putting CompositeDataInstance in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case DATAKEY:
                        oi = getObjectInstanceFromTupleName(objTupleName);
                        if (oi == null) {
                            getLogger().log(Level.SEVERE, "No object instance retrievable from tuple {0} provided for JMX object instance in {1}", new Object[]{objTupleName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else if (attribName == null || datakey == null) {
                            getLogger().log(Level.SEVERE, "No attrib or datakey (or null name) provided to -datakey dash-command  in {1}", new Object[]{objTupleName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            datakey = datakey.trim();
                            // /* Debug */ System.out.println("attrib: " + attribName + " datakey: " + datakey + "***");
                            try {
                                Object compDataObj = myJmxh.attributeCompositeDataFromObjectInstance(oi, attribName, datakey);
                                put(compDataObj);
                            } catch (AS400SecurityException | MBeanException | AttributeNotFoundException | ErrorCompletingRequestException | IOException | InstanceNotFoundException | InterruptedException | ObjectDoesNotExistException | ReflectionException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Error getting or putting attributes in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case MBEANINFO:
                        oi = getObjectInstanceFromTupleName(objTupleName);
                        if (oi == null) {
                            getLogger().log(Level.SEVERE, "No tuple or null tuple {0} provided for JMX object instance in {1}", new Object[]{objTupleName, getNameAndDescription()});
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                mBeanInfo = myJmxh.getMBeanInfo(oi.getObjectName());
                                put(mBeanInfo);
                            } catch (InstanceNotFoundException | IntrospectionException | ReflectionException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                                getLogger().log(Level.SEVERE, "Error getting or putting MBeanInfo in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case QUERY:
                        switch (queryString) {
                            case "names":
                                queryNames(myJmxh);
                                break;
                            case "mbeans":
                                queryMBeans(myJmxh);
                                break;
                            case "class":
                                queryMBeansByClass(myJmxh, queryTypeName);
                                break;
                            default:
                                getLogger().log(Level.SEVERE, "Unknown query {0} in {1}", new Object[]{queryString, getNameAndDescription()});
                                setCommandResult(COMMANDRESULT.FAILURE);

                        }
                        break;
                    case NOOP:
                        break;
                    default:
                        getLogger().log(Level.SEVERE, "Unhandled operation in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    private JMXHelper instanceJMXHelper(String protocol, String host, Integer port, String urlPath) {
        JMXHelper helper = null;
        if (host == null | port == null) {
            getLogger().log(Level.SEVERE, "Hostname or port null in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            helper = new JMXHelper(protocol, host, port, urlPath);
        }
        return helper;
    }

    private void queryNames(JMXHelper myJmxh) {
        ObjectNameHashSet onhs = null;
        try {
            onhs = myJmxh.queryNames(ObjectName.WILDCARD, null);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error querying object names from JMX instance in " + getNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (onhs != null) {
            try {
                put(onhs);
            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                getLogger().log(Level.SEVERE, "Error putting object names from JMX instance in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private void queryMBeans(JMXHelper myJmxh) {
        ObjectInstanceHashSet oihs = null;
        try {
            oihs = myJmxh.queryMBeans(ObjectName.WILDCARD, null);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error querying MBeans from JMX instance in " + getNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (oihs != null) {
            try {
                put(oihs);
            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                getLogger().log(Level.SEVERE, "Error putting MBean query set from JMX instance in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private void queryMBeansByClass(JMXHelper myJmxh, String classname) {
        try {
            put(myJmxh.queryMbeansByClass(classname));
        } catch (IOException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
            getLogger().log(Level.SEVERE, "Error getting or putting MBean query set from JMX instance in " + getNameAndDescription(), ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    private ObjectInstance get(JMXHelper jmxh, String domain, String type, String name) throws IOException, MalformedObjectNameException, InstanceNotFoundException {
        ObjectInstance oi = null;
        Generics.ObjectNameHashSet onhs = jmxh.queryNames(new ObjectName(domain + ":type=" + type.trim() + ",name=" + name.trim()), null);
        // /*Debug*/ System.err.println("***" + onhs);
        for (ObjectName on : onhs) {
            oi = jmxh.getObjectInstance(on);
            break; // just grab the first for now
        }
        return oi;
    }

    private ObjectInstance getObjTupleValue(Tuple t) {
        ObjectInstance oi = null;
        if (t != null) {
            Object tupleValue = t.getValue();
            if (tupleValue instanceof ObjectInstance) {
                oi = ObjectInstance.class.cast(tupleValue);
            } else {
                getLogger().log(Level.SEVERE, "Valued tuple which is not a ObjectInstance tuple provided to -obj in {0}", getNameAndDescription());
                // Let caller do this instead.
                // setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return oi;
    }

    private ObjectInstance getObjectInstanceFromTupleName(String objTupleName) {
        ObjectInstance oi = null;
        if (objTupleName == null) {
            getLogger().log(Level.SEVERE, "No tuple name provided for JMX object instance in {0}", getNameAndDescription());
        } else {
            Tuple objTuple = getTuple(objTupleName);
            if (objTuple == null) {
                getLogger().log(Level.SEVERE, "No tuple instance or null tuple provided for JMX object instance in {0}", getNameAndDescription());
            } else {
                oi = getObjTupleValue(objTuple);
            }
        }
        return oi;
    }

    @Override
    protected void reinit() {
        super.reinit();
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return jmx(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
