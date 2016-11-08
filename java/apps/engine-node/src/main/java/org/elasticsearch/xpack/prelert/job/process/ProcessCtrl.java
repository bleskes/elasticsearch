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
package org.elasticsearch.xpack.prelert.job.process;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.Job;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;


/**
 * Utility class for running a Prelert process<br>
 * The process runs in a clean environment.
 */
public class ProcessCtrl {
    private static final Logger LOGGER = Loggers.getLogger(ProcessCtrl.class);

    /**
     * Autodetect API native program name
     */
    public static final String AUTODETECT = "prelert_autodetect";
    /**
     * The normalisation native program name
     */
    public static final String NORMALIZE = "prelert_normalize";
    /**
     * Name of the config setting containing the path to the logs directory
     */
    public static final String PRELERT_LOGS_PROPERTY = "prelert.logs";
    private static final int DEFAULT_MAX_NUM_RECORDS = 500;
    /**
     * The maximum number of anomaly records that will be written each bucket
     */
    public static final Setting<Integer> MAX_ANOMALY_RECORDS_SETTING = Setting.intSetting("max.anomaly.records", DEFAULT_MAX_NUM_RECORDS,
            Property.NodeScope);

    /*
     * General arguments
     */
    public static final String LOG_ID_ARG = "--logid=";


    /**
     * Host for the Elasticsearch we'll pass on to the Autodetect API program
     */
    // NORELEASE This is for the process to write state directly to ES
    // which won't happen in the final product. See #18
    public static final String ES_HOST = "localhost";

    /**
     * Default Elasticsearch HTTP port
     */
    // NORELEASE This is for the process to write state directly to ES
    // which won't happen in the final product. See #18
    public static final int ES_HTTP_PORT = 9200;

    /**
     * If this is changed, ElasticsearchJobId should also be changed
     */
    // NORELEASE This is for the process to write state directly to ES
    // which won't happen in the final product. See #18
    private static final String ES_INDEX_PREFIX = "prelertresults-";


    /*
     * Arguments used by both prelert_autodetect and prelert_normalize
     */
    public static final String BUCKET_SPAN_ARG = "--bucketspan=";
    public static final String DELETE_STATE_FILES_ARG = "--deleteStateFiles";
    public static final String IGNORE_DOWNTIME_ARG = "--ignoreDowntime";
    public static final String LENGTH_ENCODED_INPUT_ARG = "--lengthEncodedInput";
    public static final String MODEL_CONFIG_ARG = "--modelconfig=";
    public static final String QUANTILES_STATE_PATH_ARG = "--quantilesState=";
    public static final String MULTIPLE_BUCKET_SPANS_ARG = "--multipleBucketspans=";
    public static final String PER_PARTITION_NORMALIZATION = "--perPartitionNormalization";

    /*
     * Arguments used by prelert_autodetect
     */
    public static final String BATCH_SPAN_ARG = "--batchspan=";
    public static final String INFO_ARG = "--info";
    public static final String LATENCY_ARG = "--latency=";
    public static final String RESULT_FINALIZATION_WINDOW_ARG = "--resultFinalizationWindow=";
    public static final String MULTIVARIATE_BY_FIELDS_ARG = "--multivariateByFields";
    public static final String PERIOD_ARG = "--period=";
    public static final String PERSIST_INTERVAL_ARG = "--persistInterval=";
    public static final String MAX_QUANTILE_INTERVAL_ARG = "--maxQuantileInterval=";
    public static final String PERSIST_URL_BASE_ARG = "--persistUrlBase=";
    public static final String SUMMARY_COUNT_FIELD_ARG = "--summarycountfield=";
    public static final String TIME_FIELD_ARG = "--timefield=";
    public static final String VERSION_ARG = "--version";

    public static final int SECONDS_IN_HOUR = 3600;

    /**
     * Roughly how often should the C++ process persist state?  A staggering
     * factor that varies by job is added to this.
     */
    public static final long DEFAULT_BASE_PERSIST_INTERVAL = 10800; // 3 hours

    /**
     * Roughly how often should the C++ process output quantiles when no
     * anomalies are being detected?  A staggering factor that varies by job is
     * added to this.
     */
    public static final int BASE_MAX_QUANTILE_INTERVAL = 21600; // 6 hours

    /**
     * Name of the model config file
     */
    public static final String PRELERT_MODEL_CONF = "prelertmodel.conf";

    /**
     * The unknown analytics version number string returned when the version
     * cannot be read
     */
    public static final String UNKNOWN_VERSION = "Unknown version of the analytics";

    /**
     * Persisted quantiles are written to disk so they can be read by
     * the autodetect program.  All quantiles files have this extension.
     */
    private static final String QUANTILES_FILE_EXTENSION = ".json";

    /**
     * Config setting storing the flag that disables model persistence
     */
    public static final Setting<Boolean> DONT_PERSIST_MODEL_STATE_SETTING = Setting.boolSetting("no.model.state.persist", false,
            Property.NodeScope);

    public static String maxAnomalyRecordsArg(Settings settings) {
        return "--maxAnomalyRecords=" + MAX_ANOMALY_RECORDS_SETTING.get(settings);
    }

    private ProcessCtrl() {

    }

    /**
     * Set up a completely empty environment. LD_LIBRARY_PATH (or equivalent)
     * is not needed as the binaries are linked with relative paths.
     */
    public static void buildEnvironment(ProcessBuilder pb) {
        // Always clear inherited environment variables
        pb.environment().clear();
    }

    public static Path getAutodetectPath(Environment env) {
        return PrelertPlugin.resolveBinFile(env, AUTODETECT);
    }

    public static Path getNormalizePath(Environment env) {
        return PrelertPlugin.resolveBinFile(env, NORMALIZE);
    }

    public static String getAnalyticsVersion(Environment env) {
        return AnalyticsVersionHolder.detectAnalyticsVersion(env);
    }

    // Static field lazy initialization idiom
    private static class AnalyticsVersionHolder {

        private AnalyticsVersionHolder() {
        }

        private static String detectAnalyticsVersion(Environment env) {
            List<String> command = new ArrayList<>();
            command.add(getAutodetectPath(env).toString());
            command.add(VERSION_ARG);

            LOGGER.info("Getting version number from " + command);

            // Build the process
            ProcessBuilder pb = new ProcessBuilder(command);
            buildEnvironment(pb);

            try {
                Process proc = pb.start();
                try {
                    int exitValue = proc.waitFor();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8));

                    String output = "";
                    String line = reader.readLine();
                    while (line != null) {
                        output += line + '\n';
                        line = reader.readLine();
                    }

                    LOGGER.debug("autodetect version output = " + output);

                    if (exitValue < 0) {
                        return String.format(Locale.ROOT, "Error autodetect returned %d. \nError Output = '%s'.\n%s",
                                exitValue, output, UNKNOWN_VERSION);
                    }
                    return output.isEmpty() ? UNKNOWN_VERSION : output;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted reading analytics version number", ie);
                    return UNKNOWN_VERSION;
                }

            } catch (IOException e) {
                LOGGER.error("Error reading analytics version number", e);
                return UNKNOWN_VERSION;
            }
        }
    }

    /**
     * This random time of up to 1 hour is added to intervals at which we
     * tell the C++ process to perform periodic operations.  This means that
     * when there are many jobs there is a certain amount of staggering of
     * their periodic operations.  A given job will always be given the same
     * staggering interval (for a given JVM implementation).
     *
     * @param jobId The ID of the job to calculate the staggering interval for
     * @return The staggering interval
     */
    static int calculateStaggeringInterval(String jobId) {
        Random rng = new Random(jobId.hashCode());
        return rng.nextInt(SECONDS_IN_HOUR);
    }

    public static List<String> buildAutodetectCommand(Environment env, Settings settings, Job job, Logger logger, boolean ignoreDowntime) {
        List<String> command = new ArrayList<>();
        command.add(getAutodetectPath(env).toString());

        // the logging id is the job id
        String logId = LOG_ID_ARG + job.getId();
        command.add(logId);

        AnalysisConfig analysisConfig = job.getAnalysisConfig();
        if (analysisConfig != null) {
            addIfNotNull(analysisConfig.getBucketSpan(), BUCKET_SPAN_ARG, command);
            addIfNotNull(analysisConfig.getBatchSpan(), BATCH_SPAN_ARG, command);
            addIfNotNull(analysisConfig.getLatency(), LATENCY_ARG, command);
            addIfNotNull(analysisConfig.getPeriod(), PERIOD_ARG, command);
            addIfNotNull(analysisConfig.getSummaryCountFieldName(),
                    SUMMARY_COUNT_FIELD_ARG, command);
            addIfNotNull(analysisConfig.getMultipleBucketSpans(),
                    MULTIPLE_BUCKET_SPANS_ARG, command);
            if (Boolean.TRUE.equals(analysisConfig.getOverlappingBuckets())) {
                Long window = analysisConfig.getResultFinalizationWindow();
                if (window == null) {
                    window = AnalysisConfig.DEFAULT_RESULT_FINALIZATION_WINDOW;
                }
                command.add(RESULT_FINALIZATION_WINDOW_ARG + window);
            }
            if (Boolean.TRUE.equals(analysisConfig.getMultivariateByFields())) {
                command.add(MULTIVARIATE_BY_FIELDS_ARG);
            }

            if (analysisConfig.getUsePerPartitionNormalization()) {
                command.add(PER_PARTITION_NORMALIZATION);
            }
        }

        // Input is always length encoded
        command.add(LENGTH_ENCODED_INPUT_ARG);

        // Limit the number of output records
        command.add(maxAnomalyRecordsArg(settings));

        // always set the time field
        String timeFieldArg = TIME_FIELD_ARG + getTimeFieldOrDefault(job);
        command.add(timeFieldArg);

        int intervalStagger = calculateStaggeringInterval(job.getId());
        logger.debug("Periodic operations staggered by " + intervalStagger +" seconds for job '" + job.getId() + "'");

        // Supply a URL for persisting/restoring model state unless model
        // persistence has been explicitly disabled.
        if (DONT_PERSIST_MODEL_STATE_SETTING.get(settings)) {
            logger.info("Will not persist model state - "  + DONT_PERSIST_MODEL_STATE_SETTING + " setting was set");
        } else {
            String persistUrlBase = PERSIST_URL_BASE_ARG + "http://" + ES_HOST + ":" + ES_HTTP_PORT + "/" + ES_INDEX_PREFIX + job.getId();
            command.add(persistUrlBase);

            // Persist model state every few hours even if the job isn't closed
            long persistInterval = (job.getBackgroundPersistInterval() == null) ?
                    (DEFAULT_BASE_PERSIST_INTERVAL + intervalStagger) :
                        job.getBackgroundPersistInterval();
                    command.add(PERSIST_INTERVAL_ARG + persistInterval);
        }

        int maxQuantileInterval = BASE_MAX_QUANTILE_INTERVAL + intervalStagger;
        command.add(MAX_QUANTILE_INTERVAL_ARG + maxQuantileInterval);

        ignoreDowntime = ignoreDowntime
                || job.getIgnoreDowntime() == IgnoreDowntime.ONCE
                || job.getIgnoreDowntime() == IgnoreDowntime.ALWAYS;

        if (ignoreDowntime) {
            command.add(IGNORE_DOWNTIME_ARG);
        }

        return command;
    }

    private static String getTimeFieldOrDefault(Job job) {
        DataDescription dataDescription = job.getDataDescription();
        boolean useDefault = dataDescription == null
                || Strings.isNullOrEmpty(dataDescription.getTimeField());
        return useDefault ? DataDescription.DEFAULT_TIME_FIELD : dataDescription.getTimeField();
    }

    private static <T> void addIfNotNull(T object, String argKey, List<String> command) {
        if (object != null) {
            String param = argKey + object;
            command.add(param);
        }
    }

    /**
     * Return true if there is a file ES_HOME/config/prelertmodel.conf
     */
    public static boolean modelConfigFilePresent(Environment env) {
        Path modelConfPath = PrelertPlugin.resolveConfigFile(env, PRELERT_MODEL_CONF);

        return Files.isRegularFile(modelConfPath);
    }

    /**
     * The process can be initialised with both sysChangeState and
     * unusualBehaviourState if either is <code>null</code> then is
     * is not used.
     */
    public static Process buildNormaliser(Environment env, String jobId, String quantilesState, Integer bucketSpan,
            boolean perPartitionNormalization, Logger logger)
                    throws IOException {

        List<String> command = ProcessCtrl.buildNormaliserCommand(env, jobId, bucketSpan,
                perPartitionNormalization);

        if (quantilesState != null) {
            Path quantilesStateFilePath = writeNormaliserInitState(jobId, quantilesState, env);

            String stateFileArg = QUANTILES_STATE_PATH_ARG + quantilesStateFilePath;
            command.add(stateFileArg);
            command.add(DELETE_STATE_FILES_ARG);
        }

        if (modelConfigFilePresent(env)) {
            Path modelConfPath = PrelertPlugin.resolveConfigFile(env, PRELERT_MODEL_CONF);
            command.add(MODEL_CONFIG_ARG + modelConfPath.toAbsolutePath().getFileName());
        }

        // Build the process
        logger.info("Starting normaliser process with command: " + command);
        ProcessBuilder pb = new ProcessBuilder(command);
        buildEnvironment(pb);

        return pb.start();

    }

    static List<String> buildNormaliserCommand(Environment env, String jobId, Integer bucketSpan,
            boolean perPartitionNormalization)
                    throws IOException {
        List<String> command = new ArrayList<>();
        command.add(getNormalizePath(env).toString());
        addIfNotNull(bucketSpan, BUCKET_SPAN_ARG, command);
        command.add(LOG_ID_ARG + jobId);
        command.add(LENGTH_ENCODED_INPUT_ARG);
        if (perPartitionNormalization) {
            command.add(PER_PARTITION_NORMALIZATION);
        }

        return command;
    }

    /**
     * Write the normaliser init state to file.
     */
    public static Path writeNormaliserInitState(String jobId, String state, Environment env)
            throws IOException {
        // createTempFile has a race condition where it may return the same
        // temporary file name to different threads if called simultaneously
        // from multiple threads, hence add the thread ID to avoid this
        Path stateFile = Files.createTempFile(env.tmpFile(), jobId + "_quantiles_" + Thread.currentThread().getId(),
                QUANTILES_FILE_EXTENSION);

        try (BufferedWriter osw = Files.newBufferedWriter(stateFile, StandardCharsets.UTF_8);) {
            osw.write(state);
        }

        return stateFile;
    }
}
