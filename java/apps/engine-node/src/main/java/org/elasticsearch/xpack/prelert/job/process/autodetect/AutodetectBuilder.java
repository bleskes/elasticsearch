/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.process.ProcessCtrl;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.AnalysisLimitsWriter;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.FieldConfigWriter;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.ModelDebugConfigWriter;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.lists.ListDocument;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The autodetect process builder.
 */
public class AutodetectBuilder {
    private static final String CONF_EXTENSION = ".conf";
    private static final String LIMIT_CONFIG_ARG = "--limitconfig=";
    private static final String MODEL_DEBUG_CONFIG_ARG = "--modeldebugconfig=";
    private static final String FIELD_CONFIG_ARG = "--fieldconfig=";

    private JobDetails jobDetails;
    private List<Path> filesToDelete;
    private Logger logger;
    private boolean ignoreDowntime;
    private Set<ListDocument> referencedLists;
    private Optional<Quantiles> quantiles;
    private Optional<ModelSnapshot> modelSnapshot;
    private Environment env;
    private Settings settings;

    /**
     * Constructs an autodetect process builder
     *
     * @param job           The job configuration
     * @param filesToDelete This method will append File objects that need to be
     *                      deleted when the process completes
     * @param logger        The job's logger
     */
    public AutodetectBuilder(JobDetails job, List<Path> filesToDelete, Logger logger, Environment env, Settings settings) {
        this.env = env;
        this.settings = settings;
        jobDetails = Objects.requireNonNull(job);
        this.filesToDelete = Objects.requireNonNull(filesToDelete);
        this.logger = Objects.requireNonNull(logger);
        ignoreDowntime = false;
        referencedLists = new HashSet<>();
        quantiles = Optional.empty();
        modelSnapshot = Optional.empty();
    }

    /**
     * Set ignoreDowntime
     *
     * @param ignoreDowntime If true set the ignore downtime flag overriding the
     *                       setting in the job configuration
     */
    public AutodetectBuilder ignoreDowntime(boolean ignoreDowntime) {
        this.ignoreDowntime = ignoreDowntime;
        return this;
    }

    public AutodetectBuilder referencedLists(Set<ListDocument> lists) {
        referencedLists = lists;
        return this;
    }

    /**
     * Set quantiles to restore the normaliser state if any.
     *
     * @param quantiles the non-null quantiles
     */
    public AutodetectBuilder quantiles(Optional<Quantiles> quantiles) {
        this.quantiles = quantiles;
        return this;
    }

    /**
     * Set model snapshot to restore on startup if required
     *
     * @param modelSnapshot The non-null model snapshot to restore on startup
     */
    public AutodetectBuilder modelSnapshot(ModelSnapshot modelSnapshot) {
        this.modelSnapshot = Optional.of(Objects.requireNonNull(modelSnapshot));
        return this;
    }

    /**
     * Clears the environment and starts the process in that environment.
     * <code>processName</code> is not the full path it is the relative path of the
     * program from the Elasticsearch plugins directory.
     *
     * @return A Java Process object
     */
    public Process build() throws IOException {

        String restoreSnapshotId = modelSnapshot.isPresent() ? modelSnapshot.get().getSnapshotId() : null;
        List<String> command = ProcessCtrl.buildAutodetectCommand(env, settings, jobDetails, logger, restoreSnapshotId, ignoreDowntime);

        buildLimits(command);
        buildModelDebugConfig(command);

        if (ProcessCtrl.modelConfigFilePresent(env)) {
            String modelConfigFile = PrelertPlugin.resolveConfigFile(env, ProcessCtrl.PRELERT_MODEL_CONF).toString();
            command.add(ProcessCtrl.MODEL_CONFIG_ARG + modelConfigFile);
        }

        buildQuantiles(command);
        buildFieldConfig(command);
        return buildProcess(command);
    }

    private void buildLimits(List<String> command) throws IOException {
        if (jobDetails.getAnalysisLimits() != null) {
            Path limitConfigFile = Files.createTempFile(env.tmpFile(), "limitconfig", CONF_EXTENSION);
            filesToDelete.add(limitConfigFile);
            writeLimits(jobDetails.getAnalysisLimits(), limitConfigFile);
            String limits = LIMIT_CONFIG_ARG + limitConfigFile.toString();
            command.add(limits);
        }
    }

    /**
     * Write the Prelert autodetect model options to <code>emptyConfFile</code>.
     */
    private static void writeLimits(AnalysisLimits options, Path emptyConfFile) throws IOException {

        try (OutputStreamWriter osw = new OutputStreamWriter(Files.newOutputStream(emptyConfFile), StandardCharsets.UTF_8)) {
            new AnalysisLimitsWriter(options, osw).write();
        }
    }

    private void buildModelDebugConfig(List<String> command) throws IOException {
        if (jobDetails.getModelDebugConfig() != null) {
            Path modelDebugConfigFile = Files.createTempFile(env.tmpFile(), "modeldebugconfig", CONF_EXTENSION);
            filesToDelete.add(modelDebugConfigFile);
            writeModelDebugConfig(jobDetails.getModelDebugConfig(), modelDebugConfigFile);
            String modelDebugConfig = MODEL_DEBUG_CONFIG_ARG + modelDebugConfigFile.toString();
            command.add(modelDebugConfig);
        }
    }

    private static void writeModelDebugConfig(ModelDebugConfig config, Path emptyConfFile)
            throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(
                Files.newOutputStream(emptyConfFile),
                StandardCharsets.UTF_8)) {
            new ModelDebugConfigWriter(config, osw).write();
        }
    }

    private void buildQuantiles(List<String> command) throws IOException {
        if (quantiles.isPresent() && !quantiles.get().getQuantileState().isEmpty()) {
            Quantiles quantiles = this.quantiles.get();
            logger.info("Restoring quantiles for job '" + jobDetails.getId() + "'");

            Path normalisersStateFilePath = ProcessCtrl.writeNormaliserInitState(
                    jobDetails.getId(), quantiles.getQuantileState(), env);

            String quantilesStateFileArg = ProcessCtrl.QUANTILES_STATE_PATH_ARG + normalisersStateFilePath;
            command.add(quantilesStateFileArg);
            command.add(ProcessCtrl.DELETE_STATE_FILES_ARG);
        }
    }

    private void buildFieldConfig(List<String> command) throws IOException, FileNotFoundException {
        if (jobDetails.getAnalysisConfig() != null) {
            // write to a temporary field config file
            Path fieldConfigFile = Files.createTempFile(env.tmpFile(), "fieldconfig", CONF_EXTENSION);
            filesToDelete.add(fieldConfigFile);
            try (OutputStreamWriter osw = new OutputStreamWriter(
                    Files.newOutputStream(fieldConfigFile),
                    StandardCharsets.UTF_8)) {
                new FieldConfigWriter(jobDetails.getAnalysisConfig(), referencedLists, osw, logger).write();
            }

            String fieldConfig = FIELD_CONFIG_ARG + fieldConfigFile.toString();
            command.add(fieldConfig);
        }
    }

    private Process buildProcess(List<String> command) throws IOException {
        logger.info("Starting autodetect process with command: " + command);
        ProcessBuilder pb = new ProcessBuilder(command);
        ProcessCtrl.buildEnvironment(pb);
        return pb.start();
    }
}
