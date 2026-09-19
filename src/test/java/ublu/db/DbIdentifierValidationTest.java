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
package ublu.db;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the SQL identifier validation in {@link Db#validateIdentifier(String)}.
 */
public class DbIdentifierValidationTest {

    @Test
    public void simpleTableNamePasses() {
        Db.validateIdentifier("EMPLOYEE");
    }

    @Test
    public void schemaQualifiedNamePasses() {
        Db.validateIdentifier("MYSCHEMA.EMPLOYEE");
    }

    @Test
    public void nameWithUnderscorePasses() {
        Db.validateIdentifier("MY_TABLE");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullThrows() {
        Db.validateIdentifier(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void semicolonInjectionThrows() {
        Db.validateIdentifier("users; DROP TABLE users; --");
    }

    @Test(expected = IllegalArgumentException.class)
    public void quoteEscapeThrows() {
        Db.validateIdentifier("users\" OR \"1\"=\"1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void spaceInNameThrows() {
        Db.validateIdentifier("my table");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyStringThrows() {
        Db.validateIdentifier("");
    }
}
