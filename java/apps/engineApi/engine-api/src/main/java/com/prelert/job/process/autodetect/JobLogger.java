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
 ************************************************************/

package com.prelert.job.process.autodetect;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;

import com.prelert.job.process.ProcessCtrl;

/**
 * Helper class for creating and closing the logger objects associated with
 * a particular job
 */
public final class JobLogger
{
    private static final String LOG_FILE_APPENDER_NAME = "engine_api_file_appender";

    private JobLogger()
    {
        // Do nothing
    }

    /**
     * Create the job's logger.
     *
     * @param jobId
     * @return a {@link Logger} for the given {@code jobId}
     */
    public static Logger create(String jobId)
    {
        try
        {
            Logger logger = Logger.getLogger(jobId);
            logger.setAdditivity(false);
            logger.setLevel(Level.DEBUG);

            try
            {
                Path logDir = FileSystems.getDefault().getPath(ProcessCtrl.LOG_DIR, jobId);
                Files.createDirectory(logDir);

                // If we get here then we had to create the directory.  In this
                // case we always want to create the appender because any
                // pre-existing appender will be pointing to a directory of the
                // same name that must have been previously removed.  (See bug
                // 697 in Bugzilla.)
                JobLogger.close(logger);
            }
            catch (FileAlreadyExistsException e)
            {
            }

            // Get the base logger and use its configured log level.
            // If the logger is not configured it will be created and
            // inherit the root logger's config this will only happen
            // if the default log4j.properties has changed
            Logger basePrelertLogger = Logger.getLogger("com.prelert");
            logger.setLevel(basePrelertLogger.getEffectiveLevel());

            if (logger.getAppender(LOG_FILE_APPENDER_NAME) == null)
            {
                Path logFile = FileSystems.getDefault().getPath(ProcessCtrl.LOG_DIR,
                        jobId, "engine_api.log");
                RollingFileAppender fileAppender = new RollingFileAppender(
                        new EnhancedPatternLayout(
                                "%d{yyyy-MM-dd HH:mm:ss,SSS zz} [%t] %-5p %c{3} - %m%n"),
                                logFile.toString());

                fileAppender.setName(LOG_FILE_APPENDER_NAME);
                fileAppender.setMaxFileSize("1MB");
                fileAppender.setMaxBackupIndex(9);

                // Try to copy the maximum file size and maximum index from the
                // first rolling file appender of the root logger (there will
                // be one unless the user has meddled with the default config).
                // If we fail the defaults set above will remain in force.
                @SuppressWarnings("rawtypes")
                Enumeration rootAppenders = Logger.getRootLogger().getAllAppenders();
                while (rootAppenders.hasMoreElements())
                {
                    try
                    {
                        RollingFileAppender defaultFileAppender = (RollingFileAppender)rootAppenders.nextElement();
                        fileAppender.setMaximumFileSize(defaultFileAppender.getMaximumFileSize());
                        fileAppender.setMaxBackupIndex(defaultFileAppender.getMaxBackupIndex());
                        break;
                    }
                    catch (ClassCastException e)
                    {
                        // Ignore it
                    }
                }

                logger.addAppender(fileAppender);
            }

            return logger;
        }
        catch (IOException e)
        {
            Logger logger = Logger.getLogger(ProcessAndDataDescription.class);
            logger.error(String.format("Cannot create logger for job '%s' using default",
                    jobId), e);

            return logger;
        }
    }

    /**
     * Close the log appender to release the file descriptor and
     * remove it from the logger.
     *
     * @param logger
     */
    public static void close(Logger logger)
    {
        Appender appender = logger.getAppender(LOG_FILE_APPENDER_NAME);

        if (appender != null)
        {
            appender.close();
            logger.removeAppender(LOG_FILE_APPENDER_NAME);
        }
    }
}
