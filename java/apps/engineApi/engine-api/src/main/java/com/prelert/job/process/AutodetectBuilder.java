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
package com.prelert.job.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisLimits;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.process.writer.AnalysisLimitsWriter;
import com.prelert.job.process.writer.FieldConfigWriter;
import com.prelert.job.process.writer.ModelDebugConfigWriter;
import com.prelert.job.quantiles.Quantiles;

/**
 * The autodetect process builder.
 */
public class AutodetectBuilder
{
    private static final String CONF_EXTENSION = ".conf";
    private static final String LIMIT_CONFIG_ARG = "--limitconfig=";
    private static final String MODEL_DEBUG_CONFIG_ARG = "--modeldebugconfig=";
    private static final String FIELD_CONFIG_ARG = "--fieldconfig=";

    private JobDetails m_Job;
    private List<File> m_FilesToDelete;
    private Logger m_Logger;
    private boolean m_IgnoreDowntime;
    private Optional<Quantiles> m_Quantiles;
    private Optional<ModelSnapshot> m_ModelSnapshot;

    /**
     * Constructs an autodetect process builder
     *
     * @param job The job configuration
     * @param filesToDelete This method will append File objects that need to be
     * deleted when the process completes
     * @param logger The job's logger
     */
    public AutodetectBuilder(JobDetails job, List<File> filesToDelete, Logger logger)
    {
        m_Job = Objects.requireNonNull(job);
        m_FilesToDelete = Objects.requireNonNull(filesToDelete);
        m_Logger = Objects.requireNonNull(logger);
        m_IgnoreDowntime = false;
        m_Quantiles = Optional.empty();
        m_ModelSnapshot = Optional.empty();
    }

    /**
     * Set ignoreDowntime
     *
     * @param ignoreDowntime If true set the ignore downtime flag overriding the
     * setting in the job configuration
     */
    public AutodetectBuilder ignoreDowntime(boolean ignoreDowntime)
    {
        m_IgnoreDowntime = ignoreDowntime;
        return this;
    }

    /**
     * Set quantiles to restore the normaliser state if any.
     * @param quantiles the non-null quantiles
     */
    public AutodetectBuilder quantiles(Quantiles quantiles)
    {
        m_Quantiles = Optional.of(Objects.requireNonNull(quantiles));
        return this;
    }

    /**
     * Set model snapshot to restore on startup if required
     * @param modelSnapshot The non-null model snapshot to restore on startup
     */
    public AutodetectBuilder modelSnapshot(ModelSnapshot modelSnapshot)
    {
        m_ModelSnapshot = Optional.of(Objects.requireNonNull(modelSnapshot));
        return this;
    }

    /**
     * Sets the environment variables PRELERT_HOME and LIB_PATH (or platform
     * variants) and starts the process in that environment. Any inherited value
     * of LIB_PATH or PRELERT_HOME is overwritten.
     * <code>processName</code> is not the full path it is the relative path of the
     * program from the PRELERT_HOME/bin directory.
     *
     * @return A Java Process object
     * @throws IOException
     */
    public Process build() throws IOException
    {
        m_Logger.info("PRELERT_HOME is set to " + ProcessCtrl.PRELERT_HOME);

        String restoreSnapshotId = m_ModelSnapshot.isPresent() ? m_ModelSnapshot.get().getSnapshotId() : null;
        List<String> command = ProcessCtrl.buildAutoDetectCommand(m_Job, m_Logger,
                restoreSnapshotId, m_IgnoreDowntime);

        buildLimits(command);

        if (m_Job.getModelDebugConfig() != null && m_Job.getModelDebugConfig().isEnabled())
        {
            File modelDebugConfigFile = File.createTempFile("modeldebugconfig", CONF_EXTENSION);
            m_FilesToDelete.add(modelDebugConfigFile);
            writeModelDebugConfig(m_Job.getModelDebugConfig(), modelDebugConfigFile);
            String modelDebugConfig = MODEL_DEBUG_CONFIG_ARG + modelDebugConfigFile.getPath();
            command.add(modelDebugConfig);
        }

        if (ProcessCtrl.modelConfigFilePresent())
        {
            String modelConfigFile = new File(ProcessCtrl.CONFIG_DIR, ProcessCtrl.PRELERT_MODEL_CONF).getPath();
            command.add(ProcessCtrl.MODEL_CONFIG_ARG + modelConfigFile);
        }

        // Restoring the quantiles
        if (m_Quantiles.isPresent() && !m_Quantiles.get().getQuantileState().isEmpty())
        {
            Quantiles quantiles = m_Quantiles.get();
            m_Logger.info("Restoring quantiles for job '" + m_Job.getId() + "'");

            Path normalisersStateFilePath = ProcessCtrl.writeNormaliserInitState(
                    m_Job.getId(), quantiles.getQuantileState());

            String quantilesStateFileArg = ProcessCtrl.QUANTILES_STATE_PATH_ARG + normalisersStateFilePath;
            command.add(quantilesStateFileArg);
            command.add(ProcessCtrl.DELETE_STATE_FILES_ARG);
        }

        // now the actual field args
        if (m_Job.getAnalysisConfig() != null)
        {
            // write to a temporary field config file
            File fieldConfigFile = File.createTempFile("fieldconfig", CONF_EXTENSION);
            m_FilesToDelete.add(fieldConfigFile);
            try (OutputStreamWriter osw = new OutputStreamWriter(
                    new FileOutputStream(fieldConfigFile),
                    StandardCharsets.UTF_8))
            {
                new FieldConfigWriter(m_Job.getAnalysisConfig(), osw, m_Logger).write();
            }

            String fieldConfig = FIELD_CONFIG_ARG + fieldConfigFile.getPath();
            command.add(fieldConfig);
        }

        // Build the process
        m_Logger.info("Starting autodetect process with command: " +  command);
        ProcessBuilder pb = new ProcessBuilder(command);
        ProcessCtrl.buildEnvironment(pb);

        return pb.start();
    }

    private void buildLimits(List<String> command) throws IOException
    {
        if (m_Job.getAnalysisLimits() != null)
        {
            File limitConfigFile = File.createTempFile("limitconfig", CONF_EXTENSION);
            m_FilesToDelete.add(limitConfigFile);
            writeLimits(m_Job.getAnalysisLimits(), limitConfigFile);
            String limits = LIMIT_CONFIG_ARG + limitConfigFile.getPath();
            command.add(limits);
        }
    }

    /**
     * Write the Prelert autodetect model options to <code>emptyConfFile</code>.
     *
     * @param emptyConfFile
     * @throws IOException
     */
    private static void writeLimits(AnalysisLimits options, File emptyConfFile) throws IOException
    {
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(emptyConfFile),
                StandardCharsets.UTF_8))
        {
            new AnalysisLimitsWriter(options, osw).write();
        }
    }

    private static void writeModelDebugConfig(ModelDebugConfig config, File emptyConfFile)
            throws IOException
    {
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(emptyConfFile),
                StandardCharsets.UTF_8))
        {
            new ModelDebugConfigWriter(config, osw).write();
        }
    }
}
