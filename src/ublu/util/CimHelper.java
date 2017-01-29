/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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

import java.net.MalformedURLException;
import java.net.URL;
// import java.util.Enumeration;
import java.util.Locale;
import javax.cim.CIMObjectPath;
import javax.security.auth.Subject;
import javax.wbem.CloseableIterator;
import javax.wbem.WBEMException;
import javax.wbem.client.PasswordCredential;
import javax.wbem.client.UserPrincipal;
import javax.wbem.client.WBEMClient;
import javax.wbem.client.WBEMClientConstants;
import javax.wbem.client.WBEMClientFactory;
import org.sblim.cimclient.WBEMClientSBLIM;

/**
 * Helper class for an Ublu 'cim' command
 *
 * @author jax
 */
public class CimHelper {

    public static void main(String[] args) throws WBEMException, MalformedURLException {

        if (args.length < 3) {
            System.err.println("usage: CimHelper system user passwd");
            System.exit(2);
        }
        URL cimomUrl = new URL(args[0]);
        String user = args[1];
        String pw = args[2];

//        String[] supported = WBEMClientFactory.getSupportedProtocols();
//        for (String s : supported) {
//            System.out.println(s);
//        }
        WBEMClient client = WBEMClientFactory.getClient(WBEMClientConstants.PROTOCOL_CIMXML);
        WBEMClientSBLIM clientSBLIM = WBEMClientSBLIM.class.cast(client);

//        Enumeration en = clientSBLIM.getProperties().elements();
//        while (en.hasMoreElements()) {
//            System.err.println(en.nextElement());
//        }
        final CIMObjectPath path = new CIMObjectPath(cimomUrl.getProtocol(),
                cimomUrl.getHost(), String.valueOf(cimomUrl.getPort()), null, null, null);
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal(user));
        subject.getPrivateCredentials().add(new PasswordCredential(pw));

//        for (Locale l : Locale.getAvailableLocales()) {
//            System.err.println(l);
//        }
        // The library has a buggy assumption in it that the first Locale in the array is the default Locale.
        Locale[] locales = Locale.getAvailableLocales();
        Locale[] hacked_locales = new Locale[locales.length + 1];
        System.arraycopy(locales, 0, hacked_locales, 1, locales.length);
        hacked_locales[0] = Locale.getDefault();
        try {
            client.initialize(path, subject, hacked_locales);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            final CloseableIterator<CIMObjectPath> iterator = client.enumerateInstanceNames(new CIMObjectPath(null, null, null, "root/cimv2", "CIM_LogicalIdentity", null));
            try {
                while (iterator.hasNext()) {
                    final CIMObjectPath pathIter = iterator.next();
                    System.out.println(pathIter.toString());
                }
            } finally {
                iterator.close();
            }
        } catch (WBEMException e) {
            e.printStackTrace();
        }

    }
}
