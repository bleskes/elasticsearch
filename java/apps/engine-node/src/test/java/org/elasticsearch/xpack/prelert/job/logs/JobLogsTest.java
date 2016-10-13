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

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class JobLogsTest extends ESTestCase {

    public void testOperationsNotAllowedWithInvalidPath() throws UnknownJobException, JobException, IOException {
        Path pathOutsideLogsDir = FileSystems.getDefault().getPath("..", "..", "..", "etc");

        // delete
        try {
            JobLogs jobLogs = new JobLogs();
            jobLogs.deleteLogs(pathOutsideLogsDir.toString());
            fail();
        } catch (JobException e) {
            assertEquals(ErrorCodes.INVALID_LOG_FILE_PATH, e.getErrorCode());
        }
    }

    public void testSanitizePath_GivenInvalid() {
        Path filePath = FileSystems.getDefault().getPath("/opt", "prelert", "../../etc");
        try {
            Path rootDir = FileSystems.getDefault().getPath("/opt", "prelert");
            new JobLogs().sanitizePath(filePath, rootDir.toString());
            fail();
        } catch (JobException e) {
            assertEquals(ErrorCodes.INVALID_LOG_FILE_PATH, e.getErrorCode());
            assertEquals(
                    Messages.getMessage(Messages.LOGFILE_INVALID_PATH, filePath),
                    e.getMessage());
        }
    }

    public void testSanitizePath() throws JobException {
        Path filePath = FileSystems.getDefault().getPath("/opt", "prelert", "logs", "logfile.log");
        Path rootDir = FileSystems.getDefault().getPath("/opt", "prelert", "logs");
        Path normalized = new JobLogs().sanitizePath(filePath, rootDir.toString());
        assertEquals(filePath, normalized);

        Path filePathStartingDot = FileSystems.getDefault().getPath("./logs", "farequote", "logfile.log");
        normalized = new JobLogs().sanitizePath(filePathStartingDot, "./logs");
        assertEquals(filePathStartingDot.normalize(), normalized);

        Path filePathWithDotDot = FileSystems.getDefault().getPath("/opt", "prelert", "logs", "../logs/logfile.log");
        rootDir = FileSystems.getDefault().getPath("/opt", "prelert", "logs");
        normalized = new JobLogs().sanitizePath(filePathWithDotDot, rootDir.toString());

        assertEquals(filePath, normalized);
    }
}
