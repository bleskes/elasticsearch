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

package com.prelert.job.process.autodetect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.persistence.JobDataCountsPersisterFactory;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.UsagePersisterFactory;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.output.parsing.ResultsReaderFactory;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.job.usage.UsageReporter;

public class ProcessFactory
{
    private static final Logger LOGGER = Logger.getLogger(ProcessFactory.class);

    private final JobProvider m_JobProvider;
    private final ResultsReaderFactory m_ResultsReaderFactory;
    private final JobDataCountsPersisterFactory m_DataCountsPersisterFactory;
    private final UsagePersisterFactory m_UsagePersisterFactory;
    private final JobLoggerFactory m_JobLoggerFactory;



    public ProcessFactory(JobProvider jobProvider, ResultsReaderFactory resultsReaderFactory,
            JobDataCountsPersisterFactory dataCountsPersisterFactory,
            UsagePersisterFactory usagePersisterFactory, JobLoggerFactory jobLoggerFactory)
    {
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_ResultsReaderFactory = Objects.requireNonNull(resultsReaderFactory);
        m_DataCountsPersisterFactory = Objects.requireNonNull(dataCountsPersisterFactory);
        m_UsagePersisterFactory = Objects.requireNonNull(usagePersisterFactory);
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
    }

    /**
     * Create a new autodetect process from the JobDetails restoring
     * its state.
     *
     * @param job
     * @param ignoreDowntime
     * @return
     * @throws NativeProcessRunException If an error is encountered creating
     * the native process
     */
    public ProcessAndDataDescription createProcess(JobDetails job, boolean ignoreDowntime)
            throws UnknownJobException, NativeProcessRunException
    {
        String jobId = job.getId();
        Logger logger = m_JobLoggerFactory.newLogger(job.getId());
        Quantiles quantiles = m_JobProvider.getQuantiles(jobId);
        List<ModelSnapshot> modelSnapshots = m_JobProvider.modelSnapshots(jobId, 0, 1).queryResults();
        ModelSnapshot modelSnapshot = (modelSnapshots == null || modelSnapshots.isEmpty()) ? null : modelSnapshots.get(0);

        Process nativeProcess = null;
        List<File> filesToDelete = new ArrayList<>();
        try
        {
            // if state is null or empty it will be ignored
            // else it is used to restore the quantiles
            nativeProcess = ProcessCtrl.buildAutoDetect(job, quantiles, logger,
                    filesToDelete, modelSnapshot, ignoreDowntime);
        }
        catch (IOException e)
        {
            String msg = "Failed to launch process for job " + job.getId();
            LOGGER.error(msg);
            logger.error(msg, e);
            throw new NativeProcessRunException(msg,
                    ErrorCodes.NATIVE_PROCESS_START_ERROR, e);
        }


        StatusReporter sr = new StatusReporter(jobId, job.getCounts(),
                new UsageReporter(jobId,
                                  m_UsagePersisterFactory.getInstance(logger),
                                  logger),
                 m_DataCountsPersisterFactory.getInstance(logger),
                 logger, job.getAnalysisConfig().getBucketSpanOrDefault());

        ProcessAndDataDescription procAndDD = new ProcessAndDataDescription(
                nativeProcess, jobId,
                job.getDataDescription(), job.getAnalysisConfig(),
                job.getSchedulerConfig(), new TransformConfigs(job.getTransforms()), logger,
                sr,
                m_ResultsReaderFactory.newResultsParser(jobId,
                                        nativeProcess.getInputStream(), logger),
                filesToDelete
                );

        m_JobProvider.setJobStatus(jobId, JobStatus.RUNNING);

        logger.debug("Created process for job " + jobId);

        return procAndDD;
    }
}
