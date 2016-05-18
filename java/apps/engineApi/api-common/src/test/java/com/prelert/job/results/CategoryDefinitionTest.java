/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.results;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;

public class CategoryDefinitionTest
{
    @Test
    public void testEquals_GivenSameObject()
    {
        CategoryDefinition category = new CategoryDefinition();

        assertTrue(category.equals(category));
    }

    @Test
    public void testEquals_GivenObjectOfDifferentClass()
    {
        CategoryDefinition category = new CategoryDefinition();

        assertFalse(category.equals("a string"));
    }

    @Test
    public void testEquals_GivenEqualCategoryDefinitions()
    {
        CategoryDefinition category1 = new CategoryDefinition();
        category1.setCategoryId(42);
        category1.setTerms("foo bar");
        category1.setRegex(".*?foo.*?bar.*");
        category1.addExample("foo");
        category1.addExample("bar");
        CategoryDefinition category2 = new CategoryDefinition();
        category2.setCategoryId(42);
        category2.setTerms("foo bar");
        category2.setRegex(".*?foo.*?bar.*");
        category2.addExample("bar");
        category2.addExample("foo");

        assertTrue(category1.equals(category2));
        assertTrue(category2.equals(category1));
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentIds()
    {
        CategoryDefinition category1 = new CategoryDefinition();
        category1.setCategoryId(42);
        category1.setTerms("foo bar");
        category1.setRegex(".*?foo.*?bar.*");
        category1.addExample("foo");
        category1.addExample("bar");
        CategoryDefinition category2 = new CategoryDefinition();
        category2.setCategoryId(1);
        category2.setTerms("foo bar");
        category2.setRegex(".*?foo.*?bar.*");
        category2.addExample("bar");
        category2.addExample("foo");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentTerms()
    {
        CategoryDefinition category1 = new CategoryDefinition();
        category1.setCategoryId(42);
        category1.setTerms("foo bar");
        category1.setRegex(".*?foo.*?bar.*");
        category1.addExample("foo");
        category1.addExample("bar");
        CategoryDefinition category2 = new CategoryDefinition();
        category2.setCategoryId(42);
        category2.setTerms("foo");
        category2.setRegex(".*?foo.*?bar.*");
        category2.addExample("bar");
        category2.addExample("foo");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentRegex()
    {
        CategoryDefinition category1 = new CategoryDefinition();
        category1.setCategoryId(42);
        category1.setTerms("foo bar");
        category1.setRegex(".*?foo.*?bar.*");
        category1.addExample("foo");
        category1.addExample("bar");
        CategoryDefinition category2 = new CategoryDefinition();
        category2.setCategoryId(42);
        category2.setTerms("foo bar");
        category2.setRegex(".*?foo.*");
        category2.addExample("bar");
        category2.addExample("foo");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentExamples()
    {
        CategoryDefinition category1 = new CategoryDefinition();
        category1.setCategoryId(42);
        category1.setTerms("foo bar");
        category1.setRegex(".*?foo.*?bar.*");
        category1.addExample("foo");
        category1.addExample("bar");
        CategoryDefinition category2 = new CategoryDefinition();
        category2.setCategoryId(42);
        category2.setTerms("foo bar");
        category2.setRegex(".*?foo.*?bar.*");
        category2.addExample("bar");
        category2.addExample("foobar");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testHashCode_GivenEqualCategoryDefinitions()
    {
        CategoryDefinition category1 = new CategoryDefinition();
        category1.setCategoryId(42);
        category1.addExample("foo");
        category1.addExample("bar");
        CategoryDefinition category2 = new CategoryDefinition();
        category2.setCategoryId(42);
        category2.addExample("bar");
        category2.addExample("foo");

        assertEquals(category1.hashCode(), category2.hashCode());
    }

    @Test
    public void testSerialise() throws IOException
    {
        CategoryDefinition category = new CategoryDefinition();
        category.setCategoryId(42);
        category.setTerms("foo bar");
        category.setRegex(".*?foo.*?bar.*");
        category.addExample("bar");
        category.addExample("foobar");

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        category.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"regex\":\".*?foo.*?bar.*\","
                + "\"examples\":[\"bar\",\"foobar\"],"
                + "\"terms\":\"foo bar\","
                + "\"categoryId\":42"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }
}
