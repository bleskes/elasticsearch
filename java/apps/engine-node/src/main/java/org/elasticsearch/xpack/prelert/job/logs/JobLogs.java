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
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.process.ProcessCtrl;
import org.elasticsearch.xpack.prelert.settings.PrelertSettings;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manage job logs
 */
public class JobLogs {
    private static final Logger LOGGER = Loggers.getLogger(JobLogs.class);

    /**
     * If this system property is set the log files aren't deleted when
     * the job is.
     */
    public static final String DONT_DELETE_LOGS_PROP = "preserve.logs";
    private boolean m_DontDelete;

    /**
     * If -D{@value #DONT_DELETE_LOGS_PROP} is set to anything
     * (not null) the log files aren't deleted
     */
    public JobLogs()
    {
        m_DontDelete = PrelertSettings.isSet(DONT_DELETE_LOGS_PROP);
    }

    /**
     * Delete all the log files and log directory associated with a job.
     *
     * @param jobId
     * @return true if success.
     * @throws JobException If the file path is invalid i.e. jobId = ../../etc
     */
    public boolean deleteLogs(String jobId) throws JobException {
        return deleteLogs(ProcessCtrl.LOG_DIR, jobId);
    }

    /**
     * Delete all the files in the directory <pre>logDir/jobId</pre>.
     *
     * @param logDir The base directory of the log files
     * @param jobId
     * @return
     * @throws JobException If the file path is invalid i.e. jobId = ../../etc
     */
    public boolean deleteLogs(String logDir, String jobId) throws JobException {
        if (m_DontDelete) {
            return true;
        }

        Path logPath = sanitizePath(FileSystems.getDefault().getPath(logDir, jobId), logDir);

        LOGGER.info(String.format("Deleting log files %s/%s", logDir, jobId));

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
     * Normalize a file path resolving .. and . directories
     * and check the resulting path is below the {@linkplain ProcessCtrl#LOG_DIR}
     * directory.
     *
     * Throws an exception if the path is outside the logs directory
     * e.g. logs/../lic/license resolves to lic/license and would throw
     *
     * @param filePath
     * @param rootDir
     * @return
     * @throws JobException
     */
    public Path sanitizePath(Path filePath, String rootDir) throws JobException
    {
        Path normalizedPath = filePath.normalize();
        Path rootPath = FileSystems.getDefault().getPath(rootDir).normalize();
        if (normalizedPath.startsWith(rootPath) == false)
        {
            String msg = Messages.getMessage(Messages.LOGFILE_INVALID_PATH, filePath);
            LOGGER.warn(msg);
            throw new JobException(msg, ErrorCodes.INVALID_LOG_FILE_PATH);
        }

        return normalizedPath;
    }
}
