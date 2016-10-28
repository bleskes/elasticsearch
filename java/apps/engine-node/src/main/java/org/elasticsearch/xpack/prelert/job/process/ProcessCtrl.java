
package org.elasticsearch.xpack.prelert.job.process;

import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.settings.PrelertSettings;
import org.elasticsearch.xpack.prelert.utils.Strings;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.apache.lucene.util.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Utility class for running a Prelert process<br>
 * The process runs in a clean environment with only one env var set - PRELERT_HOME
 * <p>
 * This class first needs to know where PRELERT_HOME is so it checks for
 * the system property <b>prelert.home</b> and failing that looks for the
 * PRELERT_HOME env var. If neither exist prelert home is set to an empty string.
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
     * The location of Prelert Home. Equivalent to $PRELERT_HOME
     */
    public static final String PRELERT_HOME;
    /**
     * The base log file directory. Defaults to $PRELERT_HOME/logs
     * but may be overridden with the PRELERT_LOGS_PROPERTY property
     */
    public static final String LOG_DIR;
    /**
     * The config directory. Equivalent to $PRELERT_HOME/config
     */
    public static final String CONFIG_DIR;
    /**
     * The bin directory. Equivalent to $PRELERT_HOME/bin
     */
    public static final String BIN_DIR;
    /**
     * The full path to the autodetect program
     */
    public static final String AUTODETECT_PATH;
    /**
     * The full path to the normalisation program
     */
    public static final String NORMALIZE_PATH;
    /**
     * Name of the config setting containing the value of Prelert Home
     */
    public static final String PRELERT_HOME_PROPERTY = "prelert.home";
    /**
     * Name of the config setting containing the path to the logs directory
     */
    public static final String PRELERT_LOGS_PROPERTY = "prelert.logs";
    /**
     * The maximum number of anomaly records that will be written each bucket
     */
    public static final String MAX_ANOMALY_RECORDS_PROPERTY = "max.anomaly.records";
    private static final int DEFAULT_MAX_NUM_RECORDS = 500;

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
    public static final int ES_HTTP_PORT = 8080;

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
    public static final String MAX_ANOMALY_RECORDS_ARG;
    public static final String PERIOD_ARG = "--period=";
    public static final String PERSIST_INTERVAL_ARG = "--persistInterval=";
    public static final String MAX_QUANTILE_INTERVAL_ARG = "--maxQuantileInterval=";
    public static final String RESTORE_SNAPSHOT_ID = "--restoreSnapshotId=";
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
    public static final String DONT_PERSIST_MODEL_STATE = "no.model.state.persist";


    /**
     * Static initialisation finds Elasticsearch HTTP port, Prelert home and the
     * path to the executatble.
     */
    static {
        String prelertHome = PrelertSettings.getSettingOrDefault(PRELERT_HOME_PROPERTY, ".");

        String logPath = PrelertSettings.getSettingOrDefault(PRELERT_LOGS_PROPERTY,
                prelertHome + "/logs");

        int maxNumRecords = PrelertSettings.getSettingOrDefault(MAX_ANOMALY_RECORDS_PROPERTY,
                DEFAULT_MAX_NUM_RECORDS);
        MAX_ANOMALY_RECORDS_ARG = "--maxAnomalyRecords=" + maxNumRecords;

        PRELERT_HOME = prelertHome;
        BIN_DIR = new File(PRELERT_HOME, "bin").getPath();
        File executable = new File(BIN_DIR, AUTODETECT);
        AUTODETECT_PATH = executable.getPath();

        executable = new File(BIN_DIR, NORMALIZE);
        NORMALIZE_PATH = executable.getPath();

        if (logPath != null) {
            LOG_DIR = logPath;
        } else {
            File logDir = new File(PRELERT_HOME, "logs");
            LOG_DIR = logDir.getPath();
        }

        File configDir = new File(PRELERT_HOME, "config");
        CONFIG_DIR = configDir.getPath();
    }

    private ProcessCtrl() {

    }

    /**
     * Set up an environment containing the PRELERT_HOME environment variable.
     * LIB_PATH is not set as the binaries are linked with relative paths
     */
    public static void buildEnvironment(ProcessBuilder pb) {
        // Always clear inherited environment variables
        pb.environment().clear();
        pb.environment().put(PrelertSettings.PRELERT_HOME_ENV, PRELERT_HOME);

        LOGGER.info(String.format("Process Environment = " + pb.environment().toString()));
    }


    public static String getAnalyticsVersion() {
        return AnalyticsVersionHolder.s_AnalyticsVersion;
    }

    // Static field lazy initialization idiom
    private static class AnalyticsVersionHolder {
        static final String s_AnalyticsVersion = detectAnalyticsVersion();

        private AnalyticsVersionHolder() {
        }

        private static String detectAnalyticsVersion() {
            List<String> command = new ArrayList<>();
            command.add(AUTODETECT_PATH);
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
                        return String.format("Error autodetect returned %d. \nError Output = '%s'.\n%s",
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
     * Get the C++ process to print a JSON document containing some of the usage
     * and license info
     */
    public static synchronized String getInfo() {
        List<String> command = new ArrayList<>();
        command.add(AUTODETECT_PATH);
        command.add(INFO_ARG);

        LOGGER.info("Getting info from " + command);

        // Build the process
        ProcessBuilder pb = new ProcessBuilder(command);
        buildEnvironment(pb);

        try {
            Process proc = pb.start();
            try {
                int exitValue = proc.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));

                String output = reader.readLine();
                LOGGER.debug("autodetect info output = " + output);

                if (exitValue >= 0 && output != null) {
                    return output;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted reading autodetect info", ie);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading autodetect info", e);
        }

        // On error return an empty JSON document
        return "{}";
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

    public static List<String> buildAutodetectCommand(JobDetails job, Logger logger, String restoreSnapshotId, boolean ignoreDowntime) {
        List<String> command = new ArrayList<>();
        command.add(AUTODETECT_PATH);

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
        command.add(MAX_ANOMALY_RECORDS_ARG);

        // always set the time field
        String timeFieldArg = TIME_FIELD_ARG + getTimeFieldOrDefault(job);
        command.add(timeFieldArg);

        int intervalStagger = calculateStaggeringInterval(job.getId());
        logger.debug("Periodic operations staggered by " + intervalStagger +" seconds for job '" + job.getId() + "'");

        // Supply a URL for persisting/restoring model state unless model
        // persistence has been explicitly disabled.
        if (PrelertSettings.isSet(DONT_PERSIST_MODEL_STATE)) {
            logger.info("Will not persist model state - "  + DONT_PERSIST_MODEL_STATE + " setting was set");
        } else {
            if (Strings.isNullOrEmpty(restoreSnapshotId) == false) {
                command.add(RESTORE_SNAPSHOT_ID + restoreSnapshotId);
            }

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

    private static String getTimeFieldOrDefault(JobDetails job) {
        DataDescription dataDescription = job.getDataDescription();
        boolean useDefault = dataDescription == null
                || Strings.isNullOrEmpty(dataDescription.getTimeField());
        return useDefault ? DataDescription.DEFAULT_TIME_FIELD : dataDescription.getTimeField();
    }

    private static void addIfNotNull(List<String> list, String argKey, List<String> command) {
        if (list != null) {
            String param = argKey + String.join(",", list);
            command.add(param);
        }
    }

    private static <T> void addIfNotNull(T object, String argKey, List<String> command) {
        if (object != null) {
            String param = argKey + object;
            command.add(param);
        }
    }

    /**
     * Return true if there is a file PRELERT_HOME/config/prelertmodel.conf
     *
     * @return
     */
    public static boolean modelConfigFilePresent() {
        File f = new File(CONFIG_DIR, PRELERT_MODEL_CONF);

        return f.exists() && !f.isDirectory();
    }

    /**
     * The process can be initialised with both sysChangeState and
     * unusualBehaviourState if either is <code>null</code> then is
     * is not used.
     *
     * @param jobId
     * @param quantilesState            Set to <code>null</code> to be ignored
     * @param bucketSpan                If <code>null</code> then use the program default
     * @param perPartitionNormalization
     * @param logger
     * @return
     * @throws IOException
     */
    public static Process buildNormaliser(String jobId, String quantilesState,
                                          Integer bucketSpan, boolean perPartitionNormalization, Logger logger)
            throws IOException {
        logger.info("PRELERT_HOME is set to " + PRELERT_HOME);

        List<String> command = ProcessCtrl.buildNormaliserCommand(jobId, bucketSpan,
                perPartitionNormalization);

        if (quantilesState != null) {
            Path quantilesStateFilePath = writeNormaliserInitState(jobId, quantilesState);

            String stateFileArg = QUANTILES_STATE_PATH_ARG + quantilesStateFilePath;
            command.add(stateFileArg);
            command.add(DELETE_STATE_FILES_ARG);
        }

        if (modelConfigFilePresent()) {
            String modelConfigFile = new File(CONFIG_DIR, PRELERT_MODEL_CONF).getPath();
            command.add(MODEL_CONFIG_ARG + modelConfigFile);
        }

        // Build the process
        logger.info("Starting normaliser process with command: " + command);
        ProcessBuilder pb = new ProcessBuilder(command);
        buildEnvironment(pb);

        return pb.start();

    }

    static List<String> buildNormaliserCommand(String jobId, Integer bucketSpan,
                                               boolean perPartitionNormalization)
            throws IOException {
        List<String> command = new ArrayList<>();
        command.add(NORMALIZE_PATH);
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
     *
     * @param jobId
     * @param state
     * @return The state file path
     * @throws IOException
     */
    public static Path writeNormaliserInitState(String jobId, String state)
            throws IOException {
        // createTempFile has a race condition where it may return the same
        // temporary file name to different threads if called simultaneously
        // from multiple threads, hence add the thread ID to avoid this
        Path stateFile = Files.createTempFile(jobId + "_quantiles_" + Thread.currentThread().getId(),
                QUANTILES_FILE_EXTENSION);

        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(stateFile.toFile().getPath()),
                StandardCharsets.UTF_8)) {
            osw.write(state);
        }

        return stateFile;
    }
}
