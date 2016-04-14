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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

public class ElasticsearchDotNotationReverserTest
{
    @Test
    public void testResultsMap() throws JsonProcessingException
    {
        ElasticsearchDotNotationReverser reverser = createReverser();

        String expected = "{\"complex\":{\"nested\":{\"structure\":{\"first\":\"x\"," +
                "\"second\":\"y\"},\"value\":\"z\"}},\"cpu\":{\"system\":\"5\"," +
                "\"user\":\"10\",\"wait\":\"1\"},\"simple\":\"simon\"}";

        String actual = new ObjectMapper().writeValueAsString(reverser.getResultsMap());
        assertEquals(expected, actual);
    }

    @Test
    public void testMappingsMap() throws JsonProcessingException
    {
        ElasticsearchDotNotationReverser reverser = createReverser();

        String expected = "{\"complex\":{\"properties\":{\"nested\":{\"properties\":" +
                "{\"structure\":{\"properties\":{\"first\":{\"type\":\"string\"}," +
                "\"second\":{\"type\":\"string\"}},\"type\":\"object\"}," +
                "\"value\":{\"type\":\"string\"}},\"type\":\"object\"}}," +
                "\"type\":\"object\"},\"cpu\":{\"properties\":{\"system\":" +
                "{\"type\":\"string\"},\"user\":{\"type\":\"string\"}," +
                "\"wait\":{\"type\":\"string\"}},\"type\":\"object\"}," +
                "\"simple\":{\"type\":\"string\"}}";

        String actual = new ObjectMapper().writeValueAsString(reverser.getMappingsMap());
        assertEquals(expected, actual);
    }

    private ElasticsearchDotNotationReverser createReverser()
    {
        ElasticsearchDotNotationReverser reverser = new ElasticsearchDotNotationReverser();
        // This should get ignored as it's a reserved field name
        reverser.add("bucketSpan", "3600");
        reverser.add("simple", "simon");
        reverser.add("cpu.user", "10");
        reverser.add("cpu.system", "5");
        reverser.add("cpu.wait", "1");
        // This should get ignored as one of its segments is a reserved field name
        reverser.add("foo.bucketSpan", "3600");
        reverser.add("complex.nested.structure.first", "x");
        reverser.add("complex.nested.structure.second", "y");
        reverser.add("complex.nested.value", "z");
        return reverser;
    }
}
