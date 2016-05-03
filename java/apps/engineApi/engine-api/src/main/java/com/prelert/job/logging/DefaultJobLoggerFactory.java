/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
 ************************************************************/

package com.prelert.job.logging;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;

/**
 * Thread-safe class to create/close job specific loggers
 *
 * The factory is responsible for counting references to the logger
 * of a particular job and only really closes a logger when there are
 * no more references to it.
 */
public class DefaultJobLoggerFactory implements JobLoggerFactory
{
    private static final String LOG_FILE_APPENDER_NAME = "engine_api_file_appender";
    private static final String LOG_FILE_NAME = "engine_api.log";
    private static final String MAX_FILE_SIZE = "1MB";
    private static final int MAX_FILES_KEPT = 9;
    private static final String PATTERN = "%d{yyyy-MM-dd HH:mm:ss,SSS zz} [%t] %-5p %c{3} - %m%n";

    /**
     * Guarded by its own lock
     */
    private static final Map<String, Integer> REF_COUNT_MAP = new HashMap<>();

    private final String m_LogDir;

    public DefaultJobLoggerFactory(String logDir)
    {
        m_LogDir = logDir;
    }

    /**
     * Create the job's logger.
     *
     * @param jobId
     * @return a {@link Logger} for the given {@code jobId}
     */
    @Override
    public Logger newLogger(String jobId)
    {
        try
        {
            Logger logger = Logger.getLogger(jobId);
            logger.setAdditivity(false);
            logger.setLevel(Level.DEBUG);

            createLoggerDirectory(jobId, logger);

            // Get the base logger and use its configured log level.
            // If the logger is not configured it will be created and
            // inherit the root logger's config.  This will only happen
            // if the default log4j.properties has changed.
            Logger basePrelertLogger = Logger.getLogger("com.prelert");
            logger.setLevel(basePrelertLogger.getEffectiveLevel());

            if (logger.getAppender(LOG_FILE_APPENDER_NAME) == null)
            {
                logger.addAppender(createRollingFileAppender(basePrelertLogger, jobId));
            }

            synchronized (REF_COUNT_MAP)
            {
                int increasedCount = REF_COUNT_MAP.getOrDefault(jobId, 0) + 1;
                REF_COUNT_MAP.put(jobId, increasedCount);
            }

            return logger;
        }
        catch (IOException e)
        {
            Logger logger = Logger.getLogger(DefaultJobLoggerFactory.class);
            logger.error(String.format("Cannot create logger for job '%s' using default",
                    jobId), e);

            return logger;
        }
    }

    private void createLoggerDirectory(String jobId, Logger logger) throws IOException
    {
        try
        {
            Path logDir = FileSystems.getDefault().getPath(m_LogDir, jobId);
            Files.createDirectory(logDir);

            // If we get here then we had to create the directory.  In this
            // case we always want to create the appender because any
            // pre-existing appender will be pointing to a directory of the
            // same name that must have been previously removed.  (See bug
            // 697 in Bugzilla.)
            forceClose(logger);
        }
        catch (FileAlreadyExistsException e)
        {
            // Do nothing
        }
    }

    private void forceClose(Logger logger)
    {
        Appender appender = logger.getAppender(LOG_FILE_APPENDER_NAME);

        if (appender != null)
        {
            appender.close();
            logger.removeAppender(LOG_FILE_APPENDER_NAME);
        }
    }

    private RollingFileAppender createRollingFileAppender(Logger basePrelertLogger, String jobId) throws IOException
    {
        Path logFile = FileSystems.getDefault().getPath(m_LogDir, jobId, LOG_FILE_NAME);
        RollingFileAppender fileAppender = new RollingFileAppender(
                new EnhancedPatternLayout(PATTERN), logFile.toString());

        fileAppender.setName(LOG_FILE_APPENDER_NAME);
        fileAppender.setMaxFileSize(MAX_FILE_SIZE);
        fileAppender.setMaxBackupIndex(MAX_FILES_KEPT);

        // Try to copy the layout, maximum file size and maximum index from
        // the first rolling file appender of the com.prelert logger (there
        // will be one unless the user has meddled with the default config).
        // If we fail the defaults set above will remain in force.
        @SuppressWarnings("rawtypes")
        Enumeration baseAppenders = basePrelertLogger.getAllAppenders();
        while (baseAppenders.hasMoreElements())
        {
            try
            {
                RollingFileAppender defaultFileAppender = (RollingFileAppender)baseAppenders.nextElement();
                fileAppender.setLayout(defaultFileAppender.getLayout());
                fileAppender.setMaximumFileSize(defaultFileAppender.getMaximumFileSize());
                fileAppender.setMaxBackupIndex(defaultFileAppender.getMaxBackupIndex());
                break;
            }
            catch (ClassCastException e)
            {
                // Ignore it
            }
        }
        return fileAppender;
    }

    /**
     * Close the log appender to release the file descriptor and
     * remove it from the logger.
     *
     * @param logger
     */
    @Override
    public void close(String jobId, Logger logger)
    {
        synchronized (REF_COUNT_MAP)
        {
            int decreasedCount = REF_COUNT_MAP.getOrDefault(jobId, 1) - 1;
            if (decreasedCount <= 0)
            {
                forceClose(logger);
                REF_COUNT_MAP.remove(jobId);
            }
            else
            {
                REF_COUNT_MAP.put(jobId, decreasedCount);
            }
        }
    }
}
