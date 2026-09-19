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
 * Unit tests for {@link Tuple}.
 */
public class TupleTest {

    @Test
    public void constructorSetsKey() {
        Tuple t = new Tuple("@myvar");
        assertEquals("@myvar", t.getKey());
    }

    @Test
    public void constructorSetsValue() {
        Tuple t = new Tuple("@myvar", "hello");
        assertEquals("hello", t.getValue());
    }

    @Test
    public void setValueUpdatesAndReturnsSelf() {
        Tuple t = new Tuple("@myvar");
        Tuple returned = t.setValue("world");
        assertSame("setValue should return self", t, returned);
        assertEquals("world", t.getValue());
    }

    @Test
    public void nullValueIsStoredAndRetrievable() {
        Tuple t = new Tuple("@myvar", null);
        assertNull(t.getValue());
    }

    @Test
    public void setValueToNullIsRetrievable() {
        Tuple t = new Tuple("@myvar", "original");
        t.setValue(null);
        assertNull(t.getValue());
    }

    @Test
    public void tupleCharConstantIsAtSign() {
        assertEquals('@', Tuple.TUPLECHAR);
    }
}
