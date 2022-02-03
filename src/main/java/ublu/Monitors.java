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

import ublu.util.SysShepHelper;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.SystemStatus;
import java.beans.PropertyVetoException;
import java.io.IOException;

/**
 * Monitor OS/400
 *
 * @author jwoehr
 */
public class Monitors {

    private AS400 as400;

    /**
     * Get the host instance
     *
     * @return the host instance
     */
    public AS400 getAs400() {
        return as400;
    }

    /**
     * Set the host instance
     *
     * @param as400 the host instance
     */
    protected final void setAs400(AS400 as400) {
        this.as400 = as400;
    }

    private Monitors() {
    }

    /**
     * Instance identify system and user
     *
     * @param system
     * @param userid
     * @param password
     * @throws PropertyVetoException
     */
    public Monitors(String system, String userid, String password) throws PropertyVetoException {
        setAs400(AS400Factory.newAS400(system, userid, password));
    }

    /**
     * Instance identify system and user
     *
     * @param as400 the associated system
     */
    public Monitors(AS400 as400) {
        setAs400(as400);
    }

    /**
     * Get major version of OS400
     *
     * @return major version of OS400
     */
    public String osMajorVersion() {
        String metricName = "OS400|Version|Major";
        SysShepHelper.STATUS status = SysShepHelper.STATUS.OK;
        String message = "OS400 Major Version";
        int majorVersion = 0;
        try {
            majorVersion = getAs400().getVersion();
        } catch (AS400SecurityException | IOException ex) {
            status = SysShepHelper.STATUS.WARNING;
            message = (ex.getLocalizedMessage());
        }
        return SysShepHelper.format(metricName, majorVersion, status, message);

    }

    /**
     * Get major/minor version of OS400
     *
     * @return String representing i/OS version;revision/modification as SysShep
     * datapoints
     */
    public String osVersionVRM() {
        String metricName = "OS400|Version|VRM";
        SysShepHelper.STATUS status = SysShepHelper.STATUS.OK;
        String message;
        int vrm = 0;
        try {
            vrm = getAs400().getVRM();
            message = /* Integer.toString(vrm, 16) + " - */ "OS400 Version Release and Modification V" + String.valueOf(vrm >> 16) + "R" + String.valueOf((vrm & 0xffff) >> 8) + "M" + String.valueOf(vrm & 0xff);
        } catch (AS400SecurityException | IOException ex) {
            status = SysShepHelper.STATUS.WARNING;
            message = (ex.getLocalizedMessage());
        }
        return SysShepHelper.format(metricName, vrm, status, message);

    }

    /**
     * Get datapoints for System Shepherd
     *
     * @return string representing the System Shepherd datapoints for all our
     * monitoring technology
     */
    public String systemStatus() {
        StringBuilder result = new StringBuilder();
        SystemStatus sysStatus = new SystemStatus(getAs400());
        String metric = "";
        int value;
        float floatValue;
        long longValue;
        try {
            metric = "OS400|System|Jobs|ActiveJobs";
            value = sysStatus.getActiveJobsInSystem();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of jobs active in the system (jobs that have been started, but have not yet ended), including both user and system jobs."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Threads|ActiveThreads";
            value = sysStatus.getActiveThreadsInSystem();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of initial and secondary threads in the system (threads that have been started, but have not yet ended), including both user and system threads."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsEndedWithPrinterOutputWaitingToPrint";
            value = sysStatus.getBatchJobsEndedWithPrinterOutputWaitingToPrint();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of completed batch jobs that produced printer output that is waiting to print."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsEnding";
            value = sysStatus.getBatchJobsEnding();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of batch jobs that are in the process of ending."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsHeldOnJobQueue";
            value = sysStatus.getBatchJobsHeldOnJobQueue();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of batch jobs that were submitted, but were held before they could begin running."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsHeldWhileRunning";
            value = sysStatus.getBatchJobsHeldWhileRunning();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of batch jobs that had started running, but are now held."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsOnAHeldJobQueue";
            value = sysStatus.getBatchJobsOnAHeldJobQueue();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of batch jobs on job queues that have been assigned to a subsystem, but are being held."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsOnUnassignedJobQueue";
            value = sysStatus.getBatchJobsOnUnassignedJobQueue();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of batch jobs on job queues that have not been assigned to a subsystem."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsRunning";
            value = sysStatus.getBatchJobsRunning();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of batch jobs currently running on the system."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsWaitingForMessage";
            value = sysStatus.getBatchJobsWaitingForMessage();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of batch jobs waiting for a reply to a message before they can continue to run."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Batch|BatchJobsWaitingToRunOrAlreadyScheduled";
            value = sysStatus.getBatchJobsWaitingToRunOrAlreadyScheduled();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of batch jobs on the system that are currently waiting to run."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Processor|CurrentProcessingCapacity";
            floatValue = sysStatus.getCurrentProcessingCapacity();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The amount of current processing capacity of the partition."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Storage|CurrentUnprotectedStorageUsed";
            value = sysStatus.getCurrentUnprotectedStorageUsed();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The current amount of storage in use for temporary objects."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Jobs|JobsInSystem";
            value = sysStatus.getJobsInSystem();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The total number of user jobs and system jobs that are currently in the system."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Storage|MainStorageSize";
            longValue = sysStatus.getMainStorageSize();
            result.append(SysShepHelper.format(metric, longValue, SysShepHelper.STATUS.OK, "The amount of main storage, in kilobytes, in the system."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Jobs|MaximumJobsInSystem";
            longValue = sysStatus.getMaximumJobsInSystem();
            result.append(SysShepHelper.format(metric, longValue, SysShepHelper.STATUS.OK, "The maximum number of jobs that are allowed on the system."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Storage|MaximumUnprotectedStorageUsed";
            value = sysStatus.getMaximumUnprotectedStorageUsed();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The largest amount of storage for temporary object used at any one time since the last IPL."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Partition|NumberOfPartitions";
            value = sysStatus.getNumberOfPartitions();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of partitions on the system."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Partition|NumberOfProcessors";
            value = sysStatus.getNumberOfProcessors();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of processors that are currently active in this partition."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Partition|PartitionIdentifier";
            value = sysStatus.getPartitionIdentifier();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The identifier for the current partition in which the API is running."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Partition|PercentCurrentInteractivePerformance";
            floatValue = sysStatus.getPercentCurrentInteractivePerformance();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of interactive performance assigned to this logical partition."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Processor|PercentDBCapability";
            floatValue = sysStatus.getPercentDBCapability();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of processor database capability that was used during the elapsed time."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Object|PercentPermanentAddresses";
            floatValue = sysStatus.getPercentPermanentAddresses();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the maximum possible addresses for permanent objects that have been used."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Memory|PercentPermanent256MBSegmentsUsed";
            floatValue = sysStatus.getPercentPermanent256MBSegmentsUsed();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the maximum possible permanent 256MB segments that have been used."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Memory|PercentPermanent4GBSegmentsUsed";
            floatValue = sysStatus.getPercentPermanent4GBSegmentsUsed();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the maximum possible permanent 4GB segments that have been used."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Processor|PercentProcessingUnitUsed";
            floatValue = sysStatus.getPercentProcessingUnitUsed();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The average of the elapsed time during which the processing units were in use."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Processor|PercentSharedProcessorPoolUsed";
            floatValue = sysStatus.getPercentSharedProcessorPoolUsed();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the total shared processor pool capacity used by all partitions using the pool during the elapsed time."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Processor|PercentSystemASPUsed";
            floatValue = sysStatus.getPercentSystemASPUsed();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the system storage pool currently in use."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Object|PercentTemporaryAddresses";
            floatValue = sysStatus.getPercentTemporaryAddresses();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the maximum possible addresses for temporary objects that have been used."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Memory|PercentTemporary256MBSegmentsUsed";
            floatValue = sysStatus.getPercentTemporary256MBSegmentsUsed();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the maximum possible temporary 256MB segments that have been used."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Memory|PercentTemporary4GBSegmentsUsed";
            floatValue = sysStatus.getPercentTemporary4GBSegmentsUsed();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the maximum possible temporary 4GB segments that have been used."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Memory|PercentUncappedCPUCapacityUsed";
            floatValue = sysStatus.getPercentUncappedCPUCapacityUsed();
            result.append(SysShepHelper.format(metric, floatValue, SysShepHelper.STATUS.OK, "The percentage of the uncapped shared processing capacity for the partition that was used during the elapsed time."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Pools|PoolsNumber";
            value = sysStatus.getPoolsNumber();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of system pools."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|Processor|ProcessorSharingAttribute";
            value = sysStatus.getProcessorSharingAttribute();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The processor sharing attribute."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|System|RestrictedStateFlag";
            value = sysStatus.getProcessorSharingAttribute();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "True if the system is in restricted state; false otherwise."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|Storage|SystemASP";
            value = sysStatus.getSystemASP();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The storage capacity of the system auxiliary storage pool (ASP1)."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|Storage|TotalAuxiliaryStorage";
            value = sysStatus.getTotalAuxiliaryStorage();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The total auxiliary storage (in millions of bytes) on the system."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|User|UsersCurrentSignedOn";
            value = sysStatus.getUsersCurrentSignedOn();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of users currently signed on the system."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|User|UsersSignedOffWithPrinterOutputWaitingToPrint";
            value = sysStatus.getUsersSignedOffWithPrinterOutputWaitingToPrint();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of sessions that have ended with printer output files waiting to print."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|User|UsersSuspendedByGroupJobs";
            value = sysStatus.getUsersSuspendedByGroupJobs();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of user jobs that have been temporarily suspended by group jobs so that another job may be run."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|User|UsersSuspendedBySystemRequest";
            value = sysStatus.getUsersSuspendedBySystemRequest();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of user jobs that have been temporarily suspended by system request jobs so that another job may be run."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        result.append("\n");
        try {
            metric = "OS400|User|UsersTemporarilySignedOff";
            value = sysStatus.getUsersTemporarilySignedOff();
            result.append(SysShepHelper.format(metric, value, SysShepHelper.STATUS.OK, "The number of user jobs that have been disconnected due to either the selection of option 80 (Temporary sign-off) or the entry of the Disconnect Job (DSCJOB) command."));
        } catch (AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | ObjectDoesNotExistException ex) {
            result.append(SysShepHelper.format(metric, 0, SysShepHelper.STATUS.WARNING, ex.getLocalizedMessage()));
        }
        return result.toString();
    }

    /**
     * Test routine
     *
     * @param args systemName userId password
     * @throws PropertyVetoException
     */
    public static void main(String args[])
            throws PropertyVetoException {
        String systemName = args[0];
        String userId = args[1];
        String password = args[2];
        Monitors os400monitors = new Monitors(systemName, userId, password);
        System.out.println(os400monitors.osMajorVersion());
        System.out.println(os400monitors.osVersionVRM());
        System.out.println(os400monitors.systemStatus());
    }
}
