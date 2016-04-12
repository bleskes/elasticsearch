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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.rs.provider.JobConfigurationParseException;

public class DetectorMessageBodyReaderTest
{
    @Test
    public void testIsReadable()
    {
        DetectorMessageBodyReader reader = new DetectorMessageBodyReader();

        assertFalse(reader.isReadable(JobConfiguration.class,
                                    mock(Type.class),
                                    new Annotation [] {},
                                    mock(MediaType.class)));

        assertTrue(reader.isReadable(Detector.class,
                                    mock(Type.class),
                                    new Annotation [] {},
                                    mock(MediaType.class)));
    }

    @Test(expected=WebApplicationException.class)
    public void testInvalidMediaType() throws IOException
    {
        DetectorMessageBodyReader reader = new DetectorMessageBodyReader();

        reader.readFrom(Detector.class, mock(Type.class), new Annotation [] {},
                        MediaType.APPLICATION_ATOM_XML_TYPE, new MultivaluedHashMap<String, String>(),
                        mock(InputStream.class));
    }

    @Test
    public void testReadDetector() throws IOException
    {
        final String FLIGHT_CENTRE_DETECTOR =
                "{\"function\":\"max\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}";

        DetectorMessageBodyReader reader = new DetectorMessageBodyReader();

        Detector detector = reader.readFrom(Detector.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(FLIGHT_CENTRE_DETECTOR.getBytes(StandardCharsets.UTF_8)));

        Detector expected = new Detector();
        expected.setFunction("max");
        expected.setFieldName("responsetime");
        expected.setByFieldName("airline");

        assertEquals(expected, detector);
    }

    @Test(expected=JobConfigurationParseException.class)
    public void testReadDetector_GivenTrailingJunk() throws IOException
    {
        final String FLIGHT_CENTRE_DETECTOR_PLUS_JUNK =
                "{\"function\":\"max\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}junk";

        DetectorMessageBodyReader reader = new DetectorMessageBodyReader();

        reader.readFrom(Detector.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(FLIGHT_CENTRE_DETECTOR_PLUS_JUNK.getBytes(StandardCharsets.UTF_8)));
    }
}
