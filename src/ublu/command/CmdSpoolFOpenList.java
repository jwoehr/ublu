/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.list.OpenListException;
import com.ibm.as400.access.list.SpooledFileOpenList;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Level;
import ublu.util.Generics.ThingArrayList;

/**
 * Command to get a list of spooled files via JTOpen SpooledFileOpenList and
 * treat them as objects.
 *
 * @author jwoehr
 */
public class CmdSpoolFOpenList extends Command {

    {
        setNameAndDescription("splfol", "/0 [-as400 ~@as400] [-to datasink] [--,-splfol ~@splfol] "
                + "[-addsort ~@{COPIES_LEFT_TO_PRINT | CURRENT_PAGE | DATE_OPENED | DEVICE_TYPE | FORM_TYPE | JOB_NAME | JOB_NUMBER | JOB_SYSTEM | JOB_USER | NAME | NUMBER | OUTPUT_QUEUE_LIBRARY | OUTPUT_QUEUE_NAME | PRINTER_ASSIGNED | PRINTER_NAME | PRIORITY | SCHEDULE | SIZE | STATUS | TIME_OPENED | TOTAL_PAGES | USER_DATA} @tf "
                + "| -blocksize ~@{ numentries } | -clearsort | -close "
                + "| -fdate ~@{sy} ~@{sm} ~@{sd} ~@{ey} ~@{em} ~@{ed} | -fdevs  ~@list_of_devs "
                + "| -fform ~@{formType} | -fjob  ~@{jobName} ~@{jobUser} ~@{jobNumber} | -fjobsys ~@{sysname} "
                + "| -foutq  ~@{list_of_ifsOutQs} "
                + "| -fstat ~@list_of[*CLOSED | *DEFERRED | *SENDING | *FINISHED | *HELD | *MESSAGE | *OPEN | *PENDING | *PRINTER | *READY | *SAVED | *WRITING] "
                + "| -fudata ~@{userData} | -fusers ~@list_of_users | -format ~@{100 | 200 | 300} "
                + "| -get | -getsome ~@{offset} ~@{length} | -length | -new | -open | -qblocksize | -qformat  | -qsystem] "
                + ": open list of the spooled files on system sorted and filtered");
    }

    /**
     * ctor /0
     */
    public CmdSpoolFOpenList() {
    }

    enum OPS {
        NEW,
        BLOCKSIZE,
        CLOSE,
        ADDSORT,
        CLEARSORT,
        FILTER_DATE,
        FILTER_DEVS,
        FILTER_FORM,
        FILTER_JOB,
        FILTER_JOBSYS,
        FILTER_OUTQ,
        FILTER_STAT,
        FILTER_UDATA,
        FILTER_USERS,
        FORMAT,
        GET,
        GETSOME,
        LENGTH,
        OPEN,
        QBLOCKSIZE,
        QFORMAT,
        QSYSTEM
    }

    /**
     * Return spooled file list as objects for a given userid
     *
     * @param argArray the remainder of the command stream
     * @return the new remainder of the command stream
     */
    public ArgArray spoolfOpenList(ArgArray argArray) {
        OPS op = OPS.NEW;
        SpooledFileOpenList splfolist = null;
        Integer sortConstant = null;
        Boolean sortDirection = true;
        Integer offset = null;
        Integer count = null;
        Integer blocksize = null;
        String formatSelector = null;
        ThingArrayList f_users = null;
        ThingArrayList f_devs = null;
        String formType = null;
        String jobName = null;
        String jobUser = null;
        String jobNumber = null;
        String jobSystem = null;
        ThingArrayList f_ifsOutQs = null;
        ThingArrayList f_statuses = null;
        String userData = null;
        int sy = 0, sm = 0, sd = 0, ey = 0, em = 0, ed = 0;
        while (argArray.hasDashCommand() && getCommandResult() != COMMANDRESULT.FAILURE) {
            String dashCommand = argArray.parseDashCommand();
            switch (dashCommand) {
                case "-as400":
                    setAs400fromTupleOrPop(argArray);
                    break;
                case "-to":
                    setDataDestfromArgArray(argArray);
                    break;
                case "--":
                case "-splfol":
                    splfolist = argArray.nextTupleOrPop().value(SpooledFileOpenList.class);
                    break;
                case "-addsort":
                    op = OPS.ADDSORT;
                    try {
                        sortConstant = SpooledFileOpenList.class.getDeclaredField(argArray.nextMaybeQuotationTuplePopStringTrim().toUpperCase()).getInt(SpooledFileOpenList.class);
                    } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
                        getLogger().log(Level.SEVERE, "Exception finding sort constant in " + getNameAndDescription(), ex);
                        setCommandResult(COMMANDRESULT.FAILURE);
                    }
                    sortDirection = argArray.nextBooleanTupleOrPop();
                    break;
                case "-blocksize":
                    op = OPS.BLOCKSIZE;
                    blocksize = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-clearsort":
                    op = OPS.CLEARSORT;
                    break;
                case "-close":
                    op = OPS.CLOSE;
                    break;
                case "-fdate":
                    op = OPS.FILTER_DATE;
                    sy = argArray.nextIntMaybeQuotationTuplePopString();
                    sm = argArray.nextIntMaybeQuotationTuplePopString();
                    sd = argArray.nextIntMaybeQuotationTuplePopString();
                    ey = argArray.nextIntMaybeQuotationTuplePopString();
                    em = argArray.nextIntMaybeQuotationTuplePopString();
                    ed = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-fdevs":
                    op = OPS.FILTER_DEVS;
                    f_devs = argArray.nextTupleOrPop().value(ThingArrayList.class);
                    break;
                case "-fform":
                    op = OPS.FILTER_FORM;
                    formType = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-fjob":
                    op = OPS.FILTER_JOB;
                    jobName = argArray.nextMaybeQuotationTuplePopStringTrim();
                    jobUser = argArray.nextMaybeQuotationTuplePopStringTrim();
                    jobNumber = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "fjobsys":
                    op = OPS.FILTER_JOBSYS;
                    jobSystem = argArray.nextMaybeQuotationTuplePopStringTrim();
                    break;
                case "-foutq":
                    op = OPS.FILTER_OUTQ;
                    f_ifsOutQs = argArray.nextTupleOrPop().value(ThingArrayList.class);
                    break;
                case "-fstat":
                    op = OPS.FILTER_STAT;
                    f_statuses = argArray.nextTupleOrPop().value(ThingArrayList.class);
                    break;
                case "-fudata":
                    op = OPS.FILTER_UDATA;
                    userData = argArray.nextMaybeQuotationTuplePopString();
                    break;
                case "-fusers":
                    op = OPS.FILTER_USERS;
                    f_users = argArray.nextTupleOrPop().value(ThingArrayList.class);
                    break;
                case "-format":
                    op = OPS.FORMAT;
                    formatSelector = formatSel(argArray.nextIntMaybeQuotationTuplePopString());
                    break;
                case "-get":
                    op = OPS.GET;
                    break;
                case "-getsome":
                    op = OPS.GETSOME;
                    offset = argArray.nextIntMaybeQuotationTuplePopString();
                    count = argArray.nextIntMaybeQuotationTuplePopString();
                    break;
                case "-length":
                    op = OPS.LENGTH;
                    break;
                case "-new":
                    op = OPS.NEW;
                    break;
                case "-open":
                    op = OPS.OPEN;
                    break;
                case "-qblocksize":
                    op = OPS.QBLOCKSIZE;
                    break;
                case "-qformat":
                    op = OPS.QFORMAT;
                    break;
                case "-qsystem":
                    op = OPS.QSYSTEM;
                    break;
                default:
                    unknownDashCommand(dashCommand);
            }
        }
        if (havingUnknownDashCommand()) {
            setCommandResult(COMMANDRESULT.FAILURE);
        } else if (getCommandResult() != COMMANDRESULT.FAILURE) {
            if (splfolist == null) {
                if (argArray.size() < (getAs400() == null ? 3 : 0)) {
                    logArgArrayTooShortError(argArray);
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    splfolist = getSpooledFileOpenListFromArgs(argArray);
                }
            }
            if (getCommandResult() != COMMANDRESULT.FAILURE) {
                ThingArrayList tal = null;
                switch (op) {
                    case ADDSORT:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            splfolist.addSortField(sortConstant, sortDirection);
                        }
                        break;
                    case CLEARSORT:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            splfolist.clearSortFields();
                        }
                        break;
                    case BLOCKSIZE:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            splfolist.setEnumerationBlockSize(blocksize);
                        }
                        break;
                    case CLOSE:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            try {
                                splfolist.close();
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception closing SpooledFileOpenList in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case FILTER_DATE:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            Calendar startC = Calendar.getInstance();
                            startC.set(sy, sm, sd, 0, 0, 0);
                            Calendar endC = Calendar.getInstance();
                            endC.set(ey, em, ed, 0, 0, 0);
                            splfolist.setFilterCreationDate(startC.getTime(), endC.getTime());
                        }
                        break;
                    case FILTER_DEVS:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (f_devs != null) {
                                splfolist.setFilterDevices(f_devs.toStringArray());
                            } else {
                                getLogger().log(Level.SEVERE, "Empty dev list provided to {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case FILTER_FORM:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (formType != null) {
                                splfolist.setFilterFormType(formType);
                            } else {
                                getLogger().log(Level.SEVERE, "Empty form type provided to {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case FILTER_JOB:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (jobName != null && jobNumber != null && jobUser != null) {
                                splfolist.setFilterJobInformation(jobName, jobNumber, jobUser);
                            } else {
                                getLogger().log(Level.SEVERE, "Empty job filter {0} {1} {2} provided to {3}", new Object[]{jobName, jobNumber, jobUser, getNameAndDescription()});
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case FILTER_JOBSYS:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (jobSystem != null) {
                                splfolist.setFilterJobSystemName(jobSystem);
                            } else {
                                getLogger().log(Level.SEVERE, "Empty sysname provided to {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case FILTER_OUTQ:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (f_ifsOutQs != null) {
                                splfolist.setFilterOutputQueues(f_ifsOutQs.toStringArray());
                            } else {
                                getLogger().log(Level.SEVERE, "Empty outq list provided to {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case FILTER_STAT:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (f_statuses != null) {
                                splfolist.setFilterStatuses(f_statuses.toStringArray());
                            } else {
                                getLogger().log(Level.SEVERE, "Empty status list provided to {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case FILTER_UDATA:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (userData != null) {
                                splfolist.setFilterUserData(userData);
                            } else {
                                getLogger().log(Level.SEVERE, "Invalid user data provided to {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;

                    case FILTER_USERS:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (f_users != null) {
                                splfolist.setFilterUsers(f_users.toStringArray());
                            } else {
                                getLogger().log(Level.SEVERE, "Empty user list provided to {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case FORMAT:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            if (formatSelector != null) {
                                splfolist.setFormat(formatSelector);
                            } else {
                                getLogger().log(Level.SEVERE, "Invalid format provided to {0}", getNameAndDescription());
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case GET:
                        Enumeration e = null;
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            try {
                                e = splfolist.getItems();
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException | OpenListException ex) {
                                getLogger().log(Level.SEVERE, "Exception getting SpooledFileOpenList in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            if (e != null) {
                                tal = new ThingArrayList(e);
                            }
                            try {
                                put(tal);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception putting SpooledFileOpenList in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case GETSOME:
                        Object[] items = null;
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            try {
                                items = splfolist.getItems(offset, count);
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException | OpenListException ex) {
                                getLogger().log(Level.SEVERE, "Exception getting SpooledFileOpenList items in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                            if (items != null) {
                                tal = new ThingArrayList(items);
                            }
                            try {
                                put(tal);
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                                getLogger().log(Level.SEVERE, "Exception putting SpooledFileOpenList items in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case LENGTH:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            try {
                                put(splfolist.getLength());
                            } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException | OpenListException ex) {
                                getLogger().log(Level.SEVERE, "Exception getting or putting SpooledFileOpenList length in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case NEW:
                        try {
                            put(splfolist);
                        } catch (SQLException | IOException | AS400SecurityException | ErrorCompletingRequestException | InterruptedException | ObjectDoesNotExistException | RequestNotSupportedException ex) {
                            getLogger().log(Level.SEVERE, "Exception putting SpooledFileOpenList in " + getNameAndDescription(), ex);
                            setCommandResult(COMMANDRESULT.FAILURE);
                        }
                        break;
                    case OPEN:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            try {
                                splfolist.open();
                            } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException | OpenListException ex) {
                                getLogger().log(Level.SEVERE, "Exception opening SpooledFileOpenList in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case QBLOCKSIZE:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            try {
                                put(splfolist.getEnumerationBlockSize());
                            } catch (AS400SecurityException | SQLException | RequestNotSupportedException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception getting or putting blocksize in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case QFORMAT:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            try {
                                put(splfolist.getFormat());
                            } catch (AS400SecurityException | SQLException | RequestNotSupportedException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception getting or putting format in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                    case QSYSTEM:
                        if (splfolist == null) {
                            noSplfOL();
                        } else {
                            try {
                                put(splfolist.getSystem());
                            } catch (AS400SecurityException | SQLException | RequestNotSupportedException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
                                getLogger().log(Level.SEVERE, "Exception getting or putting system in " + getNameAndDescription(), ex);
                                setCommandResult(COMMANDRESULT.FAILURE);
                            }
                        }
                        break;
                }
            }
        }
        return argArray;
    }

    private void noSplfOL() {
        getLogger().log(Level.SEVERE, "No SpoolFileOpenList instance provided to {0}", getNameAndDescription());
        setCommandResult(COMMANDRESULT.FAILURE);
    }

    private String formatSel(Integer sel) {
        String result = null;
        switch (sel) {
            case 100:
                result = SpooledFileOpenList.FORMAT_0100;
                break;
            case 200:
                result = SpooledFileOpenList.FORMAT_0200;
                break;
            case 300:
                result = SpooledFileOpenList.FORMAT_0300;
                break;
        }
        return result;

    }

    private SpooledFileOpenList getSpooledFileOpenListFromArgs(ArgArray argArray) {
        SpooledFileOpenList splfol = null;
        AS400 a = getAs400();
        if (a == null) {
            try {
                setAs400(as400FromArgs(argArray));
            } catch (PropertyVetoException ex) {
                getLogger().log(Level.SEVERE, "Could not instance AS400 system from provided arguments in " + getNameAndDescription(), ex);
                setCommandResult(COMMANDRESULT.FAILURE);
            }
        }
        if (a != null) {
            splfol = new SpooledFileOpenList(a);
        }
        return splfol;
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return spoolfOpenList(args);
    }

    @Override
    public void reinit() {
        super.reinit();
    }

    @Override
    public COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
