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
 ***********************************************************/

package com.prelert.job.process;

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

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataDescription;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.process.writer.AnalysisLimitsWriter;
import com.prelert.job.process.writer.FieldConfigWriter;
import com.prelert.job.process.writer.ModelDebugConfigWriter;
import com.prelert.job.quantiles.Quantiles;


/**
 * Utility class for running a Prelert process<br>
 * The environment variables PRELERT_HOME and LIB_PATH (or platform variants)
 * are set for the environment of the launched process, any inherited values
 * of LIB_PATH or PRELERT_HOME are overwritten.
 *
 * This class first needs to know where PRELERT_HOME is so it checks for
 * the system property <b>prelert.home</b> and failing that looks for the
 * PRELERT_HOME env var. If neither exist prelert home is set to an empty string.
 */
public class ProcessCtrl
{
    /**
     * System property storing the Elasticsearch HTTP port
     */
    public static final String ES_HTTP_PORT_PROP = "es.http.port";

    /**
     * Default Elasticsearch HTTP port
     */
    public static final String DEFAULT_ES_HTTP_PORT = "9200";

    /**
     * Elasticsearch HTTP port we'll pass on to the Autodetect API program
     */
    public static final String ES_HTTP_PORT;

    /**
     * Autodetect API native program name
     */
    public static final String AUTODETECT_API = "prelert_autodetect_api";
    /**
     * The normalisation native program name
     */
    public static final String NORMALIZE_API = "prelert_normalize_api";
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
     * The full to the autodetect program
     */
    public static final String AUTODETECT_PATH;
    /**
     *  The full to the normalisation program
     */
    public static final String NORMALIZE_PATH;
    /**
     * Equivalent to $PRELERT_HOME/lib + $PRELERT_HOME/cots/lib
     * (or $PRELERT_HOME/cots/bin on Windows)
     */
    public static final String LIB_PATH;
    /**
     * The name of the platform path environment variable.
     * See {@linkplain #OSX_LIB_PATH_ENV}, {@linkplain #LINUX_LIB_PATH_ENV}
     * and {@linkplain #WIN_LIB_PATH_ENV}, {@linkplain #SOLARIS_LIB_PATH_ENV}
     */
    public static final String LIB_PATH_ENV;
    /**
     * Name of the System property containing the value of Prelert Home
     */
    public static final String PRELERT_HOME_PROPERTY = "prelert.home";
    /**
     * Name of the environment variable PRELERT_HOME
     */
    public static final String PRELERT_HOME_ENV = "PRELERT_HOME";
    /**
     * Name of the System property path to the logs directory
     */
    public static final String PRELERT_LOGS_PROPERTY = "prelert.logs";
    /**
     * The maximum number of anomaly records that will be written each bucket
     */
    public static final String MAX_ANOMALY_RECORDS_PROPERTY = "max.anomaly.records";
    /**
     * Name of the environment variable PRELERT_LOGS
     */
    public static final String PRELERT_LOGS_ENV = "PRELERT_LOGS";
    /**
     * OSX library path variable
     */
    public static final String OSX_LIB_PATH_ENV = "DYLD_LIBRARY_PATH";
    /**
     * Linux library path variable
     */
    public static final String LINUX_LIB_PATH_ENV = "LD_LIBRARY_PATH";
    /**
     * Windows library path variable
     */
    public static final String WIN_LIB_PATH_ENV = "Path";
    /**
     * Solaris library path variable
     */
    public static final String SOLARIS_LIB_PATH_ENV = "LD_LIBRARY_PATH_64";

    /*
     * General arguments
     */
    public static final String LOG_ID_ARG = "--logid=";

    /*
     * Arguments used by both prelert_autodetect_api and prelert_normalize_api
     */
    public static final String BUCKET_SPAN_ARG = "--bucketspan=";
    public static final String DELETE_STATE_FILES_ARG = "--deleteStateFiles";
    public static final String LENGTH_ENCODED_INPUT_ARG = "--lengthEncodedInput";
    public static final String MODEL_CONFIG_ARG = "--modelconfig=";
    public static final String QUANTILES_STATE_PATH_ARG = "--quantilesState=";

    /*
     * Arguments used by prelert_autodetect_api
     */
    public static final String BATCH_SPAN_ARG = "--batchspan=";
    public static final String FIELD_CONFIG_ARG = "--fieldconfig=";
    public static final String INFO_ARG = "--info";
    public static final String LIMIT_CONFIG_ARG = "--limitconfig=";
    public static final String LATENCY_ARG = "--latency=";
    public static final String MAX_ANOMALY_RECORDS_ARG;
    public static final String MODEL_DEBUG_CONFIG_ARG = "--modeldebugconfig=";
    public static final String PERIOD_ARG = "--period=";
    public static final String PERSIST_INTERVAL_ARG = "--persistInterval=10800"; // 3 hours
    public static final String PERSIST_URL_BASE_ARG = "--persistUrlBase=";
    public static final String SUMMARY_COUNT_FIELD_ARG = "--summarycountfield=";
    public static final String TIME_FIELD_ARG = "--timefield=";
    public static final String VERSION_ARG = "--version";

    private static final String CONF_EXTENSION = ".conf";

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
     * System property storing the flag that disables model persistence
     */
    public static final String DONT_PERSIST_MODEL_STATE = "no.model.state.persist";


    /**
     * Static initialisation finds Elasticsearch HTTP port, Prelert home and the
     * path to the binaries.  It also sets the lib path to
     * PRELERT_HOME/(lib|bin) + PRELERT_HOME/cots/(lib|bin)
     */
    static
    {
        if (System.getProperty(ES_HTTP_PORT_PROP) != null)
        {
            ES_HTTP_PORT = System.getProperty(ES_HTTP_PORT_PROP);
        }
        else
        {
            ES_HTTP_PORT = DEFAULT_ES_HTTP_PORT;
        }

        String prelertHome = "";
        if (System.getProperty(PRELERT_HOME_PROPERTY) != null)
        {
            prelertHome = System.getProperty(PRELERT_HOME_PROPERTY);
        }
        else if (System.getenv().containsKey(PRELERT_HOME_ENV))
        {
            prelertHome = System.getenv().get(PRELERT_HOME_ENV);
        }

        String logPath = null;
        if (System.getProperty(PRELERT_LOGS_PROPERTY) != null)
        {
            logPath = System.getProperty(PRELERT_LOGS_PROPERTY);
        }
        else if (System.getenv().containsKey(PRELERT_LOGS_ENV))
        {
            logPath = System.getenv().get(PRELERT_LOGS_ENV);
        }

        String maxNumRecords = "500";
        if (System.getProperty(MAX_ANOMALY_RECORDS_PROPERTY) != null)
        {
            try
            {
                Integer.parseInt(System.getProperty(MAX_ANOMALY_RECORDS_PROPERTY));

                // this is an integer so use it
                maxNumRecords = System.getProperty(MAX_ANOMALY_RECORDS_PROPERTY);

            }
            catch (NumberFormatException e)
            {

                Logger.getLogger(ProcessCtrl.class).error(
                            "Cannot parse " + MAX_ANOMALY_RECORDS_PROPERTY + "="
                            + System.getProperty(MAX_ANOMALY_RECORDS_PROPERTY) +
                            " as a number");
            }
        }
        MAX_ANOMALY_RECORDS_ARG = "--maxAnomalyRecords=" + maxNumRecords;

        PRELERT_HOME = prelertHome;
        File executable = new File(new File(PRELERT_HOME, "bin"), AUTODETECT_API);
        AUTODETECT_PATH = executable.getPath();

        executable = new File(new File(PRELERT_HOME, "bin"), NORMALIZE_API);
        NORMALIZE_PATH = executable.getPath();

        if (logPath != null)
        {
            LOG_DIR = logPath;
        }
        else
        {
            File logDir = new File(PRELERT_HOME, "logs");
            LOG_DIR = logDir.toString();
        }

        File configDir = new File(PRELERT_HOME, "config");
        CONFIG_DIR = configDir.toString();

        String libSubDirectory = "lib";
        if (SystemUtils.IS_OS_MAC_OSX)
        {
            LIB_PATH_ENV = OSX_LIB_PATH_ENV;
        }
        else if (SystemUtils.IS_OS_WINDOWS)
        {
            LIB_PATH_ENV = WIN_LIB_PATH_ENV;
            libSubDirectory = "bin";
        }
        else if (SystemUtils.IS_OS_LINUX)
        {
            LIB_PATH_ENV = LINUX_LIB_PATH_ENV;
        }
        else if (SystemUtils.IS_OS_SUN_OS)
        {
            LIB_PATH_ENV = SOLARIS_LIB_PATH_ENV;
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported platform " + SystemUtils.OS_NAME);
        }

        File libDir = new File(PRELERT_HOME, libSubDirectory);
        File cotsDir = new File(new File(PRELERT_HOME, "cots"), libSubDirectory);
        LIB_PATH = libDir.getPath() + File.pathSeparatorChar + cotsDir.getPath();
    }

    private static final Logger LOGGER = Logger.getLogger(ProcessCtrl.class);

    private ProcessCtrl()
    {

    }

    /**
     * Set up an environment containing the PRELERT_HOME and LD_LIBRARY_PATH
     * (or equivalent) environment variables.
     */
    static void buildEnvironment(ProcessBuilder pb)
    {
        // Always clear inherited environment variables
        pb.environment().clear();

        pb.environment().put(PRELERT_HOME_ENV, PRELERT_HOME);
        pb.environment().put(LIB_PATH_ENV, LIB_PATH);

        LOGGER.debug(String.format("Process Environment = " + pb.environment().toString()));
    }


    public static String getAnalyticsVersion()
    {
        return AnalyticsVersionHolder.s_AnalyticsVersion;
    }

    // Static field lazy initialization idiom
    private static class AnalyticsVersionHolder
    {
        static final String s_AnalyticsVersion = detectAnalyticsVersion();

        private AnalyticsVersionHolder()
        {
        }

        private static String detectAnalyticsVersion()
        {
            List<String> command = new ArrayList<>();
            command.add(AUTODETECT_PATH);
            command.add(VERSION_ARG);

            LOGGER.info("Getting version number from " + command);

            // Build the process
            ProcessBuilder pb = new ProcessBuilder(command);
            buildEnvironment(pb);

            try
            {
                Process proc = pb.start();
                try
                {
                    int exitValue = proc.waitFor();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(proc.getErrorStream(),
                                    StandardCharsets.UTF_8));

                    String output = "";
                    String line = reader.readLine();
                    while (line != null)
                    {
                        output += line + '\n';
                        line = reader.readLine();
                    }

                    LOGGER.debug("autodetect version output = " + output);

                    if (exitValue < 0)
                    {
                        return String.format("Error autodetect returned %d. \nError Output = '%s'.\n%s",
                                exitValue, output, UNKNOWN_VERSION);
                    }
                    return output.isEmpty() ? UNKNOWN_VERSION : output;
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted reading analytics version number", ie);
                    return UNKNOWN_VERSION;
                }

            }
            catch (IOException e)
            {
                LOGGER.error("Error reading analytics version number", e);
                return UNKNOWN_VERSION;
            }
        }
    }

    /**
     * Get the C++ process to print a JSON document containing some of the usage
     * and license info
     */
    public synchronized static String getInfo()
    {
        List<String> command = new ArrayList<>();
        command.add(AUTODETECT_PATH);
        command.add(INFO_ARG);

        LOGGER.info("Getting info from " + command);

        // Build the process
        ProcessBuilder pb = new ProcessBuilder(command);
        buildEnvironment(pb);

        try
        {
            Process proc = pb.start();
            try
            {
                int exitValue = proc.waitFor();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream(),
                                StandardCharsets.UTF_8));

                String output = reader.readLine();
                LOGGER.debug("autodetect info output = " + output);

                if (exitValue >= 0 && output != null)
                {
                    return output;
                }
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted reading autodetect info", ie);
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Error reading autodetect info", e);
        }

        // On error return an empty JSON document
        return "{}";
    }

    /**
     * Sets the environment variables PRELERT_HOME and LIB_PATH (or platform
     * variants) and starts the process in that environment. Any inherited value
     * of LIB_PATH or PRELERT_HOME is overwritten.
     * <code>processName</code> is not the full path it is the relative path of the
     * program from the PRELERT_HOME/bin directory.
     *
     * @param job The job configuration
     * @param quantiles if <code>null</code> this parameter is
     * ignored else the quantiles' state is restored from this object
     * @param logger The job's logger
     * @param filesToDelete This method will append File objects that need to be
     * deleted when the process completes
     *
     * @return A Java Process object
     * @throws IOException
     */
    public static Process buildAutoDetect(JobDetails job, Quantiles quantiles, Logger logger,
            List<File> filesToDelete)
    throws IOException
    {
        logger.info("PRELERT_HOME is set to " + PRELERT_HOME);

        List<String> command = ProcessCtrl.buildAutoDetectCommand(job, logger);

        if (job.getAnalysisLimits() != null)
        {
            File limitConfigFile = File.createTempFile("limitconfig", CONF_EXTENSION);
            filesToDelete.add(limitConfigFile);
            writeLimits(job.getAnalysisLimits(), limitConfigFile);
            String limits = LIMIT_CONFIG_ARG + limitConfigFile.toString();
            command.add(limits);
        }

        if (job.getModelDebugConfig() != null && job.getModelDebugConfig().isEnabled())
        {
            File modelDebugConfigFile = File.createTempFile("modeldebugconfig", CONF_EXTENSION);
            filesToDelete.add(modelDebugConfigFile);
            writeModelDebugConfig(job.getModelDebugConfig(), modelDebugConfigFile);
            String modelDebugConfig = MODEL_DEBUG_CONFIG_ARG + modelDebugConfigFile.toString();
            command.add(modelDebugConfig);
        }

        if (modelConfigFilePresent())
        {
            String modelConfigFile = new File(CONFIG_DIR, PRELERT_MODEL_CONF).toString();
            command.add(MODEL_CONFIG_ARG + modelConfigFile);
        }


        // Restoring the quantiles
        if (quantiles != null && !quantiles.getState().isEmpty())
        {
            logger.info("Restoring quantiles for job '" + job.getId() + "'");

            Path normalisersStateFilePath =
                    writeNormaliserInitState(job.getId(), quantiles.getState());

            String quantilesStateFileArg = QUANTILES_STATE_PATH_ARG + normalisersStateFilePath;
            command.add(quantilesStateFileArg);
            command.add(DELETE_STATE_FILES_ARG);
        }

        // now the actual field args
        if (job.getAnalysisConfig() != null)
        {
            // write to a temporary field config file
            File fieldConfigFile = File.createTempFile("fieldconfig", CONF_EXTENSION);
            filesToDelete.add(fieldConfigFile);
            try (OutputStreamWriter osw = new OutputStreamWriter(
                    new FileOutputStream(fieldConfigFile),
                    StandardCharsets.UTF_8))
            {
                new FieldConfigWriter(job.getAnalysisConfig(), osw, logger).write();
            }

            String fieldConfig = FIELD_CONFIG_ARG + fieldConfigFile.toString();
            command.add(fieldConfig);
        }

        // Build the process
        logger.info("Starting autodetect process with command: " +  command);
        ProcessBuilder pb = new ProcessBuilder(command);
        buildEnvironment(pb);

        return pb.start();
    }

    static List<String> buildAutoDetectCommand(JobDetails job, Logger logger)
    {
        List<String> command = new ArrayList<>();
        command.add(AUTODETECT_PATH);

        // the logging id is the job id
        String logId = LOG_ID_ARG + job.getId();
        command.add(logId);

        if (job.getAnalysisConfig() != null)
        {
            if (job.getAnalysisConfig().getBucketSpan() != null)
            {
                String bucketspan = BUCKET_SPAN_ARG + job.getAnalysisConfig().getBucketSpan();
                command.add(bucketspan);
            }
            if (job.getAnalysisConfig().getBatchSpan() != null)
            {
                String batchspan = BATCH_SPAN_ARG + job.getAnalysisConfig().getBatchSpan();
                command.add(batchspan);
            }
            if (job.getAnalysisConfig().getLatency() != null)
            {
                String latency = LATENCY_ARG + job.getAnalysisConfig().getLatency();
                command.add(latency);
            }
            if (job.getAnalysisConfig().getPeriod() != null)
            {
                String period = PERIOD_ARG + job.getAnalysisConfig().getPeriod();
                command.add(period);
            }
            if (job.getAnalysisConfig().getSummaryCountFieldName() != null)
            {
                String summaryCountField = SUMMARY_COUNT_FIELD_ARG + job.getAnalysisConfig().getSummaryCountFieldName();
                command.add(summaryCountField);
            }
        }

        // Input is always length encoded
        command.add(LENGTH_ENCODED_INPUT_ARG);

        // Limit the number of output records
        command.add(MAX_ANOMALY_RECORDS_ARG);

        String timeField = DataDescription.DEFAULT_TIME_FIELD;

        DataDescription dataDescription = job.getDataDescription();
        if (dataDescription != null)
        {
            if (dataDescription.getTimeField() != null &&
                    dataDescription.getTimeField().isEmpty() == false)
            {
                timeField = dataDescription.getTimeField();
            }
        }
        // always set the time field
        String timeFieldArg = TIME_FIELD_ARG + timeField;
        command.add(timeFieldArg);

        // Supply a URL for persisting/restoring model state unless model
        // persistence has been explicitly disabled.
        if (System.getProperty(DONT_PERSIST_MODEL_STATE) != null)
        {
            logger.info("Will not persist model state - " +
                    DONT_PERSIST_MODEL_STATE + " property was specified");
        }
        else
        {
            String persistUrlBase = PERSIST_URL_BASE_ARG +
                    "http://localhost:" + ES_HTTP_PORT + '/' + job.getId();
            command.add(persistUrlBase);

            // Persist model state every few hours even if the job isn't closed
            command.add(PERSIST_INTERVAL_ARG);
        }

        return command;
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

    /**
     * Return true if there is a file PRELERT_HOME/config/prelertmodel.conf
     * @return
     */
    private static boolean modelConfigFilePresent()
    {
        File f = new File(CONFIG_DIR, PRELERT_MODEL_CONF);

        return f.exists() && !f.isDirectory();
    }

    /**
     * The process can be initialised with both sysChangeState and
     * unusualBehaviourState if either is <code>null</code> then is
     * is not used.
     *
     * @param jobId
     * @param quantilesState Set to <code>null</code> to be ignored
     * @param bucketSpan If <code>null</code> then use the program default
     * @param logger
     * @return
     * @throws IOException
     */
    public static Process buildNormaliser(String jobId, String quantilesState,
            Integer bucketSpan, Logger logger)
    throws IOException
    {
        logger.info("PRELERT_HOME is set to " + PRELERT_HOME);

        List<String> command = ProcessCtrl.buildNormaliserCommand(jobId, bucketSpan);

        if (quantilesState != null)
        {
            Path quantilesStateFilePath = writeNormaliserInitState(jobId, quantilesState);

            String stateFileArg = QUANTILES_STATE_PATH_ARG + quantilesStateFilePath;
            command.add(stateFileArg);
            command.add(DELETE_STATE_FILES_ARG);
        }

        if (modelConfigFilePresent())
        {
            String modelConfigFile = new File(CONFIG_DIR, PRELERT_MODEL_CONF).toString();
            command.add(MODEL_CONFIG_ARG + modelConfigFile);
        }

        // Build the process
        logger.info("Starting normaliser process with command: " +  command);
        ProcessBuilder pb = new ProcessBuilder(command);
        buildEnvironment(pb);

        return pb.start();

    }

    static List<String> buildNormaliserCommand(String jobId, Integer bucketSpan)
    throws IOException
    {
        List<String> command = new ArrayList<>();
        command.add(NORMALIZE_PATH);

        if (bucketSpan != null)
        {
            String bucketSpanArg = BUCKET_SPAN_ARG + bucketSpan.toString();
            command.add(bucketSpanArg);
        }

        String logId = LOG_ID_ARG + jobId;
        command.add(logId);

        command.add(LENGTH_ENCODED_INPUT_ARG);

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
    private static Path writeNormaliserInitState(String jobId, String state)
    throws IOException
    {
        // createTempFile has a race condition where it may return the same
        // temporary file name to different threads if called simultaneously
        // from multiple threads, hence add the thread ID to avoid this
        Path stateFile = Files.createTempFile(jobId + "_quantiles_" + Thread.currentThread().getId(),
                                                QUANTILES_FILE_EXTENSION);

        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(stateFile.toString()),
                StandardCharsets.UTF_8))
        {
            osw.write(state);
        }

        return stateFile;
    }
}
