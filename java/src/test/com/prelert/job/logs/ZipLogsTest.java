/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

import org.junit.Test;

import com.prelert.job.UnknownJobException;


/**
 * Tests for the {@link com.prelert.job.logs.JobLogs}  
 * Zip logs directory function.
 */
public class ZipLogsTest 
{
	public static final String LOG_CONTENTS = new String(
		"2014-02-03 16:47:24,665 GMT DEBUG [3506] CLogger.cc@385 Logger re-initialised using properties file /Source/prelert_home/config/log4cxx.properties\n"
		+ "2014-02-03 16:47:24,666 GMT DEBUG [3506] CLogger.cc@389 uname -a : Darwin DKyles-MacBook-Pro 12.5.0 Darwin Kernel Version 12.5.0: Sun Sep 29 13:33:47 PDT 2013; root:xnu-2050.48.12~1/RELEASE_X86_64 x86_64\n"
		+ "2014-02-03 16:47:24,666 GMT INFO  [3506] Main.cc@127 prelert_autodetect_api (64 bit): Version based on 4.2.1 (Build DEVELOPMENT BUILD by dkyle) Copyright (c) Prelert Ltd 2006-2014\n"
		+ "2014-02-03 16:47:24,666 GMT DEBUG [3506] CLicense.cc@1133 License expires at 2014-07-08T13:08:10+0100 : 1404821290|any|prelert|68a6bcad50afee56d91f160a485b2636f96f83bd\n"
		+ "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 responsetime\n"
		+ "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 by\n"
		+ "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 airline\n"
		+ "2014-02-03 16:47:24,671 GMT DEBUG [3506] CCsvInputParser.cc@410 Parse field names\n"
		+ "2014-02-03 16:47:24,671 GMT INFO  [3506] CCsvInputParser.cc@155 Using time in field '_time'\n"
		+ "2014-02-03 16:47:24,671 GMT DEBUG [3506] CAnomalyDetector.cc@752 Creating new detector for key individual count//count///\n"
		+ "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@753 Detector count 1\n"
		+ "2014-02-03 16:47:24,672 GMT DEBUG [3506] CFirstTimeStore.cc@67 Set first time for partition / to 1350824400\n"
		+ "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@752 Creating new detector for key individual metric/responsetime/airline///\n"
		+ "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@753 Detector count 2\n"
		+ "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3086 for person DJA\n"
		+ "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3086 for person JQA\n"
		+ "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3105 for person GAL\n"
		+ "2014-02-03 16:47:25,773 GMT INFO  [3506] CCmdSkeleton.cc@215 Handled 175909 records\n"
		+ "2014-02-03 16:47:25,775 GMT DEBUG [3506] CAnomalyDetector.cc@684 Persisted state for key 'individual count//count///'\n"
		+ "2014-02-03 16:47:25,784 GMT DEBUG [3506] CAnomalyDetector.cc@684 Persisted state for key 'individual metric/responsetime/airline///'\n"
	);
	
	@Test
	public void testZip() 
	throws IOException, UnknownJobException
	{
		Path tempDir = Files.createTempDirectory(null);
		
		final String DIR_NAME = "TestId";
		final int LOG_FILE_COUNT = 5; 		
		String [] logFileNames = new String[LOG_FILE_COUNT];
		
		for (int i=0; i<LOG_FILE_COUNT; i++)
		{
			Path logFile = Files.createTempFile(tempDir, "", ".log");
			logFileNames[i] = DIR_NAME + "/" + logFile.getFileName().toString();
			
			ByteArrayInputStream is = new ByteArrayInputStream(
					LOG_CONTENTS.getBytes(Charset.forName("UTF-8")));
			
			Files.copy(is, logFile, StandardCopyOption.REPLACE_EXISTING);
		}
		
		
		JobLogs jobLogs = new JobLogs();

		byte[] compressedLogs = jobLogs.zippedLogFiles(new File(tempDir.toString()), DIR_NAME);
		
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(compressedLogs));

		ZipEntry dir = zis.getNextEntry();
		assertTrue(dir.isDirectory());
		assertEquals(dir.getName(), DIR_NAME + "/");
		
		Arrays.sort(logFileNames);
		for (int i=0; i<LOG_FILE_COUNT; i++)
		{
			ZipEntry e = zis.getNextEntry();			
			assertFalse(e.isDirectory());
			assertTrue(Arrays.binarySearch(logFileNames, e.getName()) >= 0);
		}

		// delete temporary files
		DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir);
		for (Path f : stream)
		{
			Files.delete(f);
		}
		Files.delete(tempDir);
	}
	
}
