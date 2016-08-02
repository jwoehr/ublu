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
package ublu.util;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.PlatformManagedObject;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class to help examine the JVM in which the containing application is running.
 *
 * @author jwoehr
 */
public class JVMHelper {

    private Set<Class<? extends PlatformManagedObject>> managementInterfaces;

    /**
     * Display factors about JVM on which this program is running.
     */
    public JVMHelper() {
        managementInterfaces = ManagementFactory.getPlatformManagementInterfaces();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append('\n');
        sb.append("---\n");
        sb.append(displayManagementInterfaces());
        sb.append("---\n");
        sb.append(displayManagementBean(ManagementFactory.getOperatingSystemMXBean()));
        sb.append("---\n");
        sb.append(displayManagementBean(ManagementFactory.getRuntimeMXBean()));
        sb.append("---\n");
        sb.append(displayManagementBean(ManagementFactory.getMemoryMXBean()));
        sb.append("---\n");
        sb.append(displayMemoryManagerMXBeanList(ManagementFactory.getMemoryManagerMXBeans()));
        sb.append("---\n");
        sb.append(displayGarbageCollectorMXBeanList(ManagementFactory.getGarbageCollectorMXBeans()));
        sb.append("---\n");
        sb.append(displayManagementBean(ManagementFactory.getClassLoadingMXBean()));
        sb.append("---\n");
        sb.append(displayManagementBean(ManagementFactory.getThreadMXBean()));
        sb.append("---\n");
        return sb.toString();
    }

    private String displayManagementInterfaces() {
        StringBuilder sb = new StringBuilder();
        Iterator it = managementInterfaces.iterator();
        while (it.hasNext()) {
            Object pmo = it.next();
            sb.append(pmo.toString()).append('\n');
        }
        return sb.toString();
    }

    private String displayMemoryManagerMXBeanList(List<MemoryManagerMXBean> lmmmxb) {
        StringBuilder sb = new StringBuilder();
        Iterator<MemoryManagerMXBean> it = lmmmxb.iterator();
        while (it.hasNext()) {
            sb.append(displayManagementBean(it.next()));
        }
        return sb.toString();
    }

    private String displayManagementBean(MemoryManagerMXBean mxBean) {
        StringBuilder sb = new StringBuilder();
        if (mxBean != null) {
            Generics.StringArrayList memoryPoolNames = new Generics.StringArrayList(mxBean.getMemoryPoolNames());
            sb.append("Memory Manager:\t").append(mxBean.getName()).append("\n");
            sb.append("Pool names:\t").append(memoryPoolNames).append('\n');
        }
        return sb.toString();
    }

    private String displayManagementBean(ClassLoadingMXBean mxBean) {
        StringBuilder sb = new StringBuilder();
        if (mxBean != null) {
            sb.append(mxBean.toString()).append('\n');
            sb.append(mxBean.getObjectName().toString()).append('\n');
            sb.append("Loaded Class Count:\t").append(mxBean.getLoadedClassCount()).append('\n');
            sb.append("Total Loaded Class Count:\t").append(mxBean.getTotalLoadedClassCount()).append('\n');
            sb.append("Unloaded Class Count:\t").append(mxBean.getUnloadedClassCount()).append('\n');
        }
        return sb.toString();
    }

    private String displayManagementBean(OperatingSystemMXBean mxBean) {
        StringBuilder sb = new StringBuilder();
        if (mxBean != null) {
            sb.append(mxBean.toString());
            sb.append(mxBean.getName());
            sb.append('\n').append(mxBean.getObjectName().toString()).append('\n');
            sb.append("Architecture:\t").append(mxBean.getArch()).append('\n');
            sb.append("Operating System:\t").append(mxBean.getName()).append('\n');;
            sb.append("Version:\t").append(mxBean.getVersion()).append('\n');
            sb.append("Available Processors:\t").append(mxBean.getAvailableProcessors()).append('\n');
            sb.append("System Load Average:\t").append(mxBean.getSystemLoadAverage()).append('\n');
        }
        return sb.toString();
    }

    private String displayManagementBean(RuntimeMXBean mxBean) {
        StringBuilder sb = new StringBuilder();
        if (mxBean != null) {
            sb.append(mxBean.toString());
            sb.append(mxBean.getName()).append('\n');
            sb.append(mxBean.getObjectName().toString()).append('\n');
            sb.append("Spec Name:\t").append(mxBean.getSpecName()).append('\n');
            sb.append("Spec Vendor:\t").append(mxBean.getSpecVendor()).append('\n');
            sb.append("Spec Version:\t").append(mxBean.getSpecVersion()).append('\n');
            sb.append("Management Spec Version:\t").append(mxBean.getManagementSpecVersion()).append('\n');
            sb.append("Boot Class Path:\t").append(mxBean.getBootClassPath()).append('\n');
            sb.append("Class Path:\t").append(mxBean.getClassPath()).append('\n');
            sb.append("Library Path:\t").append(mxBean.getLibraryPath()).append('\n');
            sb.append("Input Arguments:\t").append(mxBean.getInputArguments()).append('\n');
        }
        return sb.toString();
    }

    private String displayManagementBean(ThreadMXBean mxBean) {
        StringBuilder sb = new StringBuilder();
        if (mxBean != null) {
            sb.append(mxBean.toString());
            sb.append(mxBean.getObjectName().toString()).append('\n');
            sb.append("Thread Count:\t").append(mxBean.getThreadCount()).append('\n');
            sb.append("Daemon Thread Count:\t").append(mxBean.getDaemonThreadCount()).append('\n');
            sb.append("Total Started Thread Count:\t").append(mxBean.getTotalStartedThreadCount()).append('\n');
            sb.append("Peak Thread Count:\t").append(mxBean.getPeakThreadCount()).append('\n');
            sb.append("Current Thread Cpu Time:\t");
            try {
                sb.append(mxBean.getCurrentThreadCpuTime()).append('\n');
            } catch (UnsupportedOperationException ex) {
                sb.append("Operation not supported")
                        .append('\t')
                        .append(ex)
                        .append('\n');
            }
            sb.append("Current Thread User Time:\t");
            try {
                sb.append(mxBean.getCurrentThreadUserTime()).append('\n');

            } catch (UnsupportedOperationException ex) {
                sb.append("Operation not supported")
                        .append('\t')
                        .append(ex)
                        .append('\n');
            }
            sb.append("Thread IDs:\t");
            for (long i : mxBean.getAllThreadIds()) {
                sb.append(i).append(' ');
            }
            sb.replace(sb.length() - 1, sb.length(), "\n");
        }
        return sb.toString();
    }

    private String displayManagementBean(MemoryMXBean mxBean) {
        StringBuilder sb = new StringBuilder();
        if (mxBean != null) {
            mxBean.setVerbose(true);
            sb.append("Memory object name:\t").append(mxBean.getObjectName()).append('\n');
            sb.append("Heap usage:\t").append(mxBean.getHeapMemoryUsage()).append('\n');
            sb.append("Non-heap usage:\t").append(mxBean.getNonHeapMemoryUsage()).append('\n');
            sb.append("ObjectPendingFinalizationCount:\t").append(mxBean.getObjectPendingFinalizationCount()).append('\n');
        }
        return sb.toString();
    }

    private String displayGarbageCollectorMXBeanList(List<GarbageCollectorMXBean> lgcmxb) {
        StringBuilder sb = new StringBuilder();
        Iterator<GarbageCollectorMXBean> it = lgcmxb.iterator();
        while (it.hasNext()) {
            sb.append(displayManagementBean(it.next()));
        }
        return sb.toString();
    }

    private String displayManagementBean(GarbageCollectorMXBean mxBean) {
        StringBuilder sb = new StringBuilder();
        if (mxBean != null) {
            sb.append(mxBean.getObjectName()).append('\n');
            sb.append(mxBean.getName()).append('\n');
            sb.append("Memory Pool Names:\t");
            for (String mpName : mxBean.getMemoryPoolNames()) {
                sb.append(mpName).append(' ');
            }
            sb.append("Collection Count:\t").append(mxBean.getCollectionCount()).append('\n');
            sb.append("Collection Time:\t").append(mxBean.getCollectionTime()).append('\n');
        }
        return sb.toString();
    }
//    /**
//     *
//     * @param args
//     */
//    public static void main(String[] args) {
//        JVMHelper jVMHelper = new JVMHelper();
//        System.out.println(jVMHelper.toString());
//        System.exit(0);
//    }
}
