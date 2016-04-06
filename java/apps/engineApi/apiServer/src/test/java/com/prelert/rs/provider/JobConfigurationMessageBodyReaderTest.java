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
import static org.junit.Assert.assertNull;
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

import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.transform.Condition;
import com.prelert.job.transform.Operator;
import com.prelert.job.transform.TransformConfig;

public class JobConfigurationMessageBodyReaderTest
{
    @Test
    public void testIsReadable()
    {
        JobConfigurationMessageBodyReader reader = new JobConfigurationMessageBodyReader();

        assertTrue(reader.isReadable(JobConfiguration.class,
                                    mock(Type.class),
                                    new Annotation [] {},
                                    mock(MediaType.class)));

        assertFalse(reader.isReadable(JobDetails.class,
                                    mock(Type.class),
                                    new Annotation [] {},
                                    mock(MediaType.class)));
    }

    @Test(expected=WebApplicationException.class)
    public void testInvalidMediaType() throws IOException
    {
        JobConfigurationMessageBodyReader reader = new JobConfigurationMessageBodyReader();

        reader.readFrom(JobConfiguration.class, mock(Type.class), new Annotation [] {},
                        MediaType.APPLICATION_ATOM_XML_TYPE, new MultivaluedHashMap<String, String>(),
                        mock(InputStream.class));
    }

    @Test
    public void testReadConfigNoTransforms() throws IOException
    {
        final String FLIGHT_CENTRE_JOB_CONFIG = "{\"id\":\"flightcentre-csv\","
                + "\"description\":\"Flight Centre Job\","
                + "\"analysisConfig\" : {"
                + "\"bucketSpan\":3600,"
                + "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] "
                + "},"
                + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"_time\", \"timeFormat\" : \"epoch\"},"
                + "\"analysisLimits\": {\"modelMemoryLimit\":2000, \"categorizationExamplesLimit\":3}"
                + "}";


        JobConfigurationMessageBodyReader reader = new JobConfigurationMessageBodyReader();

        JobConfiguration config = reader.readFrom(JobConfiguration.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(FLIGHT_CENTRE_JOB_CONFIG.getBytes(StandardCharsets.UTF_8)));


        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");

        assertEquals("flightcentre-csv", config.getId());
        assertEquals("Flight Centre Job", config.getDescription());

        assertEquals(ac, config.getAnalysisConfig());
        assertEquals(dd, config.getDataDescription());

        AnalysisLimits al = new AnalysisLimits(2000, 3L);
        assertEquals(al, config.getAnalysisLimits());

        assertNull(config.getTransforms());
    }

    @Test
    public void testReadConfig() throws IOException
    {
        final String FLIGHT_CENTRE_JOB_CONFIG = "{\"id\":\"flightcentre-csv\","
                + "\"description\":\"Flight Centre Job\","
                + "\"analysisConfig\" : {"
                + "\"bucketSpan\":3600,"
                + "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] },"
                + "\"transforms\":[{\"transform\":\"a_function\", \"inputs\":\"field1\", \"outputs\":\"out_field\"}],"
                + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"_time\", \"timeFormat\" : \"epoch\"},"
                + "\"analysisLimits\": {\"modelMemoryLimit\":2000}"
                + "}";


        JobConfigurationMessageBodyReader reader = new JobConfigurationMessageBodyReader();

        JobConfiguration config = reader.readFrom(JobConfiguration.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(FLIGHT_CENTRE_JOB_CONFIG.getBytes("UTF-8")));


        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");

        assertEquals("flightcentre-csv", config.getId());
        assertEquals("Flight Centre Job", config.getDescription());

        assertEquals(ac, config.getAnalysisConfig());
        assertEquals(dd, config.getDataDescription());

        AnalysisLimits al = new AnalysisLimits(2000, null);
        assertEquals(al, config.getAnalysisLimits());

        TransformConfig tr = new TransformConfig();
        tr.setTransform("a_function");
        tr.setInputs(Arrays.asList("field1"));
        tr.setOutputs(Arrays.asList("out_field"));

        assertEquals(1, config.getTransforms().size());
        assertEquals(Arrays.asList(tr), config.getTransforms());
    }

    @Test
    public void testReadConfig_transformHasCondition() throws IOException
    {
        final String FLIGHT_CENTRE_JOB_CONFIG = "{\"id\":\"flightcentre-csv\","
                + "\"description\":\"Flight Centre Job\","
                + "\"analysisConfig\" : {"
                + "\"bucketSpan\":3600,"
                + "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}] },"
                + "\"transforms\":[{\"transform\":\"exclude\", \"inputs\":\"field1\", \"condition\":"
                    + "{\"operator\":\"match\", \"value\":\".*\"}}],"
                + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"_time\", \"timeFormat\" : \"epoch\"},"
                + "\"analysisLimits\": {\"modelMemoryLimit\":2000}"
                + "}";


        JobConfigurationMessageBodyReader reader = new JobConfigurationMessageBodyReader();

        JobConfiguration config = reader.readFrom(JobConfiguration.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(FLIGHT_CENTRE_JOB_CONFIG.getBytes("UTF-8")));


        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setTimeField("_time");

        assertEquals("flightcentre-csv", config.getId());
        assertEquals("Flight Centre Job", config.getDescription());

        assertEquals(ac, config.getAnalysisConfig());
        assertEquals(dd, config.getDataDescription());

        AnalysisLimits al = new AnalysisLimits(2000, null);
        assertEquals(al, config.getAnalysisLimits());

        TransformConfig tr = new TransformConfig();
        tr.setTransform("exclude");
        tr.setCondition(new Condition(Operator.MATCH, ".*"));
        tr.setInputs(Arrays.asList("field1"));

        assertEquals(1, config.getTransforms().size());
        assertEquals(Arrays.asList(tr), config.getTransforms());
    }

    @Test
    public void testReadConfigWithArrayOfTransformInputs() throws IOException
    {
        final String FARE_QUOTE_TIME_FORMAT_CONFIG = "{"
                + "\"description\":\"Farequote Time Format Job\","
                + "\"analysisConfig\" : {"
                + "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]},"
                + "\"transforms\":[{\"transform\":\"a_function\", \"inputs\":[\"field1\", \"field2\"], \"outputs\":[\"out_field\"]}],"
                + "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"time\", "
                + "\"timeFormat\":\"yyyy-MM-dd HH:mm:ssX\"} }}";


        JobConfigurationMessageBodyReader reader = new JobConfigurationMessageBodyReader();

        JobConfiguration config = reader.readFrom(JobConfiguration.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(FARE_QUOTE_TIME_FORMAT_CONFIG.getBytes("UTF-8")));


        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");
        dd.setTimeField("time");

        assertEquals("Farequote Time Format Job", config.getDescription());

        assertEquals(ac, config.getAnalysisConfig());
        assertEquals(dd, config.getDataDescription());

        assertNull(config.getAnalysisLimits());

        TransformConfig tr = new TransformConfig();
        tr.setTransform("a_function");
        tr.setInputs(Arrays.asList("field1", "field2"));
        tr.setOutputs(Arrays.asList("out_field"));

        assertEquals(1, config.getTransforms().size());
        assertEquals(Arrays.asList(tr), config.getTransforms());
    }

}
