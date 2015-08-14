/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.rs.provider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.DataCounts;

public class DataCountsWriterTest
{
    @Test
    public void testIsWritable()
    {
        DataCountsWriter writer = new DataCountsWriter();

        assertFalse(writer.isWriteable(String.class, mock(Type.class), null, null));
        assertTrue(writer.isWriteable(
                DataCounts.class, mock(ParameterizedType.class), null, null));
    }

    @Test
    public void testSerialise() throws WebApplicationException, IOException
    {
        DataCounts counts = createCounts();
        DataCountsWriter writer = new DataCountsWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writer.writeTo(counts, DataCounts.class, mock(Type.class),
                new Annotation[] {}, MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<String, Object>(),
                output);

        String content = new String(output.toByteArray());

        ObjectMapper jsonMapper = new ObjectMapper();
        DataCounts out = jsonMapper.readValue(content, new TypeReference<DataCounts>() {} );

        assertEquals(counts, out);
    }

    @Test
    public void testCalculatedFieldsAreSerialised() throws WebApplicationException, IOException
    {
        DataCounts counts = createCounts();
        DataCountsWriter writer = new DataCountsWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writer.writeTo(counts, DataCounts.class, mock(Type.class),
                new Annotation[] {}, MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<String, Object>(),
                output);

        String content = new String(output.toByteArray());

        // check this calculated fields are serialised
        assertTrue(content.contains("inputRecordCount"));
        assertTrue(content.contains("processedFieldCount"));

        // this field should not be serialised
        assertFalse(content.contains("analysedFieldsPerRecord"));
    }

    private DataCounts createCounts()
    {
        DataCounts count = new DataCounts();
        count.setBucketCount(20L);
        count.setFailedTransformCount(2);
        count.setInputBytes(100);
        count.setInputFieldCount(21);
        count.setInvalidDateCount(3);
        count.setMissingFieldCount(1);
        count.setOutOfOrderTimeStampCount(4);
        count.setProcessedRecordCount(6);

        return count;
    }
}
