/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
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
package ublu.util;

import ublu.AS400Factory;
import com.ibm.as400.access.AS400;
import com.ibm.jtopenlite.command.CommandConnection;
import com.ibm.jtopenlite.command.CommandResult;
import com.ibm.jtopenlite.command.program.journal.RetrieveJournalEntries;
import com.ibm.jtopenlite.command.program.journal.RetrieveJournalEntriesListener;
import com.ibm.jtopenlite.command.program.journal.RetrieveJournalEntriesSelection;
import java.io.IOException;

/**
 * Examine Journal Entries
 *
 * @author jwoehr
 */
public class JournalHelper implements RetrieveJournalEntriesListener {

    private AS400 myAs400;
    private StringBuilder lastRetrieval;
    private CommandResult lastCommandResult;

    private String getSystemName() {
        String systemName = null;
        if (myAs400 != null) {
            systemName = myAs400.getSystemName();
        }
        return systemName;
    }

    private String getUserId() {
        String userId = null;
        if (myAs400 != null) {
            userId = myAs400.getUserId();
        }
        return userId;
    }

    private String getPassword() {
        String password = null;
        if (myAs400 != null) {
            password = AS400Factory.retrievePassword(myAs400);
        }
        return password;
    }

    /**
     * Fetch the StringBuilder full of the journal text last retrieved
     *
     * @return the journal text last retrieved
     */
    public StringBuilder getLastRetrieval() {
        return lastRetrieval;
    }

    private void resetLastRetrieval() {
        this.lastRetrieval = new StringBuilder();
    }

    private void appendLastRetrieval(StringBuilder s) {
        this.lastRetrieval.append(s);
    }

    /**
     * Return result of last retrieval
     *
     * @return result of last retrieval
     */
    public CommandResult getLastCommandResult() {
        return lastCommandResult;
    }

    private void setLastCommandResult(CommandResult lastCommandResult) {
        this.lastCommandResult = lastCommandResult;
    }

    /**
     * Get associated AS400 instance
     *
     * @return associated AS400 instance
     */
    public AS400 getMyAs400() {
        return myAs400;
    }

    /**
     * Set associated AS400 instance
     *
     * @param myAs400
     */
    public void setMyAs400(AS400 myAs400) {
        this.myAs400 = myAs400;
    }

    private JournalHelper() {
    }

    /**
     * Ctor/1 on associated AS400 instance
     *
     * @param as400 associated AS400 instance
     */
    public JournalHelper(AS400 as400) {
        this();
        myAs400 = as400;
    }

    /**
     * Not implemented
     *
     * @param numberOfEntriesRetrieved
     * @param continuationHandle
     */
    @Override
    public void newJournalEntries(int numberOfEntriesRetrieved, char continuationHandle) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param pointerHandle
     * @param sequenceNumber
     * @param journalCode
     * @param entryType
     * @param timestamp
     * @param jobName
     * @param userName
     * @param jobNumber
     * @param programName
     * @param object
     * @param count
     * @param indicatorFlag
     * @param commitCycleIdentifier
     * @param userProfile
     * @param systemName
     * @param journalIdentifier
     * @param referentialConstraint
     * @param trigger
     * @param incompleteData
     * @param objectNameIndicator
     * @param ignoreDuringJournalChange
     * @param minimizedEntrySpecificData
     */
    @Override
    public void newEntryData(int pointerHandle, long sequenceNumber, char journalCode, String entryType, String timestamp, String jobName, String userName, String jobNumber, String programName, String object, int count, char indicatorFlag, long commitCycleIdentifier, String userProfile, String systemName, String journalIdentifier, char referentialConstraint, char trigger, char incompleteData, char objectNameIndicator, char ignoreDuringJournalChange, char minimizedEntrySpecificData) {
        StringBuilder sb = new StringBuilder();
        sb.append("SEQ=").append(sequenceNumber)
                .append(" CODE=").append(journalCode)
                .append(" TYPE=").append(entryType)
                .append(" TS=").append(timestamp)
                .append(" JOB=").append(jobName).append("/").append(userName).append("/").append(jobNumber)
                .append(" PROGRAM=").append(programName)
                .append(" OBJECT=").append(object)
                .append(" USER=").append(userProfile)
                .append('\n');
        appendLastRetrieval(sb);
    }

    /**
     * Contact host to obtain a string representing all the journal entries
     * requested.
     *
     * @param interpreter host interpreter
     * @param libraryName library journal is found in
     * @param numEntries number of entries to return (if possible and if they
     * fit)
     * @param journalName name of journal to read
     * @param averageEntrySize estimate of average size of each entry
     * @return the string representing all the journal entries requested
     * @throws IOException
     */
    public String fetchJournalEntries(Interpreter interpreter, String libraryName, String journalName, int numEntries, int averageEntrySize) throws IOException {
        CommandConnection connection;
        connection = CommandConnection.getConnection(getSystemName(), getUserId(), getPassword());
        RetrieveJournalEntriesListener listener = this;
        String format = RetrieveJournalEntries.FORMAT_RJNE0100;
        resetLastRetrieval();
        RetrieveJournalEntries retrieveJournalEntries
                = new RetrieveJournalEntries(numEntries * averageEntrySize, journalName, libraryName, format, listener);
        RetrieveJournalEntriesSelection selection = new RetrieveJournalEntriesSelection();
        selection.addEntry(RetrieveJournalEntries.KEY_NUMBER_OF_ENTRIES, numEntries);
        retrieveJournalEntries.setSelectionListener(selection);
        if (connection != null) {
            setLastCommandResult(connection.call(retrieveJournalEntries));
        }
        return getLastRetrieval().toString();
    }
//
//    /**
//     *
//     * @param library
//     * @param journal
//     */
//    public void testRoutine(String library, String journal) {
//        /* Test code until we figure out how to make RetrieveJournalEntries work with an as400 instance */
//        String[] testArgs = {getSystemName(), getUserId(), getPassword(), library, journal};
//        CallRetrieveJournalEntries.main(testArgs);
//        /* end of test code */
//    }
}
