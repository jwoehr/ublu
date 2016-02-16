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

/**
 * Format System Shepherd datapoints
 *
 * @deprecated use SysShepHelper instead
 * @author jwoehr
 */
public class SysShepFormatter {

    /**
     * The status of the datapoint
     */
    public static enum STATUS {

        /**
         * okay
         */
        OK,
        /**
         * warning
         */
        WARNING,
        /**
         * critical
         */
        CRITICAL
    }

    /**
     * Format a datapoint with a {@code long} value
     *
     * @param metricName
     * @param longValue
     * @param status
     * @param message
     * @return the datapoint formatted
     */
    public static String format(String metricName, long longValue, STATUS status, String message) {
        StringBuilder bldr = new StringBuilder(metricName);
        bldr.append("\t");
        bldr.append(String.valueOf(longValue));
        bldr.append("\t");
        bldr.append(status.ordinal());
        bldr.append("\t");
        bldr.append(message);
        return bldr.toString();
    }

    /**
     * Format a datapoint with a {@code float} value
     *
     * @param metricName
     * @param floatValue
     * @param status
     * @param message
     * @return the datapoint formatted
     */
    public static String format(String metricName, float floatValue, STATUS status, String message) {
        StringBuilder bldr = new StringBuilder(metricName);
        bldr.append("\t");
        bldr.append(String.valueOf(floatValue));
        bldr.append("\t");
        bldr.append(status.ordinal());
        bldr.append("\t");
        bldr.append(message);
        return bldr.toString();
    }

    /**
     * Format a datapoint with an {@code int} value
     *
     * @param metricName
     * @param value
     * @param status
     * @param message
     * @return the datapoint formatted
     */
    public static String format(String metricName, int value, STATUS status, String message) {
        StringBuilder bldr = new StringBuilder(metricName);
        bldr.append("\t");
        bldr.append(String.valueOf(value));
        bldr.append("\t");
        bldr.append(status.ordinal());
        bldr.append("\t");
        bldr.append(message);
        return bldr.toString();
    }

    /**
     * Test routine
     *
     * @param args metricName value(int) status
     */
    public static void main(String args[]) {
        String metricName = args[0];
        int value = Integer.parseInt(args[1]);
        SysShepFormatter.STATUS status = STATUS.valueOf(args[2]);
        String message = args[3];
        System.out.println(SysShepFormatter.format(metricName, value, status, message));
    }
}
