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

package com.prelert.job.process.autodetect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
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

    public ProcessFactory(JobProvider jobProvider, ResultsReaderFactory resultsReaderFactory,
            JobDataCountsPersisterFactory dataCountsPersisterFactory,
            UsagePersisterFactory usagePersisterFactory)
    {
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_ResultsReaderFactory = Objects.requireNonNull(resultsReaderFactory);
        m_DataCountsPersisterFactory = Objects.requireNonNull(dataCountsPersisterFactory);
        m_UsagePersisterFactory = Objects.requireNonNull(usagePersisterFactory);
    }

    /**
     * Create a new autodetect process restoring its state if persisted
     *
     * @param jobId
     * @return
     * @throws UnknownJobException If there is no job with <code>jobId</code>
     * @throws NativeProcessRunException
     */
    public ProcessAndDataDescription createProcess(String jobId) throws UnknownJobException,
            NativeProcessRunException
    {
        Optional<JobDetails> job = m_JobProvider.getJobDetails(jobId);

        if (!job.isPresent())
        {
            throw new UnknownJobException(jobId);
        }

        return createProcess(job.get(), true);
    }

    /**
     * Create a new autodetect process from the JobDetails restoring
     * its state if <code>restoreState</code> is true.
     *
     * @param job
     * @param restoreState Will attempt to restore the state but it isn't an
     * error if there is no state to restore
     * @return
     * @throws UnknownJobException If there is no job with <code>jobId</code>
     * @throws NativeProcessRunException If an error is encountered creating
     * the native process
     */
    private ProcessAndDataDescription createProcess(JobDetails job, boolean restoreState)
            throws UnknownJobException, NativeProcessRunException
    {
        String jobId = job.getId();

        Logger logger = JobLogger.create(job.getId());

        Quantiles quantiles = null;
        if (restoreState)
        {
            quantiles = m_JobProvider.getQuantiles(jobId);
        }

        Process nativeProcess = null;
        List<File> filesToDelete = new ArrayList<>();
        try
        {
            // if state is null or empty it will be ignored
            // else it is used to restore the quantiles
            nativeProcess = ProcessCtrl.buildAutoDetect(job, quantiles, logger, filesToDelete);
        }
        catch (IOException e)
        {
            String msg = "Failed to launch process for job " + job.getId();
            LOGGER.error(msg);
            logger.error(msg, e);
            throw new NativeProcessRunException(msg,
                    ErrorCodes.NATIVE_PROCESS_START_ERROR, e);
        }


        ProcessAndDataDescription procAndDD = new ProcessAndDataDescription(
                nativeProcess, jobId,
                job.getDataDescription(), job.getTimeout(), job.getAnalysisConfig(),
                new TransformConfigs(job.getTransforms()), logger,
                new StatusReporter(jobId, job.getCounts(),
                        new UsageReporter(jobId,
                                          m_UsagePersisterFactory.getInstance(logger),
                                          logger),
                         m_DataCountsPersisterFactory.getInstance(logger),
                         logger),
                m_ResultsReaderFactory.newResultsParser(jobId,
                        nativeProcess.getInputStream(),
                        logger),
                filesToDelete
                );

        m_JobProvider.setJobStatus(jobId, JobStatus.RUNNING);

        logger.debug("Created process for job " + jobId);

        return procAndDD;
    }
}
