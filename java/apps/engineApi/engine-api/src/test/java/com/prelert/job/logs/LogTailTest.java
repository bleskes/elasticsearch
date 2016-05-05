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

package com.prelert.job.logs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

import org.junit.Test;

import com.prelert.job.UnknownJobException;

/**
 * Tests the tail file functions in {@link com.prelert.job.logs.JobLogs}
 */
public class LogTailTest
{
	public static final String [] LOG_CONTENTS = new String [] {
			"2014-02-03 16:47:24,665 GMT DEBUG [3506] CLogger.cc@385 Logger re-initialised using properties file /Source/prelert_home/config/log4cxx.properties\n"
			, "2014-02-03 16:47:24,666 GMT DEBUG [3506] CLogger.cc@389 uname -a : Darwin DKyles-MacBook-Pro 12.5.0 Darwin Kernel Version 12.5.0: Sun Sep 29 13:33:47 PDT 2013; root:xnu-2050.48.12~1/RELEASE_X86_64 x86_64\n"
			, "2014-02-03 16:47:24,666 GMT INFO  [3506] Main.cc@127 prelert_autodetect_api (64 bit): Version based on 4.2.1 (Build DEVELOPMENT BUILD by dkyle) Copyright (c) Prelert Ltd 2006-2014\n"
			, "2014-02-03 16:47:24,666 GMT DEBUG [3506] CLicense.cc@1133 License expires at 2014-07-08T13:08:10+0100 : 1404821290|any|prelert|68a6bcad50afee56d91f160a485b2636f96f83bd\n"
			, "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 responsetime\n"
			, "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 by\n"
			, "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 airline\n"
			, "2014-02-03 16:47:24,671 GMT DEBUG [3506] CCsvInputParser.cc@410 Parse field names\n"
			, "2014-02-03 16:47:24,671 GMT INFO  [3506] CCsvInputParser.cc@155 Using time in field '_time'\n"
			, "2014-02-03 16:47:24,671 GMT DEBUG [3506] CAnomalyDetector.cc@752 Creating new detector for key individual count//count///\n"
			, "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@753 Detector count 1\n"
			, "2014-02-03 16:47:24,672 GMT DEBUG [3506] CFirstTimeStore.cc@67 Set first time for partition / to 1350824400\n"
			, "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@752 Creating new detector for key individual metric/responsetime/airline///\n"
			, "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@753 Detector count 2\n"
			, "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3086 for person DJA\n"
			, "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3086 for person JQA\n"
			, "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3105 for person GAL\n"
			, "2014-02-03 16:47:25,773 GMT INFO  [3506] CCmdSkeleton.cc@215 Handled 175909 records\n"
			, "2014-02-03 16:47:25,775 GMT DEBUG [3506] CAnomalyDetector.cc@684 Persisted state for key 'individual count//count///'\n"
			, "2014-02-03 16:47:25,784 GMT DEBUG [3506] CAnomalyDetector.cc@684 Persisted state for key 'individual metric/responsetime/airline///'\n"
		};

	@Test
	public void testTailFile()
	throws IOException, UnknownJobException
	{
		// write the log to temp file
		File tmpLogFile = File.createTempFile("tmp", ".log");
		FileWriter fw = new FileWriter(tmpLogFile);
		for (String ln : LOG_CONTENTS)
		{
			fw.append(ln);
		}
		fw.close();

		JobLogs jobLogs = new JobLogs();
		String lastTenLines = jobLogs.tail(tmpLogFile, "SomeTestJob", 10,
				JobLogs.EXPECTED_LINE_LENGTH);

		// get the last 10 lines
		String [] lines = lastTenLines.split("\n");
		assertEquals(lines.length, 10);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);

		// ask for more lines than will be returned
		String allLines = jobLogs.tail(tmpLogFile, "SomeTestJob", 100,
				JobLogs.EXPECTED_LINE_LENGTH);

		lines = allLines.split("\n");
		assertEquals(lines.length, LOG_CONTENTS.length);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);

		// use a small expected line length so it has to jump back further
		// into the file after the initial starting pos guess is wrong.
		String last12Lines = jobLogs.tail(tmpLogFile, "SomeTestJob", 12, 100);

		lines = last12Lines.split("\n");
		assertEquals(lines.length, 12);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);

		// use a small expected line length and ask for more lines than will be returned
		allLines = jobLogs.tail(tmpLogFile, "SomeTestJob", 100, 20);

		lines = allLines.split("\n");
		assertEquals(lines.length, LOG_CONTENTS.length);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);

		// Get last 10 lines with a tiny expected line length
		lastTenLines = jobLogs.tail(tmpLogFile, "SomeTestJob", 10, 20);
		lines = lastTenLines.split("\n");
		assertEquals(lines.length, 10);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);

		// Get last 5 lines with a tiny expected line length
		String lastFiveLines = jobLogs.tail(tmpLogFile, "SomeTestJob", 5, 20);
		lines = lastFiveLines.split("\n");
		assertEquals(lines.length, 5);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);


		// Get last 5 lines with an overly large expected line length
		lastFiveLines = jobLogs.tail(tmpLogFile, "SomeTestJob", 5, 10000);
		lines = lastFiveLines.split("\n");
		assertEquals(lines.length, 5);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);

		// Get all lines with an overly large expected line length
		allLines = jobLogs.tail(tmpLogFile, "SomeTestJob", 100, 10000);
		lines = allLines.split("\n");
		assertEquals(lines.length, LOG_CONTENTS.length);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);

		// clean up
		tmpLogFile.delete();
	}

	@Test
	public void testTailWithVeryLargeNumberOfLines()
	throws IOException, UnknownJobException
	{
        // write the log to temp file
        File tmpLogFile = File.createTempFile("tmp", ".log");
        FileWriter fw = new FileWriter(tmpLogFile);
        for (String ln : LOG_CONTENTS)
        {
            fw.append(ln);
        }
        fw.close();

        JobLogs jobLogs = new JobLogs();
        String allLines = jobLogs.tail(tmpLogFile, "SomeTestJob", 500000,
                JobLogs.EXPECTED_LINE_LENGTH);

        String lines [] = allLines.split("\n");
        assertEquals(lines.length, LOG_CONTENTS.length);
        verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);


        // clean up
        tmpLogFile.delete();
	}

	@Test
	public void testReadEntireFile() throws IOException
	{
		// write the log to temp file
		File tmpLogFile = File.createTempFile("tmp", ".log");
		FileWriter fw = new FileWriter(tmpLogFile);
		for (String ln : LOG_CONTENTS)
		{
			fw.append(ln);
		}
		fw.close();


		JobLogs jobLogs = new JobLogs();
		String allLines = jobLogs.readFileToString(tmpLogFile.toPath());
		String [] lines = allLines.split("\n");
		assertEquals(lines.length, LOG_CONTENTS.length);
		verifyLinesEqual(LOG_CONTENTS.length - lines.length, lines.length, lines);

		// clean up
		tmpLogFile.delete();
	}

	private void verifyLinesEqual(int offset, int count, String [] lines)
	{
		for (int i=0; i<count; i++)
		{
			assertEquals(lines[i], LOG_CONTENTS[i+offset].replace("\n", ""));
		}
	}
}
