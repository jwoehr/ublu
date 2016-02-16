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
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * Command to manipulate Collections
 *
 * @author jwoehr
 */
public class CmdCollection extends Command {

    {
        setNameAndDescription("collection", "/0? [--,-collection ~@collection] [-to datasink] [-show | -size]  : manipulate collections of objects");
    }

    private enum OPS {

        SHOW, SIZE, NOOP
    }

    /**
     * Perform the work of manipulating a collection.
     *
     * @param argArray the input arg array
     * @return what's left of the arg array
     */
    public ArgArray collection(ArgArray argArray) {
        OPS op = OPS.SHOW;
        Tuple collectionTuple = null;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-to":
                    String destName = argArray.next();
                    setDataDest(DataSink.fromSinkName(destName));
                    break;
//                case "-from":
//                    String srcName = argArray.next();
//                    setDataSrc(DataSink.fromSinkName(srcName));
//                    break;
                case "--":
                case "-collection":
                    collectionTuple = argArray.nextTupleOrPop();
                    break;
                case "-show":
                    op = OPS.SHOW;
                    break;
                case "-size":
                    op = OPS.SIZE;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            Collection collection = null;
            if (collectionTuple != null) {
                Object tupleValue = collectionTuple.getValue();
                if (tupleValue instanceof Collection) {
                    collection = Collection.class.cast(tupleValue);
                } else {
                    getLogger().log(Level.SEVERE, "Valued tuple which is not a Collection tuple provided to --collection in {0}", getNameAndDescription());
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
            if (collection != null) {
                switch (op) {
                    case SIZE:
                        int size = collection.size();
                        try {
                            put(size);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting member of Collection in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case SHOW:
                        Iterator it = collection.iterator();
                        while (it.hasNext()) {
                            try {
                                put(it.next());
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error putting member of Collection in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    default:
                        getLogger().log(Level.SEVERE, "Unahndled operation {0} in {1}", new Object[]{op, getNameAndDescription()});
                        setCommandResult(COMMANDRESULT.FAILURE);
                }
            }
        }
        return argArray;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return collection(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
