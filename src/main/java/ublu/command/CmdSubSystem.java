/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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
package ublu.command;

import ublu.util.ArgArray;
import ublu.util.Generics.SubsystemArrayList;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectAlreadyExistsException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.Subsystem;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Manage subsystems
 *
 * @author jwoehr
 */
public class CmdSubSystem extends Command {

    {
        setNameAndDescription("subsys",
                "/3? [-as400 ~@as400] [--,-subsys ~@subsys] [-to datasink] [-subsyspath ~@{subsysIFSpath}] [-authoritystring ~@{authoritystring}] [-timelimit ~@{intval}] [-assignprivate ~@{sequencenumber} ~@{size} ~@{activityLevel} | -assignshared ~@{sequencenumber} ~@{poolname} | -change [description ~@{text} | displayfile ~@{path} | languagelibrary ~@{lib}} | maxactivejobs ~@${int}] | -create | -delete | -end | -endall | -new,-instance | -list | -query [description | activejobs | displayfilepath | languagelibrary | library | maxactivejobs | monitorjob | name | objectdescription | path | pool | pools ~@{sequencenumber} | status | system] | -refresh | -remove ~@{sequencenumber} | -start ] system userid password : manipulate subsystems");
    }

    enum OPS {

        ASSIGNPRIVATE, ASSIGNSHARED, CREATE, CHANGE, DELETE, END, ENDALL, EXISTS, INSTANCE, LIST, QUERY, REFRESH, REMOVE, START
    }

    /**
     * Command to manage subsystems
     *
     * @param argArray passed-in arg array
     * @return rest of arg array
     */
    public ArgArray cmdSubsys(ArgArray argArray) {
        Subsystem subsystem = null;
        String subsystemIFSPath = null;
        String authorityString = null;
        String changeString = null;
        String changeValue = null;
        String queryString = null;
        String poolName = null;
        Integer poolSize = null;
        Integer activityLevel = null;
        Integer timeLimit = null;
        Integer sequenceNumber = null;
        Tuple subsystemTuple = null;
        OPS op = OPS.INSTANCE;
        while (argArray.hasDashCommand()) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                case "-subsys":
                    subsystemTuple = argArray.nextTupleOrPop();
                    break;
                case "-authoritystring":
                    authorityString = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-assignprivate":
                    op = OPS.ASSIGNPRIVATE;
                    sequenceNumber = argArray.nextIntMaybeQuotationTuplePopString();
                    poolSize = argArray.nextIntMaybeQuotationTuplePopString();
                    activityLevel = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-assignshared":
                    op = OPS.ASSIGNSHARED;
                    sequenceNumber = argArray.nextIntMaybeQuotationTuplePopString();
                    poolName = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-change":
                    op = OPS.CHANGE;
                    changeString = argArray.next().toLowerCase().trim();
                    changeValue = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                case "-create":
                    op = OPS.CREATE;
                    break;
                case "-delete":
                    op = OPS.DELETE;
                    break;
                case "-end":
                    op = OPS.END;
                    break;
                case "-endall":
                    op = OPS.ENDALL;
                    break;
                case "-exists":
                    op = OPS.EXISTS;
                    break;
                case "-new":
                case "-instance":
                    op = OPS.INSTANCE;
                    break;
                case "-list":
                    op = OPS.LIST;
                    break;
                case "-query":
                    op = OPS.QUERY;
                    queryString = argArray.nextMaybeQuotationTuplePopString();
                    if (queryString.trim().toLowerCase().equals("ports")) {
                        sequenceNumber = argArray.nextIntMaybeQuotationTuplePopString();
                    }
                    break;
                case "-refresh":
                    op = OPS.REFRESH;
                    break;
                case "-remove":
                    op = OPS.REMOVE;
                    sequenceNumber = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-start":
                    op = OPS.START;
                    break;
                case "-timelimit":
                    timeLimit = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-subsyspath":
                    subsystemIFSPath = argArray.nextMaybeQuotationTuplePopString().trim();
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            if (subsystemTuple != null) {
                Object o = subsystemTuple.getValue();
                if (o instanceof Subsystem) {
                    subsystem = Subsystem.class.cast(o);
                } else {
                    getLogger().log(Level.SEVERE, "Tuple does represent a Subsystem in {0}", new Object[]{getNameAndDescription()});
                    setCommandResult(COMMANDRESULT.FAILURE);
                }
            } else if (getAs400() == null) {
                if (argArray.size() < 3) {
                    logArgArrayTooShortError(argArray);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    try {
                        setAs400FromArgs(argArray);
                    } catch (PropertyVetoException ex) {
                        getLogger().log(Level.SEVERE, "Can't set AS400 from arguments", ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
            }
            switch (op) {
                case INSTANCE:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        try {
                            if (subsystem.exists()) {
                                subsystem.refresh();
                            }
                            put(subsystem);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting subsystem in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No subsystem instance in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case LIST:
                    if (getAs400() != null) {
                        try {
                            put(new SubsystemArrayList(Subsystem.listAllSubsystems(getAs400())).refresh());
                        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException | SQLException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting subsystem list in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400 instance for listing subsystems in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case ASSIGNPRIVATE:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    try {
                        if (subsystem != null) {
                            if (sequenceNumber != null) {
                                if (poolSize != null) {
                                    if (activityLevel != null) {
                                        subsystem.assignPool(sequenceNumber, poolSize, activityLevel);
                                    } else {
                                        getLogger().log(Level.SEVERE, "No activity level to assign private pool in  {0}", getNameAndDescription());
                                        setCommandResult(COMMANDRESULT.FAILURE);
                                    }
                                } else {
                                    getLogger().log(Level.SEVERE, "No pool size to assign private pool in  {0}", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            } else {
                                getLogger().log(Level.SEVERE, "No sequence number to assign private pool in  {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No subsystem instance for changing subsystem in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Error assigning private pool to subsystem " + subsystem + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case ASSIGNSHARED:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    try {
                        if (subsystem != null) {
                            if (sequenceNumber != null) {
                                if (poolName != null) {
                                    subsystem.assignPool(sequenceNumber, poolName);
                                } else {
                                    getLogger().log(Level.SEVERE, "No pool name to assign shared pool in  {0}", getNameAndDescription());
                                    setCommandResult(COMMANDRESULT.FAILURE);
                                }
                            } else {
                                getLogger().log(Level.SEVERE, "No sequence number to assign shared pool in  {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No subsystem instance for changing subsystem in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Error assigning shared pool to subsystem " + subsystem + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case CHANGE:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    try {
                        if (subsystem != null) {
                            if (changeString != null) {
                                switch (changeString) {
                                    case "description":
                                        subsystem.changeDescriptionText(changeValue);
                                        break;
                                    case "displayfile":
                                        subsystem.changeDisplayFilePath(changeValue);
                                        break;
                                    case "languagelibrary":
                                        subsystem.changeLanguageLibrary(changeValue);
                                        break;
                                    case "maxactivejobs":
                                        subsystem.changeMaximumActiveJobs(Integer.parseInt(changeValue));
                                }
                            } else {
                                getLogger().log(Level.SEVERE, "No change string to change attribute in  {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        } else {
                            getLogger().log(Level.SEVERE, "No subsystem instance for changing subsystem in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                        getLogger().log(Level.SEVERE, "Error creating subsystem " + subsystem + inNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;

                case CREATE:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        try {
                            if (authorityString != null) {
                                subsystem.create(authorityString);
                            } else {
                                subsystem.create();
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectAlreadyExistsException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error creating subsystem " + subsystem + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No subsystem instance for creating subsystem in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case DELETE: {
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        try {
                            subsystem.delete();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException ex) {
                            getLogger().log(Level.SEVERE, "Error deleting subsystem " + subsystem + inNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No subsystem instance for deleting subsystem in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
                break;
                case END:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        try {
                            if (timeLimit == null) {
                                subsystem.endImmediately();
                            } else {
                                subsystem.end(timeLimit);
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error ending subsystem in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No subsystem instance for deleting subsystem in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case ENDALL:
                    if (getAs400() != null) {
                        try {
                            if (timeLimit != null) {
                                Subsystem.endAllSubsystems(getAs400(), timeLimit);
                            } else {
                                Subsystem.endAllSubsystemsImmediately(getAs400());
                            }
                        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException ex) {
                            getLogger().log(Level.SEVERE, "Error ending all subsystems in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No AS400 instance for listing subsystems in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case EXISTS:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        try {
                            put(subsystem.exists());
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Error putting subsystem existence in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    }
                    break;
                case QUERY: {
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        if (queryString == null) {
                            getLogger().log(Level.SEVERE, "Empty query string in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                put(querySubSystem(subsystem, queryString, sequenceNumber));
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Error getting or putting subsystem monitor job in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No subsystem instance for testing existence of subsystem in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
                break;
                case REMOVE:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        if (sequenceNumber == null) {
                            getLogger().log(Level.SEVERE, "No sequence number removing pool in {0}", getNameAndDescription());
                            setCommandResult(COMMANDRESULT.FAILURE);
                        } else {
                            try {
                                subsystem.removePool(sequenceNumber);
                            } catch (IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Error removing pool in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No subsystem instance for removing subsystem in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
                case REFRESH: {
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        try {
                            subsystem.refresh();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error refreshing subsystem attributes in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No subsystem instance for refreshing subsystem in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                }
                break;
                case START:
                    if (subsystem == null) {
                        subsystem = getSubsystem(subsystemIFSPath);
                    }
                    if (subsystem != null) {
                        try {
                            subsystem.start();
                        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
                            getLogger().log(Level.SEVERE, "Error starting subsystem in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                    } else {
                        getLogger().log(Level.SEVERE, "No subsystem instance for starting subsystem in {0}", getNameAndDescription());
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    break;
            }
        }
        return argArray;
    }

    private Subsystem getSubsystem(String subsystemIFSPath) {
        Subsystem subsystem = null;
        if (getAs400() != null) {
            if (subsystemIFSPath == null) {
                getLogger().log(Level.SEVERE, "No subsystem name nor subsystem tuple in {0}", new Object[]{getNameAndDescription()});
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                subsystem = new Subsystem(getAs400(), subsystemIFSPath);
            }
        }
        return subsystem;
    }

    private Object querySubSystem(Subsystem subsys, String queryString, Integer sequenceNumber) throws AS400SecurityException, ErrorCompletingRequestException, AS400Exception, InterruptedException, IOException, ObjectDoesNotExistException {
        Object result = null;
        switch (queryString.toLowerCase()) {
            case "description":
                result = subsys.getDescriptionText();
                break;
            case "activejobs":
                result = subsys.getCurrentActiveJobs();
                break;
            case "displayfilepath":
                result = subsys.getDisplayFilePath();
                break;
            case "languagelibrary":
                result = subsys.getLanguageLibrary();
                break;
            case "library":
                result = subsys.getLibrary();
                break;
            case "maxactivejobs":
                result = subsys.getMaximumActiveJobs();
                break;
            case "monitorjob":
                result = subsys.getMonitorJob();
                break;
            case "name":
                result = subsys.getName();
                break;
            case "objectdescription":
                result = subsys.getObjectDescription();
                break;
            case "path":
                result = subsys.getPath();
                break;
            case "pool":
                result = subsys.getPool(sequenceNumber);
                break;
            case "pools":
                result = subsys.getPools();
                break;
            case "status":
                result = subsys.getStatus();
                break;
            case "system":
                result = subsys.getSystem();
                break;
            default:
                getLogger().log(Level.SEVERE, "Unknown query string in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
        }
        return result;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return cmdSubsys(args);
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
