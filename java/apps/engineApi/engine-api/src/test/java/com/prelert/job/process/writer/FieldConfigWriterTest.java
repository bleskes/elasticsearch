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

package com.prelert.job.process.writer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.Test;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;

public class FieldConfigWriterTest
{
    @Test
    public void testMultipleDetectorsToConfFile()
    throws IOException
    {
        List<Detector> detectors = new ArrayList<>();

        Detector d = new Detector();
        d.setFieldName("Integer_Value");
        d.setByFieldName("ts_hash");
        detectors.add(d);
        Detector d2 = new Detector();
        d2.setFunction("count");
        d2.setByFieldName("ipaddress");
        detectors.add(d2);
        Detector d3 = new Detector();
        d3.setFunction("max");
        d3.setFieldName("Integer_Value");
        d3.setOverFieldName("ts_hash");
        detectors.add(d3);
        Detector d4 = new Detector();
        d4.setFunction("rare");
        d4.setByFieldName("ipaddress");
        d4.setPartitionFieldName("host");
        detectors.add(d4);

        AnalysisConfig config = new AnalysisConfig();
        config.setDetectors(detectors);

        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        try (OutputStreamWriter osw = new OutputStreamWriter(ba, StandardCharsets.UTF_8))
        {
            BasicConfigurator.configure();
            Logger logger = Logger.getLogger(FieldConfigWriterTest.class);

            new FieldConfigWriter(config, osw, logger).write();
        }

        // read the ini file - all the settings are in the global section
        StringReader reader = new StringReader(ba.toString("UTF-8"));

        Config iniConfig = new Config();
        iniConfig.setLineSeparator(new String(new char [] {WriterConstants.NEW_LINE}));
        iniConfig.setGlobalSection(true);

        Ini fieldConfig = new Ini();
        fieldConfig.setConfig(iniConfig);
        fieldConfig.load(reader);

        Section section = fieldConfig.get(iniConfig.getGlobalSectionName());

        Assert.assertEquals(detectors.size(), section.size());

        String value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.1.clause");
        Assert.assertEquals("Integer_Value by ts_hash", value);
        value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.2.clause");
        Assert.assertEquals("count by ipaddress", value);
        value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.3.clause");
        Assert.assertEquals("max(Integer_Value) over ts_hash", value);
        value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.4.clause");
        Assert.assertEquals("rare by ipaddress partitionfield=host", value);
    }

    @Test
    public void testWrite_GivenConfigHasCategorizationField() throws IOException
    {
        Detector d = new Detector();
        d.setFieldName("Integer_Value");
        d.setByFieldName("ts_hash");

        AnalysisConfig config = new AnalysisConfig();
        config.setDetectors(Arrays.asList(d));
        config.setCategorizationFieldName("foo");
        OutputStreamWriter writer = mock(OutputStreamWriter.class);
        Logger logger = mock(Logger.class);
        FieldConfigWriter fieldConfigWriter = new FieldConfigWriter(config, writer, logger);

        fieldConfigWriter.write();

        verify(writer).write("detector.1.clause = Integer_Value by ts_hash categorizationfield=foo\n");
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void testWrite_GivenConfigHasInfluencers() throws IOException
    {
        Detector d = new Detector();
        d.setFieldName("Integer_Value");
        d.setByFieldName("ts_hash");

        AnalysisConfig config = new AnalysisConfig();
        config.setDetectors(Arrays.asList(d));
        config.setInfluencers(Arrays.asList("sun", "moon", "earth"));

        OutputStreamWriter writer = mock(OutputStreamWriter.class);
        Logger logger = mock(Logger.class);
        FieldConfigWriter fieldConfigWriter = new FieldConfigWriter(config, writer, logger);

        fieldConfigWriter.write();

        verify(writer).write("detector.1.clause = Integer_Value by ts_hash\n" +
                "influencer.1 = sun\n" +
                "influencer.2 = moon\n" +
                "influencer.3 = earth\n");
        verifyNoMoreInteractions(writer);
    }
}
