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

import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.io.IOException;
import java.nio.file.Path;

public class JobLogsTest extends ESTestCase {

    public void testOperationsNotAllowedWithInvalidPath() throws UnknownJobException, JobException, IOException {
        Path pathOutsideLogsDir = PathUtils.getDefaultFileSystem().getPath("..", "..", "..", "etc");

        Environment env = new Environment(Settings.EMPTY);

        // delete
        try {
            JobLogs jobLogs = new JobLogs(env);
            jobLogs.deleteLogs(env, pathOutsideLogsDir.toString());
            fail();
        } catch (JobException e) {
            assertEquals(ErrorCodes.INVALID_LOG_FILE_PATH, e.getErrorCode());
        }
    }

    public void testSanitizePath_GivenInvalid() {

        Environment env = new Environment(Settings.EMPTY);
        Path filePath = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "../../etc");
        try {
            Path rootDir = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert");
            new JobLogs(env).sanitizePath(filePath, rootDir);
            fail();
        } catch (JobException e) {
            assertEquals(ErrorCodes.INVALID_LOG_FILE_PATH, e.getErrorCode());
            assertEquals(
                    Messages.getMessage(Messages.LOGFILE_INVALID_PATH, filePath),
                    e.getMessage());
        }
    }

    public void testSanitizePath() throws JobException {

        Environment env = new Environment(Settings.EMPTY);
        Path filePath = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "logs", "logfile.log");
        Path rootDir = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "logs");
        Path normalized = new JobLogs(env).sanitizePath(filePath, rootDir);
        assertEquals(filePath, normalized);

        Path logDir = PathUtils.getDefaultFileSystem().getPath("./logs");
        Path filePathStartingDot = logDir.resolve("farequote").resolve("logfile.log");
        normalized = new JobLogs(env).sanitizePath(filePathStartingDot, logDir);
        assertEquals(filePathStartingDot.normalize(), normalized);

        Path filePathWithDotDot = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "logs", "../logs/logfile.log");
        rootDir = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "logs");
        normalized = new JobLogs(env).sanitizePath(filePathWithDotDot, rootDir);

        assertEquals(filePath, normalized);
    }
}
