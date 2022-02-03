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
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.SignonEvent;
import com.ibm.as400.access.SignonHandlerAdapter;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A custom signon handler for those instances when there's a problem with the
 * credentials provided by user. Prompts the user text mode.
 *
 * @author jwoehr
 */
public class SignonHandler extends SignonHandlerAdapter {

    /**
     * Creates a new instance of SignonHandler
     */
    public SignonHandler() {
    }

    @Override
    public boolean passwordIncorrect(SignonEvent event) {
        boolean result = false;
        AS400 as400 = AS400.class.cast(event.getSource());
        try {
            result = pollForPassword(as400);
        } catch (IOException ex) {
            Logger.getLogger(SignonHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    @Override
    public boolean userIdUnknown(SignonEvent event) {
        boolean result = false;
        AS400 as400 = AS400.class.cast(event.getSource());
        try {
            result = pollForUserId(as400);
        } catch (IOException | PropertyVetoException ex) {
            Logger.getLogger(SignonHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    @Override
    public void exceptionOccurred(SignonEvent event)
            throws AS400SecurityException {
        boolean reThrowing = true;
        AS400 as400 = AS400.class
                .cast(event.getSource());
        AS400SecurityException aS400SecurityException = event.getException();
        int asexRc = aS400SecurityException.getReturnCode();
        /* Debug */ Logger.getLogger(SignonHandler.class
                .getName()).log(Level.INFO, "AS400SecurityException retcode is {0}", asexRc);
        switch (asexRc) {
            case AS400SecurityException.PASSWORD_ERROR:
            case AS400SecurityException.PASSWORD_INCORRECT:
                try {
                    if (pollForPassword(as400)) {
                        reThrowing = false;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SignonHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
        }
        if (reThrowing) {
            throw aS400SecurityException;
        }
    }

    /**
     * Polls user for a password and if gotten resets password field of as400
     *
     * @param as400 the system we are trying to get a valid password for
     * @return true if got a password from user
     * @throws IOException
     */
    protected boolean pollForPassword(AS400 as400) throws IOException {
        boolean result = false;
        String passwordPrompt = "Please enter a valid password for " + as400.getSystemName() + " (will not echo):";
        char[] password;
        if (System.console() != null) {
            password = System.console().readPassword(passwordPrompt);
        } else {
            if (Ublu.ubluSingleton.isGoubluing()) {
                System.out.println(passwordPrompt);
            } else {
                System.out.print(passwordPrompt);
            }
            password = new BufferedReader(new InputStreamReader(System.in)).readLine().trim().toCharArray();
        }
        String pString = new String(password).trim();
        if (!pString.isEmpty()) {
            as400.setPassword(pString);
            result = true;
        }
        return result;
    }

    /**
     * Polls user for a userid and if gotten resets userid field of as400
     *
     * @param as400 the system we are trying to get a valid userid for
     * @return true if got a userid from user
     * @throws IOException
     * @throws PropertyVetoException
     */
    protected boolean pollForUserId(AS400 as400) throws IOException, PropertyVetoException {
        boolean result = false;
        String useridPrompt = "Please enter a valid userid for " + as400.getSystemName() + ": ";
        if (Ublu.ubluSingleton.isGoubluing()) {
            System.out.println(useridPrompt);
        } else {
            System.out.print(useridPrompt);
        }
        byte[] userid = new byte[16]; // longer than real userid swallows lf
        int numRead = System.in.read(userid);
        if (numRead > 0) {
            String uidString = new String(userid).trim();
            as400.setUserId(uidString);
            result = true;
        }
        return result;
    }
}
