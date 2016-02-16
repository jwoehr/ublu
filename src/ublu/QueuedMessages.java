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
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.QueuedMessage;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Enumeration;

/**
 * @deprecated Currently not used, possibly later should be a factor of CmdMsgQ
 * @author Administrator
 */
public class QueuedMessages {

    /**
     *
     */
    protected QueuedMessages() {
    }

    /**
     * Get an enumeration of all messages in a particular queue. Does not reply
     * them, i.e., does not delete them from the original queue
     *
     * @param as400
     * @param path IFS path to the queue, e.g., /QSYS.LIB/QSYSOPR.MSG or
     * /QUSRSYS.LIB/SOMEUSR.MSG
     * @return an enumeration of all the messages waiting, accessing them does
     * not reply them so does not delete them
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     */
    public static Enumeration getAllQueuedMessagesForQPath(AS400 as400, String path)
            throws AS400SecurityException, ErrorCompletingRequestException, IOException, InterruptedException, ObjectDoesNotExistException {
        MessageQueue mq = new MessageQueue(as400, path);
        return mq.getMessages();
    }

    /**
     * Retrieves (without replying/deleting) all messages in a given queue and
     * prints 'em out.
     *
     * @param args system userid password IFSpath-to-queue
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws IOException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws PropertyVetoException
     */
    public static void main(String args[]) throws AS400SecurityException, ErrorCompletingRequestException,
            IOException, InterruptedException, ObjectDoesNotExistException, PropertyVetoException {
        String system = args[0];
        String userid = args[1];
        String passwd = args[2];
        String path = args[3];
        StringBuilder sb = new StringBuilder();
        AS400 as400 = AS400Factory.newAS400(system, userid, passwd);
        Enumeration e = getAllQueuedMessagesForQPath(as400, path);
        while (e.hasMoreElements()) {
            QueuedMessage msg = QueuedMessage.class.cast(e.nextElement());
            sb.append(msg.toString()).append("\n");
        }
        System.out.print(sb.toString() + "\n");
    }
}
