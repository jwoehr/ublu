/*
 * Copyright (c) 2024, Jack J. Woehr jwoehr@softwoehr.com
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
 * ARE DISCLAIMED.
 */
package ublu.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link DataSink}.
 */
public class DataSinkTest {

    @Test
    public void atPrefixYieldsTupleSinkType() {
        assertEquals(DataSink.SINKTYPE.TUPLE, DataSink.sinkNameType("@myvar"));
    }

    @Test
    public void httpPrefixYieldsUrlSinkType() {
        assertEquals(DataSink.SINKTYPE.URL, DataSink.sinkNameType("http://example.com"));
    }

    @Test
    public void nullColonYieldsNullSinkType() {
        assertEquals(DataSink.SINKTYPE.NULL, DataSink.sinkNameType("NULL:"));
    }

    @Test
    public void stdColonYieldsStdSinkType() {
        assertEquals(DataSink.SINKTYPE.STD, DataSink.sinkNameType("STD:"));
    }

    @Test
    public void errColonYieldsErrSinkType() {
        assertEquals(DataSink.SINKTYPE.ERR, DataSink.sinkNameType("ERR:"));
    }

    @Test
    public void tildeYieldsLifoSinkType() {
        assertEquals(DataSink.SINKTYPE.LIFO, DataSink.sinkNameType("~"));
    }

    @Test
    public void plainNameYieldsFileSinkType() {
        assertEquals(DataSink.SINKTYPE.FILE, DataSink.sinkNameType("/tmp/output.txt"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullNameThrowsIllegalArgumentException() {
        DataSink.sinkNameType(null);
    }
}
