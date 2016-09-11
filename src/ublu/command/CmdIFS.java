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
package ublu.command;

import ublu.util.ArgArray;
import ublu.util.DataSink;
import ublu.util.Generics.StringArrayList;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSFileOutputStream;
import com.ibm.as400.access.IFSFileWriter;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.beans.PropertyVetoException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Level;
import ublu.util.Generics.ByteArrayList;

/**
 * Command to access Integrated File System stream files
 *
 * @author jwoehr
 */
public class CmdIFS extends Command {

    {
        setNameAndDescription("ifs",
                "/4? [-ifs,-- @ifsfile] [-as400 @as400] [-create | -delete | -exists | -file | -list | -mkdirs | -read ~@{offset} ~@{chars} | -write [~@{string }] | -writebin ] [-to datasink] [-from datasink] ~@{/fully/qualified/pathname} ~@{system} ~@{user} ~@{password} : integrated file system access");
    }

    /**
     * The functions performed by the ifs command
     */
    protected static enum FUNCTIONS {

        /**
         * Create an IFS file
         */
        CREATE,
        /**
         * Delete an IFS file
         */
        DELETE,
        /**
         * Test existence of an IFS file
         */
        EXISTS,
        /**
         * Instance object representing file
         */
        FILE,
        /**
         * List directory of IFS files
         */
        LIST,
        /**
         * Create a dir or dir hierarchy
         */
        MKDIRS,
        /**
         * Nada
         */
        NOOP,
        /**
         * Read an IFS file
         */
        READ,
        /**
         * Write an IFS text file
         */
        WRITE,
        /**
         * Write an IFS binary file
         */
        WRITEBIN
    }

    /**
     * Create instance
     */
    public CmdIFS() {
    }

    private Tuple ifsFileTuple = null;

    /**
     * Parse arguments and perform IFS operations
     *
     * @param argArray passed-in arg array
     * @return rest of arg array
     */
    public ArgArray ifs(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.NOOP;
        String writeableString = null;
        int offset = 0;
        int numToRead = 0;

        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-ifs":
                case "--":
                    ifsFileTuple = argArray.nextTupleOrPop();
                    break;
                case "-as400":
                    setAs400(getAS400Tuple(argArray.next()));
                    break;
                case "-to":
                    setDataDest(DataSink.fromSinkName(argArray.next()));
                    break;
                case "-from":
                    setDataSrc(DataSink.fromSinkName(argArray.next()));
                    break;
                case "-create":
                    function = FUNCTIONS.CREATE;
                    break;
                case "-delete":
                    function = FUNCTIONS.DELETE;
                    break;
                case "-exists":
                    function = FUNCTIONS.EXISTS;
                    break;
                case "-file":
                    function = FUNCTIONS.FILE;
                    break;
                case "-list":
                    function = FUNCTIONS.LIST;
                    break;
                case "-mkdirs":
                    function = FUNCTIONS.MKDIRS;
                    break;
                case "-noop":
                    break;
                case "-read":
                    function = FUNCTIONS.READ;
                    offset = argArray.nextIntMaybeQuotationTuplePopString();
                    numToRead = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-write":
                    function = FUNCTIONS.WRITE;
                    if (getDataSrc().getType() == DataSink.SINKTYPE.STD) {
                        writeableString = argArray.nextMaybeQuotationTuplePopString();
                    }
                    break;
                case "-writebin":
                    function = FUNCTIONS.WRITEBIN;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            switch (function) {
                case CREATE:
                    ifsCreate(argArray);
                    break;
                case DELETE:
                    ifsDelete(argArray);
                    break;
                case EXISTS:
                    ifsExists(argArray);
                    break;
                case FILE:
                    ifsFile(argArray);
                    break;
                case LIST:
                    ifsList(argArray);
                    break;
                case MKDIRS:
                    ifsMkdirs(argArray);
                    break;
                case NOOP:
                    ifsNoop();
                    break;
                case READ:
                    ifsRead(argArray, offset, numToRead);
                    break;
                case WRITE:
                    ifsWrite(argArray, writeableString);
                    break;
                case WRITEBIN:
                    ifsWriteBin(argArray);
                    break;
            }
        }
        return argArray;
    }

    private IFSFile getIFSFileFromDataSink(DataSink datasink) {
        IFSFile ifsFile = null;
        if (datasink.getType() == DataSink.SINKTYPE.TUPLE) {
            Tuple t = getTuple(datasink.getName());
            Object o = t.getValue();
            ifsFile = o instanceof IFSFile ? IFSFile.class.cast(o) : null;
        }
        return ifsFile;
    }

    private IFSFile getIFSFileFromEponymous() {
        IFSFile ifsFile = null;
        if (ifsFileTuple != null) {
            Object o = ifsFileTuple.getValue();
            ifsFile = o instanceof IFSFile ? IFSFile.class.cast(o) : null;
        }
        return ifsFile;
    }

    private IFSFile getIFSFileFromDataSource() {
        IFSFile ifsFile = null;
        ifsFile = getIFSFileFromEponymous();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromDataSink(getDataSrc());
        }
        return ifsFile;
    }

    private IFSFile getIFSFileFromDataDest() {
        IFSFile ifsFile = null;
        ifsFile = getIFSFileFromEponymous();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromDataSink(getDataDest());
        }
        return ifsFile;
    }

    private IFSFile getIFSFileFromArgArray(ArgArray argArray) {
        IFSFile ifsFile = null;
        if (getAs400() == null && argArray.size() < 3) { // if no passed-in AS400 instance and not enough args to generate one
            logArgArrayTooShortError(argArray);
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            String fqpn = argArray.nextMaybeQuotationTuplePopString();
            if (getAs400() == null) {
                try {
                    setAs400FromArgs(argArray);
                } catch (PropertyVetoException ex) {
                    getLogger().log(Level.SEVERE,
                            "The ifs commmand encountered an exception getting an IFS file from the supplied command arguments.", ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (getAs400() != null) {
                ifsFile = new IFSFile(getAs400(), fqpn);
            }
        }
        return ifsFile;
    }

    private String getTextToWrite(AS400 as400, String writeableString) throws FileNotFoundException, IOException {
        String text = null;
        DataSink dsrc = getDataSrc();
        switch (dsrc.getType()) {
            case STD:
                text = writeableString;
                break;
            case FILE:
                File f = new File(dsrc.getName());
                FileReader fr = new FileReader(f);
                int length = new Long(f.length()).intValue();
                char[] in = new char[length];
                fr.read(in);
                text = new String(in);
                break;
            case TUPLE:
                text = getTuple(dsrc.getName()).getValue().toString();
                break;
        }
        return text;
    }

    private void ifsCreate(ArgArray argArray) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                put(ifsFile.createNewFile());
            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | InterruptedException | ObjectDoesNotExistException | ErrorCompletingRequestException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -create operation", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private void ifsDelete(ArgArray argArray) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                put(ifsFile.delete());
            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -create operation", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private void ifsExists(ArgArray argArray) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                put(ifsFile.exists());
            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception putting the result to the destination datasink.", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private void ifsFile(ArgArray argArray) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        try {
            put(ifsFile);
        } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
            getLogger().log(Level.SEVERE,
                    "The ifs commmand encountered an exception in the -file operation.", ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    private void ifsList(ArgArray argArray) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                StringArrayList sal = new StringArrayList(ifsFile.list());
                put(sal);
            } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -list operation.", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private void ifsMkdirs(ArgArray argArray) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                put(ifsFile.mkdirs());
            } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -mkdirs operation.", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private void ifsNoop() {
        try {
            put(FUNCTIONS.NOOP.name());
        } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
            getLogger().log(Level.SEVERE, "The ifs commmand encountered an exception in the -noop operation.", ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    private void ifsRead(ArgArray argArray, int offset, int length) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                IFSFileInputStream ifsIn = new IFSFileInputStream(ifsFile);
                byte[] data = new byte[length];
                int numRead = ifsIn.read(data, offset, length);
                String s = new String(data, 0, numRead);
                put(s);
            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in putting from the -read operation.", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private void ifsWrite(ArgArray argArray, String writeableString) {
        IFSFile ifsFile = getIFSFileFromDataDest();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                String text = getTextToWrite(ifsFile.getSystem(), writeableString);
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new IFSFileWriter(ifsFile)))) {
                    // writer.print(aS400Text);
                    writer.print(text);
                }
            } catch (AS400SecurityException | IOException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -write operation.", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
    }

    private byte[] getBytesToWrite() throws FileNotFoundException, IOException {
        byte[] result = null;
        DataSink ds = getDataSrc();
        switch (ds.getType()) {
            case STD:
                getLogger().log(Level.SEVERE, "Cannot write a binary file from STD: in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
                break;
            case FILE:
                File f = new File(ds.getName());
                Long length = f.length();
                result = new byte[length.intValue()];
                FileInputStream fis = new FileInputStream(ds.getName());
                int numread = fis.read(result);
                if (numread != length) {
                    getLogger().log(Level.WARNING, "File is {0} bytes long but {1} bytes were read in {2}", new Object[]{length, numread, getNameAndDescription()});
                }
                break;
            case TUPLE:
                Object o = getTuple(getDataSrc().getName()).getValue();
                if (o instanceof ByteArrayList) {
                    result = ByteArrayList.class.cast(o).byteArray();
                } else if (o instanceof byte[]) {
                    result = (byte[]) o;
                } else {
                    getLogger().log(Level.SEVERE, "Tuple {0} is not a byte array in {1}", new Object[]{getDataSrc().getName(), getNameAndDescription()});
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
                break;
        }
        return result;
    }

    private void ifsWriteBin(ArgArray argArray) {
        IFSFile ifsFile = null;
        ifsFile = getIFSFileFromDataDest();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            byte[] bytes = null;
            try {
                bytes = getBytesToWrite();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Exception reading file in -writebin in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);

            }
            if (bytes != null) {
                try (IFSFileOutputStream ifsout = new IFSFileOutputStream(ifsFile)) {
                    // writer.print(aS400Text);
                    ifsout.write(bytes);
                } catch (AS400SecurityException | IOException ex) {
                    getLogger().log(Level.SEVERE,
                            "Exception encountered in the -writebin operation of " + getNameAndDescription(), ex);
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else {
                getLogger().log(Level.SEVERE, "No bytes provided in the -write operation of {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -writebin operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    @Override
    public ArgArray cmd(ArgArray args
    ) {
        reinit();
        return ifs(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
