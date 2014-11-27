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

import java.io.IOException;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.dateparsing.CannotParseTimestampException;
import com.prelert.job.process.dateparsing.DateTransformer;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;

abstract class AbstractDataToProcessWriter implements DataToProcessWriter
{
    protected final LengthEncodedWriter m_LengthEncodedWriter;
    protected final DataDescription m_DataDescription;
    protected final AnalysisConfig m_AnalysisConfig;
    protected final StatusReporter m_StatusReporter;
    protected final JobDataPersister m_JobDataPersister;
    protected final Logger m_Logger;
    protected final DateTransformer m_DateTransformer;

    protected AbstractDataToProcessWriter(LengthEncodedWriter lengthEncodedWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            StatusReporter statusReporter, JobDataPersister jobDataPersister, Logger logger,
            DateTransformer dateTransformer)
    {
        m_LengthEncodedWriter = Objects.requireNonNull(lengthEncodedWriter);
        m_DataDescription = Objects.requireNonNull(dataDescription);
        m_AnalysisConfig = Objects.requireNonNull(analysisConfig);
        m_StatusReporter = Objects.requireNonNull(statusReporter);
        m_JobDataPersister = Objects.requireNonNull(jobDataPersister);
        m_Logger = Objects.requireNonNull(logger);
        m_DateTransformer = Objects.requireNonNull(dateTransformer);
    }

    protected Long transformTimeAndWrite(String[] record, int timeFieldIndex, long lastEpoch,
            long inputFieldCount) throws IOException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        long epoch = 0L;
        try
        {
            epoch = m_DateTransformer.transform(record[timeFieldIndex]);
        }
        catch (CannotParseTimestampException e)
        {
            m_StatusReporter.reportDateParseError(inputFieldCount);
            m_Logger.error(e.getMessage());
            return null;
        }

        if (epoch < lastEpoch)
        {
            // out of order
            m_StatusReporter.reportOutOfOrderRecord(inputFieldCount);
        }
        else
        {   // write record
            record[timeFieldIndex] = Long.toString(epoch);
            m_LengthEncodedWriter.writeRecord(record);
            m_JobDataPersister.persistRecord(epoch, record);
            m_StatusReporter.reportRecordWritten(inputFieldCount);
        }
        return epoch;
    }
}
