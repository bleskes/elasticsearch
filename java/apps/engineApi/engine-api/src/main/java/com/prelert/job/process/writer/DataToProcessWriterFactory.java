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

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfigs;

/**
 * Factory for creating the suitable writer depending on
 * whether the data format is JSON or not, and on the kind
 * of date transformation that should occur.
 */
public class DataToProcessWriterFactory
{

    /**
     * Constructs a {@link DataToProcessWriter} depending on
     * the data format and the time transformation.
     *
     * @return A {@link JsonDataToProcessWriter} if the data
     *  format is JSON or otherwise a {@link CsvDataToProcessWriter}
     */
    public DataToProcessWriter create(boolean includeControlField, RecordWriter writer,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            SchedulerConfig schedulerConfig, TransformConfigs transforms,
            StatusReporter statusReporter, JobDataPersister jobDataPersister, Logger logger)
    {
        switch (dataDescription.getFormat())
        {
            case JSON:
            case ELASTICSEARCH:
                return new JsonDataToProcessWriter(includeControlField, writer, dataDescription, analysisConfig,
                        schedulerConfig, transforms, statusReporter, jobDataPersister, logger);
            case DELIMITED:
                return new CsvDataToProcessWriter(includeControlField, writer, dataDescription, analysisConfig,
                        transforms, statusReporter, jobDataPersister, logger);
            case SINGLE_LINE:
                return new SingleLineDataToProcessWriter(includeControlField, writer, dataDescription, analysisConfig,
                        transforms, statusReporter, jobDataPersister, logger);
            default:
                throw new IllegalArgumentException();
        }
    }
}
