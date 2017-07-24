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
package ublu;

import com.ibm.as400.access.AS400;
// import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.SecureAS400;
// import java.io.IOException;

/**
 * Extend so we can retrieve the password
 *
 * @author jwoehr
 */
public class SecureAS400Extender extends SecureAS400 {

    private String cachedPassword = "";

    private void setCachedPassword(String cachedPassword) {
        this.cachedPassword = cachedPassword;
    }

    /**
     * Get the password originally supplied by the user for use with JTOpenLite
     *
     * @return the password originally supplied by the user for use with
     * JTOpenLite
     */
    public String getCachedPassword() {
        return cachedPassword;
    }

    /**
     * Return the cached password if it's an extender instance, null otherwise
     *
     * @param as400 an instance maybe of AS400Extender or SecureAS400Extender
     * @return the cached password
     */
    public static String getCachedPassword(AS400 as400) {
        String result = null;
        if (as400 instanceof AS400Extender) {
            result = AS400Extender.class.cast(as400).getCachedPassword();
        } else if (as400 instanceof SecureAS400Extender) {
            result = SecureAS400Extender.class.cast(as400).getCachedPassword();
        }
        return result;
    }

    /**
     * Instance
     *
     * @param systemName systemName
     * @param userId userId
     * @param password password
     */
    public SecureAS400Extender(String systemName, String userId, String password) {
        super(systemName, userId, password);
        setCachedPassword(password);
    }

//    public static void main(String[] args) throws AS400SecurityException, IOException {
//        String system = args[0];
//        String user = args[1];
//        String password = args[2];
//        SecureAS400 sa = new SecureAS400(system, user, password);
//        sa.connectService(AS400.CENTRAL);
//        System.out.println("Result of connect CENTRAL is: " + sa.isConnected(AS400.CENTRAL));
//    }
}
