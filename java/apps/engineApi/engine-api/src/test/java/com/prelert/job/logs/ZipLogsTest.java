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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	    // Create the log files in a temp directory
	    Path tempDir = Files.createTempDirectory(null);

	    Map<Path, List<Path>> directoryListing = new HashMap<>();

	    Path level1Dir = Paths.get(tempDir.toString(), "level1");
	    Files.createDirectory(level1Dir);
	    List<Path> files = new ArrayList<>();
	    files.add(Files.createFile(level1Dir.resolve("log1.log")));
	    files.add(Files.createFile(level1Dir.resolve("log2.log")));
	    files.add(Files.createFile(level1Dir.resolve("log3.log")));
	    directoryListing.put(level1Dir, files);

        Path level21Dir = Paths.get(level1Dir.toString(), "level2-1");
        Files.createDirectory(level21Dir);
        files = new ArrayList<>();
        files.add(Files.createFile(level21Dir.resolve("log4.log")));
        directoryListing.put(level21Dir, files);

        Path level22Dir = Paths.get(level1Dir.toString(), "level2-2");
        Files.createDirectory(level22Dir);
        files = new ArrayList<>();
        files.add(Files.createFile(level22Dir.resolve("log5.log")));
        files.add(Files.createFile(level22Dir.resolve("log6.log")));
        directoryListing.put(level22Dir, files);

        Path level3Dir = Paths.get(level22Dir.toString(), "level3");
        Files.createDirectory(level3Dir);
        files = new ArrayList<>();
        files.add(Files.createFile(level3Dir.resolve("log7.log")));
        directoryListing.put(level3Dir, files);


        final String ZIP_ROOT = "zip_root";
        Set<String> relativeFilePaths = new HashSet<>();
        relativeFilePaths.add(ZIP_ROOT + "/level1/log1.log");
        relativeFilePaths.add(ZIP_ROOT + "/level1/log2.log");
        relativeFilePaths.add(ZIP_ROOT + "/level1/log3.log");
        relativeFilePaths.add(ZIP_ROOT + "/level1/level2-1/log4.log");
        relativeFilePaths.add(ZIP_ROOT + "/level1/level2-2/log5.log");
        relativeFilePaths.add(ZIP_ROOT + "/level1/level2-2/log6.log");
        relativeFilePaths.add(ZIP_ROOT + "/level1/level2-2/level3/log7.log");

		for (List<Path> paths : directoryListing.values())
		{
		    for (Path path : paths)
		    {
    			ByteArrayInputStream is = new ByteArrayInputStream(
    					LOG_CONTENTS.getBytes(Charset.forName("UTF-8")));

    			Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
		    }
		}


		// Zip the directory
		JobLogs jobLogs = new JobLogs();

		byte[] compressedLogs = jobLogs.zippedLogFiles(tempDir.toFile(), ZIP_ROOT);
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(compressedLogs));

		ZipEntry dir = zis.getNextEntry();
		assertTrue(dir.isDirectory());
		assertEquals(dir.getName(), ZIP_ROOT + "/");

		ZipEntry e = zis.getNextEntry();
		while (e != null)
		{
			assertFalse(e.isDirectory());
			assertTrue(relativeFilePaths.contains(e.getName()));

			e = zis.getNextEntry();
		}


		// clean up
		deleteDirectory(tempDir);
	}

	private void deleteDirectory(Path dir) throws IOException
	{
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))
        {
            for (Path p : stream)
            {
                if (p.toFile().isDirectory())
                {
                    deleteDirectory(p);
                }
                if (p.toFile().isFile())
                {
                    Files.delete(p);
                }
            }
        }

        Files.delete(dir);
	}
}
