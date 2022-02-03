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
import com.ibm.as400.access.PrintObject;
import com.ibm.as400.access.PrintObjectTransformedInputStream;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetch a spooled file with transformation to a printer whose formatting
 * nonsense we can handle. If we ever find a pure ASCII transform we'll use it.
 * As things stand we use IBM4019 transform, their simplest ASCII printer.
 *
 * @author jwoehr
 */
public class TransformedSpooledFileFetcher {

    private final SpooledFile mySpooledFile;
    private static final Logger LOG = Logger.getLogger(TransformedSpooledFileFetcher.class.getName());

    /**
     * Instance with the relevant fields instanced.
     *
     * @param system target host
     * @param name name of spooled file
     * @param number number of spooled file
     * @param jobName name of creating job
     * @param jobUser name of user whose job it was
     * @param jobNumber d'oh
     * @param jobSysName name of the subsystem job ran in
     * @param createDate The date is encoded in a character string with the
     * following format, CYYMMDD where: <ul> <li>C is the Century where 0
     * indicates years 19xx and 1 indicates years 20xx</li> <li>YY is the
     * Year</li> <li>MM is the Month</li> <li>DD is the Day</li></ul>
     * @param createTime The time is encoded in a character string with the
     * following format, HHMMSS where: <ul> <li>HH - Hour</li> <li>MM -
     * Minutes</li> <li>SS - Seconds</li></ul>
     * @see com.ibm.as400.access.SpooledFile
     */
    public TransformedSpooledFileFetcher(AS400 system, String name, int number, String jobName, String jobUser, String jobNumber, String jobSysName, String createDate, String createTime) {
        mySpooledFile = new SpooledFile(system, name, number, jobName, jobUser, jobNumber, jobSysName, createDate, createTime);
    }

    /**
     * Instance with the relevant fields instanced, somewhat fewer than the
     * other ctor.
     *
     * @param system target host
     * @param name name of spooled file
     * @param number number of spooled file
     * @param jobName name of creating job
     * @param jobUser name of user whose job it was
     * @param jobNumber d'oh
     */
    public TransformedSpooledFileFetcher(AS400 system, String name, int number, String jobName, String jobUser, String jobNumber) {
        mySpooledFile = new SpooledFile(system, name, number, jobName, jobUser, jobNumber);
    }

    /**
     * Instance directly from a spooled file object
     *
     * @param mySpooledFile the spooled file we wish to fetch.
     */
    public TransformedSpooledFileFetcher(SpooledFile mySpooledFile) {
        this.mySpooledFile = mySpooledFile;
    }

    /**
     * Fetch the spool file transformed to our printer specification.
     *
     * <p>
     * We're using IBM 4019 as the simplest ASCII printer model with the least
     * formatting to plow through and remove. If we find a pure ASCII printer
     * model we'll use that instead.</p>
     *
     * @return the entire transformed spooled file
     * @throws AS400Exception
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
        PrintParameterList ppl = new PrintParameterList();
        // ppl.setParameter(PrintObject.ATTR_MFGTYPE, "*IBM4019");
        // ppl.setParameter(PrintObject.ATTR_MFGTYPE, "*IBM4029");
        ppl.setParameter(PrintObject.ATTR_MFGTYPE, "*IBM4019");
        // ppl.setParameter(PrintObject.ATTR_MFGTYPE, "*HP4");
        // ppl.setParameter(PrintObject.ATTR_SCS2ASCII, "*YES");
        try (PrintObjectTransformedInputStream printObjectTransformedInputStream = mySpooledFile.getTransformedInputStream(ppl)) {
            int available = printObjectTransformedInputStream.available();
            while (available > 0) {
                // LOG.log(Level.INFO, "available: " + available);
                byte[] bytes = new byte[available];
                // LOG.log(Level.INFO, "bytes.length: " + bytes.length);
                int bytesRead = printObjectTransformedInputStream.read(bytes);
                // LOG.log(Level.INFO, "bytesRead: " + bytesRead);
                if (bytesRead > 0) {
                    String s = new String(bytes);
                    sb.append(s);
                }
                available = printObjectTransformedInputStream.available();
                // LOG.log(Level.INFO, "available: " + available);

            }
        }
        // return OS400SpooledFileFetcher.peelPrintFormatting(OS400SpooledFileFetcher.sanitizeAscii(sb.toString()));
        // return OS400SpooledFileFetcher.sanitizeAscii(sb.toString());
        return sb.toString();
    }

    /**
     * A test of fetching the transformed spooled file and transforming it.
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
            AS400 as400 = AS400Factory.newAS400(sysName, userId, password);
            TransformedSpooledFileFetcher fetcher = new TransformedSpooledFileFetcher(as400, spoolFileName, spoolNumber, jobName, jobUser, jobNumber);
            System.out.print(fetcher.fetchSpooledFile());
            System.out.flush();
        } catch (AS400SecurityException | PropertyVetoException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex) {
            LOG.log(Level.SEVERE, "Exception fetching spooled file", ex);
        }

    }
}
