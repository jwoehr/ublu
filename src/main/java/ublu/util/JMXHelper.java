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

import ublu.util.Generics.ObjectInstanceHashSet;
import ublu.util.Generics.ObjectNameHashSet;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JVM Remote Management support
 *
 * @author jwoehr
 */
public class JMXHelper {

    private String protocol;
    private String host;
    private Integer port;
    private String urlPath;
    private JMXConnector jMXConnector;
    private JMXServiceURL jMXServiceURL;

    /**
     * Get composed service url
     *
     * @return composed service url
     */
    public JMXServiceURL getjMXServiceURL() {
        return jMXServiceURL;
    }

    /**
     * Set composed service url
     *
     * @param jMXServiceURL
     */
    public void setjMXServiceURL(JMXServiceURL jMXServiceURL) {
        this.jMXServiceURL = jMXServiceURL;
    }

    /**
     * Get connector instance
     *
     * @return Get connector instance
     */
    public JMXConnector getjMXConnector() {
        return jMXConnector;
    }

    /**
     * Set connector instance
     *
     * @param jMXConnector connector instance
     */
    public void setjMXConnector(JMXConnector jMXConnector) {
        this.jMXConnector = jMXConnector;
    }

    /**
     * Get protocol string
     *
     * @return protocol string
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set protocol string
     *
     * @param protocol protocol string
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Get host
     *
     * @return host
     */
    public String getHost() {
        return host;
    }

    /**
     * Set host
     *
     * @param host host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     *
     * @return host
     */
    public Integer getPort() {
        return port;
    }

    /**
     *
     * @param port
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * <b>Note:</b>This code may use the term "urlPath" differently than JDK.
     * Here it means "the final portion of the URL after the host and portname",
     * typically <code>/jmxrmi</code>
     *
     * @return the final portion of the URL after the host and portname
     */
    public String getUrlPath() {
        return urlPath;
    }

    /**
     * <b>Note:</b>This code may use the term "urlPath" differently than JDK.
     * Here it means "the final portion of the URL after the host and portname",
     * typically <code>/jmxrmi</code>
     *
     * @param urlpath
     */
    public void setUrlPath(String urlpath) {
        this.urlPath = urlpath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append('\n');
        sb.append("Host:\t").append(host).append('\n');
        sb.append("Port:\t").append(port).append('\n');
        sb.append("Protocol:\t").append(protocol).append('\n');
        sb.append("Host:\t").append(urlPath).append('\n');
        if (jMXConnector != null) {
            sb.append("JMXConnector:\t").append(jMXConnector).append('\n');
        }
        if (jMXServiceURL != null) {
            sb.append("JMXServiceURL:\t").append(jMXServiceURL).append('\n');
        }
        return sb.toString();
    }

    private JMXHelper() {
        this.protocol = "rmi";
    }

    /**
     * ctor/3
     *
     * @param host
     * @param port
     * @param urlPath
     */
    public JMXHelper(String host, Integer port, String urlPath) {
        this();
        this.host = host;
        this.port = port;
        this.urlPath = urlPath;
    }

    /**
     * ctor /4
     *
     * @param protocol
     * @param host
     * @param port
     * @param urlPath
     */
    public JMXHelper(String protocol, String host, Integer port, String urlPath) {
        this(host, port, urlPath);
        this.protocol = protocol;
    }

    private JMXServiceURL formulateServiceURL() throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append("service:jmx:")
                .append(getProtocol()).append(':')
                .append("///jndi/")
                .append(getProtocol())
                .append("://")
                .append(getHost());
        if (getPort() != null) {
            sb.append(':').append(getPort());
        }
        if (getUrlPath() != null) {
            sb.append(getUrlPath());
        }
        JMXServiceURL serviceURL
                = new JMXServiceURL(sb.toString());
        return serviceURL;
    }

    /**
     * Connect to host with no authentication used.
     *
     * @throws MalformedURLException
     * @throws IOException
     */
    public void connect() throws MalformedURLException, IOException {
        connect(null);
    }

    /**
     * Connect with a map of properties set
     *
     * @param env
     * @throws MalformedURLException
     * @throws IOException
     */
    public void connect(Map<String, ?> env) throws MalformedURLException, IOException {
        jMXServiceURL = formulateServiceURL();
        JMXConnector jmxc = JMXConnectorFactory.connect(jMXServiceURL, env);
        setjMXConnector(jmxc);
    }

    /**
     * Connect to JMX server with a role and password, if it is set up for
     * plaintext password
     *
     * @param role
     * @param password
     * @throws MalformedURLException
     * @throws IOException
     */
    public void connect(String role, String password) throws MalformedURLException, IOException {
        Map<String, String[]> env = new HashMap<>();
        String[] creds = {role, password};
        env.put(JMXConnector.CREDENTIALS, creds);
        connect(env);
    }

    /**
     * close the JMX connection
     *
     * @throws IOException
     */
    public void close() throws IOException {
        getjMXConnector().close();
    }

    /**
     * Get the MBean Server instance
     *
     * @return MBean Server instance
     * @throws IOException
     */
    public MBeanServerConnection getMBeanServerConnection() throws IOException {
        return getjMXConnector().getMBeanServerConnection();
    }

    /**
     * Get names of MBeans served
     *
     * @param name
     * @param query
     * @return names of MBeans served
     * @throws IOException
     */
    public ObjectNameHashSet queryNames(ObjectName name,
            QueryExp query) throws IOException {
        return new ObjectNameHashSet(getMBeanServerConnection().queryNames(name, query));
    }

    /**
     * Get MBean instances served by this MBeanServer instance
     *
     * @param name
     * @param query
     * @return MBean instances served by this MBeanServer instance
     * @throws IOException
     */
    public ObjectInstanceHashSet queryMBeans(ObjectName name,
            QueryExp query) throws IOException {
        return new ObjectInstanceHashSet((getMBeanServerConnection().queryMBeans(name, query)));

    }

    /**
     * Get a specific MBean instance by ObjectName
     *
     * @param name
     * @return specific MBean instance for name
     * @throws MalformedObjectNameException
     * @throws InstanceNotFoundException
     * @throws IOException
     */
    public ObjectInstance getObjectInstance(ObjectName name) throws MalformedObjectNameException, InstanceNotFoundException, IOException {
        ObjectInstance oi = getMBeanServerConnection().getObjectInstance(name);
        // /* Debug */ System.err.println("***" + oi.getObjectName().getKeyPropertyList());
        return oi;
    }

    /**
     * Get a list of MBeans by Object Instance class name
     *
     * @param classname
     * @return list of MBeans
     * @throws IOException
     */
    public ObjectInstanceHashSet queryMbeansByClass(String classname) throws IOException {
        ObjectInstanceHashSet oihs = new ObjectInstanceHashSet(queryMBeans(ObjectName.WILDCARD, null));
        ObjectInstanceHashSet result = new ObjectInstanceHashSet();
        Iterator<ObjectInstance> it = oihs.iterator();
        while (it.hasNext()) {
            ObjectInstance oi = it.next();
            if (oi.getClassName().equals(classname)) {
                result.add(oi);
            }
        }
        return result;
    }

    /**
     *
     * @param objname
     * @param protocol
     * @param params
     * @param signature
     * @return operation result object
     * @throws InstanceNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     * @throws IOException
     */
    public Object invoke(ObjectName objname, String protocol, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return getMBeanServerConnection().invoke(objname, protocol, params, signature);
    }

    /**
     * Get the value of an attribute by name from an instance identified by
     * object name
     *
     * @param objectName
     * @param attribute
     * @return value of attribute
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    public Object getAttribute(ObjectName objectName, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        return getMBeanServerConnection().getAttribute(objectName, attribute);
    }

    /**
     * Get an attribute list from an instance identified by object name
     *
     * @param objectName
     * @param attributes
     * @return attribute list
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    public AttributeList getAttributes(ObjectName objectName, String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
        return getMBeanServerConnection().getAttributes(objectName, attributes);
    }

    /**
     * Get info on an instance identified by object name
     *
     * @param objectName
     * @return info on instance
     * @throws InstanceNotFoundException
     * @throws IntrospectionException
     * @throws ReflectionException
     * @throws IOException
     */
    public MBeanInfo getMBeanInfo(ObjectName objectName) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        return getMBeanServerConnection().getMBeanInfo(objectName);
    }

    /**
     * Get a composite data instance from an Object name and an attribute name
     *
     * @param objectName
     * @param attribute
     * @return composite data instance
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    public javax.management.openmbean.CompositeDataSupport attributeCompositeDataInstanceFromObjectName(ObjectName objectName, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        javax.management.openmbean.CompositeDataSupport cds = null;
        Object attributeObject = getMBeanServerConnection().getAttribute(objectName, attribute);
        if (attributeObject instanceof javax.management.openmbean.CompositeDataSupport) {
            cds = javax.management.openmbean.CompositeDataSupport.class.cast(attributeObject);
        }
        return cds;
    }

    /**
     * Get a composite data instance from an ObjectName and an attribute name
     *
     * @param objectInstance
     * @param attribute
     * @return composite data instance
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    public javax.management.openmbean.CompositeDataSupport attributeCompositeDataInstanceFromObjectInstance(ObjectInstance objectInstance, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        return attributeCompositeDataInstanceFromObjectName(objectInstance.getObjectName(), attribute);
    }

    /**
     * Get a composite data object from an ObjectInstance, an attribute name and
     * a key.
     *
     * @param objectName
     * @param attribute
     * @param key
     * @return composite data object
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    public Object attributeCompositeDataFromObjectName(ObjectName objectName, String attribute, String key) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        Object o = null;
        javax.management.openmbean.CompositeDataSupport cds = attributeCompositeDataInstanceFromObjectName(objectName, attribute);
        if (cds != null) {
            o = cds.get(key);
        }
        return o;
    }

    /**
     *
     * @param oi
     * @param attribute
     * @param key
     * @return composite data
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    public Object attributeCompositeDataFromObjectInstance(ObjectInstance oi, String attribute, String key) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        return attributeCompositeDataFromObjectName(oi.getObjectName(), attribute, key);
    }

//    /**
//     * Run a quick demo
//     *
//     * @param host
//     * @param port
//     * @param urlPath
//     * @throws MalformedURLException
//     * @throws IOException
//     */
//    public static void demo(String host, int port, String urlPath) throws MalformedURLException, IOException {
//        JMXHelper helper = new JMXHelper(host, port, urlPath);
//        helper.connect();
//        ObjectNameHashSet onhs = helper.queryNames(ObjectName.WILDCARD, null);
//        System.out.println("Object Names:");
//        System.out.println("-------------");
//        System.out.println(onhs);
//        ObjectInstanceHashSet oihs = helper.queryMBeans(ObjectName.WILDCARD, null);
//        System.out.println("Object Instances:");
//        System.out.println("-----------------");
//        System.out.println(oihs);
//    }
//
//    /**
//     * Run a demo of JMX access to a remote JVM. Start a JVM with a command
//     * like:<br>
//     * <code>
//     * java \<br>
//     * -Dcom.sun.management.jmxremote.port=9999 \<br>
//     * -Dcom.sun.management.jmxremote.authenticate=false \<br>
//     * -Dcom.sun.management.jmxremote.ssl=false -jar<br>
//     * /opt/api-java/ublu.jar<br>
//     * </code> Then run this main (for the example above) as<br>
//     * <code>java \<br>
//     * -cp /opt/api-java/ublu.jar \<br>
//     * ublu.util.JMXHelper localhost 9999 /jmxrmi </code><br>
//     * for a println of first the collection of ObjectNames and the
//     * ObjectInstances.
//     *
//     * @param args host port urlpath
//     * @throws MalformedURLException
//     * @throws IOException
//     */
//    public static void main(String[] args) throws MalformedURLException, IOException {
//        String host = args[0];
//        String port = args.length >= 2 ? args[1] : null;
//        String urlPath = args.length >= 3 ? args[2] : null;
//        demo(host, Integer.parseInt(port), urlPath);
//    }
}
