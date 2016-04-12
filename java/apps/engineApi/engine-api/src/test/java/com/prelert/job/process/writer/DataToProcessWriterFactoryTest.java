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

package com.prelert.job.process.writer;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfigs;

@RunWith(MockitoJUnitRunner.class)
public class DataToProcessWriterFactoryTest
{
    @Test
    public void testCreate_GivenDataFormatIsJson()
    {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.JSON);

        assertTrue(createWriter(dataDescription) instanceof JsonDataToProcessWriter);
    }

    @Test
    public void testCreate_GivenDataFormatIsElasticsearch()
    {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.ELASTICSEARCH);

        assertTrue(createWriter(dataDescription) instanceof JsonDataToProcessWriter);
    }

    @Test
    public void testCreate_GivenDataFormatIsCsv()
    {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.DELIMITED);

        assertTrue(createWriter(dataDescription) instanceof CsvDataToProcessWriter);
    }

    @Test
    public void testCreate_GivenDataFormatIsSingleLine()
    {
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataFormat.SINGLE_LINE);

        assertTrue(createWriter(dataDescription) instanceof SingleLineDataToProcessWriter);
    }

    private static DataToProcessWriter createWriter(DataDescription dataDescription)
    {
        DataToProcessWriterFactory factory = new DataToProcessWriterFactory();
        return factory.create(true, mock(LengthEncodedWriter.class), dataDescription,
                mock(AnalysisConfig.class), mock(SchedulerConfig.class), mock(TransformConfigs.class),
                mock(StatusReporter.class), mock(JobDataPersister.class), mock(Logger.class));
    }
}
