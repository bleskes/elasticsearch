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

package com.prelert.job.persistence.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Before;
import org.junit.Test;

import com.prelert.job.persistence.serialisation.StorageSerialisable;

public class ElasticsearchStorageSerialiserTest
{
    private XContentBuilder m_Builder;
    private ElasticsearchStorageSerialiser m_Serialiser;

    @Before
    public void setUp() throws IOException
    {
        m_Builder = XContentFactory.jsonBuilder();
        m_Serialiser = new ElasticsearchStorageSerialiser(m_Builder);
    }

    @Test
    public void testAdd() throws IOException
    {
        m_Serialiser.startObject();
        m_Serialiser.add("aBool", true);
        m_Serialiser.add("aDouble", 3.14);
        m_Serialiser.add("anInt", 18);
        m_Serialiser.add("aLong", 1234567891234L);
        m_Serialiser.add("aDate", new Date(1455753600000L));
        m_Serialiser.add("doubles", 1.1, 2.2);
        m_Serialiser.add("strings", "a", "b");
        m_Serialiser.addTimestamp(new Date(1455757200000L));
        m_Serialiser.add("nestedList", Arrays.asList(
                createSingleKeyValueSerialisable("a", "a_value"),
                createSingleKeyValueSerialisable("b", "b_value")));
        Map<String, Object> map = new HashMap<>();
        map.put("map_a", "map_a_value");
        m_Serialiser.add("aMap", map);
        m_Serialiser.startList("anEmptyList");
        m_Serialiser.endList();
        m_Serialiser.startObject("nestedObj");
        m_Serialiser.add("nested_key", "nested_value");
        m_Serialiser.endObject();
        m_Serialiser.endObject();

        String expected = "{"
                + "\"aBool\":true,"
                + "\"aDouble\":3.14,"
                + "\"anInt\":18,"
                + "\"aLong\":1234567891234,"
                + "\"aDate\":\"2016-02-18T00:00:00.000Z\","
                + "\"doubles\":[1.1,2.2],"
                + "\"strings\":[\"a\",\"b\"],"
                + "\"@timestamp\":\"2016-02-18T01:00:00.000Z\","
                + "\"nestedList\":["
                +   "{\"a\":\"a_value\"},"
                +   "{\"b\":\"b_value\"}"
                + "],"
                + "\"aMap\":{\"map_a\":\"map_a_value\"},"
                + "\"anEmptyList\":[],"
                + "\"nestedObj\":{\"nested_key\":\"nested_value\"}"
                + "}";
        assertEquals(expected, m_Builder.string());
    }

    @Test
    public void testSerialise() throws IOException
    {
        m_Serialiser.startObject();
        m_Serialiser.serialise(createSingleKeyValueSerialisable("a", "a_value"));
        m_Serialiser.endObject();

        String expected = "{\"a\":\"a_value\"}";
        assertEquals(expected, m_Builder.string());
    }

    @Test
    public void testNewDotNotationReverser()
    {
        assertTrue(m_Serialiser.newDotNotationReverser() instanceof ElasticsearchDotNotationReverser);
    }

    private static StorageSerialisable createSingleKeyValueSerialisable(String key, String value)
    {
        return s -> s.add(key, value);
    }
}
