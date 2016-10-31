/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.job.logs;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Manage job logs
 */
public class JobLogs {
    private static final Logger LOGGER = Loggers.getLogger(JobLogs.class);
    /**
     * If this system property is set the log files aren't deleted when the job
     * is.
     */
    public static final Setting<Boolean> DONT_DELETE_LOGS_SETTING = Setting.boolSetting("preserve.logs", false, Property.NodeScope);

    private boolean m_DontDelete;

    public JobLogs(Settings settings)
    {
        m_DontDelete = DONT_DELETE_LOGS_SETTING.get(settings);
    }

    /**
     * Delete all the log files and log directory associated with a job.
     *
     * @param jobId
     *            the jobId
     * @return true if success.
     * @throws JobException
     *             If the file path is invalid i.e. jobId = ../../etc
     */
    public boolean deleteLogs(Environment env, String jobId) throws JobException {
        return deleteLogs(env.logsFile().resolve(PrelertPlugin.NAME), jobId);
    }

    /**
     * Delete all the files in the directory
     *
     * <pre>
     * logDir / jobId
     * </pre>
     *
     * .
     *
     * @param logDir
     *            The base directory of the log files
     * @param jobId
     *            the jobId
     * @throws JobException
     *             If the file path is invalid i.e. jobId = ../../etc
     */
    public boolean deleteLogs(Path logDir, String jobId) throws JobException {
        if (m_DontDelete) {
            return true;
        }

        Path logPath = sanitizePath(logDir.resolve(jobId), logDir);

        LOGGER.info(String.format(Locale.ROOT, "Deleting log files %s/%s", logDir, jobId));

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(logPath)) {
            for (Path logFile : directoryStream) {
                try {
                    Files.delete(logFile);
                } catch (IOException e) {
                    String msg = "Cannot delete log file " + logDir + ". ";
                    msg += (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
                    LOGGER.warn(msg);
                }
            }
        } catch (IOException e) {
            String msg = "Cannot open the log directory " + logDir + ". ";
            msg += (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            LOGGER.warn(msg);
        }

        // delete the directory
        try {
            Files.delete(logPath);
        } catch (IOException e) {
            String msg = "Cannot delete log directory " + logDir + ". ";
            msg += (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            LOGGER.warn(msg);
            return false;
        }

        return true;
    }

    /**
     * Normalize a file path resolving .. and . directories and check the
     * resulting path is below the rootDir directory.
     *
     * Throws an exception if the path is outside the logs directory e.g.
     * logs/../lic/license resolves to lic/license and would throw
     */
    public Path sanitizePath(Path filePath, Path rootDir) throws JobException
    {
        Path normalizedPath = filePath.normalize();
        Path rootPath = rootDir.normalize();
        if (normalizedPath.startsWith(rootPath) == false)
        {
            String msg = Messages.getMessage(Messages.LOGFILE_INVALID_PATH, filePath);
            LOGGER.warn(msg);
            throw new JobException(msg, ErrorCodes.INVALID_LOG_FILE_PATH);
        }

        return normalizedPath;
    }
}
