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
 * Format and transmit System Shepherd datapoints
 *
 * @author jwoehr
 */
public class SysShepHelper {

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
     * How we compare our datapoint with the alert level
     */
    public static enum ALERTCOMPARATOR {

        /**
         *
         */
        GT,
        /**
         *
         */
        LT,
        /**
         *
         */
        GTE,
        /**
         *
         */
        LTE,
        /**
         * Always info
         */
        INFO,
        /**
         * Always warn
         */
        WARN,
        /**
         * Always alert
         */
        CRIT
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
        if (status.equals(STATUS.CRITICAL)) {
            bldr.append("\t");
            bldr.append(message);
        }
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
        if (status.equals(STATUS.CRITICAL)) {
            bldr.append("\t");
            bldr.append(message);
        }
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
        if (status.equals(STATUS.CRITICAL)) {
            bldr.append("\t");
            bldr.append(message);
        }
        return bldr.toString();
    }
    //
    // Private attributes
    private MetricName metric;
    private Number value;
    private String message;
    private Number alertlevel;
    private ALERTCOMPARATOR alertcomparator;

    /**
     * Return the name of the datapoint metric
     *
     * @return the name of the datapoint metric
     */
    public final MetricName getMetric() {
        return metric;
    }

    /**
     *
     * @param metric
     */
    public final void setMetric(MetricName metric) {
        this.metric = metric;
    }

    /**
     *
     * @return value of metric
     */
    public final Number getValue() {
        return value;
    }

    /**
     *
     * @param value value of metric
     */
    public final void setValue(Number value) {
        this.value = value;
    }

    /**
     *
     * @return message for datapoint
     */
    public final String getMessage() {
        return message;
    }

    /**
     *
     * @param message message for datapoint
     */
    public final void setMessage(String message) {
        this.message = message;
    }

    /**
     *
     * @return alert level
     */
    public final Number getAlertlevel() {
        return alertlevel;
    }

    /**
     *
     * @param alertlevel alert level
     */
    public final void setAlertlevel(Number alertlevel) {
        this.alertlevel = alertlevel;
    }

    /**
     *
     * @return the comparison value against datapoint value for an alert
     */
    public final ALERTCOMPARATOR getAlertcomparator() {
        return alertcomparator;
    }

    /**
     *
     * @param alertcomparator the comparison value against datapoint value for
     * an alert
     */
    public final void setAlertcomparator(ALERTCOMPARATOR alertcomparator) {
        this.alertcomparator = alertcomparator;
    }

    /**
     * /0 ctor
     */
    public SysShepHelper() {
    }

    /**
     *
     * Copy ctor
     *
     * @param ssh copied instance
     */
    public SysShepHelper(SysShepHelper ssh) {
        this(ssh.getMetric(), ssh.getValue(), ssh.getMessage(), ssh.getAlertlevel(), ssh.getAlertcomparator());
    }

    /**
     * Instance all
     *
     * @param metric
     * @param value
     * @param message
     * @param alertlevel
     * @param alertcomparator
     */
    public SysShepHelper(MetricName metric, Number value, String message, Number alertlevel, ALERTCOMPARATOR alertcomparator) {
        this();
        if (metric != null) {
            this.metric = new MetricName(metric.toString());
        }
        this.value = value;
        this.message = message;
        this.alertlevel = alertlevel;
        this.alertcomparator = alertcomparator;
    }

    /**
     *
     * @param ssh instance to copy
     * @return copied instance
     */
    public SysShepHelper copyFrom(SysShepHelper ssh) {
        setMetric(ssh.getMetric());
        setValue(ssh.getValue());
        setMessage(ssh.getMessage());
        setAlertlevel(ssh.getAlertlevel());
        setAlertcomparator(ssh.getAlertcomparator());
        return this;
    }

    private Class getValueClass() {
        Class c = null;
        Object o = getValue();
        if (o != null) {
            c = o.getClass();
        }
        return c;
    }

    /**
     * Get alert status based on float comparison
     *
     * @return Alert status based on float comparison
     */
    public STATUS getFloatStatus() {
        STATUS status = STATUS.OK;
        // /* debug */ System.err.println("Alert comparator is " + getAlertcomparator());
        Number theValue = getValue();
        switch (getAlertcomparator()) {
            case GT:
                // /* debug */ System.err.println("value is " + Float.class.cast(theValue) + " and alert level is " + Float.class.cast(getAlertlevel()));
                if (Float.class.cast(theValue) > Float.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case GTE:
                if (Float.class.cast(theValue) >= Float.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case LT:
                if (Float.class.cast(theValue) < Float.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case LTE:
                if (Float.class.cast(theValue) <= Float.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case INFO:
                status = STATUS.OK;
                break;
            case WARN:
                status = STATUS.WARNING;
                break;
            case CRIT:
                status = STATUS.CRITICAL;
                break;
        }
        return status;
    }

    /**
     * Get alert status based on long comparison
     *
     * @return Alert status based on long comparison
     */
    public STATUS getLongStatus() {
        STATUS status = STATUS.OK;
        Number theValue = getValue();
        switch (getAlertcomparator()) {
            case GT:
                if (Long.class.cast(theValue) > Long.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case GTE:
                if (Long.class.cast(theValue) >= Long.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case LT:
                if (Long.class.cast(theValue) < Long.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case LTE:
                if (Long.class.cast(theValue) <= Long.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case INFO:
                status = STATUS.OK;
                break;
            case WARN:
                status = STATUS.WARNING;
                break;
            case CRIT:
                status = STATUS.CRITICAL;
                break;
        }
        return status;
    }

    /**
     * Get alert status based on integer comparison
     *
     * @return alert status based on integer comparison
     */
    public STATUS getIntegerStatus() {
        STATUS status = STATUS.OK;
        Number theValue = getValue();
        switch (getAlertcomparator()) {
            case GT:
                if (Integer.class.cast(theValue) > Integer.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case GTE:
                if (Integer.class.cast(theValue) >= Integer.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case LT:
                if (Integer.class.cast(theValue) < Integer.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case LTE:
                if (Integer.class.cast(theValue) <= Integer.class.cast(getAlertlevel())) {
                    status = STATUS.CRITICAL;
                }
                break;
            case INFO:
                status = STATUS.OK;
                break;
            case WARN:
                status = STATUS.WARNING;
                break;
            case CRIT:
                status = STATUS.CRITICAL;
                break;
        }
        return status;
    }

    /**
     * Get the alert status
     *
     * @return Alert status
     * @throws ublu.util.SysShepHelper.BadNumberClassException
     */
    public STATUS getStatus() throws BadNumberClassException {
        STATUS status = STATUS.OK;
        if (getValue() != null && getAlertlevel() != null) {
            Class valueClass = getValueClass();
            if (valueClass.equals(Float.class)) {
                status = getFloatStatus();
            } else if (valueClass.equals(Long.class)) {
                status = getLongStatus();
            } else if (valueClass.equals(Integer.class)) {
                status = getIntegerStatus();
            } else {
                throw new BadNumberClassException("Bad number class for status calculation");
            }
        }
        return status;
    }

    /**
     *
     * @return Alert status
     * @throws ublu.util.SysShepHelper.BadNumberClassException
     */
    public String format() throws BadNumberClassException {
        String datapoint = null;
        Class valueClass = getValueClass();
        if (getValue() != null) {
            if (valueClass.equals(Float.class)) {
                /**
                 * DEBUG *
                 */
                // System.err.println("Status is " + getFloatStatus());
                datapoint = format(getMetric().toString(), Float.class.cast(getValue()), getStatus(), getMessage());
            } else if (valueClass.equals(Long.class)) {
                /**
                 * DEBUG *
                 */
                // System.err.println("Status is " + getLongStatus());
                datapoint = format(getMetric().toString(), Long.class.cast(getValue()), getStatus(), getMessage());
            } else if (valueClass.equals(Integer.class)) {
                /**
                 * DEBUG *
                 */
                // System.err.println("Status is " + getIntegerStatus());
                datapoint = format(getMetric().toString(), Integer.class.cast(getValue()), getStatus(), getMessage());
            } else {
                throw new BadNumberClassException("Bad number class for datapoint formulation");
            }
        } else {
            datapoint = format(getMetric().toString(), 0, STATUS.OK, getMessage());
        }
        return datapoint;
    }

    @Override
    public String toString() {
        String s;
        try {
            s = format();
        } catch (BadNumberClassException ex) {
            s = ex.toString();
        }
        return s;
    }

    /**
     * Class representing the name of a datapoint metric
     */
    public static class MetricName {

        StringBuilder myName;

        /**
         * Instance on an empty name
         */
        public MetricName() {
            myName = new StringBuilder("");
        }

        /**
         * Instance providing an initial element for the metric name
         *
         *
         * @param element initial element for the metric name
         */
        public MetricName(String element) {
            this();
            myName.append(element);
        }

        /**
         * Append an element to the metric name being formulated
         *
         * @param s an element to append
         * @return resultant metric name
         */
        public MetricName append(String s) {
            if (myName.length() > 0) {
                myName.append('|');
            }
            myName.append(s);
            return this;
        }

        @Override
        public String toString() {
            return myName.toString();
        }
    }

    /**
     * Exception for when wrong type of number provided for a SysShep datapoint.
     */
    public class BadNumberClassException extends Exception {

        /**
         * Instance on a message
         *
         * @param message the message
         */
        public BadNumberClassException(String message) {
            super(message);
        }
    }

    /**
     * Test routine
     *
     * @param args metricName value(int) status
     */
    public static void main(String args[]) {
        String metricName = args[0];
        int value = Integer.parseInt(args[1]);
        SysShepHelper.STATUS status = STATUS.valueOf(args[2]);
        String message = args[3];
        System.out.println(SysShepHelper.format(metricName, value, status, message));
    }
}
