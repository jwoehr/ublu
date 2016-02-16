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

import ublu.util.Generics.SpooledFileArrayList;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ConnectionDroppedException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.SpooledFileList;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetcher for lists of spooled files from OS400 host.
 *
 * @see com.ibm.as400.access.SpooledFileList
 * @author jwoehr
 */
public class SpooledFileLister {

    private SpooledFileList spooledFileList = null;
    private static final Logger LOG = Logger.getLogger(SpooledFileLister.class.getName());

    /**
     * Get the JTOpen SpooledFileList object we use to fetch spooled files.
     *
     * @return the JTOpen SpooledFileList object
     * @see com.ibm.as400.access.SpooledFileList
     */
    public SpooledFileList getSpooledFileList() {
        return spooledFileList;
    }

    /**
     * Set the JTOpen SpooledFileList object we use to fetch spooled files.
     *
     * @param spooledFileList the JTOpen SpooledFileList object
     * @see com.ibm.as400.access.SpooledFileList
     */
    public final void setSpooledFileList(SpooledFileList spooledFileList) {
        this.spooledFileList = spooledFileList;
    }

    /**
     * Instance with a target system from which we intend to retrieve spooled
     * files.
     *
     * @param as400 the target system
     */
    public SpooledFileLister(AS400 as400) {
        setSpooledFileList(new SpooledFileList(as400));
    }

    /**
     * Set the filter for users whose spooled files we want.
     *
     * <p>The value can be any specific value or the special value *ALL. The
     * value cannot be greater than 10 characters. The default is *ALL.</p>
     *
     * @param userFilter filter for users whose spooled files we want
     * @throws PropertyVetoException
     * @throws AS400SecurityException
     * @see com.ibm.as400.access.SpooledFileList
     */
    public void setUserFilter(String userFilter) throws PropertyVetoException, AS400SecurityException {
        getSpooledFileList().setUserFilter(userFilter);
    }

    /**
     * Fetch the spooled file list from the host and wait for it to return.
     *
     * @return an enumeration of the spooled files matching the filter.
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws RequestNotSupportedException
     */
    public Enumeration getSynchronously() throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, RequestNotSupportedException {
        getSpooledFileList().openSynchronously();
        return getSpooledFileList().getObjects();
    }

    /**
     * Fetch the spooled file list from the host and wait for it to return, then
     * put the spooled file objects from the list into a SpooledFileArrayList.
     *
     * @return the resultant SpooledFileArrayList
     * @throws AS400Exception
     * @throws AS400SecurityException
     * @throws ConnectionDroppedException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws RequestNotSupportedException
     */
    public SpooledFileArrayList getSpooledFileListSynchronously() throws AS400Exception, AS400SecurityException, ConnectionDroppedException, ErrorCompletingRequestException, InterruptedException, IOException, RequestNotSupportedException {
        SpooledFileArrayList vspf = new SpooledFileArrayList();
        getSpooledFileList().openSynchronously();
        Enumeration e = getSpooledFileList().getObjects();
        while (e.hasMoreElements()) {
            vspf.add(SpooledFile.class.cast(e.nextElement()));
        }
        return vspf;
    }

    /**
     * Test routine for fetching a spooled file list
     *
     * @param args systemName userId password userFilter
     * @see #setUserFilter(java.lang.String)
     */
    public static void main(String args[]) {
        String systemName = args[0];
        String userId = args[1];
        String password = args[2];
        String userFilter = args[3];
        try {
            AS400 as400 = AS400Factory.newAS400(systemName, userId, password);
            SpooledFileLister lister = new SpooledFileLister(as400);
            lister.setUserFilter(userFilter);
            Enumeration e = lister.getSynchronously();
            while (e.hasMoreElements()) {
                SpooledFile spf = SpooledFile.class.cast(e.nextElement());
                System.out.println(spf.getName() + " " + spf.getNumber() + " " + spf.getJobName() + " " + spf.getJobUser() + " " + spf.getJobNumber() + " " + spf.getCreateDate() + " " + spf.getCreateTime());
            }
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | PropertyVetoException | RequestNotSupportedException ex) {
            LOG.log(Level.SEVERE, "Exception fetching list of spooled files", ex);
        }
    }
}