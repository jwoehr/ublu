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
package ublu.win;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Set;

/**
 * Manipulate properties that control behavior of system, also for user-defined
 * properties.
 *
 * @author jwoehr
 */
public class WinProps {

    /**
     *
     */
    public static final String PROPSCOMMENT = "Ublu Windowing Properties";

    Properties myProperties;
    UbluWinController myUbluWinController;

    /**
     *
     * @param ubluWinController
     */
    public WinProps(UbluWinController ubluWinController) {
        this();
        myUbluWinController = ubluWinController;
    }

    /**
     * Instance with a new Properties member
     */
    public WinProps() {
        myProperties = new Properties();
        setDefaultWindowingProperties();
    }

    /**
     * Instance on an extant Properties
     *
     * @param ubluWinController
     * @param properties
     */
    public WinProps(UbluWinController ubluWinController, Properties properties) {
        myUbluWinController = ubluWinController;
        myProperties = properties;
    }

    /**
     * Read in props from a props file
     *
     * @param filepath
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void readIn(String filepath) throws FileNotFoundException, IOException {
        Properties p = new Properties();
        try (InputStream is = new FileInputStream(filepath)) {
            try (InputStreamReader isr = new InputStreamReader(is)) {
                p.load(isr);
            }
            is.close();
        }
        myProperties.putAll(p);
    }

    /**
     * Write out props to a props file
     *
     * @param filepath
     * @param comment
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void writeOut(String filepath, String comment) throws FileNotFoundException, IOException {
        try (FileOutputStream fos = new FileOutputStream(filepath)) {
            myProperties.store(fos, comment);
        }
    }

    /**
     * Get a property
     *
     * @param propname
     * @return property string
     */
    public String get(String propname) {
        return myProperties.getProperty(propname);
    }

    /**
     * Get a property with a default value if that property isn't set
     *
     * @param propname
     * @param defaultValue
     * @return property string
     */
    public String get(String propname, String defaultValue) {
        return myProperties.getProperty(propname, defaultValue);
    }

    /**
     * Set a property to a string value
     *
     * @param propname
     * @param value
     */
    public void set(String propname, String value) {
        myProperties.setProperty(propname, value);
    }

    /**
     * Get all property keys that are set
     *
     * @return all keys
     */
    public Set keySet() {
        return myProperties.keySet();
    }

    /**
     *
     * @return
     */
    public final WinProps setDefaultWindowingProperties() {
        set("UbluTextAreaFont", "Lucida Sans Typewriter");
        set("UbluInputAreaFont", "Lucida Sans Typewriter");
        set("UbluInputAreaFontSize", "13");
        set("UbluTextAreaFontStyle", "0");
        set("UbluTextAreaFontSize", "13");
        set("UbluInputAreaFontStyle", "0");
        set("UbluTextAreaFGColor", "ff333333");
        set("UbluInputAreaFGColor", "ff333333");
        set("UbluTextAreaBGColor", "ffffff");
        set("UbluInputAreaBGColor", "ffffff");
        return this;
    }

    /**
     *
     * @param filepath
     * @throws IOException
     */
    public void writeWindowingProperties(String filepath) throws IOException {
        writeOut(filepath, PROPSCOMMENT);
    }
}
