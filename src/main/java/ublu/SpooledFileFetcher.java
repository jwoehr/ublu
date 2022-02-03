/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
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

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.PrintObjectInputStream;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @deprecated Use {@link ublu.TransformedSpooledFileFetcher} instead
 * @author Administrator
 */
public class SpooledFileFetcher {

    private final SpooledFile mySpooledFile;
    private static final Logger LOG = Logger.getLogger(SpooledFileFetcher.class.getName());

    /**
     *
     * @param system
     * @param name
     * @param number
     * @param jobName
     * @param jobUser
     * @param jobNumber
     * @param jobSysName
     * @param createDate
     * @param createTime
     */
    public SpooledFileFetcher(AS400 system, String name, int number, String jobName, String jobUser, String jobNumber, String jobSysName, String createDate, String createTime) {
        mySpooledFile = new SpooledFile(system, name, number, jobName, jobUser, jobNumber, jobSysName, createDate, createTime);
    }

    /**
     *
     * @param system
     * @param name
     * @param number
     * @param jobName
     * @param jobUser
     * @param jobNumber
     */
    public SpooledFileFetcher(AS400 system, String name, int number, String jobName, String jobUser, String jobNumber) {
        mySpooledFile = new SpooledFile(system, name, number, jobName, jobUser, jobNumber);
    }

    /**
     * Removes non-printing control chars of the sort that litter OS/400 spooled
     * print files
     *
     * @param input string to clean up
     * @return cleaned up string
     */
    public static String sanitizeAscii(String input) {
        // String regex = "[\000-\011[\013-\014[\016-\037[\0177]]]]";
        // ^\\p{Print}"
        String s = input.replaceAll("[\\x7f]", "\n");
        s = s.replaceAll("\\?D\\?\\?", "\n\t");
        // String regex = "[^\\p{Print}^\\x20^\\n^\\t]";
        String regex = "[^\\p{Print}^\\n]";
        return s.replaceAll(regex, "")/* .replaceAll("\r", "\n")*/;
    }

    /**
     *
     * @param input
     * @return
     */
    public static String peelPrintFormatting(String input) {
        // String s = input.replace("\\D*(\\d\\d\\d)","\\1");
        String s = input.replaceAll("\\{", "\t");
        int firstTab = s.indexOf("\t");
        /* Debug */
        // LOG.log(Level.INFO, "index of first tab is " + firstTab);
        /* End Debug */
        if (firstTab > -1) {
            s = s.substring(firstTab);
        }

        return s /* .replaceAll("\\r","\\n") */;
    }

    /**
     *
     * @return @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    public String fetchSpooledFile() throws AS400Exception,
            AS400SecurityException,
            ErrorCompletingRequestException,
            IOException,
            InterruptedException,
            RequestNotSupportedException {
        StringBuilder sb = new StringBuilder();
        try (PrintObjectInputStream printObjectInputStream = mySpooledFile.getInputStream()) {
            int available = printObjectInputStream.available();
            while (available > 0) {
                LOG.log(Level.INFO, "available: {0}", available);
                byte[] bytes = new byte[available];
                LOG.log(Level.INFO, "bytes.length: {0}", bytes.length);
                int bytesRead = printObjectInputStream.read(bytes);
                LOG.log(Level.INFO, "bytesRead: {0}", bytesRead);
                if (bytesRead > 0) {
                    String s = new String(bytes, "IBM037");
                    sb.append(s);
                }
                available = printObjectInputStream.available();
                LOG.log(Level.INFO, "available: {0}", available);

            }
        }
        return peelPrintFormatting(sanitizeAscii(sb.toString()));
        // return sb.toString();
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        String sysName = args[0];
        String userId = args[1];
        String password = args[2];
        String spoolFileName = args[3];
        int spoolNumber = Integer.parseInt(args[4]);
        String jobName = args[5];
        String jobUser = args[6];
        String jobNumber = args[7];

        try {
            AS400 as400 = AS400Factory.newAS400(sysName, userId, password);
            SpooledFileFetcher fetcher = new SpooledFileFetcher(as400, spoolFileName, spoolNumber, jobName, jobUser, jobNumber);
            System.out.print(fetcher.fetchSpooledFile());
            System.out.flush();
        } catch (AS400SecurityException | PropertyVetoException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex) {
            LOG.log(Level.SEVERE, "Exception fetching spooled file", ex);
        }

    }
}
