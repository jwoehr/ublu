/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, 2022, Jack J. Woehr jwoehr@softwoehr.com 
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
package ublu;

/*
 * AS400Factory.java
 * Instances AS400 objects
 * @author jwoehr
 * Created on August 8, 2006, 9:10 AM
 *
 */
import ublu.util.Interpreter;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.SecureAS400;
import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * A factory to produce correctly configured AS400 objects.
 *
 * @author jjw
 */
public class AS400Factory {

    /**
     * Never creates a new instance of AS400Factory. Not used.
     */
    private AS400Factory() {
    }

    /**
     * SSL or no?
     */
    public enum SIGNON_SECURITY_TYPE {

        /**
         * no security
         */
        NONE,
        /**
         * use secure sockets layer
         */
        SSL
    }

    /**
     * Types of handlers for failed signons
     */
    public enum SIGNON_HANDLER_TYPE {

        /**
         * Use JTOpen
         */
        BUILTIN,
        /**
         * Use our text-mode handler
         */
        CUSTOM,
        /**
         * Fail operation, no handling
         */
        NULL
    }

    /**
     * Create a new AS400 object with our custom signon handler
     *
     * @return the new AS400 object
     */
    protected static AS400 newAS400() {
        AS400 as400 = new AS400();
        as400.setSignonHandler(new SignonHandler());
        return as400;
    }

    /**
     * Create a new AS400 object with our custom signon handler and with the
     * system name, user id and password already set. Connections (when made)
     * will be via SSL if the property <code> signon.security.type </code> is
     * set to <code> SSL </code>, plain if that property is unset or set to <code> none
     * </code>.
     *
     * @deprecated use public static AS400 newAS400(Interpreter interpreter,
     * String systemName, String userid, String password) instead
     * @return the new AS400 object
     * @param systemName name or dotted ip
     * @param userid d'oh
     * @param password d'oh
     * @throws java.beans.PropertyVetoException if server name or user id cannot
     * be set
     */
    public static AS400 newAS400(String systemName, String userid, String password)
            throws PropertyVetoException {
        AS400 as400 = newAS400();
        as400.setSystemName(systemName);
        as400.setUserId(userid);
        as400.setPassword(password.toCharArray());
        return as400;
    }

    /**
     * Create a new AS400 object with the correct security type and signon
     * handler using our extenders to have the password at our disposal for use
     * with JTOpenLite classes.
     *
     * @param signon_security_type is this none or ssl?
     * @param signon_handler_type handler type for failed signons
     * @param systemName systemName
     * @param userId userId
     * @param password password
     * @return the new AS400 object
     */
    protected static AS400 newAS400(SIGNON_SECURITY_TYPE signon_security_type,
            SIGNON_HANDLER_TYPE signon_handler_type,
            String systemName,
            String userId,
            String password) {
        return newAS400(signon_security_type, signon_handler_type, systemName, userId, password, null);
    }

    /**
     * Create a new AS400 object with the correct security type and signon
     * handler using our extenders to have the password at our disposal for use
     * with JTOpenLite classes. Optionally specify a certificate file for SSL.
     *
     * @param signon_security_type is this none or ssl?
     * @param signon_handler_type handler type for failed signons
     * @param systemName systemName
     * @param userId userId
     * @param password password
     * @param certificatePath path to certificate file (optional, for SSL only)
     * @return the new AS400 object
     */
    protected static AS400 newAS400(SIGNON_SECURITY_TYPE signon_security_type,
            SIGNON_HANDLER_TYPE signon_handler_type,
            String systemName,
            String userId,
            String password,
            String certificatePath) {
        AS400 as400
                = signon_security_type == SIGNON_SECURITY_TYPE.NONE
                        ? new AS400(systemName, userId, password.toCharArray())
                        : new SecureAS400(systemName, userId, password.toCharArray());
        
        // Apply custom certificate if provided and using SSL
        if (signon_security_type == SIGNON_SECURITY_TYPE.SSL && certificatePath != null && !certificatePath.isEmpty()) {
            try {
                SSLSocketFactory sslSocketFactory = createSSLSocketFactoryFromCertificate(certificatePath);
                ((SecureAS400) as400).setSocketFactory(sslSocketFactory);
            } catch (Exception e) {
                Logger.getLogger(AS400Factory.class.getName())
                        .log(Level.SEVERE, "Failed to load certificate from " + certificatePath, e);
            }
        }
        
        switch (signon_handler_type) {
            case CUSTOM:
                as400.setSignonHandler(new SignonHandler());
                break;
            case NULL:
                as400.setSignonHandler(new NullSignonHandler());
                break;
            case BUILTIN:
                break;
        }
        return as400;
    }

    /**
     * Create a new AS400 object with the correct security type and signon
     * handler using our extenders to have the password at our disposal for use
     * with JTOpenLite classes.
     *
     * @param signon_security_type is this none or ssl?
     * @param signon_handler_type handler type for failed signons
     * @param systemName systemName
     * @param userId userId
     * @param password password
     * @param additionalAuthenticationFactor e.g., MFA TOTP
     * @throws AS400SecurityException
     * @throws IOException
     * @return the new AS400 object
     */
    protected static AS400 newAS400(SIGNON_SECURITY_TYPE signon_security_type,
            SIGNON_HANDLER_TYPE signon_handler_type,
            String systemName,
            String userId,
            char[] password,
            char[] additionalAuthenticationFactor) throws AS400SecurityException, IOException {
        return newAS400(signon_security_type, signon_handler_type, systemName, userId, password, additionalAuthenticationFactor, null);
    }

    /**
     * Create a new AS400 object with the correct security type and signon
     * handler using our extenders to have the password at our disposal for use
     * with JTOpenLite classes. Optionally specify a certificate file for SSL.
     *
     * @param signon_security_type is this none or ssl?
     * @param signon_handler_type handler type for failed signons
     * @param systemName systemName
     * @param userId userId
     * @param password password
     * @param additionalAuthenticationFactor e.g., MFA TOTP
     * @param certificatePath path to certificate file (optional, for SSL only)
     * @throws AS400SecurityException
     * @throws IOException
     * @return the new AS400 object
     */
    protected static AS400 newAS400(SIGNON_SECURITY_TYPE signon_security_type,
            SIGNON_HANDLER_TYPE signon_handler_type,
            String systemName,
            String userId,
            char[] password,
            char[] additionalAuthenticationFactor,
            String certificatePath) throws AS400SecurityException, IOException {
        AS400 as400
                = signon_security_type == SIGNON_SECURITY_TYPE.NONE
                        ? new AS400(systemName, userId, password, additionalAuthenticationFactor)
                        : new SecureAS400(systemName, userId, password, additionalAuthenticationFactor);
        
        // Apply custom certificate if provided and using SSL
        if (signon_security_type == SIGNON_SECURITY_TYPE.SSL && certificatePath != null && !certificatePath.isEmpty()) {
            try {
                SSLSocketFactory sslSocketFactory = createSSLSocketFactoryFromCertificate(certificatePath);
                ((SecureAS400) as400).setSocketFactory(sslSocketFactory);
            } catch (Exception e) {
                Logger.getLogger(AS400Factory.class.getName())
                        .log(Level.SEVERE, "Failed to load certificate from " + certificatePath, e);
                throw new IOException("Failed to load certificate from " + certificatePath, e);
            }
        }
        
        switch (signon_handler_type) {
            case CUSTOM:
                as400.setSignonHandler(new SignonHandler());
                break;
            case NULL:
                as400.setSignonHandler(new NullSignonHandler());
                break;
            case BUILTIN:
                break;
        }
        return as400;
    }

    /**
     * Create a new AS400 object with our custom signon handler and with the
     * system name, user id and password already set. Connections (when made)
     * will be via SSL if the property <code> signon.security.type </code> is
     * set to <code> SSL </code>, plain if that property is unset or set to <code> none
     * </code>.
     *
     *
     * @return the new AS400 object
     * @param interpreter the interpreter calling us
     * @param systemName name or dotted ip
     * @param userid d'oh
     * @param password d'oh
     * @throws java.beans.PropertyVetoException if server name or user id cannot
     * be set
     */
    public static AS400 newAS400(Interpreter interpreter, String systemName, String userid, String password)
            throws PropertyVetoException {
        AS400 as400 = newAS400(getSignonSecurityType(interpreter), getSignonHandlerType(interpreter), systemName, userid, password);
        return as400;
    }

    /**
     * Create a new AS400 object with our custom signon handler and with the
     * system name, user id and password already set. Connections (when made)
     * will be via SSL if <code> signon_security_type </code> argument is set to
     * <code> SSL </code>.
     *
     * @return the new AS400 object
     * @param interpreter the interpreter calling us
     * @param systemName name or dotted ip
     * @param userid d'oh
     * @param password d'oh
     * @param signon_security_type provides whether we want SSL
     * @throws java.beans.PropertyVetoException if server name or user id cannot
     * be set
     */
    public static AS400 newAS400(Interpreter interpreter, String systemName, String userid, String password, SIGNON_SECURITY_TYPE signon_security_type)
            throws PropertyVetoException {
        String certificatePath = interpreter.getProperty("ssl.certificate.path", null);
        AS400 as400 = newAS400(signon_security_type, getSignonHandlerType(interpreter), systemName, userid, password, certificatePath);
        return as400;
    }

    /**
     * Create a new AS400 object with our custom signon handler and with the
     * system name, user id and password already set. Connections (when made)
     * will be via SSL if <code> signon_security_type </code> argument is set to
     * <code> SSL </code>. Optionally specify a certificate file path.
     *
     * @return the new AS400 object
     * @param interpreter the interpreter calling us
     * @param systemName name or dotted ip
     * @param userid d'oh
     * @param password d'oh
     * @param signon_security_type provides whether we want SSL
     * @param certificatePath path to certificate file (optional, for SSL only)
     * @throws java.beans.PropertyVetoException if server name or user id cannot
     * be set
     */
    public static AS400 newAS400(Interpreter interpreter, String systemName, String userid, String password, SIGNON_SECURITY_TYPE signon_security_type, String certificatePath)
            throws PropertyVetoException {
        AS400 as400 = newAS400(signon_security_type, getSignonHandlerType(interpreter), systemName, userid, password, certificatePath);
        return as400;
    }
    /**
     * Create a new AS400 object with our custom signon handler and with the
     * system name, user id and password already set.Connections (when made)
     * will be via SSL if <code> signon_security_type </code> argument is set to
     * <code> SSL </code>.
     *
     * @return the new AS400 object
     * @param interpreter the interpreter calling us
     * @param systemName name or dotted ip addr
     * @param userid d'oh
     * @param password d'oh
     * @param additionalAuthenticationFactor e.g. TOTP for MFA
     * @param signon_security_type provides whether we want SSL
     * @throws java.beans.PropertyVetoException if server name or user id cannot
     * be set
     * @throws com.ibm.as400.access.AS400SecurityException
     * @throws java.io.IOException
     */
    public static AS400 newAS400(Interpreter interpreter, String systemName, String userid, char [] password, char[] additionalAuthenticationFactor, SIGNON_SECURITY_TYPE signon_security_type)
            throws PropertyVetoException, AS400SecurityException, IOException {
        String certificatePath = interpreter.getProperty("ssl.certificate.path", null);
        AS400 as400 = newAS400(signon_security_type, getSignonHandlerType(interpreter), systemName, userid, password, additionalAuthenticationFactor, certificatePath);
        return as400;
    }

    /**
     * Create a new AS400 object with our custom signon handler and with the
     * system name, user id and password already set. Connections (when made)
     * will be via SSL if <code> signon_security_type </code> argument is set to
     * <code> SSL </code>. Optionally specify a certificate file path.
     *
     * @return the new AS400 object
     * @param interpreter the interpreter calling us
     * @param systemName name or dotted ip addr
     * @param userid d'oh
     * @param password d'oh
     * @param additionalAuthenticationFactor e.g. TOTP for MFA
     * @param signon_security_type provides whether we want SSL
     * @param certificatePath path to certificate file (optional, for SSL only)
     * @throws java.beans.PropertyVetoException if server name or user id cannot
     * be set
     * @throws com.ibm.as400.access.AS400SecurityException
     * @throws java.io.IOException
     */
    public static AS400 newAS400(Interpreter interpreter, String systemName, String userid, char [] password, char[] additionalAuthenticationFactor, SIGNON_SECURITY_TYPE signon_security_type, String certificatePath)
            throws PropertyVetoException, AS400SecurityException, IOException {
        AS400 as400 = newAS400(signon_security_type, getSignonHandlerType(interpreter), systemName, userid, password, additionalAuthenticationFactor, certificatePath);
        return as400;
    }

    private static SIGNON_HANDLER_TYPE propertyStringToSignonHandlerType(String s) {
        SIGNON_HANDLER_TYPE sht = SIGNON_HANDLER_TYPE.CUSTOM;
        switch (s.toUpperCase()) {
            case "CUSTOM":
                sht = SIGNON_HANDLER_TYPE.CUSTOM;
                break;
            case "NULL":
                sht = SIGNON_HANDLER_TYPE.NULL;
                break;
            case "BUILTIN":
                sht = SIGNON_HANDLER_TYPE.BUILTIN;
                break;
            default:
                Logger.getLogger(SignonHandler.class.getName())
                        .log(Level.WARNING, "Unknown signon handler type {0} set in interpreter property signon.handler.type (CUSTOM | NULL | BUILTIN) using CUSTOM.", s);
        }
        return sht;
    }

    private static SIGNON_SECURITY_TYPE propertyStringToSignonSecurityType(String s) {
        SIGNON_SECURITY_TYPE sst = SIGNON_SECURITY_TYPE.NONE;
        switch (s.toUpperCase()) {
            case "SSL":
                sst = SIGNON_SECURITY_TYPE.SSL;
//                /* debug */ System.err.println("Using secure sockets.");
                break;
            case "NONE":
                sst = SIGNON_SECURITY_TYPE.NONE;
                break;
            default:
                Logger.getLogger(SignonHandler.class.getName())
                        .log(Level.WARNING, "Unknown signon security type {0} set in interpreter property signon.security.type (NONE | SSL) using NONE.", s);
        }
        return sst;
    }

    private static SIGNON_HANDLER_TYPE getSignonHandlerType(Interpreter interpreter) {
        String s = interpreter.getProperty("signon.handler.type", "CUSTOM");
        return propertyStringToSignonHandlerType(s);
    }

    private static SIGNON_SECURITY_TYPE getSignonSecurityType(Interpreter interpreter) {
        String s = interpreter.getProperty("signon.security.type", "NONE");
        return propertyStringToSignonSecurityType(s);
    }

    /**
     * Convert a service name to its JTOpen integer value.
     *
     * @param serviceName the service name
     * @return the JTOpen integer value
     */
    public static Integer serviceNameToInteger(String serviceName) {
        Integer serviceInteger = null;
        switch (serviceName.toUpperCase()) {
            case "CENTRAL":
                serviceInteger = AS400.CENTRAL;
                break;
            case "COMMAND":
                serviceInteger = AS400.COMMAND;
                break;
            case "DATABASE":
                serviceInteger = AS400.DATABASE;
                break;
            case "DATAQUEUE":
                serviceInteger = AS400.DATAQUEUE;
                break;
            case "FILE":
                serviceInteger = AS400.FILE;
                break;
            case "PRINT":
                serviceInteger = AS400.PRINT;
                break;
            case "RECORDACCESS":
                serviceInteger = AS400.RECORDACCESS;
                break;
            case "SIGNON":
                serviceInteger = AS400.SIGNON;
                break;
        }
        return serviceInteger;
    }

    /**
     * Create an SSLSocketFactory from a certificate file.
     * Supports X.509 certificates in PEM or DER format.
     *
     * @param certificatePath path to the certificate file
     * @return SSLSocketFactory configured with the certificate
     * @throws Exception if certificate loading or SSL context creation fails
     */
    private static SSLSocketFactory createSSLSocketFactoryFromCertificate(String certificatePath) throws Exception {
        // Load the certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert;
        
        try (FileInputStream fis = new FileInputStream(certificatePath)) {
            cert = cf.generateCertificate(fis);
        }
        
        // Create a KeyStore containing the certificate
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("server-cert", cert);
        
        // Create a TrustManager that trusts the certificate in our KeyStore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        
        // Create an SSLContext with our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new java.security.SecureRandom());
        
        return sslContext.getSocketFactory();
    }
}
