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

import ublu.util.Generics.SystemValueHashMap;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SystemValue;
import com.ibm.as400.access.SystemValueList;
import java.io.IOException;
import java.util.Set;

/**
 * Help manipulate system values for host
 *
 * @author jwoehr
 */
public class SysValHelper {

    private SystemValueList systemValueList;
    private SystemValueHashMap systemValueHashMap;

    private SysValHelper() {
    }

    /**
     * Instance on an as400
     *
     * @param as400
     */
    public SysValHelper(AS400 as400) {
        this();
        systemValueList = new SystemValueList(as400);
    }

    /**
     * Get the host system
     *
     * @return the host system
     */
    public AS400 getSystem() {
        return systemValueList.getSystem();
    }

    /**
     * Instances the hash map of system values with the values from the group
     * selected
     *
     * @param group constant group indicator
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws ObjectDoesNotExistException
     */
    public void instanceSystemValues(int group) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
        this.systemValueHashMap = new SystemValueHashMap(systemValueList.getGroup(group));

    }

    /**
     * Get the keyset of the hash map of system values
     *
     * @return the keyset of the hash map
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     */
    public Set<String> keySet() throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException, RequestNotSupportedException {
        Set<String> keyset = null;
        if (systemValueHashMap != null) {
            keyset = systemValueHashMap.keySet();
        }
        return keyset;
    }

    /**
     * Return a system value object by key from the hash map of system values
     *
     * @param key sought
     * @return the value
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     */
    public SystemValue getSystemValue(String key) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException, RequestNotSupportedException {
        SystemValue sv = null;
        if (systemValueHashMap != null) {
            sv = systemValueHashMap.get(key);
        }
        return sv;
    }

    /**
     * Return the SystemValue's value as an object by key
     *
     * @param key sought
     * @return the system value contained in the SystemValue object or null
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     */
    public Object getValue(String key) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException, RequestNotSupportedException {
        Object o = null;
        if (systemValueHashMap != null) {
            SystemValue sv = systemValueHashMap.get(key);
            if (sv != null) {
                o = sv.getValue();
            }
        }
        return o;
    }

    /**
     * Test if hash map of system values contains a key
     *
     * @param key sought
     * @return t|f
     */
    public boolean hasKey(String key) {
        return systemValueHashMap.containsKey(key);
    }

    /**
     * Set a value by key
     *
     * @param key
     * @param value
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws IOException
     * @throws RequestNotSupportedException
     */
    public void set(String key, Object value) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, RequestNotSupportedException {
        SystemValue sv = systemValueHashMap.get(key);
        if (sv != null) {
            sv.setValue(value);
        }
    }

    /**
     * Return the value list associated with a host
     *
     * @return the value list associated with a host
     */
    public SystemValueList getSystemValueList() {
        return systemValueList;
    }

    /**
     * The hash map of systemvalue objects
     *
     * @return The hash map of systemvalue objects
     */
    public SystemValueHashMap getSystemValueHashMap() {
        return systemValueHashMap;
    }
}
