/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
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
import ublu.util.DataSink;
import ublu.util.Generics.StringArrayList;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
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
import java.io.FileOutputStream;
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

    static {
        setNameAndDescription("ifs",
                "/4? [-ifs,-- ~@ifsfile] [-as400 @as400] [-to datasink] [-tofile ~@filepath] [-from datasink] [-fromfile ~@{filepath}] [-length ~@{length}] [-offset ~@{offset}] [-pattern ~@{pattern}] [-b] [-t] [-create | -delete | -exists | -file | -list | -mkdirs | -query ~@{[ccsid|name|ownername|owneruid|path|r|w|x} | -read | -rename ~@{/fully/qualified/path/name} | -set ~@{[ccsid|readonly]} ~@{value} | -size | -write [~@{string }] | -writebin ] ~@{/fully/qualified/pathname} ~@{system} ~@{user} ~@{password} : integrated file system access");
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
         * Get attrib
         */
        QUERY,
        /**
         * Read an IFS file
         */
        READ,
        /**
         * Rename file
         */
        RENAME,
        /**
         * Set attrib
         */
        SET,
        /**
         * Write an IFS text file
         */
        WRITE,
        /**
         * Write an IFS binary file
         */
        WRITEBIN,
        /**
         * Get length of IFS file
         *
         */
        SIZE
    }

    /**
     * Create instance
     */
    public CmdIFS() {
    }

    private Tuple ifsFileTuple = null;
    private Integer binOffset = null;
    private Integer binLength = null;
    boolean translate = false;
    boolean binary = false;
    String pattern = null;

    /**
     * Parse arguments and perform IFS operations
     *
     * @param argArray passed-in arg array
     * @return rest of arg array
     */
    public ArgArray ifs(ArgArray argArray) {
        FUNCTIONS function = FUNCTIONS.FILE;
        String writeableString = null;
        String attribName = null;
        String attribValue = null;
        String newFQPname = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-ifs":
                case "--":
                    ifsFileTuple = argArray.nextTupleOrPop();
                    break;
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "-tofile":
                    setDataDest(DataSink.fileSinkFromTuple(argArray.nextTupleOrPop()));
                    break;
                case "-from":
                    setDataSrcfromArgArray(argArray);
                    break;
                case "-fromfile":
                    setDataSrc(DataSink.fileSinkFromTuple(argArray.nextTupleOrPop()));
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
                case "-pattern":
                    pattern = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-query":
                    function = FUNCTIONS.QUERY;
                    attribName = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-read":
                    function = FUNCTIONS.READ;
                    break;
                case "-rename":
                    function = FUNCTIONS.RENAME;
                    newFQPname = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-set":
                    function = FUNCTIONS.SET;
                    attribName = argArray.nextMaybeQuotationTuplePopString();
                    attribValue = argArray.nextMaybeQuotationTuplePopString();
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
                case "-offset":
                    binOffset = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-length":
                    binLength = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-size":
                    function = FUNCTIONS.SIZE;
                    break;
                case "-b":
                    binary = true;
                    break;
                case "-t":
                    translate = true;
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
                case QUERY:
                    ifsQuery(argArray, attribName);
                    break;
                case READ:
                    ifsRead(argArray);
                    break;
                case RENAME:
                    try {
                        put(ifsRename(argArray, newFQPname));
                    } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                        getLogger().log(Level.SEVERE,
                                "Encountered an exception putting IFS file result from rename in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case SET:
                    ifsSet(argArray, attribName, attribValue);
                    break;
                case SIZE:
                    ifsSize(argArray);
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
            ifsFile = t.value(IFSFile.class);
        }
        return ifsFile;
    }

    private IFSFile getIFSFileFromEponymous() {
        return ifsFileTuple == null ? null : ifsFileTuple.value(IFSFile.class);
    }

    private IFSFile getIFSFileFromDataSource() {
        IFSFile ifsFile;
        ifsFile = getIFSFileFromEponymous();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromDataSink(getDataSrc());
        }
        return ifsFile;
    }

    private IFSFile getIFSFileFromDataDest() {
        IFSFile ifsFile;
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
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -create operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
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
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -delete operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
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
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -exists operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
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
                StringArrayList sal;
                if (pattern == null) {
                    sal = new StringArrayList(ifsFile.list());
                } else {
                    sal = new StringArrayList(ifsFile.list(pattern));
                }
                put(sal);
            } catch (SQLException | RequestNotSupportedException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -list operation.", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -list operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
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
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -mkdirs operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
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

    private void ifsRead(ArgArray argArray) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                IFSFileInputStream ifsIn = new IFSFileInputStream(ifsFile);
                int readLength = binLength == null ? new Long(ifsFile.length()).intValue() : binLength;
                byte[] data = new byte[readLength];
                int numRead = ifsIn.read(data,
                        binOffset == null ? 0 : binOffset,
                        readLength
                );
                if (!binary) {
                    String s;
                    if (translate) {
                        s = new AS400Text(data.length, ifsFile.getCCSID(), ifsFile.getSystem()).toObject(data).toString();
                    } else {
                        s = new String(data);
                    }
                    put(s);
                } else {
                    switch (getDataDest().getType()) {
                        case FILE:
                            FileOutputStream fos = new FileOutputStream(getDataDest().getName());
                            fos.write(data);
                            break;
                        case TUPLE:
                            put(data);
                            break;
                        case STD:
                            put(data);
                            break;
                        default:
                    }
                }
            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in putting from the -read operation.", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -read operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
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
                if (translate) {
                    byte[] bytes = new AS400Text(text.length(), ifsFile.getCCSID(), ifsFile.getSystem()).toBytes(text);
                    try (IFSFileOutputStream ifsout = new IFSFileOutputStream(ifsFile)) {
                        ifsout.write(bytes, binOffset == null ? 0 : binOffset, binLength == null ? bytes.length : binLength);
                    } catch (AS400SecurityException | IOException ex) {
                        getLogger().log(Level.SEVERE,
                                "Exception encountered in the -write operation of " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                } else {
                    try (PrintWriter writer = new PrintWriter(new BufferedWriter(new IFSFileWriter(ifsFile)))) {
                        writer.print(text);
                    }
                }
            } catch (AS400SecurityException | IOException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -write operation.", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -write operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
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
        IFSFile ifsFile;
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
                    ifsout.write(bytes, binOffset == null ? 0 : binOffset, binLength == null ? bytes.length : binLength);
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

    private void ifsSize(ArgArray argArray) {
        IFSFile ifsFile;
        ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                put(ifsFile.length());
            } catch (IOException | RequestNotSupportedException | SQLException | AS400SecurityException | InterruptedException | ObjectDoesNotExistException | ErrorCompletingRequestException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -size operation", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -size operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    private void ifsQuery(ArgArray argArray, String query) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                switch (query.toLowerCase()) {
                    case "ccsid":
                        put(ifsFile.getCCSID());
                        break;
                    case "name":
                        put(ifsFile.getName());
                        break;
                    case "ownername":
                        put(ifsFile.getOwnerName());
                        break;
                    case "owneruid":
                        put(ifsFile.getOwnerUID());
                        break;
                    case "path":
                        put(ifsFile.getAbsolutePath());
                        break;
                    case "r":
                        put(ifsFile.canRead());
                        break;
                    case "w":
                        put(ifsFile.canWrite());
                        break;
                    case "x":
                        put(ifsFile.canExecute());
                        break;
                    default:
                        getLogger().log(Level.SEVERE, "Unknown query string {0} in {1}", new Object[]{query, getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                }
            } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -query operation", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }

        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -query operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    private void ifsSet(ArgArray argArray, String setString, String value) {
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null) {
            try {
                value = value.trim();
                switch (setString.toLowerCase()) {
                    case "ccsid":
                        ifsFile.setCCSID(Integer.parseInt(value));
                        break;
                    case "readonly":
                        try {
                            put(ifsFile.setReadOnly(value.toLowerCase().equals("true")));
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | SQLException ex) {
                            getLogger().log(Level.SEVERE,
                                    "The ifs commmand encountered an exception in the -set operation", ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    default:
                        getLogger().log(Level.SEVERE, "Unknown set string {0} in {1}", new Object[]{setString, getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                }
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE,
                        "The ifs commmand encountered an exception in the -set operation", ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }

        } else {
            getLogger().log(Level.SEVERE, "No ifs file object provided to the -query operation in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        }
    }

    private IFSFile ifsRename(ArgArray argArray, String newFQPname) {
        IFSFile result = null;
        IFSFile ifsFile = getIFSFileFromDataSource();
        if (ifsFile == null) {
            ifsFile = getIFSFileFromArgArray(argArray);
        }
        if (ifsFile != null && newFQPname != null) {
            result = new IFSFile(ifsFile.getSystem(), newFQPname);
            try {
                ifsFile.renameTo(result);
            } catch (IOException | PropertyVetoException ex) {
                getLogger().log(Level.SEVERE, "Exception renaming " + ifsFile.getAbsolutePath() + " in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return ifs(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
