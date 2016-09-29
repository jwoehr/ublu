/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com http://www.softwoehr.com
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

/**
 * Show license
 *
 * @author jwoehr
 */
public class License {

    private static final String[] LICENSE_TEXT = {
        "/*",
        " * Ublu Midrange and Mainframe Life Cycle Extension Language",
        " * Author : Jack J. Woehr jwoehr@softwoehr.com",
        " * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com",
        " * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com http://www.softwoehr.com",
        " * All rights reserved.",
        " *",
        " * Redistribution and use in source and binary forms, with or without",
        " * modification, are permitted provided that the following conditions are met:",
        " *",
        " * * Redistributions of source code must retain the above copyright notice, this",
        " *   list of conditions and the following disclaimer.",
        " * * Redistributions in binary form must reproduce the above copyright notice,",
        " *   this list of conditions and the following disclaimer in the documentation",
        " *   and/or other materials provided with the distribution.",
        " *",
        " * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"",
        " * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE",
        " * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE",
        " * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE",
        " * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR",
        " * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF",
        " * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS",
        " * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN",
        " * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)",
        " * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE",
        " * POSSIBILITY OF SUCH DAMAGE.",
        " */"
    };

    /**
     * String representing license text.
     *
     * @return String representing license text.
     */
    public static String show() {
        StringBuilder sb = new StringBuilder();
        for (String s : LICENSE_TEXT) {
            sb.append(s).append('\n');
        }
        sb.append('\n');
        sb.append("Ublu calls the following open source libraries, the licenses\n")
                .append("for which you should have received with the Ublu distribution\n")
                .append("and which otherwise can be found on their respective websites:\n");
        sb.append(Ublu.openSourceList());
        return sb.toString();
    }

    private License() {
    }
}
