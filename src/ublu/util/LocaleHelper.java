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
     * get language
     *
     * @return language
     */
    public String getLanguage() {
        return currentLocale.getLanguage();
    }

    /**
     * get country
     *
     * @return country
     */
    public String getCountry() {
        return currentLocale.getCountry();
    }

    /**
     * get current locale
     *
     * @return current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Get msg res bundle
     *
     * @return msg res bundle
     */
    public ResourceBundle getMessages() {
        return messages;
    }

    /**
     * not used
     */
    private LocaleHelper() {
    }

    /**
     * Ctor/3
     *
     * @param language language
     * @param country country
     * @param messageResourcePath messageResourcePath
     */
    public LocaleHelper(String language, String country, String messageResourcePath) {
        setLocale(language, country);
        setMessageBundle(messageResourcePath);
    }

    /**
     * Copy Ctor
     *
     * @param lh model
     */
    public LocaleHelper(LocaleHelper lh) {
        setLocale(lh.currentLocale.getLanguage(), lh.currentLocale.getCountry());
        // /* 1.8 */ setMessageBundle(lh.messages.getBaseBundleName());
        this.messages = lh.messages;
    }

    /**
     * Get message for key
     *
     * @param key key
     * @return message
     */
    public String getString(String key) {
        String result = null;
        if (messages != null) {
            result = messages.getString(key);
        }
        return result;
    }

    /**
     * set locale
     *
     * @param language language
     * @param country country
     */
    public final void setLocale(String language, String country) {
        currentLocale = new Locale(language == null ? "en" : language, country == null ? "US" : country);
    }

    /**
     * instance active message bundle for given locale
     *
     * @param messageResourcePath path to messages
     */
    public final void setMessageBundle(String messageResourcePath) {
        this.messages = ResourceBundle.getBundle(messageResourcePath, currentLocale);
    }

    /**
     * reinstance active message bundle for current locale
     */
    public final void resetMessageBundle() {
        setMessageBundle(messages.getBaseBundleName());
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
