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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

import com.prelert.job.JobConfiguration;
import com.prelert.job.transform.TransformConfig;

public class TransformConfigMessageBodyReaderTest
{

    @Test
    public void testIsReadable()
    {
        TransformConfigMessageBodyReader reader = new TransformConfigMessageBodyReader();

        assertFalse(reader.isReadable(JobConfiguration.class,
                                    mock(Type.class),
                                    new Annotation [] {},
                                    mock(MediaType.class)));

        assertTrue(reader.isReadable(TransformConfig.class,
                                    mock(Type.class),
                                    new Annotation [] {},
                                    mock(MediaType.class)));
    }

    @Test(expected=WebApplicationException.class)
    public void testInvalidMediaType() throws IOException
    {
        TransformConfigMessageBodyReader reader = new TransformConfigMessageBodyReader();

        reader.readFrom(TransformConfig.class, mock(Type.class), new Annotation [] {},
                        MediaType.APPLICATION_ATOM_XML_TYPE, new MultivaluedHashMap<String, String>(),
                        mock(InputStream.class));
    }

    @Test
    public void testReadTransformConfig() throws IOException
    {
        final String DATE_TIME_CONCAT =
                "{\"transform\":\"concat\",\"inputs\":[\"date\",\"time\"],\"outputs\":\"datetime\"}";

        TransformConfigMessageBodyReader reader = new TransformConfigMessageBodyReader();

        TransformConfig transformConfig = reader.readFrom(TransformConfig.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(DATE_TIME_CONCAT.getBytes(StandardCharsets.UTF_8)));

        TransformConfig expected = new TransformConfig();
        expected.setTransform("concat");
        expected.setInputs(Arrays.asList("date", "time"));
        expected.setOutputs(Arrays.asList("datetime"));

        assertEquals(expected, transformConfig);
    }

}
