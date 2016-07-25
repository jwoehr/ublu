/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
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

import ublu.util.Generics.ByteArrayList;
import ublu.util.Generics.ByteArrayListArrayList;

/**
 * Utility class to split a byte array into an array of byte arrays from width
 * specs. Used in our database code to split byte data from a single column for
 * insertion into multiple smaller columns.
 *
 * @see ublu.command.CmdRs
 * @author jwoehr
 */
public class ByteArraySplitter {

    private final ByteArrayList srcByteArrayList;

    /**
     * Construct from an array of byte.
     *
     * @param src array of byte to be assimilated (and later split)
     */
    public ByteArraySplitter(byte[] src) {
        srcByteArrayList = new ByteArrayList(src);
    }

    /**
     * Given n split widths as an array of int, split the ByteArray into n
     * ByteArrays split in order per the widths.
     *
     * @param splitWidths array of widths for each split
     * @return an ArrayList of ByteArrayLists constituting the split data
     */
    public ByteArrayListArrayList split(int[] splitWidths) {
        ByteArrayListArrayList result = new ByteArrayListArrayList();
        for (int width : splitWidths) {
            ByteArrayList b = new ByteArrayList();
            for (int i = 0; i < width; i++) {
                b.add(srcByteArrayList.remove(0));
            }
            result.add(b);
        }
        return result;
    }
}
