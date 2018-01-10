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
package ublu.command;

import ublu.util.ArgArray;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import ublu.util.Generics.ByteArrayList;

/**
 * Get a list of objects on the system
 *
 * @author jwoehr
 */
public class CmdSocket extends Command {

    {
        setNameAndDescription("sock", "/0 [-to datasink] [--,-sock ~@sock] [-host ~@{host_or_ip_addr}] [-port ~@{portnum}] [-locaddr ~@{local_addr}] [-locport ~@{local_portnum}] [-usessl] [-ssl @tf] [-instance | -close | -avail | -read ~@{count} | -write ~@bytes] : create and manipulate sockets");
    }

    /**
     * the operations we know
     */
    protected enum OPS {

        /**
         * Create the socket
         */
        INSTANCE,
        /**
         * available count
         */
        AVAIL,
        /**
         * read
         */
        READ,
        /**
         * write
         */
        WRITE,
        /**
         * close
         */
        CLOSE,
        /**
         * query setting
         */
        QUERY,
        /**
         * set setting
         */
        SET

    }

    /**
     * Arity-0 ctor
     */
    public CmdSocket() {
    }
    private String host = null;
    private Integer portnum = null;
    private String localAddr = null;
    private Integer localPort = null;
    private boolean usessl = false;
    private Tuple sockTuple = null;
    private Tuple writeTuple = null;
    private int readCount = 0;

    /**
     * retrieve a (filtered) list of OS400 Objects on the system
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray doSock(ArgArray argArray) {
        OPS op = OPS.INSTANCE;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-sock":
                case "--":
                    sockTuple = argArray.nextTupleOrPop();
                    break;
                case "-new":
                    op = OPS.INSTANCE;
                    break;
                case "-port":
                    portnum = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-host":
                    host = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-locaddr":
                    localAddr = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-locport":
                    localPort = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-usessl":
                    usessl = true;
                    break;
                case "-ssl":
                    usessl = argArray.nextBooleanTupleOrPop().booleanValue();
                    break;
                case "-close":
                    op = OPS.CLOSE;
                    break;
                case "-avail":
                    op = OPS.AVAIL;
                    break;
                case "-read":
                    op = OPS.READ;
                    readCount = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-write":
                    op = OPS.WRITE;
                    writeTuple = argArray.nextTupleOrPop();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Socket socket = null;
            byte[] b;
            ByteArrayList bytesRead = null;
            switch (op) {
                case INSTANCE: {
                    try {
                        socket = usessl ? sslSockInstance() : sockInstance();
                    } catch (IOException ex) {
                        getLogger().log(Level.SEVERE, "Exception creating socket in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
                if (socket != null) {
                    try {
                        put(socket);
                    } catch (SQLException | AS400SecurityException | RequestNotSupportedException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Exception putting socket in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
                break;
                case CLOSE:
                    socket = sockFromTuple();
                    if (socket != null) {
                        try {
                            socket.shutdownInput();
                            socket.shutdownOutput();
                            socket.close();
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Exception closing socket in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case AVAIL:
                    socket = sockFromTuple();
                    if (socket != null) {
                        try {
                            put(socket.getInputStream().available());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception getting or putting available count in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case READ:
                    socket = sockFromTuple();
                    if (socket != null) {
                        try {
                            InputStream is = socket.getInputStream();
                            b = new byte[readCount];
                            int numread = is.read(b);
                            bytesRead = new ByteArrayList(b, numread);

                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Exception reading socket in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        try {
                            put(bytesRead);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting read bytes in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case WRITE:
                    socket = sockFromTuple();
                    b = bytesFromWriteTuple();
                    if (socket != null) {
                        if (b != null) {
                            try {
                                OutputStream os = socket.getOutputStream();
                                os.write(b);
                            } catch (IOException ex) {
                                getLogger().log(Level.SEVERE, "Exception writing socket in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.INFO, "No bytes provided and no bytes written in {0}", getNameAndDescription());
                        }
                    }
                    break;
                default:
                    getLogger().log(Level.WARNING, "Unknown operation in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return argArray;
    }

    private Socket sockInstance() throws IOException {
        Socket so = null;
        if ((localAddr == null && localPort != null)
                || (localAddr != null && localPort == null)
                || host == null || portnum == null) {
            getLogger().log(Level.WARNING, "Incompatible settings (missing value?) in instancing socket in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (localAddr == null) {
            so = new Socket(InetAddress.getByName(host), portnum);
        } else {
            so = new Socket(InetAddress.getByName(host), portnum, InetAddress.getByName(localAddr), localPort);
        }
        return so;
    }

    private Socket sslSockInstance() throws UnknownHostException, IOException {
        Socket so = null;
        if ((localAddr == null && localPort != null)
                || (localAddr != null && localPort == null)
                || host == null || portnum == null) {
            getLogger().log(Level.WARNING, "Incompatible settings (missing value?) in instancing socket in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {

            SocketFactory sf = SSLSocketFactory.getDefault();

            if (localAddr == null) {

                so = sf.createSocket(InetAddress.getByName(host), portnum);
            } else {
                so = sf.createSocket(InetAddress.getByName(host), portnum, InetAddress.getByName(localAddr), localPort);
            }
        }
        return so;
    }

    private Socket sockFromTuple() {
        Socket so = sockTuple.value(Socket.class);
        if (so == null) {
            getLogger().log(Level.WARNING, "No socket provided in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        return so;
    }

    private byte[] bytesFromWriteTuple() {
        byte[] b = null;
        if (writeTuple != null) {
            Object o = writeTuple.getValue();
            if (o instanceof String) {
                b = o.toString().getBytes();

            } else if (o instanceof ByteArrayList) {
                b = ByteArrayList.class
                        .cast(o).byteArray();
            } else if (o instanceof byte[]) {
                b = (byte[]) o;
            }
        }
        return b;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doSock(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
