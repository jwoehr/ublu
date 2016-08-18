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
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to fetch spooled files as text. ReportFetcher uses
 * {@link ublu.TransformedSpooledFileFetcher} to fetch the EBCDIC spooled file
 * and formats it in ASCII for the simplest printer format we have found
 * (IBM4019). ReportFetcher then strips the print formatting (hack hack hack)
 * and delivers plain ASCII text.
 *
 * @author jwoehr
 */
public class ReportFetcher {

    private static final Logger LOG = Logger.getLogger(ReportFetcher.class.getName());
    /**
     * The actual fetcher from the host
     */
    protected TransformedSpooledFileFetcher myFetcher = null;
    private AS400 myAS400 = null;

    /**
     * Instance with host instanced via {@link AS400Factory}.
     *
     * @param sysName system
     * @param userId user
     * @param passwd password
     * @throws PropertyVetoException
     */
    public ReportFetcher(String sysName, String userId, String passwd) throws PropertyVetoException {
        myAS400 = AS400Factory.newAS400(sysName, userId, passwd);
    }

    /**
     * Instance with host already instanced as AS400 object.
     *
     * @param as400 AS400 instance
     * @throws PropertyVetoException
     */
    public ReportFetcher(AS400 as400) throws PropertyVetoException {
        myAS400 = as400;
    }

    /**
     * Hideous unsound special case hacks to remove the print header without
     * general knowledge of that header format.
     *
     * @param report The messy ASCII contents of the report
     * @return the report minus print header
     */
    public static String peelHeaderHack(String report) {
        String result = report;
        int offset = result.indexOf("RUN"); // All reports we currently want except one start with "RUN DATE"
        if (offset > 0) {
            result = result.substring(offset);
        } else {
            offset = result.indexOf("DUES"); // The LIST report is different
            if (offset > 0) {
                result = result.substring(offset);
            }
        }
        return result;
    }

    /**
     * Special case cleanup to rid the file of print formatting we have no docs
     * for.
     *
     * @param report the report we have fetched cluttered with IBM print
     * formatting
     * @return the cleaned up report
     */
    public static String peelPrinterControlsHack(String report) {
        String result = report.replaceAll("\\x01\\x00\\x0d", "");
        result = result.replaceAll("\\x0d\\x0c", "");
        result = result.replaceAll("\\x1b\\x5b.", "");
        result = result.replaceAll("\\x1bCB", "");
        result = result.replaceAll("\\x1b.", "");
        result = result.replaceAll("\\x01\\x00", "");
        result = result.replaceAll("\\x02\\x00", "");
        result = result.replaceAll("\\x18B\\x5c", "");
        result = result.replaceAll("\\x18\\x5c", "");
        result = result.replaceAll("\\x01\\x5c", "");
        result = result.replaceAll("\\x03\\x5c", "");
        // result = result.replaceAll("\\x00", "");
        // result = result.replaceAll("\\x01", "");
        // result = result.replaceAll("\\x03", "");
        // result = result.replaceAll("\\x04", "");
        // result = result.replaceAll("\\x0a", "");
        result = result.replaceAll("\\x0c", "");
        result = result.replaceAll("\\x0d", "");
        // result = result.replaceAll("\\x18", "");
        // result = result.replaceAll("\\xef\\xbf\\xbd", "");
        result = result.replaceAll("[^\\p{Print}^\\n]", "");
        return result;
    }

    /**
     * Instance an {@link TransformedSpooledFileFetcher} and fetch the text
     * version of the spool file. Not currently used, instead
     * {@link #fetchTidied}
     *
     * @param spoolFileName
     * @param spoolNumber
     * @param jobName
     * @param jobUser
     * @param jobNumber
     * @return
     * @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    /* public String fetch(String spoolFileName, int spoolNumber, String jobName, String jobUser, String jobNumber)
     * throws AS400Exception, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, RequestNotSupportedException {
     * String result = new TransformedSpooledFileFetcher(myAS400, spoolFileName, spoolNumber, jobName, jobUser, jobNumber).fetchSpooledFile();
     * return result;
     * }*/
    /**
     * Instance an {@link TransformedSpooledFileFetcher} and fetch the text
     * version of the spool file neatly transformed and hackingly cleaned up.
     *
     * @param spoolFileName
     * @param spoolNumber
     * @param jobName
     * @param jobUser
     * @param jobNumber
     * @return tidied report
     * @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    public String fetchTidied(String spoolFileName, int spoolNumber, String jobName, String jobUser, String jobNumber)
            throws AS400Exception, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, RequestNotSupportedException {
        String result = new TransformedSpooledFileFetcher(myAS400, spoolFileName, spoolNumber, jobName, jobUser, jobNumber).fetchSpooledFile();
        result = peelHeaderHack(result);
        result = peelPrinterControlsHack(result);
        return result;
    }

    /**
     * Using an already-instanced {@link TransformedSpooledFileFetcher} fetch
     * the text version of the spool file neatly transformed and hackingly
     * cleaned up.
     *
     * @param tsff The instance of TransformedSpooledFileFetcher with its
     * SpooledFile object.
     * @return tidied report
     * @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    public static String fetchTidied(TransformedSpooledFileFetcher tsff) throws AS400Exception, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, RequestNotSupportedException {
        String result = tsff.fetchSpooledFile();
        result = peelHeaderHack(result);
        result = peelPrinterControlsHack(result);
        return result;
    }

    /**
     * A test main to try out the report fetcher
     *
     * @param args sysName userId password spoolFileName spoolNumber jobName
     * jobUser jobNumber
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
            ReportFetcher fetcher = new ReportFetcher(sysName, userId, password);
            System.out.print(fetcher.fetchTidied(spoolFileName, spoolNumber, jobName, jobUser, jobNumber));
            System.out.flush();
        } catch (AS400SecurityException | PropertyVetoException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex) {
            LOG.log(Level.SEVERE, "Exception fetching spooled file", ex);
        }
    }
}
