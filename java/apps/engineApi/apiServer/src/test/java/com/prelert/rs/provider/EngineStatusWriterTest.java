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

package com.prelert.rs.provider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.rs.data.EngineStatus;

public class EngineStatusWriterTest
{
    @Test
    public void testIsWritable()
    {
        EngineStatusWriter writer = new EngineStatusWriter();

        assertFalse(writer.isWriteable(String.class, mock(Type.class), null, null));
        assertTrue(writer.isWriteable(
                EngineStatus.class, mock(ParameterizedType.class), null, null));
    }

    @Test
    public void testSerialise() throws WebApplicationException, IOException
    {
        EngineStatus status = new EngineStatus();
        status.setAverageCpuLoad(5.0);
        status.setActiveJobs(Arrays.asList("Job_1"));

        EngineStatusWriter writer = new EngineStatusWriter();


        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writer.writeTo(status, EngineStatus.class, mock(Type.class),
                new Annotation[] {}, MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<String, Object>(),
                output);

        String content = new String(output.toByteArray(), StandardCharsets.UTF_8);

        ObjectMapper jsonMapper = new ObjectMapper();
        EngineStatus out = jsonMapper.readValue(content, new TypeReference<EngineStatus>() {} );

        assertEquals(5.0, out.getAverageCpuLoad(), 0.00001);
        assertEquals(1,  out.getActiveJobCount());
        assertEquals(1,  out.getActiveJobs().size());
        assertEquals("Job_1", out.getActiveJobs().get(0));
    }
}
