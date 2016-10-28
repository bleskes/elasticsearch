
package org.elasticsearch.xpack.prelert.job.process.autodetect.legacy;

import org.apache.logging.log4j.Logger;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
    private List<File> filesToDelete;
    private Logger logger;
    private boolean ignoreDowntime;
    private Set<ListDocument> referencedLists;
    private Optional<Quantiles> quantiles;
    private Optional<ModelSnapshot> modelSnapshot;

    /**
     * Constructs an autodetect process builder
     *
     * @param job           The job configuration
     * @param filesToDelete This method will append File objects that need to be
     *                      deleted when the process completes
     * @param logger        The job's logger
     */
    public AutodetectBuilder(JobDetails job, List<File> filesToDelete, Logger logger) {
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
    public AutodetectBuilder quantiles(Quantiles quantiles) {
        this.quantiles = Optional.of(Objects.requireNonNull(quantiles));
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
     * Sets the environment variables PRELERT_HOME and LIB_PATH (or platform
     * variants) and starts the process in that environment. Any inherited value
     * of LIB_PATH or PRELERT_HOME is overwritten.
     * <code>processName</code> is not the full path it is the relative path of the
     * program from the PRELERT_HOME/bin directory.
     *
     * @return A Java Process object
     * @throws IOException
     */
    public Process build() throws IOException {
        logger.info("PRELERT_HOME is set to " + ProcessCtrl.PRELERT_HOME);

        String restoreSnapshotId = modelSnapshot.isPresent() ? modelSnapshot.get().getSnapshotId() : null;
        List<String> command = ProcessCtrl.buildAutodetectCommand(jobDetails, logger, restoreSnapshotId, ignoreDowntime);

        buildLimits(command);
        buildModelDebugConfig(command);

        if (ProcessCtrl.modelConfigFilePresent()) {
            String modelConfigFile = new File(ProcessCtrl.CONFIG_DIR, ProcessCtrl.PRELERT_MODEL_CONF).getPath();
            command.add(ProcessCtrl.MODEL_CONFIG_ARG + modelConfigFile);
        }

        buildQuantiles(command);
        buildFieldConfig(command);
        return buildProcess(command);
    }

    private void buildLimits(List<String> command) throws IOException {
        if (jobDetails.getAnalysisLimits() != null) {
            File limitConfigFile = File.createTempFile("limitconfig", CONF_EXTENSION);
            filesToDelete.add(limitConfigFile);
            writeLimits(jobDetails.getAnalysisLimits(), limitConfigFile);
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
    private static void writeLimits(AnalysisLimits options, File emptyConfFile) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(emptyConfFile),
                StandardCharsets.UTF_8)) {
            new AnalysisLimitsWriter(options, osw).write();
        }
    }

    private void buildModelDebugConfig(List<String> command) throws IOException {
        if (jobDetails.getModelDebugConfig() != null && jobDetails.getModelDebugConfig().isEnabled()) {
            File modelDebugConfigFile = File.createTempFile("modeldebugconfig", CONF_EXTENSION);
            filesToDelete.add(modelDebugConfigFile);
            writeModelDebugConfig(jobDetails.getModelDebugConfig(), modelDebugConfigFile);
            String modelDebugConfig = MODEL_DEBUG_CONFIG_ARG + modelDebugConfigFile.getPath();
            command.add(modelDebugConfig);
        }
    }

    private static void writeModelDebugConfig(ModelDebugConfig config, File emptyConfFile)
            throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(emptyConfFile),
                StandardCharsets.UTF_8)) {
            new ModelDebugConfigWriter(config, osw).write();
        }
    }

    private void buildQuantiles(List<String> command) throws IOException {
        if (quantiles.isPresent() && !quantiles.get().getQuantileState().isEmpty()) {
            Quantiles quantiles = this.quantiles.get();
            logger.info("Restoring quantiles for job '" + jobDetails.getId() + "'");

            Path normalisersStateFilePath = ProcessCtrl.writeNormaliserInitState(
                    jobDetails.getId(), quantiles.getQuantileState());

            String quantilesStateFileArg = ProcessCtrl.QUANTILES_STATE_PATH_ARG + normalisersStateFilePath;
            command.add(quantilesStateFileArg);
            command.add(ProcessCtrl.DELETE_STATE_FILES_ARG);
        }
    }

    private void buildFieldConfig(List<String> command) throws IOException, FileNotFoundException {
        if (jobDetails.getAnalysisConfig() != null) {
            // write to a temporary field config file
            File fieldConfigFile = File.createTempFile("fieldconfig", CONF_EXTENSION);
            filesToDelete.add(fieldConfigFile);
            try (OutputStreamWriter osw = new OutputStreamWriter(
                    new FileOutputStream(fieldConfigFile),
                    StandardCharsets.UTF_8)) {
                new FieldConfigWriter(jobDetails.getAnalysisConfig(), referencedLists, osw, logger).write();
            }

            String fieldConfig = FIELD_CONFIG_ARG + fieldConfigFile.getPath();
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
