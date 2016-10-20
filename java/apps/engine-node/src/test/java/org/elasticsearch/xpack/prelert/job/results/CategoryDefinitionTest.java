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

package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.Arrays;

public class CategoryDefinitionTest extends AbstractSerializingTestCase<CategoryDefinition> {

    @Override
    protected CategoryDefinition createTestInstance() {
        CategoryDefinition categoryDefinition = new CategoryDefinition();
        categoryDefinition.setCategoryId(randomLong());
        categoryDefinition.setTerms(randomAsciiOfLength(10));
        categoryDefinition.setRegex(randomAsciiOfLength(10));
        categoryDefinition.setMaxMatchingLength(randomLong());
        categoryDefinition.setExamples(Arrays.asList(generateRandomStringArray(10, 10, false)));
        return categoryDefinition;
    }

    @Override
    protected Writeable.Reader<CategoryDefinition> instanceReader() {
        return CategoryDefinition::new;
    }

    @Override
    protected CategoryDefinition parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return CategoryDefinition.PARSER.apply(parser, () -> matcher);
    }

    public void testEquals_GivenSameObject() {
        CategoryDefinition category = new CategoryDefinition();

        assertTrue(category.equals(category));
    }

    public void testEquals_GivenObjectOfDifferentClass() {
        CategoryDefinition category = new CategoryDefinition();

        assertFalse(category.equals("a string"));
    }

    public void testEquals_GivenEqualCategoryDefinitions() {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();

        assertTrue(category1.equals(category2));
        assertTrue(category2.equals(category1));
        assertEquals(category1.hashCode(), category2.hashCode());
    }

    public void testEquals_GivenCategoryDefinitionsWithDifferentIds() {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.setCategoryId(category1.getCategoryId() + 1);

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    public void testEquals_GivenCategoryDefinitionsWithDifferentTerms() {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.setTerms(category1.getTerms() + " additional");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    public void testEquals_GivenCategoryDefinitionsWithDifferentRegex() {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.setRegex(category1.getRegex() + ".*additional.*");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    public void testEquals_GivenCategoryDefinitionsWithDifferentMaxMatchingLength() {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.setMaxMatchingLength(category1.getMaxMatchingLength() + 1);

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    public void testEquals_GivenCategoryDefinitionsWithDifferentExamples() {
        CategoryDefinition category1 = createFullyPopulatedCategoryDefinition();
        CategoryDefinition category2 = createFullyPopulatedCategoryDefinition();
        category2.addExample("additional");

        assertFalse(category1.equals(category2));
        assertFalse(category2.equals(category1));
    }

    public void testSerialise() throws IOException {
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

    private static CategoryDefinition createFullyPopulatedCategoryDefinition() {
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
