/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2018, Jack J. Woehr jwoehr@softwoehr.com 
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

import java.net.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author jax
 */
public class WatsonHelper {

    public static String BLUEMIX_HOST = "watson-api-explorer.mybluemix.net";
    public static String URL_BASE = "https://";

    public static String watson(String host, String usrv, String[] parms) throws MalformedURLException, IOException {
        String method = "GET";

        StringBuilder params = new StringBuilder();
        boolean isFirstParam = true;
        for (String parm : parms) {
            if (isFirstParam) {
                params.append("?").append(parm);
                isFirstParam = false;
            } else {
                params.append("&").append(parm);
            }
        }
        URL url = new URL(URL_BASE + host + "/" + usrv + params.toString());
        // /* debug */ System.err.println(url);

        HttpURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestMethod(method);

        int returnCode = connection.getResponseCode();
        InputStream connectionIn;
        if (returnCode == 200) {
            connectionIn = connection.getInputStream();
        } else {
            connectionIn = connection.getErrorStream();
        }
        StringBuilder result = new StringBuilder();
        BufferedReader buffer = new BufferedReader(new InputStreamReader(connectionIn));
        String inputLine;
        while ((inputLine = buffer.readLine()) != null) {
            result.append(inputLine).append('\n');
        }
        return result.toString();
    }

    public static void main(String[] args) throws IOException {
        String usrv = args[0];
        String[] parms = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            parms[i - 1] = args[i];
        }
        System.out.println(watson(BLUEMIX_HOST, usrv, parms));
    }
}
