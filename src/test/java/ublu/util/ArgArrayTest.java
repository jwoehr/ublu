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
 * Unit tests for {@link ArgArray}.
 */
public class ArgArrayTest {

    @Test
    public void defaultConstructionProducesEmptyArray() {
        ArgArray aa = new ArgArray(null);
        assertTrue("Newly constructed ArgArray should be empty", aa.isEmpty());
    }

    @Test
    public void constructFromStringArrayRetainsElements() {
        String[] args = {"foo", "bar", "baz"};
        ArgArray aa = new ArgArray(null, args);
        assertEquals(3, aa.size());
        assertEquals("foo", aa.get(0));
        assertEquals("bar", aa.get(1));
        assertEquals("baz", aa.get(2));
    }

    @Test
    public void toHistoryLineJoinsElementsWithTrailingSpace() {
        String[] args = {"put", "@result", "hello"};
        ArgArray aa = new ArgArray(null, args);
        assertEquals("put @result hello ", aa.toHistoryLine());
    }

    @Test
    public void toHistoryLineOnEmptyArrayReturnsEmptyString() {
        ArgArray aa = new ArgArray(null);
        assertEquals("", aa.toHistoryLine());
    }

    @Test
    public void isPopTupleSignReturnsTrueForTilde() {
        assertTrue(ArgArray.isPopTupleSign("~"));
    }

    @Test
    public void isPopTupleSignReturnsFalseForOtherString() {
        assertFalse(ArgArray.isPopTupleSign("@foo"));
    }
}
