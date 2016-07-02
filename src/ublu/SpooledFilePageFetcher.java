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
package ublu;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.PrintObjectPageInputStream;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @deprecated Use TransformedSpoolFileFetcher instead
 * @author jwoehr
 */
public class SpooledFilePageFetcher {

    private static final Logger LOG = Logger.getLogger(SpooledFilePageFetcher.class.getName());
    private SpooledFile mySpooledFile = null;

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
    public SpooledFilePageFetcher(AS400 system, String name, int number, String jobName, String jobUser, String jobNumber, String jobSysName, String createDate, String createTime) {
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
    public SpooledFilePageFetcher(AS400 system, String name, int number, String jobName, String jobUser, String jobNumber) {
        mySpooledFile = new SpooledFile(system, name, number, jobName, jobUser, jobNumber);
    }

    /**
     * Removes non-printing control chars of the sort that litter OS/400 spooled
     * print files
     *
     * @param input string to clean up
     * @return cleaned up string
     */
    public static String sanitize(String input) {
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
     * @return 
     * @throws java.io.IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    public String readPage() throws IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, RequestNotSupportedException {
        StringBuilder sb = new StringBuilder();
        PrintObjectPageInputStream in = mySpooledFile.getPageInputStream(null);
        int available = in.available();
        byte[] bytes = null;
        while (available > 0) {
            bytes = new byte[available];
            int bytesRead = in.read(bytes);
            if (bytesRead > 0) {
                String s = new String(bytes, "IBM037");
                sb.append(s);
            }
            available = in.available();
        }
        // return peelPrintFormatting(sanitize(sb.toString()));
        return sb.toString();
    }

    /**
     *
     * @return @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    public String readAllPages() throws IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, RequestNotSupportedException {
        StringBuilder sb = new StringBuilder();
        PrintObjectPageInputStream in = mySpooledFile.getPageInputStream(null);
        boolean hasPage = true;
        while (hasPage) {
            int available = in.available();
            byte[] bytes = null;
            while (available > 0) {
                bytes = new byte[available];
                int bytesRead = in.read(bytes);
                if (bytesRead > 0) {
                    String s = new String(bytes, "IBM037");
                    sb.append(s);
                }
                available = in.available();
            }
            hasPage = in.nextPage();
        }
        // return peelPrintFormatting(sanitize(sb.toString()));
        return sb.toString();
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
            SpooledFilePageFetcher fetcher = new SpooledFilePageFetcher(as400, spoolFileName, spoolNumber, jobName, jobUser, jobNumber);
            System.out.print(fetcher.readAllPages());
            System.out.flush();
        } catch (AS400SecurityException | PropertyVetoException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex) {
            LOG.log(Level.SEVERE, "Exception fetching spooled file", ex);
        }

    }
}
