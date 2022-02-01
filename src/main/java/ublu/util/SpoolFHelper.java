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

import ublu.util.Generics.ByteArrayList;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.OutputQueue;
import com.ibm.as400.access.PrintObjectPageInputStream;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.PrinterFile;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.SpooledFileOutputStream;
import java.io.IOException;

/**
 * Encapsulate JTOpen SpooledFile
 *
 * @author jwoehr
 */
public class SpoolFHelper {

    private SpooledFile mySpooledFile = null;

    /**
     * Get the spooled file object
     *
     * @return the spooled file object
     */
    public SpooledFile getMySpooledFile() {
        return mySpooledFile;
    }

    /**
     * Set the spooled file object
     *
     * @param mySpooledFile the spooled file object
     */
    public final void setMySpooledFile(SpooledFile mySpooledFile) {
        this.mySpooledFile = mySpooledFile;
    }

    /**
     * ctor/0
     */
    public SpoolFHelper() {
    }

    /**
     * ctor/1
     *
     * @param splf spooled file to encapsulate
     */
    public SpoolFHelper(SpooledFile splf) {
        this();
        setMySpooledFile(splf);
    }

    /**
     * Read raw data spooled file data
     *
     * @return List of bytes comprising the original spooled file
     * @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    public ByteArrayList read() throws
            AS400Exception, AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, RequestNotSupportedException {
        ByteArrayList byteArrayList = new ByteArrayList();
        try (PrintObjectPageInputStream pageInputStream = getMySpooledFile().getPageInputStream(null)) {
            int available = pageInputStream.available();
            byte[] bytes;
            int bytesRead;
            while (available > 0) {
                bytes = new byte[available];
                bytesRead = pageInputStream.read(bytes);
                if (bytesRead > 0) {
                    byteArrayList.addAll(new ByteArrayList(bytes));
                }
                available = pageInputStream.available();
            }
        }
        return byteArrayList;
    }

    /**
     * Write list of bytes to a spooled file creating it in the process
     *
     * @param bal list of bytes
     * @param as400 the host
     * @param ppl can be null or use #defaultPrinterFile()
     * @param pf required
     * @param oq required
     * @return the new SpooledFile
     * @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     */
    public SpooledFile write(ByteArrayList bal, AS400 as400, PrintParameterList ppl, PrinterFile pf, OutputQueue oq) throws
            AS400Exception, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException {
        SpooledFile result;
        try (SpooledFileOutputStream sfos = new SpooledFileOutputStream(as400, ppl, pf, oq)) {
            sfos.write(bal.byteArray());
            result = sfos.getSpooledFile();
            sfos.flush();
        }
        return result;
    }

    /**
     * Create a default printer file instance to use with #write()
     *
     * @param as400 the host
     * @return the new printer file
     */
    public PrinterFile defaultPrinterFile(AS400 as400) {
        return new PrinterFile(as400, "/QSYS.LIB/QGPL.LIB/QPRINT.FILE");
    }

    /**
     * Create reference to default output queue
     *
     * @param as400 the host
     * @return default output queue
     */
    public OutputQueue defaultOutputQueue(AS400 as400) {
        return new OutputQueue(as400, "/QSYS.LIB/QGPL.LIB/QPRINT.OUTQ");
    }

    /**
     * Copy this SpooledFile to a new SpooledFile on this or another host
     *
     * @param as400 the host
     * @param ppl can be null
     * @param pf required
     * @param oq required
     * @return the new spooled file
     * @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws RequestNotSupportedException
     */
    public SpooledFile copy(AS400 as400, PrintParameterList ppl, PrinterFile pf, OutputQueue oq) throws AS400Exception, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, RequestNotSupportedException {
        ByteArrayList bal = read();
        return (write(bal, as400, ppl, pf, oq));
    }
}