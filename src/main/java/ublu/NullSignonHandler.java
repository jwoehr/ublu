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

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.SignonEvent;
import com.ibm.as400.access.SignonHandlerAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A custom signon handler for those instances when there's a problem with the
 * credentials provided by user. This one just shrugs and lets the operation
 * fail.
 *
 * @author jwoehr
 */
public class NullSignonHandler extends SignonHandlerAdapter {

    /**
     * Creates a new instance of NullSignonHandler
     */
    public NullSignonHandler() {
    }

    @Override
    public boolean passwordIncorrect(SignonEvent event) {
        boolean result = false;
        Logger.getLogger(NullSignonHandler.class.getName()).log(Level.SEVERE, "password incorrect");
        return result;
    }

    @Override
    public boolean userIdUnknown(SignonEvent event) {
        boolean result = false;
        Logger.getLogger(NullSignonHandler.class.getName()).log(Level.SEVERE, "user id unknown");
        return result;
    }

    @Override
    public void exceptionOccurred(SignonEvent event) throws AS400SecurityException {
        throw event.getException();
    }
}
