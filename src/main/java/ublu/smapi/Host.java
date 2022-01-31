/*
 * Copyright (c) 2015, Jack J. Woehr jax@well.com po box 51 golden co 80402-0051
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
package ublu.smapi;

/**
 * Represent a SMAPI host
 *
 * @author jax
 */
public class Host {

    private final String hostname;
    private final Integer port;
    private final String userid;
    private final String password;
    private final boolean use_ssl;

    /**
     * Instance with all factors
     *
     * @param hostname
     * @param port
     * @param username
     * @param password
     * @param use_ssl true if ssl
     */
    public Host(String hostname, Integer port, String username, String password, boolean use_ssl) {
        this.hostname = hostname;
        this.port = port;
        this.userid = username;
        this.password = password;
        this.use_ssl = use_ssl;
    }

    /**
     *
     * @return hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     *
     * @return port
     */
    public Integer getPort() {
        return port;
    }

    /**
     *
     * @return userid
     */
    public String getUserid() {
        return userid;
    }

    /**
     *
     * @return password
     */
    protected String getPassword() {
        return password;
    }

    /**
     *
     * @return true if ssl
     */
    public boolean isUse_ssl() {
        return use_ssl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" hostname=")
                .append(getHostname())
                .append(" port=")
                .append(getPort())
                .append(" username=")
                .append(getUserid())
                .append(" use_ssl=")
                .append(isUse_ssl());
        return sb.toString();
    }
}
