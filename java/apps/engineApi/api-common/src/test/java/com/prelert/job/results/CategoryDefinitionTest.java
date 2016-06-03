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
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();

        assertTrue(category1.equals(category2));
        assertTrue(category2.equals(category1));
        assertEquals(category1.hashCode(), category2.hashCode());
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentIds()
    {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.setCategoryId(category1.getCategoryId() + 1);

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentTerms()
    {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.setTerms(category1.getTerms() + " additional");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentRegex()
    {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.setRegex(category1.getRegex() + ".*additional.*");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentMaxMatchingLength()
    {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.setMaxMatchingLength(category1.getMaxMatchingLength() + 1);

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testEquals_GivenCategoryDefinitionsWithDifferentExamples()
    {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.addExample("additional");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    @Test
    public void testSerialise() throws IOException
    {
        CategoryDefinition category = createFullyPopulatedCategoryDefinition();

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        category.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"regex\":\".*?foo.*?bar.*\","
                + "\"examples\":[\"bar\",\"foo\"],"
                + "\"terms\":\"foo bar\","
                + "\"maxMatchingLength\":120,"
                + "\"categoryId\":42"
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    private static CategoryDefinition createFullyPopulatedCategoryDefinition()
    {
        CategoryDefinition category = new CategoryDefinition();
        category.setCategoryId(42);
        category.setTerms("foo bar");
        category.setRegex(".*?foo.*?bar.*");
        category.setMaxMatchingLength(120L);
        category.addExample("foo");
        category.addExample("bar");
        return category;
    }
}
