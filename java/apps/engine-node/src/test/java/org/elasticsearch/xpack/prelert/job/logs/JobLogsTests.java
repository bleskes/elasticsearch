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
package org.elasticsearch.xpack.prelert.job.logs;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.io.IOException;
import java.nio.file.Path;

public class JobLogsTests extends ESTestCase {

    public void testOperationsNotAllowedWithInvalidPath() throws IOException {
        Path pathOutsideLogsDir = PathUtils.getDefaultFileSystem().getPath("..", "..", "..", "etc");

        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(
                settings);

        // delete
        try {
            JobLogs jobLogs = new JobLogs(settings);
            jobLogs.deleteLogs(env, pathOutsideLogsDir.toString());
            fail();
        } catch (ElasticsearchException e) {
            assertEquals(ErrorCodes.INVALID_LOG_FILE_PATH.getValueString(), e.getHeader("errorCode").get(0));
        }
    }

    public void testSanitizePath_GivenInvalid() {

        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Path filePath = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "../../etc");
        try {
            Path rootDir = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert");
            new JobLogs(settings).sanitizePath(filePath, rootDir);
            fail();
        } catch (ElasticsearchException e) {
            assertEquals(ErrorCodes.INVALID_LOG_FILE_PATH.getValueString(), e.getHeader("errorCode").get(0));
            assertEquals(
                    Messages.getMessage(Messages.LOGFILE_INVALID_PATH, filePath),
                    e.getMessage());
        }
    }

    public void testSanitizePath() {

        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Path filePath = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "logs", "logfile.log");
        Path rootDir = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "logs");
        Path normalized = new JobLogs(settings).sanitizePath(filePath, rootDir);
        assertEquals(filePath, normalized);

        Path logDir = PathUtils.getDefaultFileSystem().getPath("./logs");
        Path filePathStartingDot = logDir.resolve("farequote").resolve("logfile.log");
        normalized = new JobLogs(settings).sanitizePath(filePathStartingDot, logDir);
        assertEquals(filePathStartingDot.normalize(), normalized);

        Path filePathWithDotDot = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "logs", "../logs/logfile.log");
        rootDir = PathUtils.getDefaultFileSystem().getPath("/opt", "prelert", "logs");
        normalized = new JobLogs(settings).sanitizePath(filePathWithDotDot, rootDir);

        assertEquals(filePath, normalized);
    }
}
