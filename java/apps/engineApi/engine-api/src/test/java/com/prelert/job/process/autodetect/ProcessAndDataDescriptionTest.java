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

package com.prelert.job.process.autodetect;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.process.output.parsing.ResultsReader;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;

public class ProcessAndDataDescriptionTest
{

    @Test
    public void testDeleteAssociatedFiles()
    {
        Logger logger = mock(Logger.class);

        File file1 = createFile("file1", true);
        File file2 = createFile("file2", false);
        List<File> filesToDelete = Arrays.asList(file1, file2);

        ProcessAndDataDescription processAndDataDescription = createProcess(filesToDelete, logger);

        processAndDataDescription.deleteAssociatedFiles();

        verify(file1).delete();
        verify(logger).debug("Deleted file file1");
        verify(file2).delete();
        verify(logger).warn("Failed to delete file file2");
    }

    @Test
    public void testGetUptime() throws InterruptedException
    {
        Logger logger = mock(Logger.class);
        File file1 = createFile("file1", true);
        File file2 = createFile("file2", false);
        List<File> filesToDelete = Arrays.asList(file1, file2);

        ProcessAndDataDescription processAndDataDescription = createProcess(filesToDelete, logger);

        Thread.sleep(1100);

        long uptime = processAndDataDescription.upTimeSeconds();
        assertTrue(uptime > 0);
        assertTrue(uptime <= 2);
    }

    private static File createFile(String filePath, boolean deletesSuccessfully)
    {
        File file = mock(File.class);
        when(file.getPath()).thenReturn(filePath);
        when(file.toString()).thenCallRealMethod();
        when(file.delete()).thenReturn(deletesSuccessfully);
        return file;
    }

    private ProcessAndDataDescription createProcess(List<File> filesToDelete, Logger logger)
    {
        Process process = mock(Process.class);
        InputStream errorStream = mock(InputStream.class);
        when(process.getErrorStream()).thenReturn(errorStream);

        List<TransformConfig> transforms = new ArrayList<>();
        TransformConfigs transformConfigs = new TransformConfigs(transforms);

        ProcessAndDataDescription processAndDataDescription = new ProcessAndDataDescription(
                process,
                "foo",
                new DataDescription(),
                new AnalysisConfig(),
                null,
                transformConfigs,
                logger,
                mock(StatusReporter.class),
                mock(ResultsReader.class),
                filesToDelete);

        return processAndDataDescription;
    }
}
