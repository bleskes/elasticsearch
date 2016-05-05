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
package com.prelert.job.logs;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.prelert.job.UnknownJobException;

public class JobLogsTest {

    @Test
    public void testFullJobLogFilePath() throws IOException, UnknownJobException
    {
        Path tempDir = Files.createTempDirectory(null);

        // create files with and without ".log" extension
        File logFileWithLogExtension = File.createTempFile("testFullJobLogFilePathWithDotLog", ".log",
                                                            tempDir.toFile());
        File logFileWithOutExtension = File.createTempFile("testFullJobLogFilePath", "",
                                                            tempDir.toFile());

        JobLogs jobLogs = new JobLogs();

        // filename with extension
        Path path = jobLogs.fullJobLogFilePath(tempDir.toFile().getAbsolutePath(), "",
                                    logFileWithLogExtension.getName());
        assertEquals(logFileWithLogExtension.getAbsolutePath(), path.toString());


        // request the file without specifying the .log extension
        int index = logFileWithLogExtension.getName().length() - ".log".length();
        path = jobLogs.fullJobLogFilePath(tempDir.toFile().getAbsolutePath(), "",
                logFileWithLogExtension.getName().substring(0, index));
        assertEquals(logFileWithLogExtension.getAbsolutePath(), path.toString());

        // get the file with no extension
        path = jobLogs.fullJobLogFilePath(tempDir.toFile().getAbsolutePath(), "",
                logFileWithOutExtension.getName());
        assertEquals(logFileWithOutExtension.getAbsolutePath(), path.toString());

        try
        {
            // ask for a file that doesn't exist
            path = jobLogs.fullJobLogFilePath(tempDir.toFile().getAbsolutePath(), "",
                    logFileWithOutExtension.getName() + ".log");
            assertEquals(logFileWithOutExtension.getAbsolutePath(), path.toString());
            fail();
        }
        catch (UnknownJobException e)
        {

        }

        // clean up
        logFileWithLogExtension.delete();
        logFileWithOutExtension.delete();
    }

}
