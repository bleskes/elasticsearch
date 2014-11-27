/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.dateparsing.DateFormatDateTransformer;
import com.prelert.job.process.dateparsing.DateTransformer;
import com.prelert.job.process.dateparsing.DoubleDateTransformer;
import com.prelert.job.status.StatusReporter;

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
    public DataToProcessWriter create(OutputStream outputStream,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            StatusReporter statusReporter, JobDataPersister jobDataPersister, Logger logger)
    {
        // Don't close the output stream as it causes the autodetect
        // process to quit
        LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(outputStream);

        DateTransformer dateTransformer = new DoubleDateTransformer(false);
        if (dataDescription.isTransformTime())
        {
            dateTransformer = dataDescription.isEpochMs() ? new DoubleDateTransformer(true) :
                new DateFormatDateTransformer(dataDescription.getTimeFormat());
        }

        return (dataDescription.getFormat() == DataFormat.JSON) ?
                new JsonDataToProcessWriter(lengthEncodedWriter, dataDescription, analysisConfig,
                        statusReporter, jobDataPersister, logger, dateTransformer) :
                new CsvDataToProcessWriter(lengthEncodedWriter, dataDescription, analysisConfig,
                        statusReporter, jobDataPersister, logger, dateTransformer);
    }
}
