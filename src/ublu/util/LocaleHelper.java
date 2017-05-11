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
package ublu.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Manage locale and resources for the Interpreter
 *
 * @author jax
 */
public class LocaleHelper {

    private Locale currentLocale;
    private ResourceBundle messages;

    /**
     *
     * @return
     */
    public String getLanguage() {
        return currentLocale.getLanguage();
    }

    /**
     *
     * @return
     */
    public String getCountry() {
        return currentLocale.getCountry();
    }

    /**
     *
     * @return
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     *
     * @return
     */
    public ResourceBundle getMessages() {
        return messages;
    }

    /**
     *
     * @param language
     * @param country
     * @param messageResourcePath
     */
    public LocaleHelper(String language, String country, String messageResourcePath) {
        setLocale(language, country);
        setMessageBundle(messageResourcePath);
    }

    /**
     *
     * @param key
     * @return
     */
    public String getString(String key) {
        String result = null;
        if (messages != null) {
            result = messages.getString(key);
        }
        return result;
    }

    /**
     *
     * @param language
     * @param country
     */
    public final void setLocale(String language, String country) {
        currentLocale = new Locale(language == null ? "en" : language, country == null ? "US" : country);
    }

    /**
     *
     * @param messageResourcePath
     */
    public final void setMessageBundle(String messageResourcePath) {
        this.messages = ResourceBundle.getBundle(messageResourcePath, currentLocale);
    }

    /**
     *
     */
    public final void resetMessageBundle() {
        setMessageBundle(getMessages().getBaseBundleName());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(' ')
                .append(this.currentLocale)
                .append(' ')
                .append(this.messages);
        return sb.toString();
    }
}
